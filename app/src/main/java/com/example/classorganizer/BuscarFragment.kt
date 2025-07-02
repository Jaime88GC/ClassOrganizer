package com.example.classorganizer

import android.app.AlertDialog
import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class BuscarFragment : Fragment() {

    private lateinit var campoBusqueda: EditText
    private lateinit var contenedorResultados: LinearLayout
    private lateinit var dbHelper: AdminSQLite
    private var actividades = mutableListOf<Actividad>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_buscar, container, false)

        campoBusqueda = view.findViewById(R.id.campoBusqueda)
        contenedorResultados = view.findViewById(R.id.contenedorResultados)
        dbHelper = AdminSQLite(requireContext())

        cargarActividadesDesdeDB()
        mostrarResultadosFiltrados("")

        campoBusqueda.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mostrarResultadosFiltrados(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return view
    }

    private fun cargarActividadesDesdeDB() {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM actividades", null)
        actividades.clear()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val titulo = cursor.getString(cursor.getColumnIndexOrThrow("titulo"))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
                val fecha = cursor.getString(cursor.getColumnIndexOrThrow("fechaHora"))
                val completada = cursor.getInt(cursor.getColumnIndexOrThrow("completada")) == 1
                actividades.add(Actividad(id, titulo, descripcion, fecha, completada))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
    }

    private fun mostrarResultadosFiltrados(filtro: String) {
        contenedorResultados.removeAllViews()

        val resultados = actividades.filter {
            it.titulo.contains(filtro, ignoreCase = true) ||
                    it.descripcion.contains(filtro, ignoreCase = true)
        }

        if (resultados.isEmpty()) {
            val sinResultado = TextView(requireContext()).apply {
                text = "No se encontraron actividades."
                setTextColor(Color.DKGRAY)
                textSize = 16f
            }
            contenedorResultados.addView(sinResultado)
        } else {
            for (act in resultados) {
                val colorFondo = calcularColorPorFecha(act.fechaHora, act.completada)

                val contenedor = LinearLayout(requireContext()).apply {
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

                val texto = TextView(requireContext()).apply {
                    text = "\uD83D\uDCCC ${act.titulo}\n${act.descripcion}\n\uD83D\uDCC5 ${act.fechaHora}"
                    setTextColor(Color.BLACK)
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val botonEliminar = TextView(requireContext()).apply {
                    text = "❌"
                    textSize = 18f
                    setTextColor(Color.RED)
                    setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("¿Eliminar actividad?")
                            .setMessage("¿Estás seguro de que deseas eliminar esta actividad?")
                            .setPositiveButton("Sí") { _, _ ->
                                eliminarActividadDeDB(act.id)
                                cargarActividadesDesdeDB()
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

    private fun calcularColorPorFecha(fechaHoraStr: String, completada: Boolean): Int {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val ahora = Date()

        return try {
            val fechaActividad = formato.parse(fechaHoraStr)
            val diferencia = fechaActividad.time - ahora.time
            when {
                completada -> Color.parseColor("#B0BEC5")
                diferencia <= 0 -> Color.parseColor("#EF9A9A")
                diferencia <= 2 * 60 * 60 * 1000 -> Color.parseColor("#FFF59D")
                else -> Color.parseColor("#A5D6A7")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Color.LTGRAY
        }
    }

    private fun eliminarActividadDeDB(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("actividades", "id=?", arrayOf(id.toString()))
        db.close()
    }
}
