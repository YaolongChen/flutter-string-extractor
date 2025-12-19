package person.cyl.flutterstringextractor

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression

/**
 * An Action that extracts a selected string or a string literal at the cursor to an ARB file.
 * This action can be triggered from the "Edit" menu or via a keyboard shortcut.
 */
class ExtractToArbAction : AnAction() {

    /**
     * A data class to hold information about the selected string.
     * @property text The content of the string, with quotes removed.
     * @property startOffset The starting offset of the selection in the document.
     * @property endOffset The ending offset of the selection in the document.
     */
    private data class SelectedString(val text: String, val startOffset: Int, val endOffset: Int)

    /**
     * Executes the main logic of the action.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        // 1. Get the selected string from the editor
        val selectedString = getSelectedString(editor, psiFile)
        if (selectedString == null) {
            Messages.showInfoMessage(
                MyMessageBundle.message("dialog.message.no.string.found"), "Extract Failed"
            )
            return
        }

        // 2. Format the string content for ARB
        val conversionResult = ArbFormatter.convertDartToArbFormat(selectedString.text)

        // 3. Show a dialog to get the key name from the user
        val keyName = showKeyInputDialog(project, conversionResult)
        if (keyName.isNullOrBlank()) return

        // 4. Find target ARB files
        val targetFiles = ProjectConfigManager.findTargetArbFiles(psiFile)
        if (targetFiles.isEmpty()) {
            showConfigurationError()
            return
        }

        // 5. Perform file writing and code replacement
        WriteCommandAction.runWriteCommandAction(project) {
            val successCount = ArbFileWriter.writeToArbFiles(targetFiles, keyName, conversionResult.arbValue)

            if (successCount > 0) {
                replaceStringInEditor(editor, selectedString, keyName, psiFile)
            } else {
                Messages.showErrorDialog(
                    MyMessageBundle.message("dialog.message.failed.to.write.files"),
                    MyMessageBundle.message("dialog.title.error")
                )
            }
        }
    }

    /**
     * Gets the selected text from the editor. If no text is selected,
     * it tries to find the string literal at the cursor.
     *
     * @return A [SelectedString] object, or null if no string is found.
     */
    private fun getSelectedString(editor: Editor, psiFile: PsiFile): SelectedString? {
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText

        // Case 1: User has manually selected text
        if (!selectedText.isNullOrEmpty()) {
            return SelectedString(selectedText, selectionModel.selectionStart, selectionModel.selectionEnd)
        }

        // Case 2: No selection, try to find string literal at the cursor
        val caretOffset = editor.caretModel.offset
        val elementAtCaret = psiFile.findElementAt(caretOffset)
        val stringElement = PsiTreeUtil.getParentOfType(elementAtCaret, DartStringLiteralExpression::class.java)

        if (stringElement != null) {
            val rawText = stringElement.text
            val content = ArbFormatter.stripQuotes(rawText)
            val range = stringElement.textRange
            // Give user visual feedback by selecting the string
            selectionModel.setSelection(range.startOffset, range.endOffset)
            return SelectedString(content, range.startOffset, range.endOffset)
        }

        return null
    }

    /**
     * Shows a dialog for the user to input a key for the new ARB entry.
     */
    private fun showKeyInputDialog(project: Project, conversionResult: ArbFormatter.ArbConversionResult): String? {
        return Messages.showInputDialog(
            project,
            MyMessageBundle.message(
                "dialog.message.enter.key.for.value", conversionResult.arbValue
            ),
            MyMessageBundle.message("dialog.title.extract.to.arb"),
            Messages.getQuestionIcon(),
            conversionResult.suggestedKey,
            null
        )
    }

    /**
     * Replaces the original string in the editor with the localization code.
     */
    private fun replaceStringInEditor(
        editor: Editor, selectedString: SelectedString, keyName: String, psiFile: PsiFile
    ) {
        val className = ProjectConfigManager.getLocalizationsClassName(psiFile)
        val newCode = "$className.of(context).$keyName"

        editor.document.replaceString(
            selectedString.startOffset, selectedString.endOffset, newCode
        )
        editor.selectionModel.removeSelection()
    }

    /**
     * Shows an error message when no ARB files are found.
     */
    private fun showConfigurationError() {
        Messages.showOkCancelDialog(
            MyMessageBundle.message("dialog.message.no.arb.files.found"),
            MyMessageBundle.message("dialog.title.configuration.error"),
            MyMessageBundle.message("dialog.ok"),
            MyMessageBundle.message("dialog.cancel"),
            Messages.getErrorIcon()
        )
    }
}