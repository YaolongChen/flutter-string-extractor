package person.cyl.flutterstringextractor.view

import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import person.cyl.flutterstringextractor.MyMessageBundle
import javax.swing.JPanel

class ExtractSettingComponent {
    val panel: JPanel
    private val arbDirField = JBTextField()
    private val replacementTemplateField = JBTextField()
    private val lookupKeyArbFileNameField = JBTextField()

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(MyMessageBundle.message("label.arbDir"), arbDirField, 1, false)
            .addLabeledComponent(MyMessageBundle.message("label.replaceTemplate"), replacementTemplateField, 1, false)
            .addLabeledComponent(MyMessageBundle.message("label.lookupKeyArbName"), lookupKeyArbFileNameField, 1, false)
            .addComponentFillVertically(JPanel(), 0) // 填充底部空白
            .panel
    }

    var arbDir: String
        get() = arbDirField.text
        set(newText) {
            arbDirField.text = newText
        }

    var replacementTemplate: String
        get() = replacementTemplateField.text
        set(newText) {
            replacementTemplateField.text = newText
        }

    var lookupKeyArbFileName: String
        get() = lookupKeyArbFileNameField.text
        set(newText) {
            lookupKeyArbFileNameField.text = newText
        }
}