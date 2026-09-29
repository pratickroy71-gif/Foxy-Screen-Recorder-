package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.data.PreferencesManager
import com.example.model.FacecamShape
import com.example.model.RecordingState
import com.example.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingOverlayService : Service(), LifecycleOwner {

    companion object {
        const val ACTION_SHOW = "com.example.action.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.example.action.HIDE_OVERLAY"
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var facecamView: View? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isExpanded = true
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefsManager = PreferencesManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                if (Settings.canDrawOverlays(this)) {
                    showOverlay()
                    val config = prefsManager.configFlow.value
                    if (config.facecamEnabled) {
                        showFacecam()
                    }
                }
            }
            ACTION_HIDE -> {
                hideOverlay()
                hideFacecam()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        if (overlayView != null) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16)
            y = dpToPx(140)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
            gravity = Gravity.CENTER_VERTICAL

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#EE120E28"))
                cornerRadius = dpToPx(24).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#558C52FF"))
            }
            background = bg
            elevation = dpToPx(8).toFloat()
        }

        // Recording indicator dot
        val dot = View(this).apply {
            val dotBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FFFF2A55"))
            }
            background = dotBg
            layoutParams = LinearLayout.LayoutParams(dpToPx(10), dpToPx(10)).apply {
                setMargins(0, 0, dpToPx(8), 0)
            }
        }

        // Timer Text
        val timerTv = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            text = "00:00"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, dpToPx(10), 0)
            }
        }

        // Controls container
        val controlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Pause/Resume button
        val pauseBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_media_pause)
            setColorFilter(Color.parseColor("#B388FF"))
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            setOnClickListener {
                if (RecordingController.recordingState.value == RecordingState.PAUSED) {
                    RecordingController.requestResume(this@FloatingOverlayService)
                } else {
                    RecordingController.requestPause(this@FloatingOverlayService)
                }
            }
        }

        // Screenshot button
        val shotBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_camera)
            setColorFilter(Color.parseColor("#00F5D4"))
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            setOnClickListener {
                RecordingController.requestScreenshot(this@FloatingOverlayService)
            }
        }

        // Stop button
        val stopBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#FF4D6D"))
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            setOnClickListener {
                RecordingController.requestStop(this@FloatingOverlayService)
            }
        }

        // Toggle Expand/Collapse
        val expandBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.arrow_down_float)
            setColorFilter(Color.parseColor("#8888AA"))
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            setOnClickListener {
                isExpanded = !isExpanded
                controlsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
                rotation = if (isExpanded) 0f else 180f
            }
        }

        controlsLayout.addView(pauseBtn)
        controlsLayout.addView(shotBtn)
        controlsLayout.addView(stopBtn)

        container.addView(dot)
        container.addView(timerTv)
        container.addView(controlsLayout)
        container.addView(expandBtn)

        // Make draggable
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isMoving = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isMoving = true
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(container, params)
                        } catch (_: Exception) {}
                        return isMoving
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(container, params)
            overlayView = container
        } catch (_: Exception) {}

        // Collect duration and state
        serviceScope.launch {
            RecordingController.elapsedDurationMs.collectLatest { ms ->
                timerTv.text = TimeUtils.formatDuration(ms)
            }
        }

        serviceScope.launch {
            RecordingController.recordingState.collectLatest { state ->
                when (state) {
                    RecordingState.PAUSED -> {
                        pauseBtn.setImageResource(android.R.drawable.ic_media_play)
                        dot.alpha = 0.4f
                    }
                    RecordingState.RECORDING -> {
                        pauseBtn.setImageResource(android.R.drawable.ic_media_pause)
                        dot.alpha = 1.0f
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showFacecam() {
        if (facecamView != null) return

        val config = prefsManager.configFlow.value
        val sizePx = dpToPx(config.facecamSize.dpSize)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dpToPx(20)
            y = dpToPx(80)
        }

        val card = FrameLayout(this).apply {
            val cornerRadius = if (config.facecamShape == FacecamShape.CIRCLE) (sizePx / 2f) else dpToPx(16).toFloat()
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }
            elevation = dpToPx(8).toFloat()
            setBackgroundColor(Color.BLACK)
        }

        val previewView = PreviewView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        card.addView(previewView)

        // Make facecam draggable
        card.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        params.x = initialX - dx
                        params.y = initialY - dy
                        try {
                            windowManager?.updateViewLayout(card, params)
                        } catch (_: Exception) {}
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(card, params)
            facecamView = card
            bindFacecamCamera(previewView)
        } catch (_: Exception) {}
    }

    private fun bindFacecamCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (_: Exception) {
                // Front camera unavailable or permission missing
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun hideOverlay() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
    }

    private fun hideFacecam() {
        facecamView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
            facecamView = null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        hideOverlay()
        hideFacecam()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
