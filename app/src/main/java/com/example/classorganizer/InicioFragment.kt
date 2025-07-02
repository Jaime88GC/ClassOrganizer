package com.example.classorganizer

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class InicioFragment : Fragment() {

    private lateinit var contenedorActividades: LinearLayout
    private lateinit var dbHelper: AdminSQLite
    private var actividadesEnMemoria = mutableListOf<Actividad>()

    private val nuevaActividadLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            cargarActividadesDesdeDB()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        contenedorActividades = view.findViewById(R.id.contenedorActividades)
        dbHelper = AdminSQLite(requireContext())

        val btnTodas = view.findViewById<Button>(R.id.btnTodas)
        val btnPendientes = view.findViewById<Button>(R.id.btnPendientes)
        val btnVencidas = view.findViewById<Button>(R.id.btnVencidas)
        val btnCompletadas = view.findViewById<Button>(R.id.btnCompletadas)
        val listaBotones = listOf(btnTodas, btnPendientes, btnVencidas, btnCompletadas)

        btnTodas.setOnClickListener {
            mostrarActividades(actividadesEnMemoria)
            actualizarEstiloBotonesFiltro(btnTodas, listaBotones)
        }

        btnPendientes.setOnClickListener {
            val pendientes = actividadesEnMemoria.filter { !it.completada && !esVencida(it.fechaHora) }
            mostrarActividades(pendientes)
            actualizarEstiloBotonesFiltro(btnPendientes, listaBotones)
        }

        btnVencidas.setOnClickListener {
            val vencidas = actividadesEnMemoria.filter { !it.completada && esVencida(it.fechaHora) }
            mostrarActividades(vencidas)
            actualizarEstiloBotonesFiltro(btnVencidas, listaBotones)
        }

        btnCompletadas.setOnClickListener {
            val completadas = actividadesEnMemoria.filter { it.completada }
            mostrarActividades(completadas)
            actualizarEstiloBotonesFiltro(btnCompletadas, listaBotones)
        }

        val btnCrearActividad = view.findViewById<ImageButton>(R.id.btnNuevaActividad)
        btnCrearActividad.setOnClickListener {
            val intent = Intent(requireContext(), NuevaActividadActivity::class.java)
            nuevaActividadLauncher.launch(intent)
        }

        cargarActividadesDesdeDB()
        actualizarEstiloBotonesFiltro(btnTodas, listaBotones)

        return view
    }

    private fun cargarActividadesDesdeDB() {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM actividades", null)
        actividadesEnMemoria.clear()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val titulo = cursor.getString(cursor.getColumnIndexOrThrow("titulo"))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
                val fecha = cursor.getString(cursor.getColumnIndexOrThrow("fechaHora"))
                val completada = cursor.getInt(cursor.getColumnIndexOrThrow("completada")) == 1
                actividadesEnMemoria.add(Actividad(id, titulo, descripcion, fecha, completada))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        mostrarActividades(actividadesEnMemoria)
    }

    private fun mostrarActividades(lista: List<Actividad>) {
        contenedorActividades.removeAllViews()
        for (actividad in lista) {
            agregarActividadEnPantalla(actividad)
        }
    }

    private fun agregarActividadEnPantalla(act: Actividad) {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val ahora = Date()
        val fechaAct = formato.parse(act.fechaHora)

        val colorFondo = when {
            act.completada -> Color.parseColor("#B0BEC5")
            fechaAct != null && fechaAct.before(ahora) -> Color.parseColor("#EF9A9A")
            fechaAct != null && fechaAct.time - ahora.time < 2 * 60 * 60 * 1000 -> Color.parseColor("#FFF59D")
            else -> Color.parseColor("#A5D6A7")
        }

        val contenedor = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(colorFondo)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
        }

        val texto = TextView(requireContext()).apply {
            text = "\uD83D\uDCCC ${act.titulo}\n${act.descripcion}\n\uD83D\uDCC5 ${act.fechaHora}"
            textSize = 16f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnCheck = TextView(requireContext()).apply {
            text = if (act.completada) "✅" else "☐"
            textSize = 20f
            setPadding(10, 0, 10, 0)
            setOnClickListener {
                val nueva = act.copy(completada = !act.completada)
                actualizarActividad(nueva)
                cargarActividadesDesdeDB()
            }
        }

        val btnDelete = TextView(requireContext()).apply {
            text = "❌"
            textSize = 20f
            setTextColor(Color.RED)
            setOnClickListener {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("¿Eliminar actividad?")
                    .setMessage("¿Seguro que quieres eliminar '${act.titulo}'?")
                    .setPositiveButton("Sí") { _, _ ->
                        eliminarActividad(act.id)
                        cargarActividadesDesdeDB()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        val btnEditar = TextView(requireContext()).apply {
            text = "✏️"
            textSize = 20f
            setPadding(10, 0, 10, 0)
            setOnClickListener {
                val intent = Intent(requireContext(), NuevaActividadActivity::class.java).apply {
                    putExtra("id", act.id)
                    putExtra("titulo", act.titulo)
                    putExtra("descripcion", act.descripcion)
                    putExtra("fechaHora", act.fechaHora)
                }
                nuevaActividadLauncher.launch(intent)
            }
        }


        contenedor.addView(texto)
        contenedor.addView(btnCheck)
        contenedor.addView(btnEditar)
        contenedor.addView(btnDelete)


        contenedorActividades.addView(contenedor)
    }

    private fun actualizarActividad(act: Actividad) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("titulo", act.titulo)
            put("descripcion", act.descripcion)
            put("fechaHora", act.fechaHora)
            put("completada", if (act.completada) 1 else 0)
        }
        db.update("actividades", values, "id=?", arrayOf(act.id.toString()))
        db.close()
    }

    private fun eliminarActividad(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("actividades", "id=?", arrayOf(id.toString()))
        db.close()
    }

    private fun esVencida(fechaStr: String): Boolean {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fecha = formato.parse(fechaStr)
            fecha != null && fecha.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    private fun actualizarEstiloBotonesFiltro(botonActivo: Button, botones: List<Button>) {
        botones.forEach { boton ->
            if (boton == botonActivo) {
                boton.setBackgroundColor(Color.parseColor("#2196F3"))
                boton.setTextColor(Color.WHITE)
            } else {
                boton.setBackgroundColor(Color.parseColor("#DDDDDD"))
                boton.setTextColor(Color.BLACK)
            }
        }
    }
}
