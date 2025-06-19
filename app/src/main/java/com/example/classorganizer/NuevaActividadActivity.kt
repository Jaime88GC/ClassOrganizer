package com.example.classorganizer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.*

class NuevaActividadActivity : Activity() {

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

        // Botón para guardar la actividad
        btnGuardar.setOnClickListener {
            val titulo = etTitulo.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val fecha = etFecha.text.toString().trim()

            if (titulo.isEmpty() || descripcion.isEmpty() || fecha.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guardar la actividad en SharedPreferences
            val sharedPreferences = getSharedPreferences("actividades", MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // Usamos el título como clave única para cada actividad
            val actividad = "$titulo|$descripcion|$fecha"
            val lista = sharedPreferences.getStringSet("lista", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            lista.add(actividad)

            editor.putStringSet("lista", lista)
            editor.apply()

            // Enviar los datos de vuelta a MainActivity
            val intent = Intent()
            intent.putExtra("titulo", titulo)
            intent.putExtra("descripcion", descripcion)
            intent.putExtra("fecha", fecha)

            setResult(RESULT_OK, intent)
            finish()
        }

        // Botón para regresar sin guardar
        btnVolver.setOnClickListener {
            finish() // Cierra esta actividad y regresa al MainActivity
        }
    }

    private fun mostrarDateTimePicker() {
        val calendar = Calendar.getInstance()

        // DatePicker primero
        val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            // Después de la fecha, lanza el TimePicker
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
}
