package com.apk.fileserver.server

import android.os.Environment
import android.util.Log
import com.apk.fileserver.utils.FileUtils
import com.apk.fileserver.utils.TransferRecord
import com.apk.fileserver.utils.TransferType
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * NanoHTTPD-based File Server
 * Replaced Ktor and custom FastHttpServer for better compatibility
 */
class LocalFileServer(
    private val port: Int,
    private val password: String? = null,
    private val showHiddenFiles: Boolean = false,
    private val onTransferComplete: ((TransferRecord) -> Unit)? = null,
    private val onClientConnected: ((String) -> Unit)? = null,
    private val onClientDisconnected: ((String) -> Unit)? = null
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "LocalFileServer"
        const val FILE_BUFFER = 1024 * 1024
        const val ZIP_BUFFER = 512 * 1024
        const val PIPE_BUFFER = 8 * 1024 * 1024
    }

    private val rootDir = Environment.getExternalStorageDirectory()
    private val activeSessions = mutableSetOf<String>()

    // ═══════════════════════════════════════════════
    //              START / STOP
    // ═══════════════════════════════════════════════

    override fun start() {
        start(SOCKET_READ_TIMEOUT, false)
        Log.d(TAG, "LocalFileServer started on port $port")
    }

    override fun stop() {
        super.stop()
        Log.d(TAG, "LocalFileServer stopped")
    }

    // ═══════════════════════════════════════════════
    //              MAIN ROUTER
    // ═══════════════════════════════════════════════

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        val remoteIp = session.remoteIpAddress

        // Notify UI of connection
        onClientConnected?.invoke(remoteIp)

        Log.d(TAG, "$method $uri from $remoteIp")

        if (method == Method.OPTIONS) {
            return responseOk("", "text/plain")
        }

        // Password check
        if (isPasswordProtected() && uri != "/auth") {
            if (!isAuthenticated(session)) {
                return serveLoginPage()
            }
        }

        return try {
            when {
                uri == "/auth"           -> handleAuth(session)
                uri == "/api/list"       -> handleApiList(session)
                uri == "/api/search"     -> handleApiSearch(session)
                uri == "/api/storage"    -> handleApiStorage()
                uri == "/api/mkdir"      -> handleApiMkdir(session)
                uri == "/api/delete"     -> handleApiDelete(session)
                uri == "/api/rename"     -> handleApiRename(session)
                uri == "/api/upload"     -> handleApiUpload(session)
                uri == "/api/zip"        -> handleApiZip(session)
                uri == "/api/zipfolder"  -> handleZipFolder(session)
                uri.startsWith("/files") -> handleFileDownload(session, uri)
                uri.startsWith("/thumb") -> handleThumbnail(session, uri)
                else                     -> serveMainPage()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server Error: ${e.message}", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Server Error: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════
    //              AUTH
    // ═══════════════════════════════════════════════

    private fun isPasswordProtected() = !password.isNullOrEmpty()

    private fun isAuthenticated(session: IHTTPSession): Boolean {
        if (!isPasswordProtected()) return true
        val token = session.cookies.read("session_token") ?: return false
        return activeSessions.contains(token)
    }

    private fun handleAuth(session: IHTTPSession): Response {
        if (session.method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed")
        }
        
        try { session.parseBody(mutableMapOf()) } catch (e: Exception) {}
        
        val entered = session.parameters["password"]?.get(0) ?: ""
        return if (entered == password) {
            val token = java.util.UUID.randomUUID().toString()
            activeSessions.add(token)
            val res = jsonOk("""{"success":true}""")
            session.cookies.set("session_token", token, 1)
            res
        } else {
            val res = newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", """{"success":false,"error":"Invalid password"}""")
            addCorsHeaders(res)
            res
        }
    }

    // ═══════════════════════════════════════════════
    //              API - LIST
    // ═══════════════════════════════════════════════

    private fun handleApiList(session: IHTTPSession): Response {
        val requestedPath = session.parameters["path"]?.get(0) ?: rootDir.absolutePath

        val targetFile = File(requestedPath)
        if (!isSafePath(targetFile)) return jsonError("Access denied")
        if (!targetFile.exists() || !targetFile.isDirectory) return jsonError("Directory not found")

        val items = FileUtils.listDirectory(requestedPath, showHiddenFiles)
        val jsonArray = JSONArray()

        items.forEach { item ->
            jsonArray.put(JSONObject().apply {
                put("name", item.name)
                put("path", item.path)
                put("isDirectory", item.isDirectory)
                put("size", item.size)
                put("sizeFormatted", FileUtils.formatFileSize(item.size))
                put("lastModified", item.lastModified)
                put("dateFormatted", FileUtils.formatDate(item.lastModified))
                put("mimeType", item.mimeType)
                put("extension", item.extension)
                put("icon", item.icon)
                put("isImage", FileUtils.isImage(item.extension))
                put("isVideo", FileUtils.isVideo(item.extension))
                put("isAudio", FileUtils.isAudio(item.extension))
            })
        }

        return jsonOk(JSONObject().apply {
            put("path", requestedPath)
            put("parentPath", targetFile.parent ?: rootDir.absolutePath)
            put("isRoot", requestedPath == rootDir.absolutePath)
            put("items", jsonArray)
            put("breadcrumbs", buildBreadcrumbs(requestedPath))
            put("totalItems", items.size)
        }.toString())
    }

    // ═══════════════════════════════════════════════
    //              API - SEARCH
    // ═══════════════════════════════════════════════

    private fun handleApiSearch(session: IHTTPSession): Response {
        val query = session.parameters["q"]?.get(0) ?: ""
        val searchPath = session.parameters["path"]?.get(0) ?: rootDir.absolutePath

        if (query.length < 2) return jsonError("Query too short")

        val targetFile = File(searchPath)
        if (!isSafePath(targetFile)) return jsonError("Access denied")

        val results = FileUtils.searchFiles(searchPath, query)
        val jsonArray = JSONArray()

        results.forEach { item ->
            jsonArray.put(JSONObject().apply {
                put("name", item.name)
                put("path", item.path)
                put("isDirectory", item.isDirectory)
                put("size", item.size)
                put("sizeFormatted", FileUtils.formatFileSize(item.size))
                put("dateFormatted", FileUtils.formatDate(item.lastModified))
                put("mimeType", item.mimeType)
                put("extension", item.extension)
                put("icon", item.icon)
            })
        }

        return jsonOk(JSONObject().apply {
            put("query", query)
            put("results", jsonArray)
            put("count", results.size)
        }.toString())
    }

    // ═══════════════════════════════════════════════
    //              API - STORAGE
    // ═══════════════════════════════════════════════

    private fun handleApiStorage(): Response {
        val roots = FileUtils.getAllStorageRoots()
        val jsonArray = JSONArray()

        roots.forEach { root ->
            val total = root.file.totalSpace
            val free  = root.file.freeSpace
            val used  = total - free
            jsonArray.put(JSONObject().apply {
                put("name", root.name)
                put("path", root.path)
                put("icon", root.icon)
                put("totalSpace", total)
                put("freeSpace", free)
                put("usedSpace", used)
                put("totalFormatted", FileUtils.formatFileSize(total))
                put("freeFormatted", FileUtils.formatFileSize(free))
                put("usedFormatted", FileUtils.formatFileSize(used))
                put("usedPercent", if (total > 0) (used * 100 / total).toInt() else 0)
            })
        }

        return jsonOk(JSONObject().apply {
            put("roots", jsonArray)
        }.toString())
    }

    // ═══════════════════════════════════════════════
    //              API - MKDIR
    // ═══════════════════════════════════════════════

    private fun handleApiMkdir(session: IHTTPSession): Response {
        try { session.parseBody(mutableMapOf()) } catch (e: Exception) {}
        val parentPath = session.parameters["path"]?.get(0) ?: rootDir.absolutePath
        val folderName = session.parameters["name"]?.get(0) ?: ""

        if (folderName.isEmpty()) return jsonError("Folder name required")

        val parentFile = File(parentPath)
        if (!isSafePath(parentFile)) return jsonError("Access denied")

        val result = FileUtils.createFolder(parentPath, folderName)
        return if (result.isSuccess) jsonSuccess("Folder created")
        else jsonError(result.exceptionOrNull()?.message ?: "Failed")
    }

    // ═══════════════════════════════════════════════
    //              API - DELETE
    // ═══════════════════════════════════════════════

    private fun handleApiDelete(session: IHTTPSession): Response {
        try { session.parseBody(mutableMapOf()) } catch (e: Exception) {}
        val filePath = session.parameters["path"]?.get(0) ?: ""

        if (filePath.isEmpty()) return jsonError("Path required")

        val targetFile = File(filePath)
        if (!isSafePath(targetFile)) return jsonError("Access denied")
        if (!targetFile.exists()) return jsonError("File not found")

        val result = FileUtils.deleteFileOrFolder(filePath)
        return if (result.isSuccess) jsonSuccess("Deleted")
        else jsonError(result.exceptionOrNull()?.message ?: "Failed")
    }

    // ═══════════════════════════════════════════════
    //              API - RENAME
    // ═══════════════════════════════════════════════

    private fun handleApiRename(session: IHTTPSession): Response {
        try { session.parseBody(mutableMapOf()) } catch (e: Exception) {}
        val filePath = session.parameters["path"]?.get(0) ?: ""
        val newName  = session.parameters["newName"]?.get(0) ?: ""

        if (filePath.isEmpty() || newName.isEmpty()) return jsonError("Path and name required")

        val targetFile = File(filePath)
        if (!isSafePath(targetFile)) return jsonError("Access denied")

        val result = FileUtils.renameFileOrFolder(filePath, newName)
        return if (result.isSuccess) {
            val newFile = result.getOrNull()
            jsonOk(JSONObject().apply {
                put("success", true)
                put("message", "Renamed")
                put("newPath", newFile?.absolutePath ?: "")
                put("newName", newFile?.name ?: "")
            }.toString())
        } else {
            jsonError(result.exceptionOrNull()?.message ?: "Failed")
        }
    }

    // ═══════════════════════════════════════════════
    //              API - UPLOAD
    // ═══════════════════════════════════════════════

    private fun handleApiUpload(session: IHTTPSession): Response {
        val uploadPath = session.parameters["path"]?.get(0) ?: rootDir.absolutePath
        val targetDir = File(uploadPath)
        
        if (!isSafePath(targetDir)) return jsonError("Access denied")
        if (!targetDir.exists()) targetDir.mkdirs()

        return try {
            val files = mutableMapOf<String, String>()
            session.parseBody(files)

            val uploadedFiles = mutableListOf<File>()
            files.forEach { (key, tempPath) ->
                val originalName = session.parameters[key]?.get(0) ?: "uploaded_file"
                val safeFile = getUniqueFile(targetDir, FileUtils.sanitizeFileName(originalName))
                
                val tempFile = File(tempPath)
                if (tempFile.exists()) {
                    if (tempFile.renameTo(safeFile)) {
                        uploadedFiles.add(safeFile)
                        onTransferComplete?.invoke(
                            TransferRecord(
                                fileName = safeFile.name,
                                filePath = safeFile.absolutePath,
                                fileSize = safeFile.length(),
                                type     = TransferType.UPLOAD,
                                clientIp = session.remoteIpAddress
                            )
                        )
                    } else {
                        // Fallback copy if rename fails
                        tempFile.inputStream().use { input ->
                            safeFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile.delete()
                        uploadedFiles.add(safeFile)
                    }
                }
            }

            jsonOk(JSONObject().apply {
                put("success", true)
                put("message", "${uploadedFiles.size} file(s) uploaded")
                val arr = JSONArray()
                uploadedFiles.forEach { f ->
                    arr.put(JSONObject().apply {
                        put("name", f.name)
                        put("size", f.length())
                        put("sizeFormatted", FileUtils.formatFileSize(f.length()))
                    })
                }
                put("files", arr)
            }.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Upload error: ${e.message}")
            jsonError("Upload failed: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════
    //              ZIP - STREAMING
    // ═══════════════════════════════════════════════

    private fun handleApiZip(session: IHTTPSession): Response {
        try { session.parseBody(mutableMapOf()) } catch (e: Exception) {}
        val pathsParam = session.parameters["paths"]?.get(0) ?: ""
        val paths = parsePaths(pathsParam)

        if (paths.isEmpty()) return jsonError("No files selected")

        for (path in paths) {
            if (!isSafePath(File(path))) return jsonError("Access denied: $path")
        }

        val zipName = "LocalShare_${System.currentTimeMillis()}.zip"
        return createStreamingZipResponse(paths, zipName)
    }

    private fun handleZipFolder(session: IHTTPSession): Response {
        val folderPath = session.parameters["path"]?.get(0) ?: rootDir.absolutePath
        val folder = File(folderPath)
        
        if (!isSafePath(folder)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Access denied")
        if (!folder.exists() || !folder.isDirectory) return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Folder not found")

        return createStreamingZipResponse(listOf(folder.absolutePath), "${folder.name}.zip")
    }

    private fun createStreamingZipResponse(paths: List<String>, zipName: String): Response {
        val pipedInput  = PipedInputStream(PIPE_BUFFER)
        val pipedOutput = PipedOutputStream(pipedInput)

        Thread {
            try {
                ZipOutputStream(BufferedOutputStream(pipedOutput, ZIP_BUFFER)).use { zos ->
                    zos.setLevel(1)
                    for (path in paths) {
                        val file = File(path)
                        if (file.isFile) writeFileToZip(zos, file, file.name)
                        else if (file.isDirectory) writeFolderToZip(zos, file, file.name)
                    }
                    zos.finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "ZIP streaming error", e)
            } finally {
                try { pipedOutput.close() } catch (ignored: Exception) {}
            }
        }.apply {
            isDaemon = true
            name = "ZipStream"
            start()
        }

        val res = newChunkedResponse(Response.Status.OK, "application/zip", pipedInput)
        res.addHeader("Content-Disposition", "attachment; filename=\"$zipName\"")
        addCorsHeaders(res)
        return res
    }

    private fun writeFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists() || !file.canRead()) return
        val ext = file.extension.lowercase()
        val alreadyCompressed = ext in setOf(
            "mp4","mkv","avi","mov","webm","3gp","flv","m4v",
            "mp3","aac","ogg","flac","m4a","opus",
            "jpg","jpeg","png","gif","webp","heic",
            "zip","rar","7z","gz","bz2","apk","pdf"
        )
        try {
            val entry = ZipEntry(entryName)
            entry.time = file.lastModified()
            if (alreadyCompressed) {
                entry.method = ZipEntry.STORED
                entry.size = file.length()
                entry.crc = computeCrc32(file)
            } else {
                entry.method = ZipEntry.DEFLATED
            }
            zos.putNextEntry(entry)
            val buffer = ByteArray(FILE_BUFFER)
            FileInputStream(file).buffered(FILE_BUFFER).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } > 0) {
                    zos.write(buffer, 0, read)
                }
            }
            zos.closeEntry()
        } catch (e: Exception) {
            try { zos.closeEntry() } catch (ignored: Exception) {}
        }
    }

    private fun writeFolderToZip(zos: ZipOutputStream, folder: File, baseName: String) {
        val files = folder.listFiles() ?: return
        if (files.isEmpty()) {
            try {
                val e = ZipEntry("$baseName/")
                e.method = ZipEntry.STORED
                e.size = 0
                e.crc = 0
                e.time = folder.lastModified()
                zos.putNextEntry(e)
                zos.closeEntry()
            } catch (ignored: Exception) {}
            return
        }
        for (file in files) {
            val entry = "$baseName/${file.name}"
            if (file.isFile) writeFileToZip(zos, file, entry)
            else if (file.isDirectory) writeFolderToZip(zos, file, entry)
        }
    }

    private fun computeCrc32(file: File): Long {
        val crc = CRC32()
        val buffer = ByteArray(FILE_BUFFER)
        try {
            FileInputStream(file).buffered(FILE_BUFFER).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } > 0) {
                    crc.update(buffer, 0, read)
                }
            }
        } catch (e: Exception) {}
        return crc.value
    }

    // ═══════════════════════════════════════════════
    //         FILE DOWNLOAD
    // ═══════════════════════════════════════════════

    private fun handleFileDownload(session: IHTTPSession, uri: String): Response {
        val filePath = try {
            URLDecoder.decode(uri.removePrefix("/files"), "UTF-8")
        } catch (e: Exception) {
            uri.removePrefix("/files")
        }

        val file = File(filePath)
        if (!isSafePath(file)) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Access denied")
        if (!file.exists()) return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        if (file.isDirectory) {
            val res = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "")
            res.addHeader("Location", "/?path=${URLEncoder.encode(filePath, "UTF-8")}")
            return res
        }

        val mimeType = FileUtils.getMimeType(FileUtils.getExtension(file.name))
        val fileSize = file.length()
        val rangeHeader = session.headers["range"]

        onTransferComplete?.invoke(
            TransferRecord(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = fileSize,
                type     = TransferType.DOWNLOAD,
                clientIp = session.remoteIpAddress
            )
        )

        return if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            handleRangeRequest(file, fileSize, mimeType, rangeHeader)
        } else {
            val res = newFixedLengthResponse(Response.Status.OK, mimeType, FileInputStream(file), fileSize)
            val ext = FileUtils.getExtension(file.name)
            if (!FileUtils.isImage(ext) && !FileUtils.isVideo(ext) && !FileUtils.isAudio(ext)) {
                res.addHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
            }
            addCorsHeaders(res)
            res
        }
    }

    private fun handleRangeRequest(file: File, fileSize: Long, mimeType: String, rangeHeader: String): Response {
        val spec = rangeHeader.removePrefix("bytes=")
        val parts = spec.split("-")
        val start = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val end = if (parts.size > 1 && parts[1].isNotEmpty()) {
            parts[1].toLongOrNull() ?: (fileSize - 1)
        } else {
            fileSize - 1
        }
        val length = end - start + 1

        val fis = FileInputStream(file)
        try {
            fis.skip(start)
            val res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, fis, length)
            res.addHeader("Content-Range", "bytes $start-$end/$fileSize")
            res.addHeader("Accept-Ranges", "bytes")
            addCorsHeaders(res)
            return res
        } catch (e: Exception) {
            try { fis.close() } catch (ignored: Exception) {}
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Range error")
        }
    }

    // ═══════════════════════════════════════════════
    //              THUMBNAIL
    // ═══════════════════════════════════════════════

    private fun handleThumbnail(session: IHTTPSession, uri: String): Response {
        val filePath = try {
            URLDecoder.decode(uri.removePrefix("/thumb"), "UTF-8")
        } catch (e: Exception) {
            uri.removePrefix("/thumb")
        }

        val file = File(filePath)
        if (!isSafePath(file) || !file.exists()) return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")

        val ext = FileUtils.getExtension(file.name)
        return if (FileUtils.isImage(ext)) {
            val res = newFixedLengthResponse(Response.Status.OK, FileUtils.getMimeType(ext), FileInputStream(file), file.length())
            addCorsHeaders(res)
            res
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not an image")
        }
    }

    // ═══════════════════════════════════════════════
    //              WEB PAGES
    // ═══════════════════════════════════════════════

    private fun serveMainPage(): Response = responseOk(WebInterface.getMainPageHtml(), MIME_HTML)

    private fun serveLoginPage(): Response {
        val res = newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_HTML, WebInterface.getLoginPageHtml())
        addCorsHeaders(res)
        return res
    }

    // ═══════════════════════════════════════════════
    //              HELPERS
    // ═══════════════════════════════════════════════

    private fun responseOk(text: String, mimeType: String): Response {
        val res = newFixedLengthResponse(Response.Status.OK, mimeType, text)
        addCorsHeaders(res)
        return res
    }

    private fun addCorsHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Range")
    }

    private fun jsonOk(json: String) = responseOk(json, "application/json")

    private fun jsonError(message: String) = jsonOk(JSONObject().apply {
        put("success", false)
        put("error", message)
    }.toString())

    private fun jsonSuccess(message: String) = jsonOk(JSONObject().apply {
        put("success", true)
        put("message", message)
    }.toString())

    private fun isSafePath(file: File): Boolean {
        return try {
            val canonical = file.canonicalPath
            val root      = rootDir.canonicalPath
            canonical.startsWith(root) || canonical.startsWith("/storage/") || canonical.startsWith("/sdcard/")
        } catch (e: Exception) {
            false
        }
    }

    private fun buildBreadcrumbs(path: String): JSONArray {
        val crumbs = JSONArray()
        val rootPath = rootDir.absolutePath
        crumbs.put(JSONObject().apply { put("name", "Home"); put("path", rootPath) })
        if (path != rootPath && path.startsWith(rootPath)) {
            val relative = path.removePrefix(rootPath)
            var current = rootPath
            relative.split("/").filter { it.isNotEmpty() }.forEach { seg ->
                current += "/$seg"
                crumbs.put(JSONObject().apply { put("name", seg); put("path", current) })
            }
        }
        return crumbs
    }

    private fun parsePaths(pathsParam: String): List<String> {
        if (pathsParam.isEmpty()) return emptyList()
        val paths = mutableListOf<String>()
        try {
            val arr = JSONArray(pathsParam)
            for (i in 0 until arr.length()) {
                val p = arr.getString(i)
                if (p.isNotEmpty()) paths.add(p)
            }
        } catch (e: Exception) {
            if (pathsParam.isNotEmpty()) paths.add(pathsParam)
        }
        return paths
    }

    private fun getUniqueFile(dir: File, fileName: String): File {
        var file = File(dir, fileName)
        if (!file.exists()) return file
        val base = fileName.substringBeforeLast(".")
        val ext = fileName.substringAfterLast(".", "")
        var counter = 1
        while (file.exists()) {
            file = if (ext.isEmpty()) File(dir, "$base ($counter)") else File(dir, "$base ($counter).$ext")
            counter++
        }
        return file
    }
}
