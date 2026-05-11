package io.github.tuanha1305.tcvn3

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.encoding.EncodingProjectManager

/**
 * Fallback path for users on IDE / JDK versions where the bootstrap could not
 * patch the encoding picker. Runs `EncodingProjectManager.setEncoding(...)`
 * directly, then triggers a reload from disk. After this action, the IDE
 * persists TCVN3 as the file's encoding in `.idea/encodings.xml`, so future
 * opens use it automatically.
 *
 * Available under Tools menu and Find Action / Command Palette as
 * "Reload File with TCVN3 Encoding".
 */
class ReloadFileAsTcvn3Action : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && !file.isDirectory && e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val cs = Tcvn3CharsetProvider.SHARED_INSTANCE

        // Save any pending edits before swapping the encoding.
        val fdm = FileDocumentManager.getInstance()
        fdm.getDocument(file)?.let { fdm.saveDocument(it) }

        // Persist the chosen charset for this file. EncodingProjectManager
        // records this in .idea/encodings.xml and signals listeners to
        // re-read the file with the new charset on next access.
        EncodingProjectManager.getInstance(project).setEncoding(file, cs)

        // Force the in-memory Document to re-load from disk using the
        // new encoding right away (otherwise the editor keeps the old
        // text until the next file reopen).
        val doc = fdm.getCachedDocument(file)
        if (doc != null) {
            ApplicationManager.getApplication().invokeLater {
                ApplicationManager.getApplication().runWriteAction {
                    fdm.reloadFromDisk(doc)
                }
            }
        }
    }
}
