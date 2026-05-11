package io.github.tuanha1305.tcvn3

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import java.lang.reflect.Field
import java.nio.charset.Charset
import java.nio.charset.spi.CharsetProvider

/**
 * Force-register TCVN3 with the running IDE by wrapping
 * `java.nio.charset.Charset.standardProvider` with a delegating
 * CharsetProvider that returns TCVN3 for the canonical name + aliases.
 *
 * Why wrap `standardProvider`:
 *   - `Charset.forName(name)` consults `standardProvider.charsetForName(name)`
 *     first. Wrapping it makes `Charset.forName("TCVN3")` resolve.
 *   - `Charset.availableCharsets()` iterates `standardProvider.charsets()` and
 *     merges the result. Wrapping it makes the IDE encoding picker - which
 *     reads from `Charset.availableCharsets()` - list "TCVN3" automatically.
 *
 * Why this is needed at all:
 *   The plugin ships META-INF/services/java.nio.charset.spi.CharsetProvider,
 *   but `Charset.providers()` scans `getSystemClassLoader()`. The plugin lives
 *   in a child classloader, so the SPI file is invisible. We work around it.
 *
 * Field-access tactic:
 *   `standardProvider` is `private static final` on JDK 25. Field.set fails
 *   for `final` static fields, so we use `sun.misc.Unsafe.putObject` against
 *   the field's static base + offset. Unsafe bypasses module access and the
 *   `final` modifier. JBR (JetBrains Runtime) leaves `sun.misc.Unsafe`
 *   accessible.
 */
@Service(Service.Level.APP)
internal class Tcvn3Bootstrap {

    init {
        val tcvn3 = Tcvn3CharsetProvider.SHARED_INSTANCE
        thisLogger().info("Tcvn3Bootstrap initializing - wrapping Charset.standardProvider")
        wrapStandardProvider(tcvn3)
        invalidateCharsetCaches() // ensure any old "not found" lookups are retried

        val probe = runCatching { Charset.forName("TCVN3").name() }
        if (probe.isSuccess) {
            val available = runCatching { "TCVN3" in Charset.availableCharsets() }.getOrDefault(false)
            thisLogger().info("Tcvn3Bootstrap done. forName=\"${probe.getOrNull()}\", listed in availableCharsets=$available")
        } else {
            thisLogger().warn("Tcvn3Bootstrap done but Charset.forName(\"TCVN3\") still fails: ${probe.exceptionOrNull()}. Use the 'Reload File with TCVN3 Encoding' action (Tools menu) to apply TCVN3 anyway.")
        }
    }

    /**
     * Replace Charset.standardProvider with a delegating wrapper that returns
     * TCVN3 for its canonical name + aliases. Idempotent: if standardProvider
     * is already our wrapper, do nothing.
     */
    private fun wrapStandardProvider(tcvn3: Charset) {
        try {
            val field = Charset::class.java.getDeclaredField("standardProvider")
            field.isAccessible = true
            val current = field.get(null) as? CharsetProvider ?: run {
                thisLogger().warn("Charset.standardProvider is not a CharsetProvider; not patching")
                return
            }
            if (current is Tcvn3DelegatingProvider) {
                thisLogger().info("Charset.standardProvider is already the TCVN3 delegate; skipping.")
                return
            }
            val wrapped = Tcvn3DelegatingProvider(current, tcvn3)
            // First try plain Field.set in case the field is not final on this JDK.
            try {
                field.set(null, wrapped)
                thisLogger().info("Wrapped Charset.standardProvider via Field.set.")
                return
            } catch (_: IllegalAccessException) {
                // Fall through to Unsafe path.
            }

            // Unsafe path - the field is final on JDK 25.
            val unsafe = getUnsafe() ?: run {
                thisLogger().warn("sun.misc.Unsafe unavailable; cannot write final field Charset.standardProvider")
                return
            }
            val base = unsafe.staticFieldBase(field)
            val offset = unsafe.staticFieldOffset(field)
            unsafe.putObject(base, offset, wrapped)
            thisLogger().info("Wrapped Charset.standardProvider via Unsafe (final static).")
        } catch (e: Throwable) {
            thisLogger().warn("Could not wrap Charset.standardProvider", e)
        }
    }

    /**
     * Clear Charset.cache1 / cache2 so a stale "not found" lookup that may
     * have populated them with a different charset for the same name does
     * not pre-empt our new provider.
     */
    private fun invalidateCharsetCaches() {
        for (name in arrayOf("cache1", "cache2")) {
            try {
                val f = Charset::class.java.getDeclaredField(name)
                f.isAccessible = true
                f.set(null, null)
            } catch (_: Throwable) { /* best effort */ }
        }
    }

    /** Reflectively obtain the singleton `sun.misc.Unsafe`. */
    private fun getUnsafe(): sun.misc.Unsafe? = try {
        val f: Field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        f.get(null) as sun.misc.Unsafe
    } catch (e: Throwable) {
        thisLogger().warn("Cannot obtain sun.misc.Unsafe", e)
        null
    }
}

/**
 * CharsetProvider that returns our TCVN3 instance for the canonical name + a
 * fixed alias set, and forwards everything else to the underlying real
 * provider. Used to wrap Charset.standardProvider.
 */
private class Tcvn3DelegatingProvider(
    private val delegate: CharsetProvider,
    private val tcvn3: Charset,
) : CharsetProvider() {

    override fun charsetForName(charsetName: String?): Charset? {
        if (charsetName == null) return null
        val normalized = charsetName.lowercase().replace(NON_ALNUM, "")
        if (normalized in MATCH) return tcvn3
        return delegate.charsetForName(charsetName)
    }

    override fun charsets(): MutableIterator<Charset> {
        val list = ArrayList<Charset>(256)
        delegate.charsets().forEachRemaining { list.add(it) }
        if (list.none { it.name() == tcvn3.name() }) list.add(tcvn3)
        return list.iterator()
    }

    companion object {
        private val NON_ALNUM = Regex("[^a-z0-9]")
        private val MATCH = setOf("tcvn3", "tcvn31", "vntcvn3", "vietnamese3")
    }
}
