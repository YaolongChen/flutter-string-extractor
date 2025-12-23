package person.cyl.flutterstringextractor

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * Object responsible for handling file I/O operations related to ARB (Application Resource Bundle) files.
 * It provides functionality to write key-value pairs to multiple ARB files simultaneously,
 * ensuring data consistency across different locales.
 */
object ArbFileWriter {

    /**
     * A lazy-initialized Gson instance configured for pretty printing and disabling HTML escaping.
     * This ensures that the generated ARB files are human-readable and preserve special characters correctly.
     */
    private val gson: Gson by lazy {
        GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }

    /**
     * A TypeToken for parsing JSON content into a LinkedHashMap.
     * LinkedHashMap is used to preserve the insertion order of keys, which is often preferred in resource files.
     */
    private val mapType = object : TypeToken<LinkedHashMap<String, Any>>() {}.type

    /**
     * Writes a key-value pair to a list of ARB files.
     *
     * This function performs the following steps:
     * 1. Checks if the key already exists in any of the target files.
     * 2. If the key exists, prompts the user for confirmation to overwrite.
     * 3. If confirmed (or if the key is new), writes the key-value pair to all specified files.
     * 4. Refreshes the Virtual File System (VFS) to ensure the IDE reflects the changes immediately.
     *
     * @param files The list of target ARB files to write to.
     * @param key The resource key to add or update.
     * @param value The resource value to associate with the key.
     * @return The number of files successfully written to.
     *         If the user chooses NOT to overwrite an existing key, it returns the total file count,
     *         treating it as a "success" so that the code replacement logic can proceed (reusing the existing key).
     */
    fun writeToArbFiles(files: List<File>, key: String, value: String): Int {
        // 1. Check for duplicate keys across all files to prevent accidental overwrites.
        if (isKeyExistInAnyFile(files, key)) {
            val overwrite = Messages.showYesNoDialog(
                MyMessageBundle.message("dialog.message.key.exists.overwrite", key),
                MyMessageBundle.message("dialog.title.duplicate.key"),
                Messages.getQuestionIcon()
            )
            // If the user chooses not to overwrite, we return the total file count.
            // This signals "success" to the caller, allowing the code replacement to proceed
            // using the existing key without modifying the ARB files.
            if (overwrite != Messages.YES) return files.size
        }

        var successCount = 0
        
        // 2. Iterate through each target file and perform the write operation.
        for (file in files) {
            try {
                // Ensure the parent directory exists.
                if (!file.parentFile.exists()) file.parentFile.mkdirs()
                
                // If the file doesn't exist, create it and initialize with an empty JSON object.
                if (!file.exists()) {
                    file.createNewFile()
                    file.writeText("{}")
                }

                // Read existing content and parse it into a Map.
                val content = file.readText()
                val map: LinkedHashMap<String, Any> = try {
                    gson.fromJson(content, mapType) ?: LinkedHashMap()
                } catch (e: Exception) {
                    // Fallback to an empty map if the file content is invalid JSON.
                    LinkedHashMap()
                }

                // Update the map with the new key-value pair.
                map[key] = value

                // Write the updated map back to the file as a JSON string.
                file.writeText(gson.toJson(map))
                
                // Refresh the file in the IDE's Virtual File System.
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)?.refresh(false, false)
                successCount++
            } catch (e: Exception) {
                e.printStackTrace()
                // Continue to the next file even if one fails, to maximize success.
            }
        }
        return successCount
    }

    /**
     * Checks if a given key exists in any of the provided ARB files.
     *
     * @param files The list of files to check.
     * @param key The key to search for.
     * @return `true` if the key is found in at least one file, `false` otherwise.
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