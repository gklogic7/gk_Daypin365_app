package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.data.local.DeskLaunchMode
import com.example.data.local.PreferencesManager
import com.example.permissions.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Event-driven BroadcastReceiver that launches DayPin 365 directly into the
 * lightweight Desk Standby Board when power/charging connects (if enabled and allowed).
 */
class ChargingDeskReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_CONNECTED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferencesManager(context.applicationContext).preferencesFlow.first()
                if (prefs.deskLaunchMode == DeskLaunchMode.AUTO_ON_CHARGING &&
                    PermissionHelper.hasOverlayPermission(context.applicationContext)
                ) {
                    val launchIntent = Intent(context.applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(MainActivity.EXTRA_OPEN_DESK_BOARD, true)
                    }
                    runCatching { context.applicationContext.startActivity(launchIntent) }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
