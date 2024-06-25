package com.google.mediapipe.examples.facelandmarker

import com.github.mikephil.charting.formatter.ValueFormatter

class HourAxisValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val hour = value.toInt() // Dividir entre 2 para mostrar tramos de media hora
        val minutes = if (value.toInt() % 2 == 0) "00" else "30" // Mostrar '00' o '30' para los minutos
        return String.format("%02d:%s", hour, minutes) // Formato HH:mm
    }
}