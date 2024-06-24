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
            val viaje = jsonObject.getJSONArray("viaje").getJSONObject(0)

            val duracion: TextView = view.findViewById(R.id.tv_duracion)
            duracion.text = viaje.getString("Duracion")

            // CARGAR MAS ASSETS
        }

        return view
    }

    private fun loadJSONFromAsset(filename: String): String? {
        return context?.assets?.open(filename)?.bufferedReader().use { it?.readText() }
    }
}