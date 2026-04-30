package person.cyl.flutterstringextractor.model

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "person.cyl.flutterstringextractor.ExtractSettingState",
    storages = [Storage("FlutterStringExtract.xml")]
)
class ExtractSettingState : PersistentStateComponent<ExtractSettingState> {
    var arbDir: String = "lib/src/l10n"
    var replacementTemplate: String = "S.of(context).{key}"
    var lookupKeyArbFileName: String? = null

    val needLookupKey: Boolean
        get() {
            return lookupKeyArbFileName != null;
        }

    override fun getState(): ExtractSettingState {
        return this
    }

    override fun loadState(state: ExtractSettingState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: ExtractSettingState
            get() = ApplicationManager.getApplication().getService(ExtractSettingState::class.java)
    }
}