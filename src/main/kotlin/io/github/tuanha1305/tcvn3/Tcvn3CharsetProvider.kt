package io.github.tuanha1305.tcvn3

import java.nio.charset.Charset
import java.nio.charset.spi.CharsetProvider

/**
 * Plugs TCVN3 into the JVM's Charset framework via the standard
 * java.nio.charset.spi.CharsetProvider service. Once this class is loaded
 * (via META-INF/services), `Charset.forName("TCVN3")` resolves and
 * Charset.availableCharsets() includes it. JetBrains IDEs read both, so the
 * encoding picker, file open/save, Find in Files, etc. all "just work".
 */
class Tcvn3CharsetProvider : CharsetProvider() {

    override fun charsets(): MutableIterator<Charset> =
        mutableListOf<Charset>(SHARED_INSTANCE).iterator()

    override fun charsetForName(name: String?): Charset? {
        if (name == null) return null
        val normalized = name.lowercase().replace(NON_ALNUM, "")
        return if (normalized in MATCHED_NAMES) SHARED_INSTANCE else null
    }

    companion object {
        // Reuse a single Charset instance; CharsetProvider implementations are
        // expected to return a stable identity per name.
        internal val SHARED_INSTANCE: Tcvn3Charset = Tcvn3Charset()

        // Match "tcvn3", "TCVN-3", "TCVN3-1", "vntcvn3", etc.
        private val NON_ALNUM = Regex("[^a-z0-9]")
        private val MATCHED_NAMES = setOf("tcvn3", "tcvn31", "vntcvn3", "vietnamese3")
    }
}
