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
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat

class InicioFragment : Fragment() {

    private lateinit var contenedorActividades: LinearLayout
    private lateinit var dbHelper: AdminSQLite
    private var actividadesEnMemoria = mutableListOf<Actividad>()

    private lateinit var btnTodas: Button
    private lateinit var btnPendientes: Button
    private lateinit var btnVencidas: Button
    private lateinit var btnCompletadas: Button
    private lateinit var listaBotones: List<Button>

    private var filtroActual: (List<Actividad>) -> List<Actividad> = { it }

    private val nuevaActividadLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accion = result.data?.getStringExtra("accion") ?: ""
            val titulo = result.data?.getStringExtra("titulo") ?: ""
            cargarActividadesDesdeDB()
            if (accion.isNotEmpty() && titulo.isNotEmpty() && accion != "agregada") {
                registrarNotificacion("Actividad '$titulo' $accion correctamente.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        contenedorActividades = view.findViewById(R.id.contenedorActividades)
        dbHelper = AdminSQLite(requireContext())

        btnTodas = view.findViewById(R.id.btnTodas)
        btnPendientes = view.findViewById(R.id.btnPendientes)
        btnVencidas = view.findViewById(R.id.btnVencidas)
        btnCompletadas = view.findViewById(R.id.btnCompletadas)
        listaBotones = listOf(btnTodas, btnPendientes, btnVencidas, btnCompletadas)

        btnTodas.setOnClickListener {
            filtroActual = { it }
            mostrarActividades(filtroActual(actividadesEnMemoria))
            actualizarEstiloBotonesFiltro(btnTodas, listaBotones)
        }

        btnPendientes.setOnClickListener {
            filtroActual = { it.filter { a -> !a.completada && !esVencida(a.fechaHora) } }
            mostrarActividades(filtroActual(actividadesEnMemoria))
            actualizarEstiloBotonesFiltro(btnPendientes, listaBotones)
        }

        btnVencidas.setOnClickListener {
            filtroActual = { it.filter { a -> !a.completada && esVencida(a.fechaHora) } }
            mostrarActividades(filtroActual(actividadesEnMemoria))
            actualizarEstiloBotonesFiltro(btnVencidas, listaBotones)
        }

        btnCompletadas.setOnClickListener {
            filtroActual = { it.filter { a -> a.completada } }
            mostrarActividades(filtroActual(actividadesEnMemoria))
            actualizarEstiloBotonesFiltro(btnCompletadas, listaBotones)
        }

        val btnCrearActividad = view.findViewById<ImageButton>(R.id.btnNuevaActividad)
        btnCrearActividad.setOnClickListener {
            val intent = Intent(requireContext(), NuevaActividadActivity::class.java)
            nuevaActividadLauncher.launch(intent)
        }

        filtroActual = { it } // por defecto mostrar todas
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

        mostrarActividades(filtroActual(actividadesEnMemoria))
    }

    private fun mostrarActividades(lista: List<Actividad>) {
        contenedorActividades.removeAllViews()

        if (lista.isEmpty()) {
            val mensajeVacio = TextView(requireContext()).apply {
                text = "No hay tareas"
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(16, 16, 16, 16)
                gravity = android.view.Gravity.CENTER
            }
            contenedorActividades.addView(mensajeVacio)
            return
        }

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
                val estado = if (nueva.completada) "completada" else "marcada como pendiente"
                registrarNotificacion("Actividad '${nueva.titulo}' $estado.")
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
                        registrarNotificacion("Actividad '${act.titulo}' eliminada.")
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

    private fun registrarNotificacion(mensaje: String) {
        dbHelper.insertarNotificacion(mensaje)
        mostrarNotificacion(mensaje)
    }

    private fun mostrarNotificacion(mensaje: String) {
        val notificationManager = requireContext().getSystemService(NotificationManager::class.java)
        val channelId = "notificaciones_canal"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Class Organizer")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(Random().nextInt(), builder.build())
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
                boton.setBackgroundColor(Color.parseColor("#2196F3")) // azul seleccionado
                boton.setTextColor(Color.WHITE)
            } else {
                boton.setBackgroundColor(Color.WHITE) // blanco para no seleccionados
                boton.setTextColor(Color.BLACK)
            }
        }
    }
}



