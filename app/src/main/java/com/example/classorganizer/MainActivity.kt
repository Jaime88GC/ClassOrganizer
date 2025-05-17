package com.example.classorganizer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var contenedorActividades: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contenedorActividades = findViewById(R.id.contenedorActividades)
        val btnCrearActividad = findViewById<ImageButton>(R.id.btnNuevaActividad)

        btnCrearActividad.setOnClickListener {
            val intent = Intent(this, NuevaActividadActivity::class.java)
            startActivityForResult(intent, 100)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val titulo = data.getStringExtra("titulo") ?: "Sin título"
            val descripcion = data.getStringExtra("descripcion") ?: ""
            val fecha = data.getStringExtra("fecha") ?: ""

            val nuevaVista = TextView(this).apply {
                text = "📌 $titulo\n$descripcion\n📅 $fecha"
                setPadding(24, 24, 24, 24)
                setBackgroundColor(Color.DKGRAY)
                setTextColor(Color.WHITE)
                textSize = 16f
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 24)
                layoutParams = params
            }

            contenedorActividades.addView(nuevaVista)
        }
    }
}