package com.example.classorganizer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AdminSQLite(context: Context) :
SQLiteOpenHelper(context, "db_actividades", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        // Crear tabla actividades con id autoincrement, titulo, descripcion, fecha, completada (0 o 1)
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
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS actividades")
        onCreate(db)
    }
}
