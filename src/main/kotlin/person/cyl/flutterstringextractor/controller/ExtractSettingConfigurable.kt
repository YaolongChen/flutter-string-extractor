package person.cyl.flutterstringextractor.controller

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.NlsContexts
import person.cyl.flutterstringextractor.model.ExtractSettingState
import person.cyl.flutterstringextractor.view.ExtractSettingComponent
import javax.swing.JComponent

class ExtractSettingConfigurable : Configurable {
    private var mySettingComponent: ExtractSettingComponent? = null

    override fun getDisplayName(): @NlsContexts.ConfigurableName String {
        return "FlutterStringExtractor"
    }

    override fun createComponent(): JComponent? {
        mySettingComponent = ExtractSettingComponent()
        return mySettingComponent?.panel
    }

    override fun isModified(): Boolean {
        val settings = ExtractSettingState.instance
        var modified = mySettingComponent?.arbDir != settings.arbDir
        modified = modified or (mySettingComponent?.replacementTemplate != settings.replacementTemplate)
        modified =
            modified or ((mySettingComponent?.lookupKeyArbFileName ?: "") != (settings.lookupKeyArbFileName ?: ""))
        return modified
    }

    override fun apply() {
        val settings = ExtractSettingState.instance
        mySettingComponent?.let { settings.arbDir = it.arbDir }
        mySettingComponent?.let { settings.replacementTemplate = it.replacementTemplate }
        mySettingComponent?.let { settings.lookupKeyArbFileName = it.lookupKeyArbFileName }
    }

    override fun reset() {
        val settings = ExtractSettingState.instance
        mySettingComponent?.arbDir = settings.arbDir
        mySettingComponent?.replacementTemplate = settings.replacementTemplate
        mySettingComponent?.lookupKeyArbFileName = settings.lookupKeyArbFileName ?: ""
    }

    override fun disposeUIResources() {
        mySettingComponent = null
        super.disposeUIResources()
    }
}