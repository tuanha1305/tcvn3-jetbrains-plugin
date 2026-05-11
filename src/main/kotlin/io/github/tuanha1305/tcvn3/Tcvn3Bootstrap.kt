package io.github.tuanha1305.tcvn3

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.encoding.EncodingManager
import java.nio.charset.Charset

/**
 * Force-register TCVN3 with the running IDE.
 *
 * The plugin ships META-INF/services/java.nio.charset.spi.CharsetProvider,
 * but Java's standard charset SPI scans `ClassLoader.getSystemClassLoader()`.
 * IntelliJ plugins live in their own classloader (a CHILD of the system one),
 * so the SPI file is invisible to `Charset.providers()` and the IDE's encoding
 * picker, which reads from `Charset.availableCharsets()`, does not list TCVN3.
 *
 * The fix is two-fold:
 *   1. Push TCVN3 into Charset's internal name cache (`cache2`) via reflection
 *      so `Charset.forName("TCVN3")` resolves anywhere in the IDE.
 *   2. Add the charset to IntelliJ's EncodingManager favorites so it surfaces
 *      in the encoding picker dropdown.
 *
 * Both operations are best-effort - they log and continue on failure so the
 * IDE still starts even if a future JDK locks down the reflection path.
 */
@Suppress("UnstableApiUsage")
internal class Tcvn3Bootstrap : ApplicationInitializedListener {

    override suspend fun execute() {
        val instance = Tcvn3CharsetProvider.SHARED_INSTANCE
        installIntoCharsetCache(instance)
        addToEncodingManagerFavorites(instance)
    }

    private fun installIntoCharsetCache(cs: Charset) {
        try {
            val field = Charset::class.java.getDeclaredField("cache2")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val cache = field.get(null) as MutableMap<String, Charset>
            cache[cs.name().lowercase()] = cs
            cache[cs.name()] = cs
            for (alias in cs.aliases()) {
                cache[alias] = cs
                cache[alias.lowercase()] = cs
            }
            thisLogger().info("TCVN3 registered in Charset.cache2 (forName works for TCVN3 + aliases)")
        } catch (t: Throwable) {
            thisLogger().warn("Could not patch Charset.cache2 - Charset.forName(\"TCVN3\") may not resolve. Falling back to manual encoding-set actions.", t)
        }
    }

    private fun addToEncodingManagerFavorites(cs: Charset) {
        try {
            val em = EncodingManager.getInstance()
            // EncodingManager.setFavorites(Collection<Charset>) is public API
            // on EncodingManagerImpl but the interface itself does not expose
            // it. Use reflection so we degrade gracefully if the API changes.
            val current = em.favorites?.toMutableList() ?: mutableListOf()
            if (current.none { it.name() == cs.name() }) {
                current.add(cs)
                val setFavorites = em.javaClass.methods.firstOrNull {
                    it.name == "setFavorites" && it.parameterCount == 1
                }
                if (setFavorites != null) {
                    setFavorites.invoke(em, current)
                    thisLogger().info("TCVN3 added to EncodingManager favorites - now visible in the encoding picker.")
                } else {
                    thisLogger().warn("EncodingManager.setFavorites not found; picker entry may be missing.")
                }
            }
        } catch (t: Throwable) {
            thisLogger().warn("Could not register TCVN3 with EncodingManager favorites.", t)
        }
    }
}
