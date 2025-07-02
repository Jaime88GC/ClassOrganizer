package com.example.classorganizer

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.widget.Toast

class PerfilFragment : Fragment() {

    private lateinit var ivFotoPerfil: ImageView
    private lateinit var tvNombre: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var btnCerrarSesion: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        ivFotoPerfil = view.findViewById(R.id.ivFotoPerfil)
        tvNombre = view.findViewById(R.id.tvNombre)
        tvCorreo = view.findViewById(R.id.tvCorreo)
        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion)

        // Datos del usuario
        val prefs = requireContext().getSharedPreferences("usuario", 0)
        val nombre = prefs.getString("nombre", "Estudiante")
        val correo = prefs.getString("correo", "estudiante@example.com")

        tvNombre.text = nombre
        tvCorreo.text = correo

        btnCerrarSesion.setOnClickListener {
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            requireActivity().finishAffinity() // Cierra la app completamente
        }


        return view
    }
}
