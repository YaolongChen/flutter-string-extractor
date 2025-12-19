package person.cyl.flutterstringextractor

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression

/**
 * An Intention Action that allows extracting a Dart string literal into an ARB file.
 * This action appears when the cursor is inside a string literal.
 */
class ExtractStringIntention : PsiElementBaseIntentionAction() {

    /**
     * The text displayed in the intention list.
     */
    override fun getText(): String = MyMessageBundle.message("intention.extract.string.to.arb.text")

    /**
     * The family name for this intention, used for grouping in settings.
     */
    override fun getFamilyName(): String = MyMessageBundle.message("intention.family.name")

    /**
     * Determines if the intention is available at the current cursor position.
     * It's available only if the element is inside a Dart string literal.
     */
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return PsiTreeUtil.getParentOfType(element, DartStringLiteralExpression::class.java) != null
    }

    /**
     * Executes the main logic of the intention action.
     */
    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (editor == null) return

        // 1. Get the string literal element at the cursor
        val stringElement = PsiTreeUtil.getParentOfType(element, DartStringLiteralExpression::class.java) ?: return

        // 2. Format the string content for ARB
        val content = ArbFormatter.stripQuotes(stringElement.text)
        val conversionResult = ArbFormatter.convertDartToArbFormat(content)

        // 3. Show a dialog to get the key name from the user
        val keyName = showKeyInputDialog(project, conversionResult)
        if (keyName.isNullOrBlank()) return

        // 4. Find target ARB files based on project configuration
        val psiFile = element.containingFile
        val targetFiles = ProjectConfigManager.findTargetArbFiles(psiFile)
        if (targetFiles.isEmpty()) {
            showConfigurationError()
            return
        }

        // 5. Perform file writing and code replacement in a single write-action
        WriteCommandAction.runWriteCommandAction(project) {
            val successCount = ArbFileWriter.writeToArbFiles(targetFiles, keyName, conversionResult.arbValue)

            if (successCount > 0) {
                replaceStringInEditor(editor, stringElement, keyName)
            } else {
                Messages.showErrorDialog("Failed to write to ARB files.", "Error")
            }
        }
    }

    /**
     * Shows a dialog to the user to input a key for the new ARB entry.
     *
     * @return The key name entered by the user, or null if canceled.
     */
    private fun showKeyInputDialog(project: Project, conversionResult: ArbFormatter.ArbConversionResult): String? {
        return Messages.showInputDialog(
            project,
            MyMessageBundle.message("dialog.message.enter.key.for.value", conversionResult.suggestedKey),
            MyMessageBundle.message("dialog.title.extract.to.arb"),
            Messages.getQuestionIcon(),
            conversionResult.suggestedKey,
            null
        )
    }

    /**
     * Replaces the original string literal in the editor with the localization code.
     */
    private fun replaceStringInEditor(editor: Editor, stringElement: DartStringLiteralExpression, keyName: String) {
        val psiFile = stringElement.containingFile
        val className = ProjectConfigManager.getLocalizationsClassName(psiFile)
        val newCode = "$className.of(context).$keyName"

        editor.document.replaceString(
            stringElement.textRange.startOffset, stringElement.textRange.endOffset, newCode
        )
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

    /**
     * This action performs its own write action, so it should not be started in one.
     */
    override fun startInWriteAction(): Boolean = false
}