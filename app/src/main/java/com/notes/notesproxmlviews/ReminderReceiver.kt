package com.notes.notesproxmlviews

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * BroadcastReceiver que recebe o alarme agendado pelo ReminderScheduler
 * e publica a notificação ao utilizador.
 * Ao clicar na notificação, abre o NoteDetailsActivity com a nota correspondente.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val docId = intent.getStringExtra("docId") ?: return
        val title = intent.getStringExtra("title") ?: context.getString(R.string.reminder_fired)

        // Limpar das SharedPreferences (o lembrete já disparou)
        ReminderScheduler.removePendingReminder(context, docId)

        // Intent para abrir a nota ao clicar na notificação
        val openIntent = Intent(context, NoteDetailsActivity::class.java).apply {
            putExtra("docId", docId)
            // flags para não criar múltiplas instâncias da activity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            docId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir a notificação
        val notification = NotificationCompat.Builder(
            context,
            context.getString(R.string.reminder_notification_channel_id)
        )
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(context.getString(R.string.reminder_fired))
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // remove a notificação ao clicar
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(docId.hashCode(), notification)
    }
}
