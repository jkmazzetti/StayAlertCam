package com.google.mediapipe.examples.facelandmarker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONObject

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
            val viaje = jsonObject.getJSONArray("viaje").getJSONObject(0)

            val duracion: TextView = view.findViewById(R.id.tv_duracion)
            duracion.text = viaje.getString("Duracion")

            val alertasFatiga: TextView = view.findViewById(R.id.tv_alertas_fatiga)
            alertasFatiga.text = "${viaje.getInt("CantidadDeAlertasFatiga")}"

            /*
            val registroAlertasFatiga: TextView = view.findViewById(R.id.tv_registro_alertas_fatiga)
            registroAlertasFatiga.text = "Registro de Alertas de Fatiga: ${viaje.getJSONArray("RegistroDeAlertasFatiga").joinToString(", ")}"
            */

            val alertasSomnolencia: TextView = view.findViewById(R.id.tv_alertas_somnolencia)
            alertasSomnolencia.text = "${viaje.getInt("CantidadDeAlertasSomnolencia")}"

            /*
            val registroAlertasSomnolencia: TextView = view.findViewById(R.id.tv_registro_alertas_somnolencia)
            registroAlertasSomnolencia.text = "Registro de Alertas de Somnolencia: ${viaje.getJSONArray("RegistroDeAlertasSomnolencia").joinToString(", ")}"
             */

            val cantidadDescansosTotales: TextView = view.findViewById(R.id.tv_cantidad_descansos_totales)
            cantidadDescansosTotales.text = "${viaje.getInt("CantidadDeDescansosTotales")}"

            val minutosTotalesDescanso: TextView = view.findViewById(R.id.tv_minutos_totales_descanso)
            minutosTotalesDescanso.text = "${viaje.getInt("MinutosTotalesDeDescanso")}"

            val minutosPromedioPorDescanso: TextView = view.findViewById(R.id.tv_minutos_promedio_por_descanso)
            minutosPromedioPorDescanso.text = "${viaje.getInt("MinutosPromedioPorDescanso")}"
        }

        return view
    }

    private fun loadJSONFromAsset(filename: String): String? {
        return context?.assets?.open(filename)?.bufferedReader().use { it?.readText() }
    }
}