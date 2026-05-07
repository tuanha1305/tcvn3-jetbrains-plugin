package io.github.tuanha1305.tcvn3.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ide.CopyPasteManager
import io.github.tuanha1305.tcvn3.Tcvn3Charset
import java.awt.datatransfer.DataFlavor

/**
 * Paste the clipboard contents interpreted as TCVN3 bytes.
 *
 * Mirror of CopyAsTcvn3BytesAction: when the clipboard came from a tool that
 * reads TCVN3 files as ANSI (Notepad/Notepad++), each character on the
 * clipboard is a Latin-1 codepoint that originally was a single TCVN3 byte.
 * This action reads the clipboard as a Latin-1 byte sequence and decodes it
 * with the TCVN3 charset before inserting.
 */
class PasteFromTcvn3BytesAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val contents = CopyPasteManager.getInstance().contents ?: return
        if (!contents.isDataFlavorSupported(DataFlavor.stringFlavor)) return
        val asLatin1 = contents.getTransferData(DataFlavor.stringFlavor) as? String ?: return

        // Each char on the clipboard is interpreted as one TCVN3 byte.
        // Strip any chars > 0xFF (paranoia) by mapping them to '?' beforehand.
        val bytes = ByteArray(asLatin1.length) { i ->
            val cp = asLatin1[i].code
            if (cp <= 0xFF) cp.toByte() else 0x3F
        }
        val decoded = String(bytes, Tcvn3Charset())

        val document = editor.document
        val sel = editor.selectionModel
        val start = if (sel.hasSelection()) sel.selectionStart else editor.caretModel.offset
        val end = if (sel.hasSelection()) sel.selectionEnd else start

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, decoded)
            editor.caretModel.moveToOffset(start + decoded.length)
            sel.removeSelection()
        }
    }
}
