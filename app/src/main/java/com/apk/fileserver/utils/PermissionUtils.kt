package com.apk.fileserver.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    // ═══════════════════════════════════════════════
    //              PERMISSION LISTS
    // ═══════════════════════════════════════════════

    /**
     * Get required storage permissions based on Android version
     * Each Android version needs different permissions
     */
    fun getRequiredPermissions(): Array<String> {
        return when {
            // Android 14+ (API 34+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }

            // Android 13 (API 33)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            }

            // Android 10 - 12 (API 29-32)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

            // Android 6 - 9 (API 23-28)
            else -> {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    /**
     * Notification permission (Android 13+)
     */
    fun getNotificationPermission(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }

    // ═══════════════════════════════════════════════
    //           CHECK PERMISSIONS
    // ═══════════════════════════════════════════════

    /**
     * Check if all required storage permissions are granted
     */
    fun hasStoragePermissions(context: Context): Boolean {
        // For Android 11+ also check MANAGE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                return true  // Has full access - best case
            }
            // Fall through to check granular permissions
        }

        // Check all required permissions
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if MANAGE_EXTERNAL_STORAGE is granted
     * This gives full file system access (Android 11+)
     */
    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Not needed below Android 11
        }
    }

    /**
     * Check notification permission
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed below Android 13
        }
    }

    /**
     * Check a single permission
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ═══════════════════════════════════════════════
    //           REQUEST PERMISSIONS
    // ═══════════════════════════════════════════════

    /**
     * Request all storage permissions at once
     */
    fun requestStoragePermissions(
        activity: Activity,
        requestCode: Int = REQUEST_CODE_STORAGE
    ) {
        val permissions = getRequiredPermissions()
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissions,
                requestCode
            )
        }
    }

    /**
     * Open system settings for MANAGE_EXTERNAL_STORAGE
     * Must be done via Settings screen (cannot request directly)
     */
    fun openManageStorageSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback: open general manage storage screen
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    )
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    // Last fallback: app settings
                    openAppSettings(context)
                }
            }
        }
    }

    /**
     * Open app permission settings page
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ═══════════════════════════════════════════════
    //           PERMISSION STATUS
    // ═══════════════════════════════════════════════

    /**
     * Get detailed permission status for UI display
     */
    fun getPermissionStatus(context: Context): PermissionStatus {
        val hasManage = hasManageStoragePermission()
        val hasStorage = hasStoragePermissions(context)
        val hasNotification = hasNotificationPermission(context)

        return PermissionStatus(
            hasFullStorageAccess = hasManage,
            hasBasicStorageAccess = hasStorage,
            hasNotificationAccess = hasNotification,
            androidVersion = Build.VERSION.SDK_INT,
            needsManagePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
            canAccessAllFiles = hasManage || hasStorage
        )
    }

    /**
     * Check if we should show rationale for a permission
     * Returns true if user denied once (but not permanently)
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Check if any storage permission rationale should be shown
     */
    fun shouldShowStorageRationale(activity: Activity): Boolean {
        return getRequiredPermissions().any { permission ->
            shouldShowRationale(activity, permission)
        }
    }

    // ═══════════════════════════════════════════════
    //           PERMISSION RESULT HANDLER
    // ═══════════════════════════════════════════════

    /**
     * Process permission request results
     * Call this from onRequestPermissionsResult in Activity
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onStorageGranted: () -> Unit,
        onStorageDenied: () -> Unit,
        onNotificationGranted: () -> Unit = {},
        onNotificationDenied: () -> Unit = {}
    ) {
        when (requestCode) {
            REQUEST_CODE_STORAGE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                ) {
                    onStorageGranted()
                } else {
                    onStorageDenied()
                }
            }

            REQUEST_CODE_NOTIFICATION -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    onNotificationGranted()
                } else {
                    onNotificationDenied()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════
    //           CONSTANTS
    // ═══════════════════════════════════════════════

    const val REQUEST_CODE_STORAGE = 1001
    const val REQUEST_CODE_NOTIFICATION = 1002
}

// ═══════════════════════════════════════════════
//              DATA CLASSES
// ═══════════════════════════════════════════════

/**
 * Holds complete permission state for UI
 */
data class PermissionStatus(
    val hasFullStorageAccess: Boolean,      // MANAGE_EXTERNAL_STORAGE granted
    val hasBasicStorageAccess: Boolean,     // READ/WRITE_EXTERNAL_STORAGE granted
    val hasNotificationAccess: Boolean,     // POST_NOTIFICATIONS granted
    val androidVersion: Int,                // Current SDK version
    val needsManagePermission: Boolean,     // Android 11+ needs manage permission
    val canAccessAllFiles: Boolean          // Either full or basic access
) {
    /**
     * Human readable summary of permission state
     */
    fun getSummary(): String {
        return when {
            hasFullStorageAccess -> "Full storage access granted"
            hasBasicStorageAccess -> "Basic storage access granted"
            else -> "Storage permission required"
        }
    }

    /**
     * Whether app can function (at minimum level)
     */
    fun canFunction(): Boolean = canAccessAllFiles
}