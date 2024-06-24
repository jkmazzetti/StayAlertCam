package com.google.mediapipe.examples.facelandmarker

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat

class CustomButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    init {
        // Obtén los atributos personalizados
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CustomButton)
        val buttonText = attributes.getString(R.styleable.CustomButton_buttonText)

        // Aplica los atributos
        text = buttonText

        // Configura el estilo del botón
        val drawable = ContextCompat.getDrawable(context, R.drawable.rounded_button)
        background = drawable
        setTextColor(ContextCompat.getColor(context, android.R.color.white))

        // Centrar el texto y establecer el tamaño del texto en 16sp
        textAlignment = TEXT_ALIGNMENT_CENTER
        textSize = 16f
        gravity = android.view.Gravity.CENTER

        // Efecto de cambio de color al presionar
        val states = arrayOf(
            intArrayOf(android.R.attr.state_pressed),
            intArrayOf()
        )

        val colors = intArrayOf(
            ContextCompat.getColor(context, R.color.pressed_color),
            ContextCompat.getColor(context, R.color.default_color)
        )

        val colorStateList = ColorStateList(states, colors)
        backgroundTintList = colorStateList

        // Libera los recursos de los atributos
        attributes.recycle()
    }
}