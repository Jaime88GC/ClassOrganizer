package com.example.classorganizer

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class AdminSQLite(context: Context) :
    SQLiteOpenHelper(context, "db_actividades", null, 3) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            """
                CREATE TABLE actividades (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    titulo TEXT,
                    descripcion TEXT,
                    fechaHora TEXT,
                    completada INTEGER DEFAULT 0
                )
            """.trimIndent()
        )

        db?.execSQL(
            """
                CREATE TABLE notificaciones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    mensaje TEXT,
                    fechaHora TEXT
                )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS actividades")
        db?.execSQL("DROP TABLE IF EXISTS notificaciones")
        onCreate(db)
    }

    /**
     * Inserta una notificación en la base, siempre guardando la hora con segundos.
     * Formato: dd/MM/yyyy HH:mm:ss
     */
    fun insertarNotificacion(mensaje: String): Long {
        val fechaHoraActual = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val db = writableDatabase
        val values = ContentValues().apply {
            put("mensaje", mensaje)
            put("fechaHora", fechaHoraActual)
        }
        val id = db.insert("notificaciones", null, values)
        db.close()
        return id
    }

    fun obtenerNotificaciones(): List<Triple<Int, String, String>> {
        val lista = mutableListOf<Triple<Int, String, String>>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, mensaje, fechaHora FROM notificaciones", null)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val mensaje = cursor.getString(1)
            val fechaHora = cursor.getString(2)
            lista.add(Triple(id, mensaje, fechaHora))
        }
        cursor.close()
        return lista
    }

    fun borrarTodasLasNotificaciones() {
        val db = writableDatabase
        db.delete("notificaciones", null, null)
        db.close()
    }

    fun borrarNotificacionPorId(id: Int) {
        val db = writableDatabase
        db.delete("notificaciones", "id = ?", arrayOf(id.toString()))
        db.close()
    }
}
