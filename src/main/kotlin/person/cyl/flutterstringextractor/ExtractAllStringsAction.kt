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
import com.jetbrains.lang.dart.psi.DartImportStatement
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import java.io.File

class ExtractAllStringsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        // 1. Find all string literals in the file
        val stringLiterals = findAllStringLiterals(psiFile)
        if (stringLiterals.isEmpty()) {
            Messages.showInfoMessage(
                MyMessageBundle.message("message.noStringLiteralsFoundInFile"),
                MyMessageBundle.message("title.ExtractAllStrings")
            )
            return
        }

        // 2. Find target ARB files
        val targetFiles = ProjectConfigManager.findTargetArbFiles(psiFile)
        if (targetFiles.isEmpty()) {
            Messages.showErrorDialog(
                MyMessageBundle.message("message.noArbFilesFound"),
                MyMessageBundle.message("dialog.title.error")
            )
            return
        }

        // 3. Prepare data for the dialog
        val data = prepareData(stringLiterals, targetFiles, psiFile)

        // 4. Show dialog to review and edit keys
        // Pass a callback to handle the OK action since the dialog is now non-modal
        val dialog = ExtractAllStringsDialog(project, data) { resultData ->
            // 5. Perform batch write and replacement
            WriteCommandAction.runWriteCommandAction(project) {
                val entriesToWrite = resultData.associate { it.key to it.value }
                val successCount = ArbFileWriter.writeBatchToArbFiles(targetFiles, entriesToWrite)

                if (successCount > 0) {
                    replaceStringsInEditor(editor, resultData, psiFile)
                } else {
                    Messages.showErrorDialog(
                        MyMessageBundle.message("error.writeArbFilesFailed"),
                        MyMessageBundle.message("dialog.title.error")
                    )
                }
            }
        }
        dialog.show()
    }

    private fun findAllStringLiterals(psiFile: PsiFile): List<DartStringLiteralExpression> {
        val literals = mutableListOf<DartStringLiteralExpression>()
        PsiTreeUtil.processElements(psiFile) { element ->
            if (element is DartStringLiteralExpression) {
                // Check if the string literal is inside an import statement
                if (PsiTreeUtil.getParentOfType(element, DartImportStatement::class.java) == null) {
                    literals.add(element)
                }
            }
            true
        }
        return literals
    }

    private fun prepareData(
        literals: List<DartStringLiteralExpression>,
        targetFiles: List<File>,
        psiFile: PsiFile
    ): List<ExtractAllStringsTableRowData> {
        val entries = mutableListOf<ExtractAllStringsTableRowData>()

        // Determine which files to use for key lookup
        val lookupFile = ProjectConfigManager.getLookupArbFile(psiFile)
        // Only search based on config or fallback to the first file, do not search all files.
        val lookupFiles = if (lookupFile != null) {
            listOf(lookupFile)
        } else {
            targetFiles.take(1)
        }

        for (literal in literals) {
            val rawText = literal.text
            val content = ArbFormatter.stripQuotes(rawText)

            // Skip empty strings
            if (content.isEmpty()) continue

            val conversionResult = ArbFormatter.convertDartToArbFormat(content)
            val arbValue = conversionResult.arbValue

            // Check if value already exists in the lookup ARB files
            // If not found, default to empty string as requested
            val key = ArbFileWriter.findKeyByValue(lookupFiles, arbValue) ?: ""

            entries.add(ExtractAllStringsTableRowData(rawText, key, arbValue, literal))
        }

        return entries
    }

    private fun replaceStringsInEditor(
        editor: Editor,
        data: List<ExtractAllStringsTableRowData>,
        psiFile: PsiFile
    ) {
        val className = ProjectConfigManager.getLocalizationsClassName(psiFile)

        // We need to replace from bottom to top to avoid offset issues
        // Filter out invalid elements (in case the file was edited while dialog was open)
        val validData = data.filter { it.element.isValid }

        val sortedData = validData.sortedByDescending { it.element.textRange.startOffset }

        for (entry in sortedData) {
            val newCode = "$className.of(context).${entry.key}"
            editor.document.replaceString(
                entry.element.textRange.startOffset,
                entry.element.textRange.endOffset,
                newCode
            )
        }
    }
}