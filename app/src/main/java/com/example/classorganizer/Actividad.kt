package com.example.classorganizer

data class Actividad(
        val id: Int,
        val titulo: String,
        val descripcion: String,
        val fechaHora: String,
        val completada: Boolean
)
