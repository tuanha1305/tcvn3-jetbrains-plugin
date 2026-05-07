package io.github.tuanha1305.tcvn3

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import java.nio.charset.CoderResult

/**
 * Vietnamese TCVN3 (a.k.a. ABC) charset for the JVM.
 *
 * Single-byte on the wire, but Unicode -> TCVN3 expands many precomposed
 * Vietnamese letters to 2 bytes (base + tone-mark). Decoding therefore uses a
 * greedy longest-match over 1- and 2-byte sequences; the encoder is a
 * straight per-codepoint lookup.
 *
 * Round-trip safety: any non-TCVN3 byte (e.g. GBK chunks in mixed-encoding
 * game data files) is decoded into the Unicode Private Use Area
 * (U+E080..U+E0FF) so it survives encode -> decode byte-identical. See
 * Tcvn3Mapping for the full table.
 */
class Tcvn3Charset internal constructor() :
    Charset(CANONICAL_NAME, ALIASES) {

    override fun contains(cs: Charset?): Boolean = cs != null && cs.name() == CANONICAL_NAME
    override fun newDecoder(): CharsetDecoder = Tcvn3Decoder(this)
    override fun newEncoder(): CharsetEncoder = Tcvn3Encoder(this)

    companion object {
        const val CANONICAL_NAME = "TCVN3"
        // Aliases reachable via Charset.forName(). JDK matches case-insensitively.
        // Keep ASCII identifiers only; punctuation in alias is allowed by JDK.
        val ALIASES: Array<String> = arrayOf("TCVN-3", "TCVN3-1", "vntcvn3")
    }
}

private class Tcvn3Decoder(cs: Charset) : CharsetDecoder(cs, 1.0f, 1.0f) {

    private var pendingLead: Int = -1

    override fun decodeLoop(`in`: ByteBuffer, out: CharBuffer): CoderResult {
        val single = Tcvn3Mapping.singleByteDecode
        val pair = Tcvn3Mapping.twoByteDecode
        val lead = Tcvn3Mapping.isLeadByte

        // Flush a deferred lead byte from a previous chunk.
        if (pendingLead != -1) {
            if (!`in`.hasRemaining()) return CoderResult.UNDERFLOW
            val next = `in`.get().toInt() and 0xFF
            val combined = pair[(pendingLead shl 8) or next]
            if (!out.hasRemaining()) {
                // Push the byte back so we re-process it when output has space.
                `in`.position(`in`.position() - 1)
                return CoderResult.OVERFLOW
            }
            if (combined != 0) {
                out.put(combined.toChar())
            } else {
                out.put(single[pendingLead].toChar())
                // The 'next' byte still needs processing; rewind.
                `in`.position(`in`.position() - 1)
            }
            pendingLead = -1
        }

        while (`in`.hasRemaining()) {
            val b = `in`.get().toInt() and 0xFF
            if (lead[b]) {
                if (!`in`.hasRemaining()) {
                    pendingLead = b
                    return CoderResult.UNDERFLOW
                }
                val next = `in`.get().toInt() and 0xFF
                val two = pair[(b shl 8) or next]
                if (two != 0) {
                    if (!out.hasRemaining()) {
                        `in`.position(`in`.position() - 2)
                        return CoderResult.OVERFLOW
                    }
                    out.put(two.toChar())
                } else {
                    if (!out.hasRemaining()) {
                        `in`.position(`in`.position() - 2)
                        return CoderResult.OVERFLOW
                    }
                    out.put(single[b].toChar())
                    // Rewind 'next' to be processed in the next iteration.
                    `in`.position(`in`.position() - 1)
                }
            } else {
                if (!out.hasRemaining()) {
                    `in`.position(`in`.position() - 1)
                    return CoderResult.OVERFLOW
                }
                out.put(single[b].toChar())
            }
        }
        return CoderResult.UNDERFLOW
    }

    override fun implFlush(out: CharBuffer): CoderResult {
        if (pendingLead != -1) {
            if (!out.hasRemaining()) return CoderResult.OVERFLOW
            out.put(Tcvn3Mapping.singleByteDecode[pendingLead].toChar())
            pendingLead = -1
        }
        return CoderResult.UNDERFLOW
    }

    override fun implReset() {
        pendingLead = -1
    }
}

private class Tcvn3Encoder(cs: Charset) : CharsetEncoder(cs, 1.0f, 2.0f) {

    override fun encodeLoop(`in`: CharBuffer, out: ByteBuffer): CoderResult {
        val table = Tcvn3Mapping.encodeTable
        while (`in`.hasRemaining()) {
            val cp = `in`.get().code
            val packed = table[cp]
            if (packed != 0) {
                val len = packed ushr 16
                if (out.remaining() < len) {
                    `in`.position(`in`.position() - 1)
                    return CoderResult.OVERFLOW
                }
                out.put((packed and 0xFF).toByte())
                if (len == 2) out.put(((packed ushr 8) and 0xFF).toByte())
            } else if (cp < 0x80) {
                if (!out.hasRemaining()) {
                    `in`.position(`in`.position() - 1)
                    return CoderResult.OVERFLOW
                }
                out.put(cp.toByte())
            } else if (cp < 0x100) {
                // Latin-1 best-effort for chars not in TCVN3 mapping.
                if (!out.hasRemaining()) {
                    `in`.position(`in`.position() - 1)
                    return CoderResult.OVERFLOW
                }
                out.put(cp.toByte())
            } else {
                // Unmappable -> let CharsetEncoder report it via the default
                // unmappable-character action (REPORT/REPLACE handled upstream).
                `in`.position(`in`.position() - 1)
                return CoderResult.unmappableForLength(1)
            }
        }
        return CoderResult.UNDERFLOW
    }
}
