package com.example.classorganizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.classorganizer.databinding.FragmentNotificacionesBinding

class NotificacionesFragment : Fragment() {

    private var _binding: FragmentNotificacionesBinding? = null
    private val binding get() = _binding!!

    private val notificaciones = listOf(
        "Tarea de Matemáticas para mañana",
        "Examen de Historia el viernes",
        "Entrega de proyecto de Ciencias el lunes",
        "Nueva actividad añadida a tu calendario",
        "Recordatorio: revisar apuntes de Química"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificacionesBinding.inflate(inflater, container, false)

        binding.recyclerNotificaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotificaciones.adapter = NotificacionAdapter(notificaciones)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
