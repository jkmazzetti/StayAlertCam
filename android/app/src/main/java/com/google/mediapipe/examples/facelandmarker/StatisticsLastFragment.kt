package com.google.mediapipe.examples.facelandmarker

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import org.json.JSONObject
import kotlin.math.max

class StatisticsLastFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics_last, container, false)

        // Example of how to load JSON data
        val jsonString = loadJSONFromAsset("info_boxes.json")
        if (jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            val viaje = jsonObject.getJSONArray("viaje")
            val ultimoViaje = viaje.getJSONObject(viaje.length() - 1)

            val duracion: TextView = view.findViewById(R.id.tv_duracion)
            val duracionObject = ultimoViaje.getJSONObject("Duracion")
            val horas = duracionObject.getInt("horas")
            val minutos = duracionObject.getInt("minutos")
            val duracionString = String.format("%02d:%02d", horas, minutos)
            duracion.text = duracionString

            val alertasFatiga: TextView = view.findViewById(R.id.tv_alertas_fatiga)
            alertasFatiga.text = "${ultimoViaje.getInt("CantidadDeAlertasFatiga")}"

            val alertasSomnolencia: TextView = view.findViewById(R.id.tv_alertas_somnolencia)
            alertasSomnolencia.text = "${ultimoViaje.getInt("CantidadDeAlertasSomnolencia")}"

            val cantidadDescansosTotales: TextView =
                view.findViewById(R.id.tv_cantidad_descansos_totales)
            cantidadDescansosTotales.text = "${ultimoViaje.getInt("CantidadDeDescansosTotales")}"

            val minutosTotalesDescanso: TextView =
                view.findViewById(R.id.tv_minutos_totales_descanso)
            minutosTotalesDescanso.text = "${ultimoViaje.getInt("MinutosTotalesDeDescanso")} min"

            val minutosPromedioPorDescanso: TextView =
                view.findViewById(R.id.tv_minutos_promedio_por_descanso)
            minutosPromedioPorDescanso.text = "${ultimoViaje.getInt("MinutosPromedioPorDescanso")} min"

        }

        return view
    }

    private fun loadJSONFromAsset(filename: String): String? {
        return context?.assets?.open(filename)?.bufferedReader().use { it?.readText() }
    }
}

// Custom ValueFormatter for integer values
class IntegerAxisValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return value.toInt().toString()
    }
}
