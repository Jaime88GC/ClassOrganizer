package com.example.classorganizer

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CalendarioFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var contenedorLista: LinearLayout
    private lateinit var btnExportar: Button
    private lateinit var dbHelper: AdminSQLite
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var actividadesDelDia: MutableList<Actividad> = mutableListOf()

    data class Actividad(
        val titulo: String,
        val descripcion: String,
        val fechaHora: String,
        val completada: Boolean
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendario, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        contenedorLista = view.findViewById(R.id.contenedorLista)
        btnExportar = view.findViewById(R.id.btnExportar)
        dbHelper = AdminSQLite(requireContext())

        val fechaActual = formatoFecha.format(Date())
        mostrarActividadesDelDia(fechaActual)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val fechaSeleccionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            mostrarActividadesDelDia(fechaSeleccionada)
        }

        btnExportar.setOnClickListener {
            if (actividadesDelDia.isNotEmpty()) {
                exportarPDFyJSON(actividadesDelDia)
                Toast.makeText(requireContext(), "Exportación completada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No hay actividades para exportar", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun mostrarActividadesDelDia(fechaSeleccionada: String) {
        contenedorLista.removeAllViews()
        actividadesDelDia.clear()

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM actividades", null)

        if (cursor.moveToFirst()) {
            do {
                val titulo = cursor.getString(cursor.getColumnIndexOrThrow("titulo"))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
                val fechaHora = cursor.getString(cursor.getColumnIndexOrThrow("fechaHora"))
                val completada = cursor.getInt(cursor.getColumnIndexOrThrow("completada")) == 1

                val soloFecha = fechaHora.split(" ")[0]

                if (soloFecha == fechaSeleccionada) {
                    actividadesDelDia.add(Actividad(titulo, descripcion, fechaHora, completada))

                    val item = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL

                        // calcular color
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val ahora = Calendar.getInstance().time
                        val fechaActividad = try {
                            sdf.parse(fechaHora)
                        } catch (e: Exception) {
                            null
                        }

                        val colorFondo = when {
                            completada -> Color.parseColor("#B0BEC5")  // gris
                            fechaActividad != null && fechaActividad.before(ahora) -> Color.parseColor("#EF9A9A")  // rojo
                            else -> Color.parseColor("#A5D6A7")  // verde
                        }

                        setBackgroundColor(colorFondo)
                        setPadding(16, 16, 16, 16)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 0, 0, 24)
                        layoutParams = params
                    }

                    val texto = TextView(requireContext()).apply {
                        text = "📌 $titulo\n$descripcion\n🕒 $fechaHora"
                        textSize = 16f
                        setTextColor(Color.BLACK)
                    }

                    item.addView(texto)
                    contenedorLista.addView(item)
                }

            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        if (contenedorLista.childCount == 0) {
            val vacio = TextView(requireContext()).apply {
                text = "No hay actividades para esta fecha."
                setTextColor(Color.DKGRAY)
                textSize = 16f
            }
            contenedorLista.addView(vacio)
        }
    }


    private fun exportarPDFyJSON(actividades: List<Actividad>) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595  // A4 en puntos
        val pageHeight = 842
        val margin = 40
        val rowHeight = 40
        val headerHeight = 50

        var paginaActual = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, paginaActual).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paintTitle = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val paintHeader = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = Color.WHITE
        }
        val paintCell = Paint().apply {
            textSize = 10f
            color = Color.BLACK
        }
        val paintLine = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1f
        }
        val paintHeaderBackground = Paint().apply {
            color = Color.rgb(63, 81, 181) // azul
        }

        // columnas
        val colTitulo = margin
        val colDescripcion = colTitulo + 120
        val colFecha = colDescripcion + 220
        val colCompletada = colFecha + 120

        fun drawHeader() {
            canvas.drawText(
                if (paginaActual == 1) "Reporte de Actividades"
                else "Reporte de Actividades (continuación)",
                margin.toFloat(),
                margin.toFloat() + 20,
                paintTitle
            )

            val yHeader = margin + 40
            canvas.drawRect(
                margin.toFloat(),
                yHeader.toFloat(),
                (pageWidth - margin).toFloat(),
                (yHeader + headerHeight).toFloat(),
                paintHeaderBackground
            )

            canvas.drawText("Título", colTitulo + 5f, yHeader + 30f, paintHeader)
            canvas.drawText("Descripción", colDescripcion + 5f, yHeader + 30f, paintHeader)
            canvas.drawText("Fecha", colFecha + 5f, yHeader + 30f, paintHeader)
            canvas.drawText("Completada", colCompletada + 5f, yHeader + 30f, paintHeader)

            // líneas verticales
            canvas.drawLine(colTitulo.toFloat(), yHeader.toFloat(), colTitulo.toFloat(), (pageHeight - margin).toFloat(), paintLine)
            canvas.drawLine(colDescripcion.toFloat(), yHeader.toFloat(), colDescripcion.toFloat(), (pageHeight - margin).toFloat(), paintLine)
            canvas.drawLine(colFecha.toFloat(), yHeader.toFloat(), colFecha.toFloat(), (pageHeight - margin).toFloat(), paintLine)
            canvas.drawLine(colCompletada.toFloat(), yHeader.toFloat(), colCompletada.toFloat(), (pageHeight - margin).toFloat(), paintLine)
            canvas.drawLine((pageWidth - margin).toFloat(), yHeader.toFloat(), (pageWidth - margin).toFloat(), (pageHeight - margin).toFloat(), paintLine)

            // línea horizontal bajo header
            canvas.drawLine(
                margin.toFloat(),
                (yHeader + headerHeight).toFloat(),
                (pageWidth - margin).toFloat(),
                (yHeader + headerHeight).toFloat(),
                paintLine
            )
        }

        var yPosition = margin + 40 + headerHeight

        drawHeader()

        actividades.forEachIndexed { index, act ->
            if (yPosition + rowHeight > pageHeight - margin) {
                pdfDocument.finishPage(page)
                paginaActual++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, paginaActual).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = margin + 40 + headerHeight
                drawHeader()
            }

            val yBase = yPosition + rowHeight / 2f + 5

            canvas.drawText(act.titulo.take(20), colTitulo + 5f, yBase, paintCell)
            canvas.drawText(act.descripcion.take(30), colDescripcion + 5f, yBase, paintCell)
            canvas.drawText(act.fechaHora, colFecha + 5f, yBase, paintCell)
            canvas.drawText(if (act.completada) "Sí" else "No", colCompletada + 5f, yBase, paintCell)

            // líneas horizontales
            canvas.drawLine(
                margin.toFloat(),
                (yPosition + rowHeight).toFloat(),
                (pageWidth - margin).toFloat(),
                (yPosition + rowHeight).toFloat(),
                paintLine
            )

            yPosition += rowHeight
        }

        pdfDocument.finishPage(page)

        // Guardar PDF
        val carpeta = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MisExportaciones")
        if (!carpeta.exists()) carpeta.mkdirs()

        val pdfPath = File(carpeta, "actividades_exportadas.pdf")
        val jsonPath = File(carpeta, "actividades_exportadas.json")

        try {
            pdfDocument.writeTo(FileOutputStream(pdfPath))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        // Crear JSON
        val jsonArray = JSONArray()
        for (act in actividades) {
            val obj = JSONObject()
            obj.put("titulo", act.titulo)
            obj.put("descripcion", act.descripcion)
            obj.put("fechaHora", act.fechaHora)
            obj.put("completada", act.completada)
            jsonArray.put(obj)
        }

        val jsonFinal = JSONObject()
        jsonFinal.put("actividades", jsonArray)

        try {
            jsonPath.writeText(jsonFinal.toString(4)) // indentado
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if ((currentLine + word).length > maxChars) {
                lines.add(currentLine.trim())
                currentLine = "$word "
            } else {
                currentLine += "$word "
            }
        }
        if (currentLine.isNotBlank()) lines.add(currentLine.trim())
        return lines
    }
}
