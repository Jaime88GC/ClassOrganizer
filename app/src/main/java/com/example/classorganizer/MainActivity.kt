package com.example.classorganizer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private lateinit var contenedorActividades: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contenedorActividades = findViewById(R.id.contenedorActividades)
        val btnCrearActividad = findViewById<ImageButton>(R.id.btnNuevaActividad)
        val btnBuscar = findViewById<ImageView>(R.id.btnBuscar)

        btnCrearActividad.setOnClickListener {
            val intent = Intent(this, NuevaActividadActivity::class.java)
            startActivityForResult(intent, 100)
        }

        btnBuscar.setOnClickListener {
            val intent = Intent(this, BuscarActividadActivity::class.java)
            startActivity(intent)
        }

        cargarActividadesGuardadas()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val titulo = data.getStringExtra("titulo") ?: "Sin título"
            val descripcion = data.getStringExtra("descripcion") ?: ""
            val fechaHora = data.getStringExtra("fecha") ?: ""

            agregarActividadEnPantalla(titulo, descripcion, fechaHora)

            val actividad = "$titulo|$descripcion|$fechaHora"
            val prefs = getSharedPreferences("actividades", MODE_PRIVATE)
            val lista = prefs.getStringSet("lista", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            lista.add(actividad)
            prefs.edit().putStringSet("lista", lista).apply()
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
                diferencia <= 0 -> Color.parseColor("#EF9A9A")
                diferencia <= 2 * 60 * 60 * 1000 -> Color.parseColor("#FFF59D")
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
}
