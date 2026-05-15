package com.notes.notesproxmlviews

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver que escuta o evento BOOT_COMPLETED.
 * Quando o dispositivo reinicia, os alarmes do AlarmManager são perdidos.
 * Este receiver re-agenda automaticamente todos os lembretes pendentes
 * que estavam guardados nas SharedPreferences.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) return

        Log.d("BootReceiver", "Device rebooted — re-scheduling pending reminders")

        val pendingReminders = ReminderScheduler.getAllPendingReminders(context)
        val now = System.currentTimeMillis()

        pendingReminders.forEach { (docId, pair) ->
            val (reminderMs, title) = pair
            if (reminderMs > now) {
                // Re-agendar apenas se ainda não passou a hora
                ReminderScheduler.scheduleReminder(context, docId, title, reminderMs)
                Log.d("BootReceiver", "Re-scheduled reminder for docId=$docId")
            } else {
                // Lembrete já passou durante o reboot — limpar
                ReminderScheduler.removePendingReminder(context, docId)
                Log.d("BootReceiver", "Removed expired reminder for docId=$docId")
            }
        }
    }
}
