package com.notes.notesproxmlviews

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Helper para agendar e cancelar lembretes de notas via AlarmManager.
 * Também persiste os lembretes pendentes em SharedPreferences para
 * serem re-agendados após um reboot do dispositivo.
 */
object ReminderScheduler {

    private const val TAG = "ReminderScheduler"
    private const val PREFS_NAME = "note_reminders"

    /**
     * Agenda um alarme que dispara a notificação do lembrete.
     *
     * @param context Contexto da aplicação
     * @param docId ID do documento Firestore da nota
     * @param title Título da nota (para mostrar na notificação)
     * @param reminderMs Timestamp em milissegundos (epoch) para o alarme
     */
    fun scheduleReminder(context: Context, docId: String, title: String, reminderMs: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("docId", docId)
            putExtra("title", title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            docId.hashCode(), // requestCode único por docId
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // setExactAndAllowWhileIdle garante disparo mesmo em Doze mode
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderMs,
                pendingIntent
            )
            savePendingReminder(context, docId, reminderMs, title)
            Log.d(TAG, "Reminder scheduled for docId=$docId at $reminderMs")
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot schedule exact alarm: ${e.message}")
        }
    }

    /**
     * Cancela o alarme de um lembrete existente.
     *
     * @param context Contexto da aplicação
     * @param docId ID do documento Firestore da nota
     */
    fun cancelReminder(context: Context, docId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            docId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        removePendingReminder(context, docId)
        Log.d(TAG, "Reminder cancelled for docId=$docId")
    }

    /**
     * Guarda os dados do lembrete em SharedPreferences.
     * Formato: "docId" -> "reminderMs|title"
     */
    private fun savePendingReminder(context: Context, docId: String, reminderMs: Long, title: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(docId, "$reminderMs|$title").apply()
    }

    /**
     * Remove o lembrete das SharedPreferences (após disparar ou cancelar).
     */
    fun removePendingReminder(context: Context, docId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(docId).apply()
    }

    /**
     * Devolve todos os lembretes pendentes guardados nas SharedPreferences.
     * Usado pelo BootReceiver para re-agendar após reboot.
     *
     * @return Mapa de docId -> Pair(reminderMs, title)
     */
    fun getAllPendingReminders(context: Context): Map<String, Pair<Long, String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val result = mutableMapOf<String, Pair<Long, String>>()
        prefs.all.forEach { (docId, value) ->
            val parts = (value as? String)?.split("|", limit = 2)
            if (parts != null && parts.size == 2) {
                val ms = parts[0].toLongOrNull() ?: return@forEach
                val title = parts[1]
                result[docId] = Pair(ms, title)
            }
        }
        return result
    }
}
