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
import kotlinx.coroutines.*

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


    private var referencePoint1: Float = 0f
    private var referencePoint2: Float = 0f
    private var leftEyeBottomPoint: Float = 0f
    private var leftEyeTopPoint: Float = 0f
    private var rightEyeBottomPoint: Float = 0f
    private var rightEyeTopPoint: Float = 0f
    private var mouthBottomPoint: Float = 0f
    private var mouthTopPoint: Float = 0f
    private var ratioLeftEye: Float = 0f
    private var ratioRightEye: Float = 0f
    private var ratioMouth: Float = 0f
    private var countBlink: Int = 0
    private var countYawn: Int = 0
    private var countTired: Int = 0
    private var isTired: Boolean = false
    private val EAR_THRESH = 0.5
    private val EAR_CONSEC_FRAMES = 3
    private val BLINK_DURATION_THRESHOLD = 100 // Duración mínima entre parpadeos en milisegundos
    private var lastYawnTime = SystemClock.elapsedRealtime()
    private val YAWN_DURATION_THRESHOLD = 6000 // Duración mínima para contar un bostezo en milisegundos (6 segundos)
    private var lastLongBlinkTime = SystemClock.elapsedRealtime()
    private val LONG_BLINK_DURATION_THRESHOLD = 5000 // Duración mínima para contar un parpadeo largo en milisegundos (5 segundos)
    private var countLongBlink = 0

    private val ventanaParpadeos = mutableListOf<Pair<Long, Long>>() // Lista para almacenar tiempos de parpadeo y su duración
    private val ventanaBostezos = mutableListOf<Long>() // Lista para almacenar tiempos de bostezos
    private val umbralFatigaParpadeosAlto = 30 // Parpadeos por minuto (alto)
    private val umbralFatigaParpadeosBajo = 10 // Parpadeos por minuto (bajo)
    private val umbralDuracionParpadeo = 100 // Duración de parpadeo en milisegundos
    private val umbralFatigaBostezos = 3 // Bostezos por minuto
    private val ventanaTiempo = 60 * 1000 // Ventana de tiempo en milisegundos (1 minuto)
    private var parpadeoEnProgreso = false
    private var bostezoEnProgreso = false
    private var inicioParpadeo = 0L
    private var inicioBostezo = 0L
    private var isDuracionPromedioParpadeo = false
    private var isPromedioParpadeo = false
    private var isPromedioBostezo = false

    private var countTiredCalls = 0
    private var lastAlertLevel = 0

    // Intervalo mínimo de tiempo entre evaluaciones en milisegundos
    private val intervaloEvaluacion = 3000 // 1 segundo
    private var ultimaEvaluacion = 0L

    private var COUNTER = 0
    private var TOTAL_BLINKS = 0
    private var lastBlinkTime = SystemClock.elapsedRealtime()

    private var isPopupShowing = false
    private var canShowPopupWarning = true
    private var canShowPopupDanger = true
    private var canShowPopupCritical = true
    private var mediaPlayer: MediaPlayer? = null

    private var tripStarted = false
    private var tripPause = false

    private var chronometerStarted = false
    private var timeElapsed: Long = 0
    private lateinit var handler: Handler
    private var runnable: Runnable? = null
    private var startTime: Long = 0
    private lateinit var pauseHandler: Handler
    private var pauseRunnable: Runnable? = null

    private var popupWindow: PopupWindow? = null

    private fun obtenerTiempoActual(): Long {
        return System.currentTimeMillis()
    }


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
        if(chronometerStarted) {
            stopChronometer()
        }
        canShowPopupWarning = true
        canShowPopupDanger = true
        super.onDestroyView()
        backgroundExecutor.shutdown()
        try {
            backgroundExecutor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Executor shutdown interrupted", e)
            Thread.currentThread().interrupt()
        }
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
        pauseHandler = Handler(Looper.getMainLooper())  // Initialize the pauseHandler

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
        println("pauso el viaje")
        runnable?.let { handler.removeCallbacks(it) }
        timeElapsed = System.currentTimeMillis() - startTime
        tripPause = true
        chronometerStarted = false
        showPausePopup()
        fragmentCameraBinding.playBtn.visibility = View.VISIBLE
        fragmentCameraBinding.pauseBtn.visibility = View.GONE

        // Iniciar el Runnable para verificar los 15 minutos
        pauseRunnable = Runnable {
            fragmentCameraBinding.containerSleepSing.setBackgroundResource(R.drawable.round_background_active)
            fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_active)
        }
        pauseHandler.postDelayed(pauseRunnable!!, 15 * 60 * 1000)  // 15 minutos en milisegundos
    }

    fun resumeChronometer() {
        startTime = System.currentTimeMillis() - timeElapsed
        handler.post(runnable!!)
        tripPause = false
        chronometerStarted = true
        fragmentCameraBinding.playBtn.visibility = View.GONE
        fragmentCameraBinding.pauseBtn.visibility = View.VISIBLE

        // Cancelar el Runnable si se reanuda antes de los 15 minutos
        pauseRunnable?.let { pauseHandler.removeCallbacks(it) }
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

        // Cancelar el Runnable si se reanuda antes de los 15 minutos
        pauseRunnable?.let { pauseHandler.removeCallbacks(it) }
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
    private fun startPopupCooldown(type_popup: String) {
        if(type_popup === "warning") {
            canShowPopupWarning = false
            Handler(Looper.getMainLooper()).postDelayed({
                canShowPopupWarning = true
            }, 10000) // 30,000 ms = 30 segundos
        }
        if (type_popup === "danger"){
            canShowPopupDanger = false
            Handler(Looper.getMainLooper()).postDelayed({
                canShowPopupDanger = true
            }, 10000) // 30,000 ms = 30 segundos
        }
        if (type_popup === "critical"){
            canShowPopupCritical = false
            Handler(Looper.getMainLooper()).postDelayed({
                canShowPopupCritical = true
            }, 10000) // 30,000 ms = 30 segundos
        }
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
    private fun showGesturePopupWarning() {
        if (isPopupShowing || !canShowPopupWarning) return

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
                startPopupCooldown("warning")
            }
            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            alertDialog.show()

            playAlertSound()
        }
    }

    private fun showGesturePopupDanger() {
        if (isPopupShowing || !canShowPopupDanger) return

        isPopupShowing = true
        activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.fragment_popup_sleep, null)
            builder.setView(dialogView)
            val alertDialog = builder.create()
            val hiddenButton: Button = dialogView.findViewById(R.id.hiddenButton)
            hiddenButton.setOnClickListener {
                alertDialog.dismiss()
                stopAlertSound()
                isPopupShowing = false
                startPopupCooldown("danger")
            }
            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            alertDialog.show()

            playAlertSound()

            // Handler para el dismiss automático y llamada a showGesturePopupCritical después de 5 segundos
            Handler(Looper.getMainLooper()).postDelayed({
                if (alertDialog.isShowing) {
                    alertDialog.dismiss()
                    stopAlertSound()
                    isPopupShowing = false
                    showGesturePopupCritical()
                }
            }, 5000) // 5000 milisegundos = 5 segundos
        }
    }

    private fun showGesturePopupCritical() {
        if (isPopupShowing || !canShowPopupCritical) return

        isPopupShowing = true
        activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.fragment_popup_critical, null)
            builder.setView(dialogView)
            val alertDialog = builder.create()
            val hiddenButton: Button = dialogView.findViewById(R.id.hiddenButton)
            val countDown: TextView = dialogView.findViewById(R.id.count_text)

            hiddenButton.setOnClickListener {
                alertDialog.dismiss()
                stopAlertSound()
                isPopupShowing = false
                startPopupCooldown("critical")
            }

            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            alertDialog.show()

            playAlertSound()

            // Iniciar cuenta regresiva de 60 segundos
            object : CountDownTimer(60000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsRemaining = millisUntilFinished / 1000
                    countDown.text = secondsRemaining.toString()
                }

                override fun onFinish() {
                    alertDialog.dismiss()
                    stopAlertSound()
                    isPopupShowing = false
                    startPopupCooldown("critical")
                }
            }.start()
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

    fun popupTiredAlerts() {
        countTiredCalls++

        when {
            countTiredCalls == 1 && lastAlertLevel < 1 -> {
                showGesturePopupWarning()
                fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_warning)
                lastAlertLevel = 1
            }
            countTiredCalls == 2 && lastAlertLevel < 2 -> {
                showGesturePopupWarning()
                fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_danger)
                fragmentCameraBinding.containerSleepSing.setBackgroundResource(R.drawable.round_background_warning)
                lastAlertLevel = 2
            }
            countTiredCalls >= 3 -> {
                showGesturePopupDanger()
                fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_danger)
                fragmentCameraBinding.containerSleepSing.setBackgroundResource(R.drawable.round_background_danger)
                lastAlertLevel = 3 // Mantener este valor para permitir ejecuciones repetidas
            }
        }
    }

    private fun isTiredGestureDetected(resultBundle: FaceLandmarkerHelper.ResultBundle) {
//        println("resultBundle ${resultBundle.result.faceLandmarks().get(0).get(386).y()}")
//        if (_fragmentCameraBinding == null) {
//            return  // Asegurarse de que la vista esté aún disponible
//        }
        val tiempoActual = obtenerTiempoActual()

        referencePoint1 = resultBundle.result.faceLandmarks().get(0).get(5).y()*1920f
        referencePoint2 = resultBundle.result.faceLandmarks().get(0).get(4).y()*1920f

        leftEyeBottomPoint = resultBundle.result.faceLandmarks().get(0).get(374).y()*1920f
        leftEyeTopPoint = resultBundle.result.faceLandmarks().get(0).get(386).y()*1920f

        rightEyeBottomPoint = resultBundle.result.faceLandmarks().get(0).get(145).y()*1920f
        rightEyeTopPoint = resultBundle.result.faceLandmarks().get(0).get(159).y()*1920f

        ratioLeftEye = (leftEyeBottomPoint - leftEyeTopPoint) / (referencePoint2 - referencePoint1)
        ratioRightEye = (rightEyeBottomPoint - rightEyeTopPoint) / (referencePoint2 - referencePoint1)

        mouthBottomPoint = resultBundle.result.faceLandmarks().get(0).get(17).y()*1920f
        mouthTopPoint = resultBundle.result.faceLandmarks().get(0).get(0).y()*1920f

        ratioMouth  = (mouthBottomPoint - mouthTopPoint) / (referencePoint2 - referencePoint1)

        // Detectar parpadeo
        if (ratioLeftEye < EAR_THRESH && ratioRightEye < EAR_THRESH) {
            if (!parpadeoEnProgreso) {
                // Iniciar el registro de un parpadeo
                parpadeoEnProgreso = true
                inicioParpadeo = tiempoActual
            } else {
                // Verificar si el parpadeo dura más de 2 segundos
                val duracionParpadeo = tiempoActual - inicioParpadeo
                if (duracionParpadeo > 2000) {
                    // Ejecutar las tres líneas de código si el parpadeo dura más de 2 segundos
                    showGesturePopupDanger()
                    fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_danger)
                    fragmentCameraBinding.containerSleepSing.setBackgroundResource(R.drawable.round_background_danger)
                    countTired = 3
                }
            }
        } else {
            if (parpadeoEnProgreso) {
                // Finalizar el registro de un parpadeo
                val duracionParpadeo = tiempoActual - inicioParpadeo
                if (duracionParpadeo > umbralDuracionParpadeo) {
                    ventanaParpadeos.add(Pair(inicioParpadeo, duracionParpadeo))
                    println("Fatiga PARPADEO $ratioRightEye")
                }
                println("Fatiga PARPADEO $ratioRightEye")
                parpadeoEnProgreso = false
            }
        }

        // Detectar bostezo
        if (ratioMouth > 7) {
            if (!bostezoEnProgreso) {
                // Iniciar el registro de un bostezoo
                bostezoEnProgreso = true
                inicioBostezo = tiempoActual
            }
        } else {
            if (bostezoEnProgreso) {
                // Finalizar el registro de un bostezo
                ventanaBostezos.add(tiempoActual)
                println("Fatiga bostezo $ratioMouth")
                bostezoEnProgreso = false
            }
        }

        // Eliminar parpadeos fuera de la ventana de tiempo
        ventanaParpadeos.removeIf { it.first < tiempoActual - ventanaTiempo }

        // Eliminar bostezos fuera de la ventana de tiempo
        ventanaBostezos.removeIf { it < tiempoActual - ventanaTiempo }

        val cantidadParpadeos = ventanaParpadeos.size
        val cantidadBostezos = ventanaBostezos.size
        val duracionPromedioParpadeo = if (cantidadParpadeos > 0) ventanaParpadeos.sumOf { it.second } / cantidadParpadeos else 0

        println("Fatiga total parpadeos 1 ${ventanaParpadeos.size}")
        println("Fatiga total final parpadeos ${ventanaParpadeos.size}, - countTiredCalls 1 $countTiredCalls, -  duracionPromedioParpadeo 1 $duracionPromedioParpadeo, - cantidadBostezos $cantidadBostezos ")
        println("Fatiga total bostezos 1 ${ventanaBostezos.size}")
        println("Fatiga countTired 1 $countTired ")
        println("ventanaTiempo $ventanaTiempo ")
        println("ventanaParpadeos $ventanaParpadeos ")

        //println("ventanaParpadeos.first().first ${ventanaParpadeos.first().first} ")

        // Solo evaluar signos de fatiga si ha pasado al menos `ventanaTiempo` milisegundos desde el inicio
        if (tiempoActual - startTime >= ventanaTiempo) {

            // Verificar si ha pasado el intervalo mínimo desde la última evaluación

                println("Entro aqui la concha de la lora ")

                val cantidadParpadeos = ventanaParpadeos.size
                val cantidadBostezos = ventanaBostezos.size
                val duracionPromedioParpadeo = if (cantidadParpadeos > 0) ventanaParpadeos.sumOf { it.second } / cantidadParpadeos else 0

            if (tiempoActual - ultimaEvaluacion >= intervaloEvaluacion) {
                ultimaEvaluacion = tiempoActual
                println("Fatiga duracionPromedioParpadeo 1 $duracionPromedioParpadeo ")
                // Verificar si hay signos de fatiga o somnolencia
                if (cantidadParpadeos >= umbralFatigaParpadeosAlto || cantidadParpadeos <= umbralFatigaParpadeosBajo ||
                    duracionPromedioParpadeo >= 400 || cantidadBostezos >= umbralFatigaBostezos) {
                    if(duracionPromedioParpadeo > 400 && !isDuracionPromedioParpadeo){
                        isDuracionPromedioParpadeo = true
                        isTired = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            isDuracionPromedioParpadeo = false
                        }, 40000)
                    } else if(!isPromedioParpadeo && (cantidadParpadeos > umbralFatigaParpadeosAlto || cantidadParpadeos < umbralFatigaParpadeosBajo)){
                        isPromedioParpadeo = true
                        isTired = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            isPromedioParpadeo = false
                        }, 10000)
                    } else if(!isPromedioBostezo && cantidadBostezos >= umbralFatigaBostezos){
                        isPromedioBostezo = true
                        isTired = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            isPromedioBostezo = false
                        }, 61000)
                    }

                } else {
                    isTired = false
                    // Resetear el contador si no hay signos de fatiga
                }

                if(isTired){
                    isTired = false
                    popupTiredAlerts()
                }

//                // Mostrar alertas según el conteo de fatiga
//                if (countTired === 1) {
//                    showGesturePopupWarning()
//                    fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_warning)
//                }
//                if (countTired === 2) {
//                    showGesturePopupWarning()
//                    fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_danger)
//                    fragmentCameraBinding.containerSleepSing.setBackgroundResource(R.drawable.round_background_warning)
//                }
//
//                if (countTired >= 3) {
//                    showGesturePopupDanger()
//                    fragmentCameraBinding.containerPersonYawn.setBackgroundResource(R.drawable.round_background_danger)
//                    fragmentCameraBinding.containerSleepSing.setBackgroundResource(R.drawable.round_background_danger)
//                }
            }
        }
    }

    // Update UI after face have been detected. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    override fun onResults(
        resultBundle: FaceLandmarkerHelper.ResultBundle
    ) {
        if (!tripStarted ) {
            return  // Si tripStarted es falso, salir sin hacer nada
        }
        activity?.runOnUiThread {
            println("resultBundle ------> $resultBundle")

            if (_fragmentCameraBinding != null) {

                // Pass necessary information to OverlayView for drawing on the canvas
                fragmentCameraBinding.overlay.setResults(
                    resultBundle.result,
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                if (!tripPause){
                    isTiredGestureDetected(resultBundle)
                }
                fragmentCameraBinding.overlay.invalidate()
            }
        }
    }

    override fun onEmpty() {
        fragmentCameraBinding.overlay.clear()
        activity?.runOnUiThread {
            faceBlendshapesResultAdapter.updateResults(null)
            faceBlendshapesResultAdapter.notifyDataSetChanged()
        }
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
