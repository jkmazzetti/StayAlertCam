/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.facelandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.max
import kotlin.math.min


class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: FaceLandmarkerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()
//    private var message: String = ""

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

//    private var reference_point1: Float = 0f
//    private var reference_point2: Float = 0f
//
//    private var left_eye_bottom_point: Float = 0f
//    private var left_eye_top_point: Float = 0f
//
//    private var right_eye_bottom_point: Float = 0f
//    private var right_eye_top_point: Float = 0f
//
//    private var mouth_bottom_point: Float = 0f
//    private var mouth_top_point: Float = 0f
//
//    private var ratio_left_eye: Float = 0f
//    private var ratio_right_eye: Float = 0f
//
//    private var ratio_mouth: Float = 0f
//
//    private var count_blink: Int = 0
//
//    private var count_yawn: Int = 0

//    private var popup_tired: TextView = findViewById(R.id.popup_tired)


    init {
        initPaints()
    }

    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

//    fun getMessage(): String {
//        return message
//    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.stay_alert_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL

//        message = "Parpadeo ${count_blink}\n Bostezo: ${count_yawn}"
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if(results == null || results!!.faceLandmarks().isEmpty()) {
            clear()
            return
        }

        results?.let { faceLandmarkerResult ->

//            println("faceLandmarkerResult -------->{ ${faceLandmarkerResult.faceLandmarks().get(0).get(386).y()} }<---------- faceLandmarkerResult")

//            reference_point1 = faceLandmarkerResult.faceLandmarks().get(0).get(5).y()*1920f
//            reference_point2 = faceLandmarkerResult.faceLandmarks().get(0).get(4).y()*1920f
//
//            left_eye_bottom_point = faceLandmarkerResult.faceLandmarks().get(0).get(374).y()*1920f
//            left_eye_top_point = faceLandmarkerResult.faceLandmarks().get(0).get(386).y()*1920f
//
//            right_eye_bottom_point = faceLandmarkerResult.faceLandmarks().get(0).get(145).y()*1920f
//            right_eye_top_point = faceLandmarkerResult.faceLandmarks().get(0).get(159).y()*1920f
//
//            ratio_left_eye = (left_eye_bottom_point - left_eye_top_point) / (reference_point2 - reference_point1)
//            ratio_right_eye = (right_eye_bottom_point - right_eye_top_point) / (reference_point2 - reference_point1)
//
//            mouth_bottom_point = faceLandmarkerResult.faceLandmarks().get(0).get(17).y()*1920f
//            mouth_top_point = faceLandmarkerResult.faceLandmarks().get(0).get(0).y()*1920f
//
//            ratio_mouth  = (mouth_bottom_point - mouth_top_point) / (reference_point2 - reference_point1)
//
//
//            if(ratio_left_eye < 0.8 && ratio_right_eye < 0.8){
//                //println("PARPADEO  ${ratio_left_eye}")
//                count_blink =+ 1
//                message = "Parpadeo ${count_blink}\n Bostezo: ${count_yawn}"
////                popup_tired.setText(message)
//            }
////            else {
////                println("NO PARPADEO OJOS ABIERTOS  ${ratio_left_eye}")
////            }
//
//            //println("bostezo  ${ratio_mouth}")
//
//            if(ratio_mouth > 7){
//                //println("bostezo  ${ratio_mouth}")
//                count_yawn =+ 1
//                message = "Parpadeo ${count_blink}\n Bostezo: ${count_yawn}"
////                popup_tired.setText(message)
//            }




            for(landmark in faceLandmarkerResult.faceLandmarks()) {
                for(normalizedLandmark in landmark) {
                    canvas.drawPoint(normalizedLandmark.x() * imageWidth * scaleFactor, normalizedLandmark.y() * imageHeight * scaleFactor, pointPaint)
                }
            }

            FaceLandmarker.FACE_LANDMARKS_CONNECTORS.forEach {
                canvas.drawLine(
                    faceLandmarkerResult.faceLandmarks().get(0).get(it!!.start()).x() * imageWidth * scaleFactor,
                    faceLandmarkerResult.faceLandmarks().get(0).get(it.start()).y() * imageHeight * scaleFactor,
                    faceLandmarkerResult.faceLandmarks().get(0).get(it.end()).x() * imageWidth * scaleFactor,
                    faceLandmarkerResult.faceLandmarks().get(0).get(it.end()).y() * imageHeight * scaleFactor,
                    linePaint)
            }
        }
    }

    fun setResults(
        //AQUI PUEDE QUE PASE EL ID DEL TEXTO XML QUE SE MODIFICA SEGUN LA CANTIDAD DE BOZTESOS Y PARPADEOS
//        popup_tired: TextView = findViewById(R.id.popup_tired),
        faceLandmarkerResults: FaceLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = faceLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
//        popup_tired.setText(message)
        invalidate()

    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
        private const val TAG = "Face Landmarker Overlay"
    }
}