package com.example.classorganizer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build

class AlarmaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra("titulo") ?: "Actividad terminada"
        val mensaje = intent.getStringExtra("mensaje") ?: "La actividad $titulo ha finalizado."

        val channelId = "canal_actividades"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal para Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones de Actividades",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Pon tu icono aquí
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Se elimina al tocar

        notificationManager.notify(titulo.hashCode(), builder.build())
    }
}
