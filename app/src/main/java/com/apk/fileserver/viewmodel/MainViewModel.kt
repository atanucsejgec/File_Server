package com.apk.fileserver.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apk.fileserver.server.LocalFileServer
import com.apk.fileserver.service.FileServerService
import com.apk.fileserver.utils.FileUtils
import com.apk.fileserver.utils.NetworkUtils
import com.apk.fileserver.utils.PermissionStatus
import com.apk.fileserver.utils.PermissionUtils
import com.apk.fileserver.utils.TransferRecord
import com.apk.fileserver.utils.TransferType
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.apk.fileserver.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication<Application>().applicationContext

    // ═══════════════════════════════════════════════
    //              STATE FLOWS
    // ═══════════════════════════════════════════════
    private val settingsRepository = SettingsRepository(context)
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val _transfers = MutableStateFlow<List<TransferRecord>>(emptyList())
    val transfers: StateFlow<List<TransferRecord>> = _transfers.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5000),
            initialValue  = AppSettings()
        )

    // ═══════════════════════════════════════════════
    //         WIFI LOCK + WAKE LOCK (KEY FIX)
    // ═══════════════════════════════════════════════

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Acquire WiFi High Performance Lock
     * Prevents WiFi chip from entering power save mode
     * This is the #1 reason for slow transfer speed
     */
    private fun acquireWifiLock() {
        try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

            // Release existing lock first
            releaseWifiLock()

            wifiLock = wifiManager.createWifiLock(
                // WIFI_MODE_FULL_HIGH_PERF = maximum throughput
                // Disables power save mode on WiFi chip
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "LocalShare:WifiLock"
            ).also { lock ->
                lock.setReferenceCounted(false)
                lock.acquire()
                android.util.Log.d("MainViewModel",
                    "WiFi HIGH PERF lock acquired")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel",
                "WiFi lock failed: ${e.message}")
        }
    }

    /**
     * Acquire CPU Wake Lock
     * Prevents CPU from sleeping during file transfer
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = context.getSystemService(
                Context.POWER_SERVICE
            ) as PowerManager

            releaseWakeLock()

            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "LocalShare:WakeLock"
            ).also { lock ->
                lock.setReferenceCounted(false)
                // Keep awake for up to 1 hour
                lock.acquire(60 * 60 * 1000L)
                android.util.Log.d("MainViewModel",
                    "CPU wake lock acquired")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel",
                "Wake lock failed: ${e.message}")
        }
    }

    private fun releaseWifiLock() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                android.util.Log.d("MainViewModel", "WiFi lock released")
            }
            wifiLock = null
        } catch (e: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                android.util.Log.d("MainViewModel", "Wake lock released")
            }
            wakeLock = null
        } catch (e: Exception) {}
    }

    // ═══════════════════════════════════════════════
    //              INTERNAL
    // ═══════════════════════════════════════════════

    private var fileServer: LocalFileServer? = null
    private var networkMonitorJob: Job?     = null

    // ═══════════════════════════════════════════════
    //              INIT
    // ═══════════════════════════════════════════════

    init {
        checkPermissions()
        checkNetworkStatus()
        startNetworkMonitor()

        // %%% FIX: Auto-start server if setting enabled %%%
        viewModelScope.launch {
            settings.collect { appSettings ->
                if (appSettings.autoStartServer &&
                    !_serverState.value.isServerRunning &&
                    _serverState.value.isWifiConnected
                ) {
                    startServer()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════
    //              PERMISSIONS
    // ═══════════════════════════════════════════════

    fun checkPermissions() {
        val status = PermissionUtils.getPermissionStatus(context)
        _uiState.update { it.copy(permissionStatus = status) }
    }

    // ═══════════════════════════════════════════════
    //              NETWORK
    // ═══════════════════════════════════════════════

    fun checkNetworkStatus() {
        val isWifi = NetworkUtils.isWifiConnected(context)
        val ip     = if (isWifi) NetworkUtils.getLocalIpAddress(context)
        else ""
        val valid  = NetworkUtils.isValidLocalIp(ip)

        _serverState.update {
            it.copy(
                isWifiConnected = isWifi,
                localIpAddress  = if (valid) ip else "",
                serverUrl       = if (valid)
                    NetworkUtils.buildServerUrl(
                        context, _settings.value.serverPort
                    )
                else ""
            )
        }
    }

    private fun startNetworkMonitor() {
        networkMonitorJob?.cancel()
        networkMonitorJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                checkNetworkStatus()
                if (_serverState.value.isServerRunning &&
                    !_serverState.value.isWifiConnected
                ) {
                    stopServer()
                    showMessage("WiFi disconnected - server stopped")
                }
            }
        }
    }

    // ═══════════════════════════════════════════════
    //              SERVER CONTROL
    // ═══════════════════════════════════════════════

    fun startServer() {
        viewModelScope.launch {
            if (!_serverState.value.isWifiConnected) {
                showMessage("Please connect to WiFi first")
                return@launch
            }
            val permStatus = _uiState.value.permissionStatus
            if (permStatus != null && !permStatus.canFunction()) {
                showMessage("Storage permission required")
                return@launch
            }
            if (_serverState.value.isServerRunning) {
                showMessage("Server is already running")
                return@launch
            }

            _serverState.update { it.copy(isStarting = true) }

            try {
                val currentSettings = settings.value

                withContext(Dispatchers.IO) {
                    val pwd = if (currentSettings.passwordEnabled)
                        currentSettings.password
                    else null

                    fileServer = LocalFileServer(
                        port                 = currentSettings.serverPort,
                        password             = pwd,
                        showHiddenFiles      = currentSettings.showHiddenFiles,
                        onTransferComplete   = { record ->
                            addTransferRecord(record)
                        },
                        onClientConnected    = { ip ->
                            updateActiveConnections(ip, true)
                        },
                        onClientDisconnected = { ip ->
                            updateActiveConnections(ip, false)
                        }
                    )
                    fileServer?.start()
                }

                acquireWifiLock()
                acquireWakeLock()

                val ip   = NetworkUtils.getLocalIpAddress(context)
                val port = currentSettings.serverPort
                val url  = NetworkUtils.buildServerUrl(context, port)
                val qr   = generateQrCode(url)

                _serverState.update {
                    it.copy(
                        isServerRunning   = true,
                        isStarting        = false,
                        localIpAddress    = ip,
                        serverUrl         = url,
                        qrCodeBitmap      = qr,
                        startTime         = System.currentTimeMillis(),
                        activeConnections = emptyList()
                    )
                }

                val serviceIntent = FileServerService.buildStartIntent(
                    context = context,
                    port    = port,
                    ip      = ip,
                    url     = url
                )
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "Foreground service start failed: ${e.message}")
                }
                showMessage("Server started on port $port")

            } catch (e: Exception) {
                releaseWifiLock()
                releaseWakeLock()
                _serverState.update {
                    it.copy(isServerRunning = false, isStarting = false)
                }
                showMessage("Failed to start: ${e.message}")
            }
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    fileServer?.stop()
                    fileServer = null
                }

                releaseWifiLock()
                releaseWakeLock()

                val stopIntent = FileServerService.buildStopIntent(context)
                context.startService(stopIntent)

                _serverState.update {
                    it.copy(
                        isServerRunning   = false,
                        isStarting        = false,
                        qrCodeBitmap      = null,
                        activeConnections = emptyList(),
                        startTime         = null
                    )
                }
                showMessage("Server stopped")
            } catch (e: Exception) {
                showMessage("Error stopping: ${e.message}")
            }
        }
    }

    fun toggleServer() {
        if (_serverState.value.isServerRunning) stopServer()
        else startServer()
    }

    // ═══════════════════════════════════════════════
    //              QR CODE
    // ═══════════════════════════════════════════════

    private suspend fun generateQrCode(content: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val size  = 512
                val hints = mapOf(
                    EncodeHintType.MARGIN          to 1,
                    EncodeHintType.ERROR_CORRECTION to
                            ErrorCorrectionLevel.M
                )
                val writer    = QRCodeWriter()
                val bitMatrix = writer.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    size, size, hints
                )
                val bitmap = Bitmap.createBitmap(
                    size, size,
                    Bitmap.Config.ARGB_8888
                )
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        bitmap.setPixel(
                            x, y,
                            if (bitMatrix[x, y]) Color.BLACK
                            else Color.WHITE
                        )
                    }
                }
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    // ═══════════════════════════════════════════════
    //              TRANSFER HISTORY
    // ═══════════════════════════════════════════════

    fun addTransferRecord(record: TransferRecord) {
        _transfers.update { current ->
            listOf(record) + current.take(99)
        }
    }

    fun clearTransferHistory() {
        _transfers.update { emptyList() }
        showMessage("History cleared")
    }

    fun getTransfersByType(type: TransferType): List<TransferRecord> {
        return _transfers.value.filter { it.type == type }
    }

    // ═══════════════════════════════════════════════
    //              CONNECTIONS
    // ═══════════════════════════════════════════════

    private fun updateActiveConnections(
        clientIp: String,
        connected: Boolean
    ) {
        _serverState.update { state ->
            val current = state.activeConnections.toMutableList()
            if (connected) {
                if (!current.contains(clientIp)) current.add(clientIp)
            } else {
                current.remove(clientIp)
            }
            state.copy(activeConnections = current)
        }
    }

    // ═══════════════════════════════════════════════
    //              SETTINGS
    // ═══════════════════════════════════════════════

    fun updateServerPort(port: Int) {
        viewModelScope.launch {
            settingsRepository.updateServerPort(port)
            // Update URL if server running
            if (_serverState.value.isServerRunning) {
                val validPort = port.coerceIn(1024, 65535)
                val url = "http://${_serverState.value.localIpAddress}:$validPort"
                _serverState.update { it.copy(serverUrl = url) }
            }
        }
    }

    fun updatePasswordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePasswordEnabled(enabled)
        }
    }

    fun updatePassword(password: String) {
        viewModelScope.launch {
            settingsRepository.updatePassword(password)
        }
    }

    fun updateDarkModeWeb(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDarkModeWeb(enabled)
        }
    }

    fun updateShowHiddenFiles(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowHiddenFiles(show)
        }
    }

    fun updateAutoStartServer(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoStartServer(enabled)
        }
    }

    // ═══════════════════════════════════════════════
    //              UI HELPERS
    // ═══════════════════════════════════════════════

    private fun showMessage(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun getServerUptime(): String {
        val start   = _serverState.value.startTime ?: return "00:00"
        val elapsed = System.currentTimeMillis() - start
        val s       = (elapsed / 1000) % 60
        val m       = (elapsed / (1000 * 60)) % 60
        val h       = elapsed / (1000 * 60 * 60)
        return when {
            h > 0   -> "%dh %02dm %02ds".format(h, m, s)
            m > 0   -> "%dm %02ds".format(m, s)
            else    -> "%ds".format(s)
        }
    }

    fun getTotalTransferred(): Long {
        return _transfers.value.sumOf { it.fileSize }
    }

    // ═══════════════════════════════════════════════
    //              CLEANUP
    // ═══════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        networkMonitorJob?.cancel()
        releaseWifiLock()
        releaseWakeLock()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fileServer?.stop()
            }
        }
    }
}

// ═══════════════════════════════════════════════
//              STATE DATA CLASSES
// ═══════════════════════════════════════════════

data class MainUiState(
    val permissionStatus: PermissionStatus? = null,
    val snackbarMessage: String? = null,
    val isLoading: Boolean = false
)

data class ServerState(
    val isWifiConnected: Boolean        = false,
    val localIpAddress: String          = "",
    val serverUrl: String               = "",
    val isServerRunning: Boolean        = false,
    val isStarting: Boolean             = false,
    val startTime: Long?                = null,
    val qrCodeBitmap: Bitmap?           = null,
    val activeConnections: List<String> = emptyList()
)

data class AppSettings(
    val serverPort: Int          = 8080,
    val passwordEnabled: Boolean = false,
    val password: String         = "",
    val darkModeWeb: Boolean     = false,
    val showHiddenFiles: Boolean = false,
    val autoStartServer: Boolean = false
)