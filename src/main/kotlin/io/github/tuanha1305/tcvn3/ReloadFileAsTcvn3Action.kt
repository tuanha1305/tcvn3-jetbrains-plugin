package io.github.tuanha1305.tcvn3

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.encoding.EncodingProjectManager
import com.intellij.openapi.vfs.encoding.EncodingUtil

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

        // Persist the chosen charset for this file.
        EncodingProjectManager.getInstance(project).setEncoding(file, cs)

        // Reload bytes through the new charset, preserving the editor state.
        EncodingUtil.reloadIn(file, cs, project)
    }
}
