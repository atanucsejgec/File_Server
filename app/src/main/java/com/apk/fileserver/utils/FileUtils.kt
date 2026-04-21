package com.apk.fileserver.utils

import android.os.Environment
import android.util.Log
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    // ═══════════════════════════════════════════════
    //              ROOT DIRECTORIES
    // ═══════════════════════════════════════════════

    /**
     * Get the primary external storage root
     * Example: /storage/emulated/0
     */
    fun getExternalStorageRoot(): File {
        return Environment.getExternalStorageDirectory()
    }

    /**
     * Get all available storage roots
     * Includes SD card if present
     */
    fun getAllStorageRoots(): List<StorageRoot> {
        val roots = mutableListOf<StorageRoot>()

        // Primary internal storage
        val internalStorage = Environment.getExternalStorageDirectory()
        if (internalStorage.exists()) {
            roots.add(
                StorageRoot(
                    name = "Internal Storage",
                    path = internalStorage.absolutePath,
                    file = internalStorage,
                    icon = "📱"
                )
            )
        }

        // Check for SD card / external volumes
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.isDirectory) {
                storageDir.listFiles()?.forEach { dir ->
                    if (dir.isDirectory &&
                        dir.name != "emulated" &&
                        dir.name != "self" &&
                        dir.canRead()
                    ) {
                        roots.add(
                            StorageRoot(
                                name = "SD Card (${dir.name})",
                                path = dir.absolutePath,
                                file = dir,
                                icon = "💾"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // SD card detection failed, ignore
        }

        return roots
    }

    // ═══════════════════════════════════════════════
    //              FILE LISTING
    // ═══════════════════════════════════════════════

    /**
     * List files in a directory, sorted:
     * 1. Folders first (alphabetical)
     * 2. Files second (alphabetical)
     */
    // In FileUtils object - replace listDirectory function

    fun listDirectory(
        path: String,
        showHiddenFiles: Boolean = false   // %%% FIX: was always false %%%
    ): List<FileItem> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) {
            return emptyList()
        }

        val items = mutableListOf<FileItem>()

        try {
            val files = dir.listFiles() ?: return emptyList()

            // Apply hidden file filter
            val folders = files
                .filter { it.isDirectory }
                .filter { showHiddenFiles || !it.isHidden }
                .sortedBy { it.name.lowercase() }

            val regularFiles = files
                .filter { it.isFile }
                .filter { showHiddenFiles || !it.isHidden }
                .sortedBy { it.name.lowercase() }

            folders.forEach { folder ->
                items.add(
                    FileItem(
                        name         = folder.name,
                        path         = folder.absolutePath,
                        isDirectory  = true,
                        size         = 0L,
                        lastModified = folder.lastModified(),
                        mimeType     = "directory",
                        extension    = "",
                        icon         = getFolderIcon(folder.name)
                    )
                )
            }

            regularFiles.forEach { file ->
                val ext = getExtension(file.name)
                items.add(
                    FileItem(
                        name         = file.name,
                        path         = file.absolutePath,
                        isDirectory  = false,
                        size         = file.length(),
                        lastModified = file.lastModified(),
                        mimeType     = getMimeType(ext),
                        extension    = ext,
                        icon         = getFileIcon(ext)
                    )
                )
            }

        } catch (e: Exception) {
            Log.e("FileUtils", "listDirectory error: ${e.message}")
        }

        return items
    }

    // ═══════════════════════════════════════════════
    //              FILE OPERATIONS
    // ═══════════════════════════════════════════════

    /**
     * Create a new folder
     * Returns true if successful
     */
    fun createFolder(parentPath: String, folderName: String): Result<File> {
        return try {
            // Sanitize folder name
            val safeName = sanitizeFileName(folderName)
            if (safeName.isEmpty()) {
                return Result.failure(Exception("Invalid folder name"))
            }

            val newFolder = File(parentPath, safeName)

            if (newFolder.exists()) {
                Result.failure(Exception("Folder already exists"))
            } else if (newFolder.mkdirs()) {
                Result.success(newFolder)
            } else {
                Result.failure(Exception("Failed to create folder"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a file or folder (recursive)
     * Returns true if successful
     */
    fun deleteFileOrFolder(path: String): Result<Boolean> {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return Result.failure(Exception("File not found"))
            }

            val deleted = if (file.isDirectory) {
                deleteRecursive(file)
            } else {
                file.delete()
            }

            if (deleted) Result.success(true)
            else Result.failure(Exception("Failed to delete"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recursively delete folder and contents
     */
    private fun deleteRecursive(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursive(child)
            }
        }
        return file.delete()
    }

    /**
     * Rename a file or folder
     */
    fun renameFileOrFolder(path: String, newName: String): Result<File> {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return Result.failure(Exception("File not found"))
            }

            val safeName = sanitizeFileName(newName)
            if (safeName.isEmpty()) {
                return Result.failure(Exception("Invalid name"))
            }

            val newFile = File(file.parent ?: "", safeName)
            if (newFile.exists()) {
                return Result.failure(Exception("Name already exists"))
            }

            if (file.renameTo(newFile)) {
                Result.success(newFile)
            } else {
                Result.failure(Exception("Failed to rename"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search files by name in a directory (recursive)
     */
    fun searchFiles(rootPath: String, query: String, maxResults: Int = 100): List<FileItem> {
        val results = mutableListOf<FileItem>()
        val root = File(rootPath)
        val lowerQuery = query.lowercase().trim()

        if (lowerQuery.isEmpty()) return emptyList()

        searchRecursive(root, lowerQuery, results, maxResults)
        return results
    }

    private fun searchRecursive(
        dir: File,
        query: String,
        results: MutableList<FileItem>,
        maxResults: Int
    ) {
        if (results.size >= maxResults) return
        if (!dir.canRead()) return

        try {
            dir.listFiles()?.forEach { file ->
                if (results.size >= maxResults) return

                if (file.name.lowercase().contains(query)) {
                    val ext = if (file.isFile) getExtension(file.name) else ""
                    results.add(
                        FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = file.isDirectory,
                            size = if (file.isFile) file.length() else 0L,
                            lastModified = file.lastModified(),
                            mimeType = if (file.isDirectory) "directory" else getMimeType(ext),
                            extension = ext,
                            icon = if (file.isDirectory) getFolderIcon(file.name)
                            else getFileIcon(ext)
                        )
                    )
                }

                // Recurse into subdirectories
                if (file.isDirectory && !file.isHidden) {
                    searchRecursive(file, query, results, maxResults)
                }
            }
        } catch (e: Exception) {
            // Skip directories we can't read
        }
    }

    // ═══════════════════════════════════════════════
    //              SIZE FORMATTING
    // ═══════════════════════════════════════════════

    /**
     * Format bytes into human readable size
     * Examples: "1.5 MB", "234 KB", "4.2 GB"
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val clampedGroup = digitGroups.coerceIn(0, units.size - 1)

        val df = DecimalFormat("#,##0.#")
        val value = bytes / Math.pow(1024.0, clampedGroup.toDouble())

        return "${df.format(value)} ${units[clampedGroup]}"
    }

    /**
     * Format transfer speed
     * Example: "5.2 MB/s"
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatFileSize(bytesPerSecond)}/s"
    }

    /**
     * Format timestamp to readable date
     * Example: "Dec 25, 2024 14:30"
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // ═══════════════════════════════════════════════
    //              MIME TYPES
    // ═══════════════════════════════════════════════

    /**
     * Get file extension (lowercase, without dot)
     * Example: "photo.JPG" → "jpg"
     */
    fun getExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    /**
     * Get MIME type from file extension
     */
    fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            // Images
            "jpg", "jpeg" -> "image/jpeg"
            "png"         -> "image/png"
            "gif"         -> "image/gif"
            "webp"        -> "image/webp"
            "bmp"         -> "image/bmp"
            "svg"         -> "image/svg+xml"
            "heic", "heif"-> "image/heic"

            // Videos
            "mp4"         -> "video/mp4"
            "mkv"         -> "video/x-matroska"
            "avi"         -> "video/x-msvideo"
            "mov"         -> "video/quicktime"
            "webm"        -> "video/webm"
            "3gp"         -> "video/3gpp"
            "flv"         -> "video/x-flv"

            // Audio
            "mp3"         -> "audio/mpeg"
            "wav"         -> "audio/wav"
            "flac"        -> "audio/flac"
            "aac"         -> "audio/aac"
            "ogg"         -> "audio/ogg"
            "m4a"         -> "audio/mp4"
            "opus"        -> "audio/opus"

            // Documents
            "pdf"         -> "application/pdf"
            "doc"         -> "application/msword"
            "docx"        -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls"         -> "application/vnd.ms-excel"
            "xlsx"        -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt"         -> "application/vnd.ms-powerpoint"
            "pptx"        -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt"         -> "text/plain"
            "csv"         -> "text/csv"
            "html", "htm" -> "text/html"
            "xml"         -> "text/xml"
            "json"        -> "application/json"

            // Archives
            "zip"         -> "application/zip"
            "rar"         -> "application/x-rar-compressed"
            "7z"          -> "application/x-7z-compressed"
            "tar"         -> "application/x-tar"
            "gz"          -> "application/gzip"

            // Code
            "kt"          -> "text/plain"
            "java"        -> "text/plain"
            "py"          -> "text/plain"
            "js"          -> "text/javascript"
            "css"         -> "text/css"

            // Apps
            "apk"         -> "application/vnd.android.package-archive"

            else          -> "application/octet-stream"
        }
    }

    // ═══════════════════════════════════════════════
    //              ICONS (EMOJI)
    // ═══════════════════════════════════════════════

    /**
     * Get emoji icon for file based on extension
     */
    fun getFileIcon(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif",
            "webp", "bmp", "svg", "heic" -> "🖼️"

            "mp4", "mkv", "avi", "mov",
            "webm", "3gp", "flv"         -> "🎬"

            "mp3", "wav", "flac", "aac",
            "ogg", "m4a", "opus"         -> "🎵"

            "pdf"                         -> "📄"
            "doc", "docx"                -> "📝"
            "xls", "xlsx"               -> "📊"
            "ppt", "pptx"               -> "📊"
            "txt"                        -> "📃"
            "csv"                        -> "📊"
            "json", "xml"               -> "📋"
            "html", "htm"               -> "🌐"

            "zip", "rar", "7z",
            "tar", "gz"                  -> "🗜️"

            "apk"                        -> "📦"

            "kt", "java", "py",
            "js", "css", "cpp"           -> "💻"

            else                         -> "📄"
        }
    }

    /**
     * Get emoji icon for folder based on name
     */
    fun getFolderIcon(folderName: String): String {
        return when (folderName.lowercase()) {
            "dcim", "camera"      -> "📷"
            "pictures", "photos"  -> "🖼️"
            "downloads"           -> "⬇️"
            "music"               -> "🎵"
            "movies", "videos"    -> "🎬"
            "documents", "docs"   -> "📝"
            "android"             -> "🤖"
            "whatsapp"            -> "💬"
            "telegram"            -> "✈️"
            "backup"              -> "💾"
            else                  -> "📁"
        }
    }

    // ═══════════════════════════════════════════════
    //              SAFETY HELPERS
    // ═══════════════════════════════════════════════

    /**
     * Remove dangerous characters from file names
     * Prevents path traversal attacks
     */
    fun sanitizeFileName(name: String): String {
        return name
            .trim()
            .replace(Regex("[/\\\\:*?\"<>|]"), "_")  // Replace illegal chars
            .replace(Regex("\\.{2,}"), ".")              // No double dots
            .take(255)                                    // Max filename length
    }

    /**
     * Validate that a path is within allowed root
     * Prevents directory traversal attacks
     */
    fun isPathSafe(rootPath: String, targetPath: String): Boolean {
        return try {
            val root = File(rootPath).canonicalPath
            val target = File(targetPath).canonicalPath
            target.startsWith(root)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if file is an image (for thumbnail generation)
     */
    fun isImage(extension: String): Boolean {
        return extension.lowercase() in listOf(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic"
        )
    }

    /**
     * Check if file is a video (for streaming)
     */
    fun isVideo(extension: String): Boolean {
        return extension.lowercase() in listOf(
            "mp4", "mkv", "avi", "mov", "webm", "3gp"
        )
    }

    /**
     * Check if file is audio (for streaming)
     */
    fun isAudio(extension: String): Boolean {
        return extension.lowercase() in listOf(
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "opus"
        )
    }
}

// ═══════════════════════════════════════════════
//              DATA CLASSES
// ═══════════════════════════════════════════════

/**
 * Represents a file or folder in the browser
 */
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val extension: String,
    val icon: String
)

/**
 * Represents a storage root (internal/SD card)
 */
data class StorageRoot(
    val name: String,
    val path: String,
    val file: File,
    val icon: String
)

/**
 * Represents a completed transfer (upload or download)
 */
data class TransferRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val type: TransferType,
    val timestamp: Long = System.currentTimeMillis(),
    val clientIp: String = "",
    val success: Boolean = true
)

/**
 * Type of file transfer
 */
enum class TransferType {
    UPLOAD,    // PC → Phone
    DOWNLOAD   // Phone → PC
}