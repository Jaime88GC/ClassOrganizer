package com.example.classorganizer

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.classorganizer.databinding.FragmentPerfilBinding
import java.util.*

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: AdminSQLite

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        val view = binding.root

        dbHelper = AdminSQLite(requireContext())

        // Datos del usuario desde preferencias
        val prefs = requireContext().getSharedPreferences("usuario", 0)
        val nombre = prefs.getString("nombre", "Estudiante")
        val correo = prefs.getString("correo", "estudiante@example.com")

        binding.tvNombre.text = nombre
        binding.tvCorreo.text = correo

        // Cargar estadísticas
        cargarEstadisticas()

        binding.btnCerrarSesion.setOnClickListener {
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            requireActivity().finishAffinity()
        }

        return view
    }

    private fun cargarEstadisticas() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT completada, fechaHora FROM actividades", null)

        var pendientes = 0
        var completadas = 0
        var vencidas = 0
        var totales = 0

        val ahora = Date()

        if (cursor.moveToFirst()) {
            do {
                val completada = cursor.getInt(0) == 1
                val fechaHora = cursor.getString(1)
                totales++

                val fecha = parseFechaFlexible(fechaHora)

                if (completada) {
                    completadas++
                    android.util.Log.d("PerfilFragment", "Completada: $fechaHora ✅")
                } else if (fecha == null) {
                    pendientes++
                    android.util.Log.w("PerfilFragment", "Fecha nula o malformada: $fechaHora 🤷")
                } else if (fecha.before(ahora)) {
                    vencidas++
                    android.util.Log.d("PerfilFragment", "Vencida: $fechaHora ⏰")
                } else {
                    pendientes++
                    android.util.Log.d("PerfilFragment", "Pendiente: $fechaHora 📝")
                }

            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        binding.tvTareasPendientes.text = "Tareas pendientes: $pendientes"
        binding.tvTareasCompletadas.text = "Tareas completadas: $completadas"
        binding.tvTareasVencidas.text = "Tareas vencidas: $vencidas"
        binding.tvTareasTotales.text = "Total de tareas: $totales"
    }

    /**
     * Intenta parsear la fecha con segundos o sin segundos.
     */
    private fun parseFechaFlexible(fechaHora: String): Date? {
        val formatos = listOf(
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm"
        )
        for (formato in formatos) {
            try {
                val sdf = java.text.SimpleDateFormat(formato, Locale.getDefault())
                return sdf.parse(fechaHora)
            } catch (_: Exception) {
                // Intenta siguiente formato
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
