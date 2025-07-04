package com.example.classorganizer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificacionAdapter(
    val notificaciones: MutableList<Triple<Int, String, String>>,
    private val onEliminar: (Int) -> Unit
) : RecyclerView.Adapter<NotificacionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView = view.findViewById(R.id.tvMensaje)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val btnEliminar: View = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = notificaciones.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (_, mensaje, fecha) = notificaciones[position]
        holder.tvMensaje.text = mensaje
        holder.tvFecha.text = fecha

        holder.btnEliminar.setOnClickListener {
            onEliminar(position)
        }
    }
}
