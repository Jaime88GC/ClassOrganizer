package com.example.classorganizer

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues

import java.util.*

class NuevaActividadActivity : Activity() {

    private var idActividad: Int? = null

    private lateinit var etTitulo: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etFecha: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnVolver: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_actividad)

        // Referencias a los elementos
        etTitulo = findViewById(R.id.etTitulo)
        etDescripcion = findViewById(R.id.etDescripcion)
        etFecha = findViewById(R.id.etFecha)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnVolver = findViewById(R.id.btnVolver)

        // Bloquea escritura manual en fecha
        etFecha.isFocusable = false
        etFecha.isClickable = true

        // Muestra selector al tocar el campo de fecha
        etFecha.setOnClickListener {
            mostrarDateTimePicker()
        }

        // Verificar si hay datos para edición
        idActividad = intent.getIntExtra("id", -1).takeIf { it != -1 }
        val tituloEdit = intent.getStringExtra("titulo")
        val descripcionEdit = intent.getStringExtra("descripcion")
        val fechaEdit = intent.getStringExtra("fechaHora")

        if (idActividad != null) {
            etTitulo.setText(tituloEdit)
            etDescripcion.setText(descripcionEdit)
            etFecha.setText(fechaEdit)
        }

        btnGuardar.setOnClickListener {
            val titulo = etTitulo.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val fecha = etFecha.text.toString().trim()

            if (titulo.isEmpty() || descripcion.isEmpty() || fecha.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbHelper = AdminSQLite(this)
            val db = dbHelper.writableDatabase

            val values = ContentValues().apply {
                put("titulo", titulo)
                put("descripcion", descripcion)
                put("fechaHora", fecha)
                put("completada", 0)
            }

            val success = if (idActividad != null) {
                // Actualizar
                db.update("actividades", values, "id=?", arrayOf(idActividad.toString())) > 0
            } else {
                // Insertar
                val idNuevo = db.insert("actividades", null, values)
                if (idNuevo != -1L) {
                    // Insertar notificación de nueva actividad en la base de datos (con segundos)
                    val mensajeNotificacion = "Nueva actividad agregada: $titulo"
                    dbHelper.insertarNotificacion(mensajeNotificacion)

                    // Mostrar notificación en barra
                    mostrarNotificacion(mensajeNotificacion)
                    true
                } else {
                    false
                }
            }
            db.close()

            if (success) {
                Toast.makeText(this, "Actividad guardada", Toast.LENGTH_SHORT).show()
                val intent = Intent()
                intent.putExtra("accion", if (idActividad != null) "modificada" else "agregada")
                intent.putExtra("titulo", etTitulo.text.toString().trim())
                setResult(Activity.RESULT_OK, intent)
                finish()

            } else {
                Toast.makeText(this, "Error al guardar en la base de datos", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón para regresar sin guardar
        btnVolver.setOnClickListener {
            finish() // Cierra esta actividad y regresa al MainActivity
        }
    }

    private fun mostrarDateTimePicker() {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                val fechaFormateada = String.format(
                    "%02d/%02d/%04d %02d:%02d",
                    dayOfMonth, month + 1, year, hourOfDay, minute
                )
                etFecha.setText(fechaFormateada)
            },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePicker.show()
        },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }

    private fun mostrarNotificacion(mensaje: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "notificaciones_canal"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Notificaciones",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Class Organizer")
            .setContentText(mensaje)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(1, builder.build())
    }
}
