package person.cyl.flutterstringextractor

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * 负责 ARB 文件的读写操作。
 */
object ArbFileWriter {

    // 使用懒加载的 Gson 实例，配置为美化输出并禁用 HTML 转义
    private val gson: Gson by lazy {
        GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }

    private val mapType = object : TypeToken<LinkedHashMap<String, Any>>() {}.type

    /**
     * 将键值对批量写入多个 ARB 文件。
     *
     * @param files 目标 ARB 文件列表。
     * @param key 要写入的键。
     * @param value 要写入的值。
     * @return 成功写入的文件数量。
     */
    fun writeToArbFiles(files: List<File>, key: String, value: String): Int {
        // 1. 检查 Key 是否已存在，如果存在则弹窗询问是否覆盖
        if (isKeyExistInAnyFile(files, key)) {
            val overwrite = Messages.showYesNoDialog(
                MyMessageBundle.message("dialog.message.key.exists.overwrite", key),
                MyMessageBundle.message("dialog.title.duplicate.key"),
                Messages.getQuestionIcon()
            )
            if (overwrite != Messages.YES) return 0
        }

        var successCount = 0
        // 2. 遍历并写入每个文件
        for (file in files) {
            try {
                // 确保父目录和文件存在
                if (!file.parentFile.exists()) file.parentFile.mkdirs()
                if (!file.exists()) {
                    file.createNewFile()
                    file.writeText("{}") // 初始化为空 JSON 对象
                }

                val content = file.readText()
                val map: LinkedHashMap<String, Any> = try {
                    gson.fromJson(content, mapType) ?: LinkedHashMap()
                } catch (e: Exception) {
                    // 如果文件内容不是有效的 JSON，则创建一个新的 Map
                    LinkedHashMap()
                }

                map[key] = value // 添加或更新键值对

                file.writeText(gson.toJson(map))
                // 刷新 VFS，确保 IDE 能立即感知到文件变化
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)?.refresh(false, false)
                successCount++
            } catch (e: Exception) {
                e.printStackTrace()
                // 即使某个文件失败，也继续尝试下一个
            }
        }
        return successCount
    }

    /**
     * 检查给定的 Key 是否存在于任何一个 ARB 文件中。
     */
    private fun isKeyExistInAnyFile(files: List<File>, key: String): Boolean {
        for (file in files) {
            if (file.exists()) {
                val content = file.readText()
                val map: Map<String, Any> = try {
                    gson.fromJson(content, mapType) ?: emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }

                if (map.containsKey(key)) {
                    return true
                }
            }
        }
        return false
    }
}