package person.cyl.flutterstringextractor

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.File

/**
 * 负责管理和解析项目配置，主要是 `pubspec.yaml`。
 */
object ProjectConfigManager {

    private const val DEFAULT_ARB_DIR = "lib/l10n"
    private const val DEFAULT_CLASS_NAME = "S"
    private const val PUBSPEC_FILE_NAME = "pubspec.yaml"
    private const val CONFIG_BLOCK_HEADER = "flutter_string_extractor:"

    /**
     * 插件配置数据类。
     * @property arbDir ARB 文件所在的目录。
     * @property className 本地化代理类名 (例如 S, AppLocalizations)。
     */
    data class PluginConfig(
        val arbDir: String = DEFAULT_ARB_DIR,
        val className: String = DEFAULT_CLASS_NAME
    )

    /**
     * 从当前文件向上查找，获取本地化代理类名。
     * @param currentFile 当前正在编辑的文件。
     * @return 配置的类名，如果未找到则返回默认值。
     */
    fun getLocalizationsClassName(currentFile: PsiFile): String {
        val pubspec = findPubspecFile(currentFile) ?: return DEFAULT_CLASS_NAME
        return readConfigFromPubspec(pubspec).className
    }

    /**
     * 查找所有需要写入的目标 ARB 文件。
     * @param currentFile 当前正在编辑的文件。
     * @return 目标 ARB 文件列表。
     */
    fun findTargetArbFiles(currentFile: PsiFile): List<File> {
        val pubspecFile = findPubspecFile(currentFile) ?: return emptyList()
        val projectRoot = pubspecFile.parent

        // 1. 读取配置
        val config = readConfigFromPubspec(pubspecFile)
        val configPath = config.arbDir

        // 2. 找到该目录
        val l10nDir = projectRoot.findFileByRelativePath(configPath)

        // 如果目录不存在，构造一个 File 对象指向预期位置
        if (l10nDir == null) {
            val fallbackDir = File(projectRoot.path, configPath)
            return if (fallbackDir.exists() && fallbackDir.isDirectory) {
                fallbackDir.listFiles { _, name -> name.endsWith(".arb") }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        }

        // 3. 扫描目录下的所有 .arb 文件
        val arbFiles = l10nDir.children.filter {
            !it.isDirectory && it.extension == "arb"
        }.map { File(it.path) }

        // 如果目录下没有 arb 文件，默认建议创建一个 app_en.arb，以便用户可以从零开始
        if (arbFiles.isEmpty()) {
            return listOf(File(l10nDir.path, "app_en.arb"))
        }

        return arbFiles
    }

    /**
     * 从当前文件开始，向上遍历目录树查找 `pubspec.yaml` 文件。
     */
    private fun findPubspecFile(currentFile: PsiFile): VirtualFile? {
        var directory = currentFile.originalFile.virtualFile.parent
        while (directory != null) {
            val pubspec = directory.findChild(PUBSPEC_FILE_NAME)
            if (pubspec != null) {
                return pubspec
            }
            directory = directory.parent
        }
        return null
    }

    /**
     * 解析 `pubspec.yaml` 文件，提取插件配置。
     * 支持 `arb_dir` 和 `localizations_class_name`。
     */
    private fun readConfigFromPubspec(pubspec: VirtualFile): PluginConfig {
        var arbDir = DEFAULT_ARB_DIR
        var className = DEFAULT_CLASS_NAME

        try {
            val content = String(pubspec.contentsToByteArray())
            val lines = content.lines()
            var insideConfigBlock = false

            for (line in lines) {
                val trimmed = line.trim()

                if (trimmed.startsWith(CONFIG_BLOCK_HEADER)) {
                    insideConfigBlock = true
                    continue
                }

                // 如果进入了配置块，但当前行不再有缩进，说明配置块结束
                if (insideConfigBlock && line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("#")) {
                    insideConfigBlock = false
                }

                if (insideConfigBlock) {
                    if (trimmed.startsWith("arb_dir:")) {
                        arbDir = trimmed.substringAfter("arb_dir:").trim()
                    }
                    if (trimmed.startsWith("localizations_class_name:")) {
                        className = trimmed.substringAfter("localizations_class_name:").trim()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 解析失败时返回默认配置
        }
        return PluginConfig(arbDir, className)
    }
}