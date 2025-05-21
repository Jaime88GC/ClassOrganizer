package com.example.classorganizer

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class BuscarActividadActivity : Activity() {

    private lateinit var campoBusqueda: EditText
    private lateinit var contenedorResultados: LinearLayout
    private var actividades: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_actividad)

        campoBusqueda = findViewById(R.id.campoBusqueda)
        contenedorResultados = findViewById(R.id.contenedorResultados)

        val prefs = getSharedPreferences("actividades", MODE_PRIVATE)
        actividades = prefs.getStringSet("lista", setOf())?.toList() ?: listOf()

        mostrarResultadosFiltrados("")

        campoBusqueda.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mostrarResultadosFiltrados(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun mostrarResultadosFiltrados(filtro: String) {
        contenedorResultados.removeAllViews()

        val resultados = actividades.filter {
            it.lowercase().contains(filtro.lowercase())
        }

        if (resultados.isEmpty()) {
            val sinResultado = TextView(this).apply {
                text = "No se encontraron actividades."
                setTextColor(Color.DKGRAY)
                textSize = 16f
            }
            contenedorResultados.addView(sinResultado)
        } else {
            for (actividad in resultados) {
                val partes = actividad.split("|")
                if (partes.size != 3) continue

                val titulo = partes[0]
                val descripcion = partes[1]
                val fechaHora = partes[2]

                val colorFondo = calcularColorPorFecha(fechaHora)

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
                    text = "📌 $titulo\n$descripcion\n📅 $fechaHora"
                    setTextColor(Color.BLACK)
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val botonEliminar = TextView(this).apply {
                    text = "❌"
                    textSize = 18f
                    setTextColor(Color.RED)
                    setOnClickListener {
                        AlertDialog.Builder(this@BuscarActividadActivity)
                            .setTitle("¿Eliminar actividad?")
                            .setMessage("¿Estás seguro de que deseas eliminar esta actividad?")
                            .setPositiveButton("Sí") { _, _ ->
                                contenedorResultados.removeView(contenedor)
                                eliminarActividadDeMemoria(titulo, descripcion, fechaHora)
                                actividades = actividades.filterNot { it == "$titulo|$descripcion|$fechaHora" }
                                mostrarResultadosFiltrados(campoBusqueda.text.toString())
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }

                contenedor.addView(texto)
                contenedor.addView(botonEliminar)
                contenedorResultados.addView(contenedor)
            }
        }
    }

    private fun calcularColorPorFecha(fechaHoraStr: String): Int {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        formato.timeZone = TimeZone.getDefault()
        val ahora = Date()

        return try {
            val fechaActividad = formato.parse(fechaHoraStr)
            val diferencia = fechaActividad.time - ahora.time

            when {
                diferencia <= 0 -> Color.parseColor("#EF9A9A")
                diferencia <= 2 * 60 * 60 * 1000 -> Color.parseColor("#FFF59D")
                else -> Color.parseColor("#A5D6A7")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Color.LTGRAY
        }
    }

    private fun eliminarActividadDeMemoria(titulo: String, descripcion: String, fechaHora: String) {
        val actividad = "$titulo|$descripcion|$fechaHora"
        val prefs = getSharedPreferences("actividades", MODE_PRIVATE)
        val lista = prefs.getStringSet("lista", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        lista.remove(actividad)
        prefs.edit().putStringSet("lista", lista).apply()
    }
}
