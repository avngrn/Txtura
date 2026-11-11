package com.txtura.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * Manages dynamic runtime permission requests with contextual rationale messages
 * and automatic redirection to App Settings if a permission is permanently denied.
 */
object DynamicPermissionManager {

    private const val PREFS_NAME = "txtura_permission_history"

    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if [permission] is granted.
     * - If granted: executes [onAlreadyGranted].
     * - If permanently denied: shows toast message and redirects user to App Settings.
     * - If not yet granted: shows [companionMessage] toast and triggers [requestLauncher].
     */
    fun checkAndRequest(
        context: Context,
        permission: String,
        companionMessage: String,
        permanentlyDeniedMessage: String? = null,
        requestLauncher: (String) -> Unit,
        onAlreadyGranted: () -> Unit
    ) {
        if (hasPermission(context, permission)) {
            onAlreadyGranted()
            return
        }

        val activity = findActivity(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val requestedBefore = prefs.getBoolean("requested_$permission", false)

        val isPermanentlyDenied = activity != null && requestedBefore && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

        if (isPermanentlyDenied) {
            val msg = permanentlyDeniedMessage ?: companionMessage
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            prefs.edit { putBoolean("requested_$permission", true) }
            Toast.makeText(context, companionMessage, Toast.LENGTH_SHORT).show()
            requestLauncher(permission)
        }
    }
}
