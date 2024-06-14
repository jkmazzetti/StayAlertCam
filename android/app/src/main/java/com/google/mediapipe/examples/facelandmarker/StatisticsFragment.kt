package com.google.mediapipe.examples.facelandmarker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.mediapipe.examples.facelandmarker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class StatisticsFragment : Fragment() {
    private lateinit var entriesContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        entriesContainer = view.findViewById(R.id.entriesContainer)

        // Load JSON from assets and create InfoBoxViews
        loadInfoBoxViewsFromJson()

        return view
    }

    private fun loadInfoBoxViewsFromJson() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Read JSON file from assets
                val json = loadJsonFromAssets("info_boxes.json")

                // Parse JSON array
                val jsonArray = JSONArray(json)

                // Remove all existing views from entriesContainer
                launch(Dispatchers.Main) {
                    entriesContainer.removeAllViews()
                }

                // Create InfoBoxViews for each JSON object
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val info = jsonObject.getString("info")
                    val title = jsonObject.getString("title")
                    val description = jsonObject.getString("description")

                    // Create InfoBoxView and add to UI
                    launch(Dispatchers.Main) {
                        val infoBoxView = InfoBoxView(requireContext()).apply {
                            setValues(info, title, description)
                        }
                        entriesContainer.addView(infoBoxView)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadJsonFromAssets(fileName: String): String {
        val inputStream = requireContext().assets.open(fileName)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        bufferedReader.useLines { lines ->
            lines.forEach {
                stringBuilder.append(it)
            }
        }
        return stringBuilder.toString()
    }
}
