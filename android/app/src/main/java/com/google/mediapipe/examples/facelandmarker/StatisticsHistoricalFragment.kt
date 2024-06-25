package com.google.mediapipe.examples.facelandmarker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import org.json.JSONObject

class StatisticsHistoricalFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics_historical, container, false)

        // Example of how to load JSON data
        val jsonString = loadJSONFromAsset("info_boxes.json")
        if (jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            val jsonArray = jsonObject.getJSONArray("viaje")

            var totalHoras = 0
            var totalMinutos = 0
            var totalViajes = 0
            var totalAlertasFatiga = 0
            var totalAlertasSomnolencia = 0
            var totalDescansos = 0
            var totalMinutosDescanso = 0

            for (i in 0 until jsonArray.length()) {
                totalViajes += 1
                val duracionObject = jsonArray.getJSONObject(i).getJSONObject("Duracion")
                totalHoras += duracionObject.getInt("horas")
                totalMinutos += duracionObject.getInt("minutos")
                totalAlertasFatiga += jsonArray.getJSONObject(i).getInt("CantidadDeAlertasFatiga")
                totalAlertasSomnolencia += jsonArray.getJSONObject(i).getInt("CantidadDeAlertasSomnolencia")
                totalDescansos += jsonArray.getJSONObject(i).getInt("CantidadDeDescansosTotales")
                totalMinutosDescanso += jsonArray.getJSONObject(i).getInt("MinutosTotalesDeDescanso")
            }

            val totalMinutosConvertidos = totalHoras * 60 + totalMinutos
            val totalHorasCalculadas = totalMinutosConvertidos / 60
            val totalMinutosRestantes = totalMinutosConvertidos % 60

            val promedioMinutos = totalMinutosConvertidos / jsonArray.length()
            val promedioHoras = promedioMinutos / 60
            val promedioMinutosRestantes = promedioMinutos % 60

            val promedioFatiga = totalAlertasFatiga / jsonArray.length()
            val promedioSomnolencia = totalAlertasSomnolencia / jsonArray.length()
            val promedioDescansos = totalDescansos / jsonArray.length()
            val promedioMinutosDescanso = totalMinutosDescanso / jsonArray.length()

            val cantidadViajes: TextView = view.findViewById(R.id.tv_totalViajes)
            cantidadViajes.text = totalViajes.toString()

            val duracionTotal: TextView = view.findViewById(R.id.tv_duracion_total)
            duracionTotal.text = String.format("%02d:%02d", totalHorasCalculadas, totalMinutosRestantes)

            val duracionPromedio: TextView = view.findViewById(R.id.tv_duracion_promedio)
            duracionPromedio.text = String.format("%02d:%02d", promedioHoras, promedioMinutosRestantes)

            val fatigaPromedio: TextView = view.findViewById(R.id.tv_promedioFatiga)
            fatigaPromedio.text = promedioFatiga.toString()

            val somnolenciaPromedio: TextView = view.findViewById(R.id.tv_promedioSomnolencia)
            somnolenciaPromedio.text = promedioSomnolencia.toString()

            val descansosPromedio: TextView = view.findViewById(R.id.tv_promedioDescansos)
            descansosPromedio.text = promedioDescansos.toString()

            val minutosDescansoPromedio: TextView = view.findViewById(R.id.tv_promedioMinDescanso)
            minutosDescansoPromedio.text = promedioMinutosDescanso.toString()

            // Nueva funcionalidad: Configurar gráfico circular y evaluación del criterio de descanso
            val pieChartDescansos: PieChart = view.findViewById(R.id.pieChartDescansos)
            setupPieChart(pieChartDescansos, totalMinutosConvertidos, totalMinutosDescanso)

            // Evaluar criterio de descanso y mostrar mensaje
            val totalIntervalos2Horas = totalMinutosConvertidos / 120

            val criterioCumplido = totalMinutosDescanso >= totalIntervalos2Horas * 15
            val criterioIntermedio = totalMinutosDescanso >= totalIntervalos2Horas * 10 && totalMinutosDescanso <= totalIntervalos2Horas * 15
            val mensajeRecomendacion: TextView = view.findViewById(R.id.tv_recomendacion)
            val cardRecomendacion: CardView = view.findViewById(R.id.card_recomendacion)

            val promedioDescansoCada2Horas = totalMinutosDescanso / totalIntervalos2Horas

            val colorSuccess = ContextCompat.getColor(requireContext(), R.color.default_color)
            val colorWarning = ContextCompat.getColor(requireContext(), R.color.popup_tired)
            val colorDanger = ContextCompat.getColor(requireContext(), R.color.popup_sleep)

            if (criterioCumplido) {
                mensajeRecomendacion.text = "¡Excelente!\n\nDescanso promedio cada 2 horas: ${promedioDescansoCada2Horas} min."
                cardRecomendacion.setCardBackgroundColor(colorSuccess)
            } else if (criterioIntermedio) {
                mensajeRecomendacion.text = "¡Vas por buen camino!\n\nDescanso promedio cada 2 horas: ${promedioDescansoCada2Horas} min."
                cardRecomendacion.setCardBackgroundColor(colorWarning)
            } else {
                mensajeRecomendacion.text = "Alerta\n\nDescanso promedio cada 2 horas: ${promedioDescansoCada2Horas} min."
                cardRecomendacion.setCardBackgroundColor(colorDanger)
            }


        }

        return view
    }

    private fun loadJSONFromAsset(filename: String): String? {
        return context?.assets?.open(filename)?.bufferedReader().use { it?.readText() }
    }

    private fun setupPieChart(pieChart: PieChart, totalViajeMinutos: Int, totalDescansoMinutos: Int) {

        // Convertir totalDescansoMinutos a horas y minutos
        val totalDescansoHoras = totalDescansoMinutos / 60
        val totalDescansoMinRestantes = totalDescansoMinutos % 60

        // Calcular el tiempo efectivo (diferencia entre el tiempo de viaje y el tiempo de descanso)
        val tiempoEfectivoMinutos = totalViajeMinutos - totalDescansoMinutos

        // Ajustar las horas y minutos para que estén en el rango adecuado
        val tiempoEfectivoHoras = tiempoEfectivoMinutos / 60
        val tiempoEfectivoMinutosRestantes = tiempoEfectivoMinutos % 60

        val totalViajeFormatted = String.format("%02d:%02d", tiempoEfectivoHoras, tiempoEfectivoMinutosRestantes)
        val totalDescansoFormatted = String.format("%02d:%02d", totalDescansoHoras, totalDescansoMinRestantes)

        val entries = listOf(
            PieEntry(totalViajeMinutos.toFloat()-totalDescansoMinutos.toFloat(), totalViajeFormatted),
            PieEntry(totalDescansoMinutos.toFloat(), totalDescansoFormatted)
        )
        val dataSet = PieDataSet(entries, "")

        // Obtener colores desde los recursos
        val colorGeneralPie = ContextCompat.getColor(requireContext(), R.color.gray)
        val colorSuccessPie = ContextCompat.getColor(requireContext(), R.color.default_color)

        dataSet.colors = listOf(colorGeneralPie, colorSuccessPie)

        val data = PieData(dataSet)
        data.setDrawValues(false) // Oculta los valores dentro del gráfico circular

        pieChart.data = data
        pieChart.description.isEnabled = false // Deshabilita la descripción del gráfico

        // Configuración de la leyenda
        val legend = pieChart.legend
        legend.isEnabled = true // Habilita la leyenda del gráfico
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false) // Asegura que la leyenda no se dibuje dentro del gráfico

        // Configurar etiquetas personalizadas para la leyenda
        val label1 = LegendEntry()
        label1.label = "Total Viaje"
        label1.formColor = colorGeneralPie // Color correspondiente al segmento

        val label2 = LegendEntry()
        label2.label = "Total Descanso"
        label2.formColor = colorSuccessPie // Color correspondiente al segmento

        legend.setCustom(listOf(label1, label2))

        pieChart.invalidate()
    }




}
