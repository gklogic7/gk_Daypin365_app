package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        val itemKind = intent.getStringExtra(AlarmScheduler.EXTRA_ITEM_KIND) ?: AlarmScheduler.KIND_REMINDER

        val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
            action = AlarmRingService.ACTION_START_ALARM
            putExtras(intent)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        if (itemId > 0L) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = ReminderRepository(context.applicationContext)
                    if (itemKind == AlarmScheduler.KIND_TASK) {
                        repo.handleTaskAlarmFiredAndRescheduleIfRecurring(itemId)
                    } else {
                        repo.handleAlarmFiredAndRescheduleIfRecurring(itemId)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
