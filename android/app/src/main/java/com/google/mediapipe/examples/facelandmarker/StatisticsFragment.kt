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

        val btnViajeActual: Button = view.findViewById(R.id.btn_ultimo_viaje)
        val btnHistorico: Button = view.findViewById(R.id.btn_historico)

        loadFragment(StatisticsHistoricalFragment())

        btnViajeActual.setOnClickListener {
            loadFragment(StatisticsLastFragment())
        }

        btnHistorico.setOnClickListener {
            loadFragment(StatisticsHistoricalFragment())
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