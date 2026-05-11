package io.github.tuanha1305.tcvn3

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.encoding.EncodingManager
import java.nio.charset.Charset

/**
 * Force-register TCVN3 with the running IDE the moment the plugin's
 * classloader is alive.
 *
 * Why a Service with `preload="await"` (set in plugin.xml) instead of
 * ApplicationInitializedListener:
 *   - ApplicationInitializedListener fires exactly ONCE at IDE startup. If a
 *     user dynamically installs / updates the plugin without restarting the
 *     IDE (which IntelliJ allows), that event already fired and the listener
 *     never runs - which is what we hit on v1.0.1.
 *   - An application service with preload is instantiated by the platform as
 *     soon as the plugin is loaded, both at startup AND on dynamic load.
 *
 * Why the bootstrap is needed at all:
 *   The plugin ships META-INF/services/java.nio.charset.spi.CharsetProvider,
 *   but Charset.providers() scans `ClassLoader.getSystemClassLoader()` and
 *   our SPI file is in the plugin classloader (a child of system) - so
 *   Charset.availableCharsets() does NOT include TCVN3, and the IDE encoding
 *   picker does not list it.
 *
 *   We compensate by:
 *     1. Pushing Tcvn3Charset into Charset's private cache so Charset.forName
 *        resolves it anywhere in the IDE.
 *     2. Adding the charset to EncodingManager.favorites so it appears in the
 *        IDE's encoding picker dropdown.
 *
 *   Both calls are best-effort and degrade gracefully.
 */
@Service(Service.Level.APP)
internal class Tcvn3Bootstrap {

    init {
        val instance = Tcvn3CharsetProvider.SHARED_INSTANCE
        thisLogger().info("Tcvn3Bootstrap initializing - registering TCVN3 charset")
        installIntoCharsetCache(instance)
        addToEncodingManagerFavorites(instance)
        thisLogger().info("Tcvn3Bootstrap done. Charset.forName(\"TCVN3\") -> " +
            runCatching { Charset.forName("TCVN3").name() }.getOrElse { "FAILED: $it" })
    }

    private fun installIntoCharsetCache(cs: Charset) {
        // Modern OpenJDK (>= 9) uses two private fields on Charset:
        //   - cache1: Object[2] = [String name, Charset cs] for a 1-entry hot cache
        //   - cache2: Map<String, Charset> for the rest
        // Older field names may exist; try a few defensively.
        val candidateFields = listOf("cache2", "cache1")
        var patched = false
        for (fieldName in candidateFields) {
            try {
                val field = Charset::class.java.getDeclaredField(fieldName)
                field.isAccessible = true
                val value = field.get(null)
                if (value is MutableMap<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    val cache = value as MutableMap<String, Charset>
                    cache[cs.name()] = cs
                    cache[cs.name().lowercase()] = cs
                    for (alias in cs.aliases()) {
                        cache[alias] = cs
                        cache[alias.lowercase()] = cs
                    }
                    thisLogger().info("Patched Charset.$fieldName with TCVN3 + ${cs.aliases().size} alias(es)")
                    patched = true
                }
            } catch (e: NoSuchFieldException) {
                // OK - field name varies by JDK; try the next.
            } catch (e: Throwable) {
                thisLogger().warn("Could not patch Charset.$fieldName", e)
            }
        }
        if (!patched) {
            thisLogger().warn("Could not patch any Charset cache field; Charset.forName(\"TCVN3\") may not resolve.")
        }
    }

    private fun addToEncodingManagerFavorites(cs: Charset) {
        try {
            val em = EncodingManager.getInstance()
            val current = em.favorites?.toMutableList() ?: mutableListOf()
            if (current.any { it.name() == cs.name() }) {
                thisLogger().info("EncodingManager already lists TCVN3 in favorites; nothing to do.")
                return
            }
            current.add(cs)
            val setFavorites = em.javaClass.methods.firstOrNull {
                it.name == "setFavorites" && it.parameterCount == 1
            }
            if (setFavorites != null) {
                setFavorites.invoke(em, current)
                thisLogger().info("TCVN3 added to EncodingManager favorites - visible in encoding picker.")
            } else {
                thisLogger().warn("EncodingManager.setFavorites not found on ${em.javaClass.name}; picker entry may be missing.")
            }
        } catch (t: Throwable) {
            thisLogger().warn("Could not register TCVN3 with EncodingManager favorites.", t)
        }
    }
}
