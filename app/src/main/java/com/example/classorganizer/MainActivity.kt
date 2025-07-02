package com.example.classorganizer

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var btnInicio: ImageView
    private lateinit var btnBuscar: ImageView
    private lateinit var btnCalendario: ImageView
    private lateinit var btnNotificaciones: ImageView
    private lateinit var btnPerfil: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnInicio = findViewById(R.id.btnInicio)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnCalendario = findViewById(R.id.btnCalendario)
        btnNotificaciones = findViewById(R.id.btnNotificaciones)
        btnPerfil = findViewById(R.id.btnPerfil)

        // Fragmento por defecto
        cargarFragmento(InicioFragment())
        actualizarIconos("inicio")

        btnInicio.setOnClickListener {
            cargarFragmento(InicioFragment())
            actualizarIconos("inicio")
        }

        btnBuscar.setOnClickListener {
            cargarFragmento(BuscarFragment())
            actualizarIconos("buscar")
        }

        btnCalendario.setOnClickListener {
            cargarFragmento(CalendarioFragment())
            actualizarIconos("calendario")
        }

        btnNotificaciones.setOnClickListener {
            cargarFragmento(NotificacionesFragment())
            actualizarIconos("notificaciones")
        }

        btnPerfil.setOnClickListener {
            cargarFragmento(PerfilFragment())
            actualizarIconos("perfil")
        }
    }

    private fun cargarFragmento(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedorFragmentos, fragment)
            .commit()
    }

    private fun actualizarIconos(seleccionado: String) {
        btnInicio.setImageResource(if (seleccionado == "inicio") R.drawable.ic_home_selected else R.drawable.ic_home)
        btnBuscar.setImageResource(if (seleccionado == "buscar") R.drawable.ic_search_selected else R.drawable.ic_search)
        btnCalendario.setImageResource(if (seleccionado == "calendario") R.drawable.ic_calendar_selected else R.drawable.ic_calendar)
        btnNotificaciones.setImageResource(if (seleccionado == "notificaciones") R.drawable.ic_notification_selected else R.drawable.ic_notification)
        btnPerfil.setImageResource(if (seleccionado == "perfil") R.drawable.ic_profile_selected else R.drawable.ic_profile)
    }
}
