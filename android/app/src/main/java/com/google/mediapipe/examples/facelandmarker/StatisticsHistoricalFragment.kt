package com.google.mediapipe.examples.facelandmarker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
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

        }

        return view
    }

    private fun loadJSONFromAsset(filename: String): String? {
        return context?.assets?.open(filename)?.bufferedReader().use { it?.readText() }
    }
}