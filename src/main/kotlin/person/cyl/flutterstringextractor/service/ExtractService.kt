package person.cyl.flutterstringextractor.service

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.jetbrains.lang.dart.psi.*

object ExtractService {
    fun extractDartString(psiFile: PsiFile): List<String> {
        val literals = mutableListOf<String>()
        PsiTreeUtil.processElements(psiFile) { element ->
            if (element is DartStringLiteralExpression) {
                if (PsiTreeUtil.getParentOfType(
                        element, DartImportStatement::class.java
                    ) == null && PsiTreeUtil.getParentOfType(
                        element, DartArrayAccessExpression::class.java
                    ) == null
                ) {
                    val childrenSize = element.children.size
                    if (childrenSize > 2) {
                        val stringBuffer: StringBuffer = StringBuffer("")
                        for ((index, child) in element.children.withIndex()) {
                            if ((index == 0 || index == childrenSize - 1) && child.elementType.toString()
                                    .contains("OPEN_QUOTE")
                            ) {
                                continue
                            }
                            if (child is DartShortTemplateEntry) {
                                child.children.asList()
                                    .firstOrNull { !it.elementType.toString().contains("SHORT_TEMPLATE_ENTRY_START") }
                                    ?.let {
                                        stringBuffer.append("{${it.text}}")
                                    }
                            }
                            if (child is DartLongTemplateEntry) {
                                child.children.asList()
                                    .firstOrNull { !it.elementType.toString().contains("LONG_TEMPLATE_ENTRY_START") }
                                    ?.let {
                                        stringBuffer.append("{${it.text}}")
                                    }
                            }
                        }
                    }
                }
            }
            true
        }
        return literals
    }
}