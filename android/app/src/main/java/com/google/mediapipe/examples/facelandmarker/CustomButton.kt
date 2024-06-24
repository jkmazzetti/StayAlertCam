package com.google.mediapipe.examples.facelandmarker

import android.content.Context
import android.graphics.drawable.GradientDrawable
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
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 50f
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, android.R.color.white))

        // Centrar el texto y establecer el tamaño del texto en 16sp
        textAlignment = TEXT_ALIGNMENT_CENTER
        textSize = 16f
        gravity = android.view.Gravity.CENTER

        // Libera los recursos de los atributos
        attributes.recycle()
    }
}
