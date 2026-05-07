// 134-entry Unicode <-> TCVN3 mapping, ported verbatim from
// iconv-lite/encodings/tcvn3.js (which itself was ported from
// tcvn3-encoding-c-sharp/src/Tcvn3MappingTables.cs).
package io.github.tuanha1305.tcvn3

internal object Tcvn3Mapping {

    // [unicodeChar, tcvn3ByteSequence-as-string]. Each char in the byte string
    // has codepoint < 0x100 and represents one byte of the TCVN3 encoding.
    val MAPPING: Array<Pair<String, String>> = arrayOf(
        "\u00C0" to "A\u00B5", "\u00C1" to "A\u00B8", "\u00C2" to "\u00A2",
        "\u00C3" to "A\u00B7", "\u00C8" to "E\u00CC", "\u00C9" to "E\u00D0",
        "\u00CA" to "\u00A3",  "\u00CC" to "I\u00D7", "\u00CD" to "I\u00DD",
        "\u00D2" to "O\u00DF", "\u00D3" to "O\u00E3", "\u00D4" to "\u00A4",
        "\u00D5" to "O\u00E2", "\u00D9" to "U\u00EF", "\u00DA" to "U\u00F3",
        "\u00DD" to "Y\u00FD",
        "\u00E0" to "\u00B5",  "\u00E1" to "\u00B8",  "\u00E2" to "\u00A9",
        "\u00E3" to "\u00B7",  "\u00E8" to "\u00CC",  "\u00E9" to "\u00D0",
        "\u00EA" to "\u00AA",  "\u00EC" to "\u00D7",  "\u00ED" to "\u00DD",
        "\u00F2" to "\u00DF",  "\u00F3" to "\u00E3",  "\u00F4" to "\u00AB",
        "\u00F5" to "\u00E2",  "\u00F9" to "\u00EF",  "\u00FA" to "\u00F3",
        "\u00FD" to "\u00FD",
        "\u0102" to "\u00A1",  "\u0103" to "\u00A8",
        "\u0110" to "\u00A7",  "\u0111" to "\u00AE",
        "\u0128" to "I\u00DC", "\u0129" to "\u00DC",
        "\u0168" to "U\u00F2", "\u0169" to "\u00F2",
        "\u01A0" to "\u00A5",  "\u01A1" to "\u00AC",
        "\u01AF" to "\u00A6",  "\u01B0" to "\u00AD",
        "\u1EA0" to "A\u00B9", "\u1EA1" to "\u00B9",
        "\u1EA2" to "A\u00B6", "\u1EA3" to "\u00B6",
        "\u1EA4" to "\u00A2\u00CA", "\u1EA5" to "\u00CA",
        "\u1EA6" to "\u00A2\u00C7", "\u1EA7" to "\u00C7",
        "\u1EA8" to "\u00A2\u00C8", "\u1EA9" to "\u00C8",
        "\u1EAA" to "\u00A2\u00C9", "\u1EAB" to "\u00C9",
        "\u1EAC" to "\u00A2\u00CB", "\u1EAD" to "\u00CB",
        "\u1EAE" to "\u00A1\u00BE", "\u1EAF" to "\u00BE",
        "\u1EB0" to "\u00A1\u00BB", "\u1EB1" to "\u00BB",
        "\u1EB2" to "\u00A1\u00BC", "\u1EB3" to "\u00BC",
        "\u1EB4" to "\u00A1\u00BD", "\u1EB5" to "\u00BD",
        "\u1EB6" to "\u00A1\u00C6", "\u1EB7" to "\u00C6",
        "\u1EB8" to "E\u00D1", "\u1EB9" to "\u00D1",
        "\u1EBA" to "E\u00CE", "\u1EBB" to "\u00CE",
        "\u1EBC" to "E\u00CF", "\u1EBD" to "\u00CF",
        "\u1EBE" to "\u00A3\u00D5", "\u1EBF" to "\u00D5",
        "\u1EC0" to "\u00A3\u00D2", "\u1EC1" to "\u00D2",
        "\u1EC2" to "\u00A3\u00D3", "\u1EC3" to "\u00D3",
        "\u1EC4" to "\u00A3\u00D4", "\u1EC5" to "\u00D4",
        "\u1EC6" to "\u00A3\u00D6", "\u1EC7" to "\u00D6",
        "\u1EC8" to "I\u00D8", "\u1EC9" to "\u00D8",
        "\u1ECA" to "I\u00DE", "\u1ECB" to "\u00DE",
        "\u1ECC" to "O\u00E4", "\u1ECD" to "\u00E4",
        "\u1ECE" to "O\u00E1", "\u1ECF" to "\u00E1",
        "\u1ED0" to "\u00A4\u00E8", "\u1ED1" to "\u00E8",
        "\u1ED2" to "\u00A4\u00E5", "\u1ED3" to "\u00E5",
        "\u1ED4" to "\u00A4\u00E6", "\u1ED5" to "\u00E6",
        "\u1ED6" to "\u00A4\u00E7", "\u1ED7" to "\u00E7",
        "\u1ED8" to "\u00A4\u00E9", "\u1ED9" to "\u00E9",
        "\u1EDA" to "\u00A5\u00ED", "\u1EDB" to "\u00ED",
        "\u1EDC" to "\u00A5\u00EA", "\u1EDD" to "\u00EA",
        "\u1EDE" to "\u00A5\u00EB", "\u1EDF" to "\u00EB",
        "\u1EE0" to "\u00A5\u00EC", "\u1EE1" to "\u00EC",
        "\u1EE2" to "\u00A5\u00EE", "\u1EE3" to "\u00EE",
        "\u1EE4" to "U\u00F4", "\u1EE5" to "\u00F4",
        "\u1EE6" to "U\u00F1", "\u1EE7" to "\u00F1",
        "\u1EE8" to "\u00A6\u00F8", "\u1EE9" to "\u00F8",
        "\u1EEA" to "\u00A6\u00F5", "\u1EEB" to "\u00F5",
        "\u1EEC" to "\u00A6\u00F6", "\u1EED" to "\u00F6",
        "\u1EEE" to "\u00A6\u00F7", "\u1EEF" to "\u00F7",
        "\u1EF0" to "\u00A6\u00F9", "\u1EF1" to "\u00F9",
        "\u1EF2" to "Y\u00FA", "\u1EF3" to "\u00FA",
        "\u1EF4" to "Y\u00FE", "\u1EF5" to "\u00FE",
        "\u1EF6" to "Y\u00FB", "\u1EF7" to "\u00FB",
        "\u1EF8" to "Y\u00FC", "\u1EF9" to "\u00FC",
    )

    // Encode lookup, indexed by Unicode codepoint (0..0xFFFF).
    // Packed Int (32-bit) layout per slot (0 = unmapped):
    //   bits  0..7  : byte 0
    //   bits  8..15 : byte 1 (0 when length == 1)
    //   bits 16..17 : length (1 or 2)
    val encodeTable: IntArray
    // Byte -> Unicode codepoint. Default: ASCII identity, otherwise PUA U+E0XX
    // for round-trip safety. TCVN3-mapped bytes overlay this default.
    val singleByteDecode: IntArray
    // (lead<<8 | second) -> Unicode codepoint, 0 if unmapped.
    val twoByteDecode: IntArray
    // 0 / 1: is this byte ever a TCVN3 lead byte for a 2-byte sequence?
    val isLeadByte: BooleanArray

    init {
        encodeTable = IntArray(0x10000)
        singleByteDecode = IntArray(256)
        twoByteDecode = IntArray(256 * 256)
        isLeadByte = BooleanArray(256)

        for (i in 0..255) {
            singleByteDecode[i] = if (i < 0x80) i else (0xE000 + i)
        }

        for ((unicodeStr, seq) in MAPPING) {
            val cp = unicodeStr[0].code
            val b0 = seq[0].code
            if (seq.length == 1) {
                encodeTable[cp] = (1 shl 16) or b0
                singleByteDecode[b0] = cp
            } else {
                val b1 = seq[1].code
                encodeTable[cp] = (2 shl 16) or (b1 shl 8) or b0
                twoByteDecode[(b0 shl 8) or b1] = cp
                isLeadByte[b0] = true
            }
        }
        // Reverse PUA (U+E080..U+E0FF -> byte 0x80..0xFF) for lossless
        // round-trip on bytes that are not TCVN3-mapped.
        for (b in 0x80..0xFF) {
            if (encodeTable[0xE000 + b] == 0) {
                encodeTable[0xE000 + b] = (1 shl 16) or b
            }
        }
    }
}
