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

            // LineChart
            val lineChart: LineChart = view.findViewById(R.id.lineChart)
            setupLineChart(lineChart, ultimoViaje)
        }

        return view
    }

    private fun setupLineChart(lineChart: LineChart, viaje: JSONObject) {
        // Disable all interactions
        lineChart.setTouchEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.isDragEnabled = false

        val entriesFatiga = ArrayList<Entry>()
        val entriesSomnolencia = ArrayList<Entry>()

        val alertasFatiga = viaje.getJSONArray("RegistroDeAlertasFatiga")
        val alertasSomnolencia = viaje.getJSONArray("RegistroDeAlertasSomnolencia")

        // Initialize counters for fatiga and somnolencia
        var countFatiga = 0
        var countSomnolencia = 0

        // Populate entries for fatiga (yellow)
        for (i in 0 until alertasFatiga.length()) {
            val time = alertasFatiga.getString(i).split(":")
            val hours = time[0].toFloat()
            val minutes = time[1].toFloat()
            val totalHours = hours + minutes / 60
            countFatiga++
            entriesFatiga.add(Entry(totalHours, i.toFloat())) // Use countFatiga to indicate a fatigue event
        }

        // Populate entries for somnolencia (red)
        for (i in 0 until alertasSomnolencia.length()) {
            val time = alertasSomnolencia.getString(i).split(":")
            val hours = time[0].toFloat()
            val minutes = time[1].toFloat()
            val totalHours = hours + minutes / 60
            countSomnolencia++
            entriesSomnolencia.add(Entry(totalHours, i.toFloat())) // Use countSomnolencia to indicate a drowsiness event
        }

        // Create DataSet for entries
        val dataSetFatiga = LineDataSet(entriesFatiga, "Alertas de Fatiga")
        dataSetFatiga.color = ContextCompat.getColor(requireContext(), R.color.popup_tired)
        dataSetFatiga.setCircleColor(ContextCompat.getColor(requireContext(), R.color.popup_tired))
        dataSetFatiga.lineWidth = 3f
        dataSetFatiga.circleRadius = 5f
        dataSetFatiga.setDrawCircleHole(false)
        dataSetFatiga.valueTextSize = 10f
        dataSetFatiga.setDrawValues(false)

        val dataSetSomnolencia = LineDataSet(entriesSomnolencia, "Alertas de Somnolencia")
        dataSetSomnolencia.color = ContextCompat.getColor(requireContext(), R.color.popup_sleep)
        dataSetSomnolencia.setCircleColor(ContextCompat.getColor(requireContext(), R.color.popup_sleep))
        dataSetSomnolencia.lineWidth = 3f
        dataSetSomnolencia.circleRadius = 5f
        dataSetSomnolencia.setDrawCircleHole(false)
        dataSetSomnolencia.valueTextSize = 10f
        dataSetSomnolencia.setDrawValues(false)

        // Combine both data sets
        val lineData = LineData(dataSetFatiga, dataSetSomnolencia)
        lineChart.data = lineData
        lineChart.setBackgroundColor(Color.parseColor("#00FFFFFF"))
        lineChart.setDrawGridBackground(false)

        // Configure X axis
        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = HourAxisValueFormatter() // Formateador personalizado para convertir valores flotantes a "HH:mm"
        xAxis.granularity = 2f // Establecer la granularidad en 2 horas
        xAxis.axisMinimum = 0f // Comenzar en cero

        // Sumar 2 horas extras a la duración del viaje
        val duracionHoras = viaje.getJSONObject("Duracion").getInt("horas")
        val duracionMinutos = viaje.getJSONObject("Duracion").getInt("minutos")
        val duracionTotalHoras = duracionHoras + 2 // Sumar 2 horas extras al viaje

        // Calcular el eje máximo para extender más allá de la duración real del viaje
        val duracionTotalFloat = duracionTotalHoras + (duracionMinutos / 60f)
        xAxis.axisMaximum = duracionTotalFloat

        // Alinear correctamente los puntos con las horas
        val labelCount = (duracionTotalHoras / 2) + 1
        xAxis.setLabelCount(labelCount, true) // Forzar el recuento de etiquetas

        // Configure Y axis
        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.valueFormatter = IntegerAxisValueFormatter() // custom formatter to convert float values to integers
        yAxisLeft.axisMinimum = 0f // start at zero
        val maxCount = max(countFatiga, countSomnolencia)
        yAxisLeft.axisMaximum = maxCount.toFloat() // add 1 to ensure the maximum value is displayed
        yAxisLeft.setDrawLabels(true) // draw labels on the left y-axis
        yAxisLeft.granularity = 1f // set granularity to 1 to force integer values

        val yAxisRight = lineChart.axisRight
        yAxisRight.setDrawLabels(false) // don't draw labels on the right y-axis
        yAxisRight.setDrawGridLines(false)

        // Set description
        val description = Description()
        description.text = "Registros de Fatiga y Somnolencia"
        lineChart.description = description
        lineChart.invalidate() // refresh
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
