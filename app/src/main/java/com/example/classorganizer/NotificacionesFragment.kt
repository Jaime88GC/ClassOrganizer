package com.example.classorganizer

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classorganizer.databinding.FragmentNotificacionesBinding

class NotificacionesFragment : Fragment() {

    private var _binding: FragmentNotificacionesBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: AdminSQLite
    private lateinit var adapter: NotificacionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificacionesBinding.inflate(inflater, container, false)

        dbHelper = AdminSQLite(requireContext())
        val notificaciones = dbHelper.obtenerNotificaciones().toMutableList()

        val formatoFecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())

        // Ordenar por fecha DESCENDENTE (más reciente primero)
        notificaciones.sortByDescending {
            formatoFecha.parse(it.third) ?: java.util.Date(0)
        }

        adapter = NotificacionAdapter(notificaciones) { index ->
            confirmarEliminar(index)
        }

        binding.recyclerNotificaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotificaciones.adapter = adapter

        return binding.root
    }

    private fun confirmarEliminar(index: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar notificación")
            .setMessage("¿Seguro que quieres eliminar esta notificación?")
            .setPositiveButton("Sí") { _, _ ->
                eliminarNotificacion(index)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun eliminarNotificacion(index: Int) {
        val noti = adapter.notificaciones[index]
        val id = noti.first
        dbHelper.borrarNotificacionPorId(id)
        adapter.notificaciones.removeAt(index)
        adapter.notifyItemRemoved(index)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



