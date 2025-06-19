package com.example.classorganizer

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private lateinit var contenedorActividades: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Crear canal de notificación para Android 8.0 y versiones superiores
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "channel_id"  // Identificador único del canal
            val channelName = "Tareas"    // Nombre del canal de notificación
            val importance = NotificationManager.IMPORTANCE_DEFAULT  // Prioridad de la notificación
            val channel = NotificationChannel(channelId, channelName, importance)

            // Crear el canal de notificación
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Solicitar el permiso de notificación para Android 13 o superior
        val permission = android.Manifest.permission.POST_NOTIFICATIONS
        val permissionStatus = ContextCompat.checkSelfPermission(this, permission)

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            // Si no tiene permiso, solicita permiso
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
        }

        contenedorActividades = findViewById(R.id.contenedorActividades)
        val btnCrearActividad = findViewById<ImageButton>(R.id.btnNuevaActividad)
        val btnBuscar = findViewById<ImageView>(R.id.btnBuscar)

        // Llamar a cargar actividades guardadas cuando se inicia la app
        cargarActividadesGuardadas()

        btnCrearActividad.setOnClickListener {
            val intent = Intent(this, NuevaActividadActivity::class.java)
            startActivityForResult(intent, 100)
        }

        btnBuscar.setOnClickListener {
            val intent = Intent(this, BuscarActividadActivity::class.java)
            startActivity(intent)
        }
    }

    // Este método maneja la respuesta del permiso de notificación
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes enviar notificaciones
                enviarNotificacion("Título", "Mensaje de notificación")
            } else {
                // Permiso denegado, maneja el caso
                Toast.makeText(this, "El permiso para mostrar notificaciones es necesario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val titulo = data.getStringExtra("titulo") ?: "Sin título"
            val descripcion = data.getStringExtra("descripcion") ?: ""
            val fechaHora = data.getStringExtra("fecha") ?: ""

            // Actualizamos la lista de actividades después de agregar una nueva actividad
            agregarActividadEnPantalla(titulo, descripcion, fechaHora)

            // Guardar la actividad en SharedPreferences
            val sharedPreferences = getSharedPreferences("actividades", MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            val actividad = "$titulo|$descripcion|$fechaHora"
            val lista = sharedPreferences.getStringSet("lista", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            lista.add(actividad)

            editor.putStringSet("lista", lista)
            editor.apply()
        }
    }

    private fun agregarActividadEnPantalla(titulo: String, descripcion: String, fechaHoraStr: String) {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        formato.timeZone = TimeZone.getDefault()
        val ahora = Date()
        var colorFondo = Color.parseColor("#A5D6A7")

        try {
            val fechaActividad = formato.parse(fechaHoraStr)
            val diferencia = fechaActividad.time - ahora.time
            colorFondo = when {
                diferencia <= 0 -> { // Rojo: La actividad ya ha pasado
                    enviarNotificacion("¡Actividad vencida!", "La actividad '$titulo' ha vencido.")
                    Color.parseColor("#EF9A9A")
                }
                diferencia <= 2 * 60 * 60 * 1000 -> { // Amarillo: Menos de 2 horas
                    enviarNotificacion("¡Actividad urgente!", "Te queda poco tiempo para hacer la actividad '$titulo'.")
                    Color.parseColor("#FFF59D")
                }
                else -> Color.parseColor("#A5D6A7")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(colorFondo)
            setPadding(16, 16, 16, 16)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
        }

        val texto = TextView(this).apply {
            text = "📌 $titulo\n$descripcion\n📅 $fechaHoraStr"
            setTextColor(Color.BLACK)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val botonEliminar = TextView(this).apply {
            text = "❌"
            textSize = 18f
            setTextColor(Color.RED)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("¿Eliminar actividad?")
                    .setMessage("¿Estás seguro de que deseas eliminar esta actividad?")
                    .setPositiveButton("Sí") { _, _ ->
                        contenedorActividades.removeView(contenedor)
                        eliminarActividadDeMemoria(titulo, descripcion, fechaHoraStr)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        contenedor.addView(texto)
        contenedor.addView(botonEliminar)
        contenedorActividades.addView(contenedor)
    }

    private fun eliminarActividadDeMemoria(titulo: String, descripcion: String, fechaHora: String) {
        val actividad = "$titulo|$descripcion|$fechaHora"
        val prefs = getSharedPreferences("actividades", MODE_PRIVATE)
        val lista = prefs.getStringSet("lista", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        lista.remove(actividad)
        prefs.edit().putStringSet("lista", lista).apply()
    }

    private fun cargarActividadesGuardadas() {
        val prefs = getSharedPreferences("actividades", MODE_PRIVATE)
        val lista = prefs.getStringSet("lista", setOf()) ?: setOf()

        for (actividad in lista) {
            val partes = actividad.split("|")
            if (partes.size == 3) {
                val titulo = partes[0]
                val descripcion = partes[1]
                val fechaHora = partes[2]
                agregarActividadEnPantalla(titulo, descripcion, fechaHora)
            }
        }
    }

    private fun enviarNotificacion(titulo: String, mensaje: String) {
        // Generar un ID único de notificación utilizando el hashCode del título
        val notificationId = titulo.hashCode()  // Usar hashCode del título como ID único

        val notificationManager = getSystemService(NotificationManager::class.java)

        val notification = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setSmallIcon(R.drawable.ic_notification) // Cambia esto por tu ícono
            .build()

        notificationManager.notify(notificationId, notification)  // Usar el ID único
    }

}
