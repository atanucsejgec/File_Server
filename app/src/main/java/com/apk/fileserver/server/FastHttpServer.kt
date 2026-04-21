package com.apk.fileserver.server

import android.util.Log
import com.apk.fileserver.utils.FileUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Custom HTTP server with maximum throughput
 *
 * Key differences from NanoHTTPD:
 * ├── Direct socket control (1MB send buffer)
 * ├── Thread pool (20 concurrent connections)
 * ├── Zero-copy file streaming
 * ├── No wrapper overhead
 * └── Tuned TCP parameters
 */
class FastHttpServer(
    private val port: Int,
    private val requestHandler: (FastHttpRequest) -> FastHttpResponse
) {

    companion object {
        private const val TAG = "FastHttpServer"

        // Buffer sizes
        const val SOCKET_SEND_BUFFER    = 2 * 1024 * 1024  // 2MB socket buffer
        const val SOCKET_RECV_BUFFER    = 128 * 1024        // 128KB receive
        const val FILE_READ_BUFFER      = 1024 * 1024       // 1MB file read
        const val HTTP_WRITE_BUFFER     = 2 * 1024 * 1024  // 2MB HTTP write

        // Connection settings
        const val MAX_THREADS           = 30
        const val SOCKET_TIMEOUT_MS     = 60_000            // 60 seconds
        const val BACKLOG               = 100
    }

    private var serverSocket: ServerSocket? = null
    private val threadPool = Executors.newFixedThreadPool(MAX_THREADS)
    @Volatile private var running = false

    // ═══════════════════════════════════════════════
    //              START / STOP
    // ═══════════════════════════════════════════════

    fun start() {
        if (running) return

        serverSocket = ServerSocket().apply {
            reuseAddress    = true
            receiveBufferSize = SOCKET_SEND_BUFFER
            bind(java.net.InetSocketAddress(port), BACKLOG)
        }

        running = true

        // Accept thread
        val acceptThread = Thread {
            Log.d(TAG, "Server started on port $port")
            while (running) {
                try {
                    val client = serverSocket?.accept() ?: break
                    applySocketOptimizations(client)
                    threadPool.execute { handleClient(client) }
                } catch (e: Exception) {
                    if (running) {
                        Log.e(TAG, "Accept error: ${e.message}")
                    }
                }
            }
            Log.d(TAG, "Accept loop ended")
        }
        acceptThread.isDaemon = true
        acceptThread.name = "HttpAccept"
        acceptThread.priority = Thread.MAX_PRIORITY
        acceptThread.start()
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (e: Exception) {}
        threadPool.shutdown()
        try {
            threadPool.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: Exception) {}
        Log.d(TAG, "Server stopped")
    }

    // ═══════════════════════════════════════════════
    //              SOCKET OPTIMIZATION
    // ═══════════════════════════════════════════════

    private fun applySocketOptimizations(socket: Socket) {
        try {
            socket.apply {
                // CRITICAL: Large send buffer = faster downloads
                sendBufferSize    = SOCKET_SEND_BUFFER
                receiveBufferSize = SOCKET_RECV_BUFFER
                // Keep connection alive
                keepAlive         = true
                // Disable Nagle: send data immediately
                // Better for large file streaming
                tcpNoDelay        = true
                // Timeout
                soTimeout         = SOCKET_TIMEOUT_MS
                // Hint OS: optimize for bandwidth
                setPerformancePreferences(0, 0, 1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Socket opt failed: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════
    //              CLIENT HANDLER
    // ═══════════════════════════════════════════════

    private fun handleClient(socket: Socket) {
        try {
            val input  = socket.getInputStream()
                .buffered(SOCKET_RECV_BUFFER)
            val output = socket.getOutputStream()

            // Handle multiple requests on same connection (keep-alive)
            var keepAlive = true
            while (keepAlive && !socket.isClosed) {
                val request = parseRequest(input) ?: break
                keepAlive = request.headers["connection"]
                    ?.lowercase() != "close"

                val response = requestHandler(request)
                sendResponse(output, response, keepAlive)

                // Flush after each response
                output.flush()

                if (!keepAlive) break
            }

        } catch (e: Exception) {
            // Client disconnected - normal
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    // ═══════════════════════════════════════════════
    //              HTTP REQUEST PARSER
    // ═══════════════════════════════════════════════

    private fun parseRequest(input: InputStream): FastHttpRequest? {
        return try {
            // Read request line
            val requestLine = readLine(input) ?: return null
            if (requestLine.isEmpty()) return null

            val parts = requestLine.split(" ")
            if (parts.size < 2) return null

            val method = parts[0].uppercase()
            val rawUri = parts[1]

            // Parse URI and query string
            val (uri, queryString) = if (rawUri.contains("?")) {
                val idx = rawUri.indexOf("?")
                Pair(
                    URLDecoder.decode(
                        rawUri.substring(0, idx), "UTF-8"
                    ),
                    rawUri.substring(idx + 1)
                )
            } else {
                Pair(
                    try {
                        URLDecoder.decode(rawUri, "UTF-8")
                    } catch (e: Exception) { rawUri },
                    ""
                )
            }

            // Parse query parameters
            val queryParams = parseQueryString(queryString)

            // Read headers
            val headers = mutableMapOf<String, String>()
            var line = readLine(input)
            while (!line.isNullOrEmpty()) {
                val colonIdx = line.indexOf(":")
                if (colonIdx > 0) {
                    val key   = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
                line = readLine(input)
            }

            // Read body if present
            val contentLength = headers["content-length"]
                ?.toIntOrNull() ?: 0
            val body = if (contentLength > 0) {
                val bodyBytes = ByteArray(contentLength)
                var offset = 0
                while (offset < contentLength) {
                    val read = input.read(
                        bodyBytes, offset, contentLength - offset
                    )
                    if (read == -1) break
                    offset += read
                }
                String(bodyBytes, 0, offset, Charsets.UTF_8)
            } else ""

            FastHttpRequest(
                method      = method,
                uri         = uri,
                queryParams = queryParams,
                headers     = headers,
                body        = body,
                inputStream = input
            )

        } catch (e: Exception) {
            Log.w(TAG, "Parse request failed: ${e.message}")
            null
        }
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) {
                if (prev == '\r'.code && sb.isNotEmpty()) {
                    sb.deleteCharAt(sb.length - 1)
                }
                return sb.toString()
            }
            sb.append(b.toChar())
            prev = b
        }
    }

    private fun parseQueryString(
        query: String
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isEmpty()) return result
        query.split("&").forEach { pair ->
            val eqIdx = pair.indexOf("=")
            if (eqIdx > 0) {
                try {
                    val key = URLDecoder.decode(
                        pair.substring(0, eqIdx), "UTF-8"
                    )
                    val value = URLDecoder.decode(
                        pair.substring(eqIdx + 1), "UTF-8"
                    )
                    result[key] = value
                } catch (e: Exception) {}
            }
        }
        return result
    }

    // ═══════════════════════════════════════════════
    //              HTTP RESPONSE SENDER
    // ═══════════════════════════════════════════════

    private fun sendResponse(
        output: OutputStream,
        response: FastHttpResponse,
        keepAlive: Boolean
    ) {
        val buffered = BufferedOutputStream(output, HTTP_WRITE_BUFFER)

        try {
            // Write status line
            val statusLine = "HTTP/1.1 ${response.statusCode} " +
                    "${response.statusText}\r\n"
            buffered.write(statusLine.toByteArray())

            // Write headers
            val connectionHeader = if (keepAlive) "keep-alive" else "close"
            buffered.write(
                "Connection: $connectionHeader\r\n".toByteArray()
            )
            buffered.write(
                "Access-Control-Allow-Origin: *\r\n".toByteArray()
            )

            // Write custom headers
            response.headers.forEach { (key, value) ->
                buffered.write("$key: $value\r\n".toByteArray())
            }

            when {
                // File streaming response
                response.file != null -> {
                    val fileSize = response.file.length()
                    buffered.write(
                        "Content-Length: $fileSize\r\n".toByteArray()
                    )
                    buffered.write("\r\n".toByteArray())
                    buffered.flush()

                    // Stream file directly with large buffer
                    streamFileToOutput(response.file, output,
                        response.rangeStart, response.rangeEnd)
                }

                // Stream input response (for ZIP)
                response.inputStream != null -> {
                    // Chunked transfer for unknown length
                    buffered.write(
                        "Transfer-Encoding: chunked\r\n".toByteArray()
                    )
                    buffered.write("\r\n".toByteArray())
                    buffered.flush()

                    // Write chunked body
                    writeChunked(response.inputStream, output)
                }

                // String body response
                else -> {
                    val bodyBytes = (response.body ?: "")
                        .toByteArray(Charsets.UTF_8)
                    buffered.write(
                        "Content-Length: ${bodyBytes.size}\r\n"
                            .toByteArray()
                    )
                    if (response.contentType.isNotEmpty()) {
                        buffered.write(
                            "Content-Type: ${response.contentType}\r\n"
                                .toByteArray()
                        )
                    }
                    buffered.write("\r\n".toByteArray())
                    buffered.write(bodyBytes)
                    buffered.flush()
                }
            }

        } catch (e: Exception) {
            Log.w(TAG, "Send response error: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════
    //         ZERO-COPY FILE STREAMING
    // ═══════════════════════════════════════════════

    /**
     * Maximum speed file streaming
     * Reads file in 1MB chunks directly to socket
     */
    private fun streamFileToOutput(
        file: File,
        output: OutputStream,
        rangeStart: Long = 0,
        rangeEnd: Long   = -1
    ) {
        val fileSize  = file.length()
        val start     = rangeStart
        val end       = if (rangeEnd < 0) fileSize - 1 else rangeEnd
        val toSend    = end - start + 1

        var remaining  = toSend
        val buffer     = ByteArray(FILE_READ_BUFFER)
        var totalSent  = 0L
        val startTime  = System.currentTimeMillis()

        try {
            FileInputStream(file).use { fis ->
                if (start > 0) fis.skip(start)
                val bis = BufferedInputStream(fis, FILE_READ_BUFFER)

                while (remaining > 0) {
                    val toRead = minOf(
                        FILE_READ_BUFFER.toLong(), remaining
                    ).toInt()
                    val read = bis.read(buffer, 0, toRead)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    remaining  -= read
                    totalSent  += read
                }
                output.flush()
            }

            // Log actual speed
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > 0) {
                val speedMBs = (totalSent.toDouble() / 1024 / 1024) /
                        (elapsed.toDouble() / 1000)
                Log.d(TAG, "Speed: ${"%.1f".format(speedMBs)} MB/s " +
                        "| Sent: ${FileUtils.formatFileSize(totalSent)} " +
                        "| Time: ${elapsed}ms")
            }

        } catch (e: Exception) {
            Log.w(TAG, "Stream error: ${e.message}")
        }
    }

    /**
     * Write chunked transfer encoding
     * Used for ZIP streaming (unknown total size)
     */
    private fun writeChunked(
        input: InputStream,
        output: OutputStream
    ) {
        val buffer = ByteArray(FILE_READ_BUFFER)
        try {
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } > 0) {
                // Write chunk size in hex
                val chunkHeader = "${bytesRead.toString(16)}\r\n"
                output.write(chunkHeader.toByteArray())
                // Write chunk data
                output.write(buffer, 0, bytesRead)
                output.write("\r\n".toByteArray())
            }
            // Final empty chunk
            output.write("0\r\n\r\n".toByteArray())
            output.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Chunked write error: ${e.message}")
        } finally {
            try { input.close() } catch (ignored: Exception) {}
        }
    }
}

// ═══════════════════════════════════════════════
//              DATA CLASSES
// ═══════════════════════════════════════════════

data class FastHttpRequest(
    val method: String,
    val uri: String,
    val queryParams: Map<String, String>,
    val headers: Map<String, String>,
    val body: String,
    val inputStream: InputStream
)

data class FastHttpResponse(
    val statusCode: Int             = 200,
    val statusText: String          = "OK",
    val contentType: String         = "application/octet-stream",
    val headers: Map<String, String> = emptyMap(),
    val body: String?               = null,
    val file: File?                 = null,
    val inputStream: InputStream?   = null,
    val rangeStart: Long            = 0,
    val rangeEnd: Long              = -1
) {
    companion object {
        fun ok(body: String, contentType: String = "application/json") =
            FastHttpResponse(
                statusCode  = 200,
                statusText  = "OK",
                contentType = contentType,
                body        = body
            )

        fun file(
            file: File,
            contentType: String,
            headers: Map<String, String> = emptyMap(),
            rangeStart: Long = 0,
            rangeEnd: Long   = -1
        ) = FastHttpResponse(
            statusCode  = if (rangeStart > 0) 206 else 200,
            statusText  = if (rangeStart > 0) "Partial Content" else "OK",
            contentType = contentType,
            headers     = headers,
            file        = file,
            rangeStart  = rangeStart,
            rangeEnd    = rangeEnd
        )

        fun stream(
            inputStream: InputStream,
            contentType: String,
            headers: Map<String, String> = emptyMap()
        ) = FastHttpResponse(
            statusCode   = 200,
            statusText   = "OK",
            contentType  = contentType,
            headers      = headers,
            inputStream  = inputStream
        )

        fun error(
            code: Int,
            message: String
        ) = FastHttpResponse(
            statusCode  = code,
            statusText  = message,
            contentType = "text/plain",
            body        = message
        )

        fun redirect(location: String) = FastHttpResponse(
            statusCode = 302,
            statusText = "Found",
            headers    = mapOf("Location" to location),
            body       = ""
        )
    }
}