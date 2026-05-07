package io.github.tuanha1305.tcvn3

import org.junit.Test
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Tcvn3CharsetTest {

    private val cs: Charset = Charset.forName("TCVN3")

    @Test fun resolvesByName() {
        assertEquals("TCVN3", cs.name())
        assertEquals(cs, Charset.forName("TCVN-3"))
        assertEquals(cs, Charset.forName("TCVN3-1"))
        assertEquals(cs, Charset.forName("vntcvn3"))
    }

    @Test fun referenceVectorsFromCSharpPort() {
        // Same vectors that the JS port and the C# port verify.
        val cases = listOf(
            "Xin ch\u00E0o" to "Xin ch\u00B5o",
            "Vi\u1EC7t Nam" to "Vi\u00D6t Nam",
            "B\u00E1o c\u00E1o" to "B\u00B8o c\u00B8o",
            "\u0110\u00E2y l\u00E0 v\u0103n b\u1EA3n" to
                "\u00A7\u00A9y l\u00B5 v\u00A8n b\u00B6n",
            "H\u1EE3p \u0111\u1ED3ng s\u1ED1 123" to
                "H\u00EEp \u00AE\u00E5ng s\u00E8 123",
        )
        for ((unicode, tcvn3) in cases) {
            val expected = tcvn3.toByteArray(Charsets.ISO_8859_1)
            val actual = unicode.toByteArray(cs)
            assertTrue(
                expected.contentEquals(actual),
                "encode(${quote(unicode)}) expected ${hex(expected)} but got ${hex(actual)}"
            )
            assertEquals(unicode, String(expected, cs))
        }
    }

    @Test fun roundTripAllVietnameseChars() {
        val all = ("\u00C0\u00C1\u00C2\u00C3\u00C8\u00C9\u00CA\u00CC\u00CD" +
            "\u00D2\u00D3\u00D4\u00D5\u00D9\u00DA\u00DD" +
            "\u00E0\u00E1\u00E2\u00E3\u00E8\u00E9\u00EA\u00EC\u00ED" +
            "\u00F2\u00F3\u00F4\u00F5\u00F9\u00FA\u00FD" +
            "\u0102\u0103\u0110\u0111\u0128\u0129\u0168\u0169" +
            "\u01A0\u01A1\u01AF\u01B0")
        for (ch in all) {
            val s = ch.toString()
            assertEquals(s, String(s.toByteArray(cs), cs))
        }
    }

    @Test fun nonTcvn3BytesUsePuaForRoundTripSafety() {
        // Byte 0xC0 has no TCVN3 mapping. Decoder must surface it as PUA, NOT
        // Latin-1 'A-grave' (which would re-encode as the 2-byte "Aµ" form
        // and corrupt the file).
        val raw = byteArrayOf(0xC0.toByte())
        val decoded = String(raw, cs)
        assertEquals(0xE0C0, decoded[0].code)
        val reencoded = decoded.toByteArray(cs)
        assertTrue(raw.contentEquals(reencoded))
    }

    @Test fun roundTripMixedTcvn3GbkBytes() {
        // Real-world game data file pattern: TCVN3 Vietnamese + GBK Chinese
        // file paths in the same byte stream. Round-trip must be byte-identical.
        val bytes = byteArrayOf(
            0x54, 0xAA.toByte(), 0x6E,                          // "Tên" in TCVN3
            0xC5.toByte(), 0xFB.toByte(), 0xB7.toByte(), 0xE7.toByte(), // GBK chunk
            0xBB.toByte(), 0xAA.toByte(), 0xC0.toByte(), 0xF6.toByte(), // GBK chunk
            0x09, 0x61, 0x31,
        )
        val rt = String(bytes, cs).toByteArray(cs)
        assertTrue(
            bytes.contentEquals(rt),
            "expected ${hex(bytes)} got ${hex(rt)}"
        )
    }

    @Test fun streamingDecoderHandlesChunkBoundaries() {
        // 'À' = bytes [0x41, 0xB5]. Split between calls; the decoder must
        // defer the 0x41 lead and emit 'À' once 0xB5 arrives.
        val dec = cs.newDecoder()
        val buf1 = java.nio.ByteBuffer.wrap(byteArrayOf(0x41))
        val buf2 = java.nio.ByteBuffer.wrap(byteArrayOf(0xB5.toByte()))
        val out = java.nio.CharBuffer.allocate(4)
        dec.decode(buf1, out, false)
        dec.decode(buf2, out, true)
        dec.flush(out)
        out.flip()
        assertEquals("\u00C0", out.toString())
    }

    private fun hex(b: ByteArray): String =
        b.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
    private fun quote(s: String): String =
        s.map { "\\u%04X".format(it.code) }.joinToString("", "\"", "\"")
}
