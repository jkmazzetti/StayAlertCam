package com.google.mediapipe.examples.facelandmarker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class StatisticsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        val btnUltimoViaje: CustomButton = view.findViewById(R.id.btn_ultimo_viaje)
        val btnHistorico: CustomButton = view.findViewById(R.id.btn_historico)

        loadFragment(StatisticsHistoricalFragment())
        btnUltimoViaje.changeBackgroundColor(R.color.gray)

        btnUltimoViaje.setOnClickListener {
            loadFragment(StatisticsLastFragment())
            btnHistorico.changeBackgroundColor(R.color.gray)
            btnUltimoViaje.changeBackgroundColor(R.color.default_color)
        }

        btnHistorico.setOnClickListener {
            loadFragment(StatisticsHistoricalFragment())
            btnUltimoViaje.changeBackgroundColor(R.color.gray)
            btnHistorico.changeBackgroundColor(R.color.default_color)
        }

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentManager = parentFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.container, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }
}