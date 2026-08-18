package com.liedetector.test

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var txtStatus: TextView
    private lateinit var btnStart: Button
    private lateinit var resultOverlay: View

    private var isAnalyzing = false
    private var blinkCount = 0
    private var stressPoints = 0
    private var wasEyeClosed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // პროგრამულად ვაწყობთ მარტივ ინტერფეისს
        val layout = android.widget.RelativeLayout(this)
        
        previewView = PreviewView(this).apply {
            layoutParams = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT
            )
        }
        layout.addView(previewView)

        resultOverlay = View(this).apply {
            layoutParams = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        layout.addView(resultOverlay)

        txtStatus = TextView(this).apply {
            text = "PRESS START TO ANALYZE"
            textSize = 24f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#80000000"))
            setPadding(32, 16, 32, 16)
            val params = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            params.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL)
            params.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP)
            params.topMargin = 100
            layoutParams = params
        }
        layout.addView(txtStatus)

        btnStart = Button(this).apply {
            text = "START AI TEST"
            textSize = 18f
            val params = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            params.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL)
            params.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM)
            params.bottomMargin = 100
            layoutParams = params
            setOnClickListener { startScan() }
        }
        layout.addView(btnStart)

        setContentView(layout)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val options = FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
            val detector = FaceDetection.getClient(options)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                @androidx.camera.core.ExperimentalGetImage
                val mediaImage = imageProxy.image
                if (mediaImage != null && isAnalyzing) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    detector.process(image)
                        .addOnSuccessListener { faces ->
                            for (face in faces) {
                                analyzeFaceMetrics(face)
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Toast.makeText(this, "Camera setup failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeFaceMetrics(face: Face) {
        val leftEye = face.leftEyeOpenProbability ?: 1.0f
        val rightEye = face.rightEyeOpenProbability ?: 1.0f
        val smile = face.smilingProbability ?: 0.0f

        // თვალის დახამხამების დეტექცია
        if (leftEye < 0.3f && rightEye < 0.3f) {
            if (!wasEyeClosed) {
                blinkCount++
                wasEyeClosed = true
            }
        } else {
            wasEyeClosed = false
        }

        // ყალბი/დაძაბული მიმიკის ანალიზი
        if (smile in 0.1f..0.5f) {
            stressPoints += 2 // დაძაბული ღიმილი
        }
    }

    private fun startScan() {
        isAnalyzing = true
        blinkCount = 0
        stressPoints = 0
        resultOverlay.visibility = View.GONE
        btnStart.isEnabled = false

        object : CountDownTimer(7000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                txtStatus.text = "ANALYZING... ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                isAnalyzing = false
                evaluateResult()
            }
        }.start()
    }

    private fun evaluateResult() {
        btnStart.isEnabled = true
        
        // ლოგიკა: თუ 7 წამში 5-ზე მეტჯერ დაახამხამა ან მიმიკური სტრესი მაღალია -> LIE
        val isLie = blinkCount > 5 || stressPoints > 10

        resultOverlay.visibility = View.VISIBLE
        if (isLie) {
            resultOverlay.setBackgroundColor(Color.parseColor("#CCFF0000")) // წითელი
            txtStatus.text = "🚨 LIE DETECTED! 🚨"
        } else {
            resultOverlay.setBackgroundColor(Color.parseColor("#CC00FF00")) // მწვანე
            txtStatus.text = "✅ TRUTH / TRUE ✅"
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
