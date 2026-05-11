package io.github.tuanha1305.tcvn3

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.encoding.EncodingManager
import java.nio.charset.Charset
import java.lang.reflect.Modifier

/**
 * Force-register TCVN3 with the running IDE the moment the plugin's
 * classloader is alive.
 *
 * IntelliJ's encoding picker reads from `Charset.availableCharsets()` and
 * `EncodingManager.favorites`. The plugin ships a Java SPI provider
 * (`META-INF/services/java.nio.charset.spi.CharsetProvider`), but the SPI
 * scans `ClassLoader.getSystemClassLoader()` and the file lives in the
 * plugin's child classloader, so the IDE never sees it.
 *
 * We compensate with two best-effort reflection hooks:
 *
 *   1. Find Charset's private name -> Charset cache (varies across JDK
 *      versions: `cache1`/`cache2` on OpenJDK 17-21, possibly different in
 *      JDK 25) and add Tcvn3Charset entries for the canonical name plus all
 *      aliases. After this, `Charset.forName("TCVN3")` resolves.
 *
 *   2. Find EncodingManager's favorites field/method (varies across IDE
 *      versions) and add Tcvn3Charset. This is what makes TCVN3 show up in
 *      the status-bar / picker dropdown.
 *
 * Failures are logged and the IDE keeps running - if the in-IDE picker can't
 * be patched, the user can still set the encoding via the
 * `ReloadFileAsTcvn3Action` (Tools menu / Command Palette).
 *
 * Service is registered with `preload="notAwait"` in plugin.xml. `await` is
 * reserved for core services; `notAwait` is allowed for plugins and still
 * forces eager instantiation, which is what we need to run this bootstrap
 * whether the plugin is loaded at IDE start or installed dynamically.
 */
@Service(Service.Level.APP)
internal class Tcvn3Bootstrap {

    init {
        val instance = Tcvn3CharsetProvider.SHARED_INSTANCE
        thisLogger().info("Tcvn3Bootstrap initializing - registering TCVN3 charset")
        installIntoCharsetCache(instance)
        addToEncodingManagerFavorites(instance)
        val probe = runCatching { Charset.forName("TCVN3").name() }
        if (probe.isSuccess) {
            thisLogger().info("Tcvn3Bootstrap done. Charset.forName(\"TCVN3\") -> ${probe.getOrNull()}")
        } else {
            thisLogger().warn("Tcvn3Bootstrap done but Charset.forName(\"TCVN3\") still fails: ${probe.exceptionOrNull()}. Users can still apply TCVN3 via the explicit \"Reload File with TCVN3 Encoding\" action.")
        }
    }

    /**
     * Walk every static field on java.nio.charset.Charset and patch the first
     * one that exposes a `Map<String, Charset>` (the JDK's internal name
     * cache). This avoids hard-coding `cache1`/`cache2` which change across
     * JDK versions.
     */
    private fun installIntoCharsetCache(cs: Charset) {
        var patched = false
        for (field in Charset::class.java.declaredFields) {
            if (!Modifier.isStatic(field.modifiers)) continue
            try {
                field.isAccessible = true
                val value = field.get(null)
                if (value !is MutableMap<*, *>) continue
                @Suppress("UNCHECKED_CAST")
                val cache = value as MutableMap<String, Charset>
                cache[cs.name()] = cs
                cache[cs.name().lowercase()] = cs
                for (alias in cs.aliases()) {
                    cache[alias] = cs
                    cache[alias.lowercase()] = cs
                }
                thisLogger().info("Patched Charset.${field.name} (${value.javaClass.simpleName}) with TCVN3 + ${cs.aliases().size} alias(es)")
                patched = true
            } catch (e: Throwable) {
                thisLogger().debug("Skipping Charset.${field.name}: $e")
            }
        }
        if (!patched) {
            // Last-resort enumeration so we can SEE in idea.log what fields
            // exist on this JDK. The field names change in JDK 25+.
            val fields = Charset::class.java.declaredFields.joinToString {
                "${it.name}:${it.type.simpleName}"
            }
            thisLogger().warn("Could not patch any Charset cache field. Fields on this JDK: $fields")
        }
    }

    /**
     * Try multiple known APIs to register the charset with IntelliJ's
     * encoding picker.
     *
     * On older IDEs `EncodingManagerImpl.setFavorites(Collection<Charset>)`
     * worked. On 2026.1 that method is gone. We fall back to mutating the
     * underlying favorites Collection in place if it is mutable, or to
     * setting the backing field directly via reflection.
     */
    private fun addToEncodingManagerFavorites(cs: Charset) {
        val em: EncodingManager = try {
            EncodingManager.getInstance()
        } catch (t: Throwable) {
            thisLogger().warn("EncodingManager.getInstance() failed", t); return
        }

        // (a) Already there? Nothing to do.
        val current = try { em.favorites } catch (_: Throwable) { null }
        if (current != null && current.any { it.name() == cs.name() }) {
            thisLogger().info("EncodingManager already lists TCVN3 in favorites.")
            return
        }

        // (b) Public setter, if it exists.
        val setMethod = em.javaClass.methods.firstOrNull {
            it.name == "setFavorites" && it.parameterCount == 1
        }
        if (setMethod != null) {
            try {
                val newList = (current?.toMutableList() ?: mutableListOf()).also { it.add(cs) }
                setMethod.invoke(em, newList)
                thisLogger().info("TCVN3 added via EncodingManager.setFavorites - visible in encoding picker.")
                return
            } catch (e: Throwable) {
                thisLogger().warn("setFavorites threw", e)
            }
        }

        // (c) Walk fields on EncodingManagerImpl and try to mutate any
        //     Collection<Charset>-like field directly.
        val implClass = em.javaClass
        var classCursor: Class<*>? = implClass
        while (classCursor != null && classCursor != Any::class.java) {
            for (field in classCursor.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) continue
                try {
                    field.isAccessible = true
                    val value = field.get(em) ?: continue
                    if (value is MutableCollection<*>) {
                        // Check if it's a Collection of Charset (sample first element).
                        val sample = value.firstOrNull()
                        if (sample == null || sample is Charset) {
                            @Suppress("UNCHECKED_CAST")
                            val coll = value as MutableCollection<Charset>
                            if (coll.none { it.name() == cs.name() }) {
                                coll.add(cs)
                                thisLogger().info("TCVN3 added to ${classCursor.simpleName}.${field.name} (${value.javaClass.simpleName}) - picker should list it.")
                                return
                            }
                        }
                    }
                } catch (_: Throwable) { /* skip */ }
            }
            classCursor = classCursor.superclass
        }

        thisLogger().warn("Could not find a way to add TCVN3 to EncodingManager favorites on this IDE. The 'Reload File with TCVN3 Encoding' action is the fallback path.")
    }
}
