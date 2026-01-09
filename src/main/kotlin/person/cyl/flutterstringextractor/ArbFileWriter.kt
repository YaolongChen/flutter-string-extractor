package person.cyl.flutterstringextractor

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

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
                val virtualFile = getOrCreateVirtualFile(file) ?: continue

                // Read existing content (Prioritize Document to avoid conflicts)
                val map = readMapFromVirtualFileOrDocument(virtualFile)

                // Update the map with the new key-value pair.
                map[key] = value

                // Write the updated map back to the file
                writeMapToVirtualFileOrDocument(virtualFile, map)
                
                successCount++
            } catch (e: Exception) {
                e.printStackTrace()
                // Continue to the next file even if one fails, to maximize success.
            }
        }
        return successCount
    }

    /**
     * Writes multiple key-value pairs to a list of ARB files.
     *
     * @param files The list of target ARB files to write to.
     * @param entries A map of key-value pairs to write.
     * @return The number of files successfully written to.
     */
    fun writeBatchToArbFiles(files: List<File>, entries: Map<String, String>): Int {
        var successCount = 0
        for (file in files) {
            try {
                val virtualFile = getOrCreateVirtualFile(file) ?: continue

                // Read existing content (Prioritize Document)
                val map = readMapFromVirtualFileOrDocument(virtualFile)

                // Only add entries where the key does not already exist in the map
                for ((key, value) in entries) {
                    if (!map.containsKey(key)) {
                        map[key] = value
                    }
                }

                // Write the updated map back to the file
                writeMapToVirtualFileOrDocument(virtualFile, map)
                
                successCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return successCount
    }

    /**
     * Helper to get or create a VirtualFile from a java.io.File.
     * Uses VFS methods for creation to ensure consistency.
     */
    private fun getOrCreateVirtualFile(file: File): VirtualFile? {
        val lfs = LocalFileSystem.getInstance()
        // Refresh to ensure we have the latest state from disk
        var virtualFile = lfs.refreshAndFindFileByIoFile(file)

        if (virtualFile == null) {
            // File doesn't exist, try to create it via VFS
            val parentFile = file.parentFile
            if (!parentFile.exists()) {
                parentFile.mkdirs()
                lfs.refreshAndFindFileByIoFile(parentFile)
            }

            val parentVFile = lfs.refreshAndFindFileByIoFile(parentFile)
            if (parentVFile != null) {
                try {
                    virtualFile = parentVFile.createChildData(this, file.name)
                    virtualFile.setBinaryContent("{}".toByteArray(StandardCharsets.UTF_8))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return virtualFile
    }

    /**
     * Helper to read and parse JSON map from a VirtualFile or its Document.
     * Prioritizes Document to get the latest in-memory content and avoid conflicts.
     */
    private fun readMapFromVirtualFileOrDocument(virtualFile: VirtualFile): LinkedHashMap<String, Any> {
        return try {
            val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            if (document != null) {
                // Read from Document (in-memory content)
                gson.fromJson(document.text, mapType) ?: LinkedHashMap()
            } else {
                // Read from VirtualFile (disk content via VFS)
                virtualFile.inputStream.use { inputStream ->
                    InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                        gson.fromJson(reader, mapType) ?: LinkedHashMap()
                    }
                }
            }
        } catch (e: Exception) {
            LinkedHashMap()
        }
    }

    /**
     * Helper to write JSON map to a VirtualFile or its Document.
     * Prioritizes Document to support Undo and avoid conflicts.
     */
    private fun writeMapToVirtualFileOrDocument(virtualFile: VirtualFile, map: Map<String, Any>) {
        val newContent = gson.toJson(map)
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        
        if (document != null) {
            // Write to Document: Supports Undo and keeps editor in sync
            document.setText(newContent)
        } else {
            // Write to VirtualFile: For files not currently open/loaded
            virtualFile.setBinaryContent(newContent.toByteArray(StandardCharsets.UTF_8))
        }
    }

    /**
     * Checks if a given key exists in any of the provided ARB files.
     *
     * @param files The list of files to check.
     * @param key The key to search for.
     * @return `true` if the key is found in at least one file, `false` otherwise.
     */
    fun isKeyExistInAnyFile(files: List<File>, key: String): Boolean {
        val lfs = LocalFileSystem.getInstance()
        for (file in files) {
            val virtualFile = lfs.findFileByIoFile(file)
            if (virtualFile != null && virtualFile.exists()) {
                val map = readMapFromVirtualFileOrDocument(virtualFile)
                if (map.containsKey(key)) {
                    return true
                }
            } else if (file.exists()) {
                // Fallback to IO if VFS doesn't know about it yet
                val map: Map<String, Any> = try {
                    file.reader(StandardCharsets.UTF_8).use { reader ->
                        gson.fromJson(reader, mapType) ?: emptyMap()
                    }
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

    /**
     * Finds the key for a given value in the provided ARB files.
     *
     * @param files The list of files to search.
     * @param value The value to search for.
     * @return The key if found, or null if not found.
     */
    fun findKeyByValue(files: List<File>, value: String): String? {
        val lfs = LocalFileSystem.getInstance()
        for (file in files) {
            val virtualFile = lfs.findFileByIoFile(file)
            if (virtualFile != null && virtualFile.exists()) {
                val map = readMapFromVirtualFileOrDocument(virtualFile)
                for ((k, v) in map) {
                    if (v is String && v == value) {
                        return k
                    }
                }
            } else if (file.exists()) {
                // Fallback to IO
                val map: Map<String, Any> = try {
                    file.reader(StandardCharsets.UTF_8).use { reader ->
                        gson.fromJson(reader, mapType) ?: emptyMap()
                    }
                } catch (e: Exception) {
                    emptyMap()
                }
                for ((k, v) in map) {
                    if (v is String && v == value) {
                        return k
                    }
                }
            }
        }
        return null
    }
}