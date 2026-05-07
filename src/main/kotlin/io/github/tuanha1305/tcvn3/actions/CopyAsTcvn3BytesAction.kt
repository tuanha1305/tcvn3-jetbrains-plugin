package io.github.tuanha1305.tcvn3.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import io.github.tuanha1305.tcvn3.Tcvn3Charset
import java.awt.datatransfer.StringSelection

/**
 * Copy the editor's selection as TCVN3 bytes presented as a Latin-1 string.
 *
 * Why: tools that show TCVN3 via .VnTime + ANSI (Notepad, Notepad++) expect
 * raw byte values at codepoints 0x00..0xFF, not real Unicode characters.
 * Pasting the IDE's Unicode selection there would store the wrong bytes after
 * those tools' ANSI conversion. This action puts a "byte-form" string on the
 * clipboard so paste -> ANSI save reproduces the original TCVN3 bytes.
 *
 * Example: selection "Tên" (U+0054 U+00EA U+006E)
 *   -> TCVN3 bytes 0x54 0xAA 0x6E
 *   -> clipboard string with chars at codepoints U+0054 U+00AA U+006E
 *   -> paste in Notepad++ (ANSI) -> document bytes 0x54 0xAA 0x6E
 */
class CopyAsTcvn3BytesAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            editor != null && editor.selectionModel.hasSelection()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selected = editor.selectionModel.selectedText ?: return
        val bytes = selected.toByteArray(Tcvn3Charset())
        // Wrap each byte as a Latin-1 codepoint so the clipboard transports
        // it cleanly through the OS Unicode clipboard.
        val asLatin1 = String(bytes, Charsets.ISO_8859_1)
        CopyPasteManager.getInstance().setContents(StringSelection(asLatin1))
    }
}
