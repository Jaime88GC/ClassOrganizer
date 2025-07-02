package com.example.classorganizer

import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class CalendarioFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var contenedorLista: LinearLayout
    private lateinit var dbHelper: AdminSQLite
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendario, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        contenedorLista = view.findViewById(R.id.contenedorLista)
        dbHelper = AdminSQLite(requireContext())

        // Mostrar actividades del día actual al abrir
        val fechaActual = formatoFecha.format(Date())
        mostrarActividadesDelDia(fechaActual)

        // Escuchar cambios en el calendario
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val fechaSeleccionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            mostrarActividadesDelDia(fechaSeleccionada)
        }

        return view
    }

    private fun mostrarActividadesDelDia(fechaSeleccionada: String) {
        contenedorLista.removeAllViews()

        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM actividades", null)

        if (cursor.moveToFirst()) {
            do {
                val titulo = cursor.getString(cursor.getColumnIndexOrThrow("titulo"))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
                val fechaHora = cursor.getString(cursor.getColumnIndexOrThrow("fechaHora"))
                val completada = cursor.getInt(cursor.getColumnIndexOrThrow("completada")) == 1

                // Extraer solo la fecha (sin hora)
                val soloFecha = fechaHora.split(" ")[0]

                if (soloFecha == fechaSeleccionada) {
                    val item = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(if (completada) Color.parseColor("#B0BEC5") else Color.parseColor("#A5D6A7"))
                        setPadding(16, 16, 16, 16)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 0, 0, 24)
                        layoutParams = params
                    }

                    val texto = TextView(requireContext()).apply {
                        text = "📌 $titulo\n$descripcion\n🕒 $fechaHora"
                        textSize = 16f
                        setTextColor(Color.BLACK)
                    }

                    item.addView(texto)
                    contenedorLista.addView(item)
                }

            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        if (contenedorLista.childCount == 0) {
            val vacio = TextView(requireContext()).apply {
                text = "No hay actividades para esta fecha."
                setTextColor(Color.DKGRAY)
                textSize = 16f
            }
            contenedorLista.addView(vacio)
        }
    }
}
