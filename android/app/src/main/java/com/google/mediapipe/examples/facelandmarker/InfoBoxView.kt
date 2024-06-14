package com.google.mediapipe.examples.facelandmarker

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.google.mediapipe.examples.facelandmarker.R

class InfoBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var infoTextView: TextView
    private var titleTextView: TextView
    private var descriptionTextView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_info_box, this, true)
        orientation = HORIZONTAL
        val padding = 15.dpToPx(context)
        setPadding(padding, padding, padding, padding)

        infoTextView = findViewById(R.id.infoTextView)
        titleTextView = findViewById(R.id.titleTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
    }

    fun setInfo(info: String) {
        infoTextView.text = info
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setDescription(description: String) {
        descriptionTextView.text = description
    }

    fun setValues(info: String, title: String, description: String) {
        setInfo(info)
        setTitle(title)
        setDescription(description)
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
