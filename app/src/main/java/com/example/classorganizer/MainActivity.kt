package com.example.classorganizer

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import android.content.pm.PackageManager

class MainActivity : Activity() {

    private lateinit var contenedorActividades: LinearLayout
    private lateinit var dbHelper: AdminSQLite
    private var actividadesEnMemoria = mutableListOf<Actividad>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = AdminSQLite(this)
        contenedorActividades = findViewById(R.id.contenedorActividades)

        val btnCrearActividad = findViewById<ImageButton>(R.id.btnNuevaActividad)
        val btnBuscar = findViewById<ImageView>(R.id.btnBuscar)

        val btnTodas = findViewById<Button>(R.id.btnTodas)
        val btnPendientes = findViewById<Button>(R.id.btnPendientes)
        val btnVencidas = findViewById<Button>(R.id.btnVencidas)
        val btnCompletadas = findViewById<Button>(R.id.btnCompletadas)

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

        // Crear canal de notificaciones para Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("channel_id", "Tareas", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Solicitar permiso para notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
            }
        }

// Después de cargar actividades desde la BD
        cargarActividadesDesdeDB()

// Aquí marcas el botón "Todas" como activo al iniciar la app
        actualizarEstiloBotonesFiltro(btnTodas, listaBotones)

        btnCrearActividad.setOnClickListener {
            val intent = Intent(this, NuevaActividadActivity::class.java)
            startActivityForResult(intent, 100)
        }

        btnBuscar.setOnClickListener {
            val intent = Intent(this, BuscarActividadActivity::class.java)
            startActivity(intent)
        }

    }

    private fun actualizarEstiloBoton(botonActivo: Button, lista: List<Button>) {
        for (btn in lista) {
            btn.setTextColor(if (btn == botonActivo) Color.BLACK else Color.GRAY)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val titulo = data.getStringExtra("titulo") ?: return
            val descripcion = data.getStringExtra("descripcion") ?: ""
            val fecha = data.getStringExtra("fecha") ?: ""

            val actividad = Actividad(0, titulo, descripcion, fecha, false)
            insertarActividad(actividad)
            cargarActividadesDesdeDB()
        }
    }

    private fun insertarActividad(act: Actividad) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("titulo", act.titulo)
            put("descripcion", act.descripcion)
            put("fechaHora", act.fechaHora)
            put("completada", if (act.completada) 1 else 0)
        }
        db.insert("actividades", null, values)
        db.close()
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
            fechaAct != null && fechaAct.before(ahora) -> {
                enviarNotificacion("¡Actividad vencida!", "La actividad '${act.titulo}' ha vencido.")
                Color.parseColor("#EF9A9A")
            }
            fechaAct != null && fechaAct.time - ahora.time < 2 * 60 * 60 * 1000 -> {
                enviarNotificacion("¡Actividad urgente!", "Te queda poco tiempo para: ${act.titulo}")
                Color.parseColor("#FFF59D")
            }
            else -> Color.parseColor("#A5D6A7")
        }

        val contenedor = LinearLayout(this).apply {
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

        val texto = TextView(this).apply {
            text = "📌 ${act.titulo}\n${act.descripcion}\n📅 ${act.fechaHora}"
            textSize = 16f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnCheck = TextView(this).apply {
            text = if (act.completada) "✅" else "☐"
            textSize = 20f
            setPadding(10, 0, 10, 0)
            setOnClickListener {
                val nueva = act.copy(completada = !act.completada)
                actualizarActividad(nueva)
                cargarActividadesDesdeDB()
            }
        }

        val btnDelete = TextView(this).apply {
            text = "❌"
            textSize = 20f
            setTextColor(Color.RED)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
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

        contenedor.addView(texto)
        contenedor.addView(btnCheck)
        contenedor.addView(btnDelete)

        contenedorActividades.addView(contenedor)
    }

    private fun enviarNotificacion(titulo: String, mensaje: String) {
        val builder = NotificationCompat.Builder(this, "channel_id")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(titulo.hashCode(), builder.build())
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
                boton.setBackgroundResource(R.drawable.btn_filtro_activo)
                boton.setTextColor(Color.WHITE)
            } else {
                boton.setBackgroundResource(R.drawable.btn_filtro_inactivo)
                boton.setTextColor(Color.BLACK)
            }
        }
    }

}
