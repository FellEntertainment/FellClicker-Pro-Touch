package com.fellentertainment.fellclickerprotouch

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var controlBar: LinearLayout
    private lateinit var targetCrosshair: TextView
    private lateinit var playPauseBtn: TextView
    private lateinit var pickTargetBtn: TextView
    private lateinit var coordLabel: TextView
    private var fullScreenPicker: FrameLayout? = null

    private val targetSize = 130
    private lateinit var targetParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ConfigManager.isOverlayActive = true

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        targetParams = WindowManager.LayoutParams(
            targetSize, targetSize, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = ConfigManager.currentConfig.targetX - (targetSize / 2)
            y = ConfigManager.currentConfig.targetY - (targetSize / 2)
        }

        targetCrosshair = TextView(this).apply {
            text = "⊕"
            textSize = 28f
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#660F0F15"))
                setStroke(4, Color.parseColor("#00E5FF"))
            }
        }

        val barParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 250
        }

        controlBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            background = GradientDrawable().apply {
                cornerRadius = 32f
                setColor(Color.parseColor("#E614141D"))
                setStroke(3, Color.parseColor("#6C5CE7"))
            }
        }

        val titleView = TextView(this).apply {
            text = "FELL TOUCH\n(2x Tipp = Stopp)"
            textSize = 10f
            setTextColor(Color.parseColor("#8E8EA0"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }

        playPauseBtn = TextView(this).apply {
            text = "▶ START"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(28, 18, 28, 18)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(Color.parseColor("#10B981"))
            }
            setOnClickListener {
                val service = AutoClickService.instance
                if (service == null) {
                    Toast.makeText(context, "Bitte Bedienungshilfe aktivieren!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (ConfigManager.isClicking) {
                    service.stopClicking()
                } else {
                    setCrosshairTouchable(false)
                    service.startClicking()
                }
            }
        }

        pickTargetBtn = TextView(this).apply {
            text = "🎯 Ziel antippen"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(20, 14, 20, 14)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(Color.parseColor("#6C5CE7"))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 14 }
            layoutParams = lp
            setOnClickListener {
                if (ConfigManager.isClicking) {
                    AutoClickService.instance?.stopClicking()
                }
                openTouchTargetPicker(overlayType)
            }
        }

        coordLabel = TextView(this).apply {
            text = "X:${ConfigManager.currentConfig.targetX}\nY:${ConfigManager.currentConfig.targetY}"
            textSize = 11f
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
        }

        val closeBtn = TextView(this).apply {
            text = "✕ Schließen"
            textSize = 12f
            setTextColor(Color.parseColor("#EF4444"))
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            setOnClickListener {
                AutoClickService.instance?.stopClicking()
                stopSelf()
            }
        }

        controlBar.addView(titleView)
        controlBar.addView(playPauseBtn)
        controlBar.addView(pickTargetBtn)
        controlBar.addView(coordLabel)
        controlBar.addView(closeBtn)

        // Drag + Doppelklick-Not-Aus auf der Steuerleiste
        enableDragAndDoubleTap(controlBar, barParams, onDoubleTap = {
            if (ConfigManager.isClicking) {
                AutoClickService.instance?.stopClicking()
                Toast.makeText(this, "Doppelklick: Clicker gestoppt!", Toast.LENGTH_SHORT).show()
            }
        }) { _, _ -> }

        // Drag für Fadenkreuz
        enableDragAndDoubleTap(targetCrosshair, targetParams, onDoubleTap = {
            if (ConfigManager.isClicking) {
                AutoClickService.instance?.stopClicking()
            }
        }) { newX, newY ->
            updateTargetCoordinates(newX + (targetSize / 2), newY + (targetSize / 2))
        }

        AutoClickService.onStateChanged = { running ->
            playPauseBtn.text = if (running) "⏹ STOPP" else "▶ START"
            (playPauseBtn.background as GradientDrawable).setColor(
                Color.parseColor(if (running) "#EF4444" else "#10B981")
            )
            setCrosshairTouchable(!running)
        }

        windowManager.addView(targetCrosshair, targetParams)
        windowManager.addView(controlBar, barParams)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun openTouchTargetPicker(overlayType: Int) {
        if (fullScreenPicker != null) return

        val pickerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        fullScreenPicker = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#5500E5FF"))
            val hint = TextView(context).apply {
                text = "Tippe jetzt auf die gewünschte Stelle\nauf dem Bildschirm!"
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(40, 40, 40, 40)
                background = GradientDrawable().apply {
                    cornerRadius = 24f
                    setColor(Color.parseColor("#EE0B0B10"))
                    setStroke(4, Color.parseColor("#00E5FF"))
                }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            }
            addView(hint)

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val touchX = event.rawX.toInt()
                    val touchY = event.rawY.toInt()
                    updateTargetCoordinates(touchX, touchY)

                    targetParams.x = touchX - (targetSize / 2)
                    targetParams.y = touchY - (targetSize / 2)
                    windowManager.updateViewLayout(targetCrosshair, targetParams)

                    Toast.makeText(context, "Ziel gesetzt: X:$touchX Y:$touchY", Toast.LENGTH_SHORT).show()
                    windowManager.removeView(this)
                    fullScreenPicker = null
                    true
                } else false
            }
        }

        windowManager.addView(fullScreenPicker, pickerParams)
    }

    private fun updateTargetCoordinates(centerX: Int, centerY: Int) {
        ConfigManager.currentConfig = ConfigManager.currentConfig.copy(
            targetX = centerX,
            targetY = centerY
        )
        coordLabel.text = "X:$centerX\nY:$centerY"
    }

    private fun setCrosshairTouchable(touchable: Boolean) {
        targetParams.flags = if (touchable) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        targetCrosshair.alpha = if (touchable) 1.0f else 0.45f
        windowManager.updateViewLayout(targetCrosshair, targetParams)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableDragAndDoubleTap(
        view: View,
        params: WindowManager.LayoutParams,
        onDoubleTap: () -> Unit,
        onMoved: (Int, Int) -> Unit
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var lastTapTime = 0L

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 350L) {
                        onDoubleTap()
                    }
                    lastTapTime = now
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 4 || abs(dy) > 4) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(view, params)
                        onMoved(params.x, params.y)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        ConfigManager.isOverlayActive = false
        AutoClickService.onStateChanged = null
        fullScreenPicker?.let { windowManager.removeView(it) }
        if (::targetCrosshair.isInitialized) windowManager.removeView(targetCrosshair)
        if (::controlBar.isInitialized) windowManager.removeView(controlBar)
        super.onDestroy()
    }
}