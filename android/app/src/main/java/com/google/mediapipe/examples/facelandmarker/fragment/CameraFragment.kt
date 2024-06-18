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
package com.google.mediapipe.examples.facelandmarker.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Camera
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.google.mediapipe.examples.facelandmarker.MainViewModel
import com.google.mediapipe.examples.facelandmarker.R
import com.google.mediapipe.examples.facelandmarker.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.os.SystemClock

class CameraFragment : Fragment(), FaceLandmarkerHelper.LandmarkerListener {

    companion object {
        private const val TAG = "Face Landmarker"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private val faceBlendshapesResultAdapter by lazy {
        FaceBlendshapesResultAdapter()
    }

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT


    private var reference_point1: Float = 0f
    private var reference_point2: Float = 0f
    private var left_eye_bottom_point: Float = 0f
    private var left_eye_top_point: Float = 0f
    private var right_eye_bottom_point: Float = 0f
    private var right_eye_top_point: Float = 0f
    private var mouth_bottom_point: Float = 0f
    private var mouth_top_point: Float = 0f
    private var ratio_left_eye: Float = 0f
    private var ratio_right_eye: Float = 0f
    private var ratio_mouth: Float = 0f
    private var count_blink: Int = 0
    private var count_yawn: Int = 0
    private var count_tired: Int = 0
    private var timeBetweenBlink: Int = 3
    private val EAR_THRESH = 0.8
    private val EAR_CONSEC_FRAMES = 3
    private val BLINK_DURATION_THRESHOLD = 100 // Duración mínima entre parpadeos en milisegundos
    private var COUNTER = 0
    private var TOTAL_BLINKS = 0
    private var lastBlinkTime = SystemClock.elapsedRealtime()

    private var isPopupShowing = false
    private var canShowPopup = true
    private var mediaPlayer: MediaPlayer? = null

    private var tripStarted = false

    private var chronometerStarted = false
    private var timeElapsed: Long = 0
    private lateinit var handler: Handler
    private var runnable: Runnable? = null
    private var startTime: Long = 0

    private var popupWindow: PopupWindow? = null




    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(), R.id.fragment_container
            ).navigate(R.id.action_camera_to_permissions)
        }

        // Start the FaceLandmarkerHelper again when users come back
        // to the foreground.
        backgroundExecutor.execute {
            if (faceLandmarkerHelper.isClose()) {
                faceLandmarkerHelper.setupFaceLandmarker()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if(this::faceLandmarkerHelper.isInitialized) {
            viewModel.setMaxFaces(faceLandmarkerHelper.maxNumFaces)
            viewModel.setMinFaceDetectionConfidence(faceLandmarkerHelper.minFaceDetectionConfidence)
            viewModel.setMinFaceTrackingConfidence(faceLandmarkerHelper.minFaceTrackingConfidence)
            viewModel.setMinFacePresenceConfidence(faceLandmarkerHelper.minFacePresenceConfidence)
            viewModel.setDelegate(faceLandmarkerHelper.currentDelegate)

            // Close the FaceLandmarkerHelper and release resources
            backgroundExecutor.execute { faceLandmarkerHelper.clearFaceLandmarker() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        // Inicializar el handler
        handler = Handler(Looper.getMainLooper())

        // Configurar el listener para el botón de iniciar viaje
        fragmentCameraBinding.btnStartTrip.setOnClickListener {
            startTrip()
        }

        // Configurar los listeners para los botones Play y Pause
        fragmentCameraBinding.playBtn.setOnClickListener {
            resumeChronometer()
        }

        fragmentCameraBinding.pauseBtn.setOnClickListener {
            pauseChronometer()
        }

        // Configurar el listener para el botón Stop
        fragmentCameraBinding.stopBtn.setOnClickListener {
            showEndPopup()
        }

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        // Create the FaceLandmarkerHelper that will handle the inference
        backgroundExecutor.execute {
            faceLandmarkerHelper = FaceLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minFaceDetectionConfidence = viewModel.currentMinFaceDetectionConfidence,
                minFaceTrackingConfidence = viewModel.currentMinFaceTrackingConfidence,
                minFacePresenceConfidence = viewModel.currentMinFacePresenceConfidence,
                maxNumFaces = viewModel.currentMaxFaces,
                currentDelegate = viewModel.currentDelegate,
                faceLandmarkerHelperListener = this
            )
        }

        // Attach listeners to UI control widgets
        initBottomSheetControls()

        val startButton: Button = view.findViewById(R.id.btn_start_trip)
        startButton.setOnClickListener {
            startTrip()
            startButton.visibility = View.GONE
        }
    }

    private fun showPausePopup() {
        // Inflar el layout del popup
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.fragment_popup_travel_pause, null)

        // Crear el PopupWindow
        popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow?.isFocusable = true

        // Mostrar el PopupWindow
        popupWindow?.showAtLocation(fragmentCameraBinding.root, Gravity.CENTER, 0, 0)

        // Manejar los botones del popup
        val btnResumeTrip = popupView.findViewById<Button>(R.id.hiddenButton_pause)

        btnResumeTrip.setOnClickListener {
            // Reanudar el cronómetro
            resumeChronometer()
            popupWindow?.dismiss()
        }
    }

    private fun showEndPopup() {

        runnable?.let { handler.removeCallbacks(it) }
        timeElapsed = System.currentTimeMillis() - startTime
        chronometerStarted = false
        fragmentCameraBinding.playBtn.visibility = View.VISIBLE
        fragmentCameraBinding.pauseBtn.visibility = View.GONE

        // Inflar el layout del popup
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.fragment_popup_travel_end, null)

        // Crear el PopupWindow
        popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow?.isFocusable = true

        // Mostrar el PopupWindow
        popupWindow?.showAtLocation(fragmentCameraBinding.root, Gravity.CENTER, 0, 0)

        // Manejar los botones del popup
        val btnEndTrip = popupView.findViewById<Button>(R.id.hiddenButton_end)
        val btnResumeTrip = popupView.findViewById<Button>(R.id.hiddenButton_continue)

        btnEndTrip.setOnClickListener {
            // Reanudar el cronómetro
            stopChronometer()
            popupWindow?.dismiss()
        }

        btnResumeTrip.setOnClickListener {
            // Reanudar el cronómetro
            resumeChronometer()
            popupWindow?.dismiss()
        }
    }

    private fun startChronometer() {
        startTime = System.currentTimeMillis()
        runnable = object : Runnable {
            override fun run() {
                val elapsedMillis = System.currentTimeMillis() - startTime
                updateChronometerText(elapsedMillis)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable!!)
        chronometerStarted = true
        fragmentCameraBinding.playBtn.visibility = View.GONE
        fragmentCameraBinding.pauseBtn.visibility = View.VISIBLE
    }

    private fun pauseChronometer() {
        runnable?.let { handler.removeCallbacks(it) }
        timeElapsed = System.currentTimeMillis() - startTime
        chronometerStarted = false
        showPausePopup()
        fragmentCameraBinding.playBtn.visibility = View.VISIBLE
        fragmentCameraBinding.pauseBtn.visibility = View.GONE
    }

    fun resumeChronometer() {
        startTime = System.currentTimeMillis() - timeElapsed
        handler.post(runnable!!)
        chronometerStarted = true
        fragmentCameraBinding.playBtn.visibility = View.GONE
        fragmentCameraBinding.pauseBtn.visibility = View.VISIBLE
    }

    private fun stopChronometer() {
        runnable?.let { handler.removeCallbacks(it) }
        timeElapsed = 0
        chronometerStarted = false
        updateChronometerText(timeElapsed)
        fragmentCameraBinding.playBtn.visibility = View.VISIBLE
        fragmentCameraBinding.pauseBtn.visibility = View.GONE
        fragmentCameraBinding.btnStartTrip.visibility = View.VISIBLE
        fragmentCameraBinding.btnContainer.visibility = View.GONE
        fragmentCameraBinding.timerContainer.visibility = View.GONE

        fragmentCameraBinding.containerSleepSing.setBackgroundResource(R.drawable.round_background_desactive)
        fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_desactive)

        tripStarted = false
    }

    private fun updateChronometerText(elapsedMillis: Long) {
        val seconds = (elapsedMillis / 1000) % 60
        val minutes = (elapsedMillis / 1000) / 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        fragmentCameraBinding.timerText.text = timeFormatted
    }

    private fun startTrip(){

        fragmentCameraBinding.btnContainer.visibility = View.VISIBLE
//        fragmentCameraBinding.timerText.visibility = View.VISIBLE

        fragmentCameraBinding.containerSleepSing.setBackgroundResource(R.drawable.round_background_active)
        fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_active)

        fragmentCameraBinding.btnStartTrip.visibility = View.GONE

        fragmentCameraBinding.timerContainer.visibility = View.VISIBLE

        tripStarted = true

        startChronometer()
    }
    private fun startPopupCooldown() {
        canShowPopup = false
        Handler(Looper.getMainLooper()).postDelayed({
            canShowPopup = true
        }, 30000) // 30,000 ms = 30 segundos
    }

    private fun playAlertSound() {
        mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound)
        mediaPlayer?.setOnPreparedListener {
            mediaPlayer?.start()
        }
        mediaPlayer?.setOnCompletionListener {
            it.release() // Liberar los recursos una vez que el sonido termina
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release() // Liberar MediaPlayer cuando se detenga la actividad o fragmento
        mediaPlayer = null
    }

    private fun stopAlertSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    private fun showGesturePopup() {
        if (isPopupShowing || !canShowPopup) return

        isPopupShowing = true
        activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.fragment_popup_tired, null)
            builder.setView(dialogView)
            val alertDialog = builder.create()
            val hiddenButton: Button = dialogView.findViewById(R.id.hiddenButton)
            hiddenButton.setOnClickListener {
                alertDialog.dismiss()
                stopAlertSound()
                isPopupShowing = false
                startPopupCooldown()
            }
            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            alertDialog.show()

            playAlertSound()
        }
    }


    private fun initBottomSheetControls() {
        // init bottom sheet settings
//        fragmentCameraBinding.bottomSheetLayout.maxFacesValue.text =
//            viewModel.currentMaxFaces.toString()
//        fragmentCameraBinding.bottomSheetLayout.detectionThresholdValue.text =
//            String.format(
//                Locale.US, "%.2f", viewModel.currentMinFaceDetectionConfidence
//            )
//        fragmentCameraBinding.bottomSheetLayout.trackingThresholdValue.text =
//            String.format(
//                Locale.US, "%.2f", viewModel.currentMinFaceTrackingConfidence
//            )
//        fragmentCameraBinding.bottomSheetLayout.presenceThresholdValue.text =
//            String.format(
//                Locale.US, "%.2f", viewModel.currentMinFacePresenceConfidence
//            )
//
//        // When clicked, lower face detection score threshold floor
//        fragmentCameraBinding.bottomSheetLayout.detectionThresholdMinus.setOnClickListener {
//            if (faceLandmarkerHelper.minFaceDetectionConfidence >= 0.2) {
//                faceLandmarkerHelper.minFaceDetectionConfidence -= 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, raise face detection score threshold floor
//        fragmentCameraBinding.bottomSheetLayout.detectionThresholdPlus.setOnClickListener {
//            if (faceLandmarkerHelper.minFaceDetectionConfidence <= 0.8) {
//                faceLandmarkerHelper.minFaceDetectionConfidence += 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, lower face tracking score threshold floor
//        fragmentCameraBinding.bottomSheetLayout.trackingThresholdMinus.setOnClickListener {
//            if (faceLandmarkerHelper.minFaceTrackingConfidence >= 0.2) {
//                faceLandmarkerHelper.minFaceTrackingConfidence -= 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, raise face tracking score threshold floor
//        fragmentCameraBinding.bottomSheetLayout.trackingThresholdPlus.setOnClickListener {
//            if (faceLandmarkerHelper.minFaceTrackingConfidence <= 0.8) {
//                faceLandmarkerHelper.minFaceTrackingConfidence += 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, lower face presence score threshold floor
//        fragmentCameraBinding.bottomSheetLayout.presenceThresholdMinus.setOnClickListener {
//            if (faceLandmarkerHelper.minFacePresenceConfidence >= 0.2) {
//                faceLandmarkerHelper.minFacePresenceConfidence -= 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, raise face presence score threshold floor
//        fragmentCameraBinding.bottomSheetLayout.presenceThresholdPlus.setOnClickListener {
//            if (faceLandmarkerHelper.minFacePresenceConfidence <= 0.8) {
//                faceLandmarkerHelper.minFacePresenceConfidence += 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, reduce the number of faces that can be detected at a
//        // time
//        fragmentCameraBinding.bottomSheetLayout.maxFacesMinus.setOnClickListener {
//            if (faceLandmarkerHelper.maxNumFaces > 1) {
//                faceLandmarkerHelper.maxNumFaces--
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, increase the number of faces that can be detected
//        // at a time
//        fragmentCameraBinding.bottomSheetLayout.maxFacesPlus.setOnClickListener {
//            if (faceLandmarkerHelper.maxNumFaces < 2) {
//                faceLandmarkerHelper.maxNumFaces++
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, change the underlying hardware used for inference.
//        // Current options are CPU and GPU
//        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
//            viewModel.currentDelegate, false
//        )
//        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(
//                    p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long
//                ) {
//                    try {
//                        faceLandmarkerHelper.currentDelegate = p2
//                        updateControlsUi()
//                    } catch(e: UninitializedPropertyAccessException) {
//                        Log.e(TAG, "FaceLandmarkerHelper has not been initialized yet.")
//                    }
//                }
//
//                override fun onNothingSelected(p0: AdapterView<*>?) {
//                    /* no op */
//                }
//            }
    }

    // Update the values displayed in the bottom sheet. Reset Facelandmarker
    // helper.
    private fun updateControlsUi() {
//        fragmentCameraBinding.bottomSheetLayout.maxFacesValue.text =
//            faceLandmarkerHelper.maxNumFaces.toString()
//        fragmentCameraBinding.bottomSheetLayout.detectionThresholdValue.text =
//            String.format(
//                Locale.US,
//                "%.2f",
//                faceLandmarkerHelper.minFaceDetectionConfidence
//            )
//        fragmentCameraBinding.bottomSheetLayout.trackingThresholdValue.text =
//            String.format(
//                Locale.US,
//                "%.2f",
//                faceLandmarkerHelper.minFaceTrackingConfidence
//            )
//        fragmentCameraBinding.bottomSheetLayout.presenceThresholdValue.text =
//            String.format(
//                Locale.US,
//                "%.2f",
//                faceLandmarkerHelper.minFacePresenceConfidence
//            )

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        backgroundExecutor.execute {
            faceLandmarkerHelper.clearFaceLandmarker()
            faceLandmarkerHelper.setupFaceLandmarker()
        }
        fragmentCameraBinding.overlay.clear()
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectFace(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectFace(imageProxy: ImageProxy) {
        faceLandmarkerHelper.detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    private fun isTiredGestureDetected(resultBundle: FaceLandmarkerHelper.ResultBundle) {
//        println("resultBundle ${resultBundle.result.faceLandmarks().get(0).get(386).y()}")

        reference_point1 = resultBundle.result.faceLandmarks().get(0).get(5).y()*1920f
        reference_point2 = resultBundle.result.faceLandmarks().get(0).get(4).y()*1920f

        left_eye_bottom_point = resultBundle.result.faceLandmarks().get(0).get(374).y()*1920f
        left_eye_top_point = resultBundle.result.faceLandmarks().get(0).get(386).y()*1920f

        right_eye_bottom_point = resultBundle.result.faceLandmarks().get(0).get(145).y()*1920f
        right_eye_top_point = resultBundle.result.faceLandmarks().get(0).get(159).y()*1920f

        ratio_left_eye = (left_eye_bottom_point - left_eye_top_point) / (reference_point2 - reference_point1)
        ratio_right_eye = (right_eye_bottom_point - right_eye_top_point) / (reference_point2 - reference_point1)

        mouth_bottom_point = resultBundle.result.faceLandmarks().get(0).get(17).y()*1920f
        mouth_top_point = resultBundle.result.faceLandmarks().get(0).get(0).y()*1920f

        ratio_mouth  = (mouth_bottom_point - mouth_top_point) / (reference_point2 - reference_point1)

        val currentTime = SystemClock.elapsedRealtime()

        if (ratio_left_eye < EAR_THRESH && ratio_right_eye < EAR_THRESH) {
            COUNTER++
        } else {
            if (COUNTER >= EAR_CONSEC_FRAMES) {
                val blinkDuration = currentTime - lastBlinkTime
                if (blinkDuration > BLINK_DURATION_THRESHOLD) {
                    TOTAL_BLINKS++
                    lastBlinkTime = currentTime
                    println("Fatiga PARPADEO  $ratio_right_eye")
                    count_blink++
                    count_tired++
                    fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_warning)
                }
            }
            COUNTER = 0
        }

        if(ratio_left_eye < 0.8 && ratio_right_eye < 0.8 && chronometerStarted){
            println("Fatiga PARPADEO  ${ratio_left_eye}")
            count_blink += 1
            count_tired += 1
            fragmentCameraBinding.containerSleepSing.setBackgroundResource(R.drawable.round_background_warning)
        }

        if(ratio_mouth > 7 && chronometerStarted){
            println("Fatiga bostezo  ${ratio_mouth}")
            count_yawn += 1
            count_tired += 1
            fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_warning)
        }
        println("Fatiga count_tired  ${count_tired}")


        if(count_tired > 15){
            showGesturePopup()
            fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_warning)
            fragmentCameraBinding.containerSleepSing.setBackgroundResource(R.drawable.round_background_warning)
        }
    }

    // Update UI after face have been detected. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    override fun onResults(
        resultBundle: FaceLandmarkerHelper.ResultBundle
    ) {
        if (!tripStarted) {
            return  // Si tripStarted es falso, salir sin hacer nada
        }
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                isTiredGestureDetected(resultBundle)
            }
        }
    }

    override fun onEmpty() {
//        fragmentCameraBinding.overlay.clear()
//        activity?.runOnUiThread {
//            faceBlendshapesResultAdapter.updateResults(null)
//            faceBlendshapesResultAdapter.notifyDataSetChanged()
//        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            faceBlendshapesResultAdapter.updateResults(null)
            faceBlendshapesResultAdapter.notifyDataSetChanged()

            if (errorCode == FaceLandmarkerHelper.GPU_ERROR) {
//                fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
//                    FaceLandmarkerHelper.DELEGATE_CPU, false
//                )
            }
        }
    }
}
