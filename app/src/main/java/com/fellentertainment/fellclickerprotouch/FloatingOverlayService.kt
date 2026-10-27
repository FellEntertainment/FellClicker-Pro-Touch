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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var controlBar: LinearLayout
    private lateinit var targetCrosshair: TextView
    private lateinit var playPauseBtn: TextView
    private lateinit var coordLabel: TextView

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

        val targetSize = 130
        val targetParams = WindowManager.LayoutParams(
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
            text = "FELL"
            textSize = 11f
            setTextColor(Color.parseColor("#8E8EA0"))
            gravity = Gravity.CENTER
        }

        playPauseBtn = TextView(this).apply {
            text = "▶ START"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(28, 20, 28, 20)
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
                    setCrosshairTouchable(false, targetParams)
                    service.startClicking()
                }
            }
        }

        coordLabel = TextView(this).apply {
            text = "X:${ConfigManager.currentConfig.targetX}\nY:${ConfigManager.currentConfig.targetY}"
            textSize = 11f
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 14f
            setTextColor(Color.parseColor("#EF4444"))
            gravity = Gravity.CENTER
            setPadding(20, 12, 20, 12)
            setOnClickListener {
                AutoClickService.instance?.stopClicking()
                stopSelf()
            }
        }

        controlBar.addView(titleView)
        controlBar.addView(playPauseBtn)
        controlBar.addView(coordLabel)
        controlBar.addView(closeBtn)

        enableDrag(controlBar, barParams) { _, _ -> }

        enableDrag(targetCrosshair, targetParams) { newX, newY ->
            val centerX = newX + (targetSize / 2)
            val centerY = newY + (targetSize / 2)
            ConfigManager.currentConfig = ConfigManager.currentConfig.copy(
                targetX = centerX,
                targetY = centerY
            )
            coordLabel.text = "X:$centerX\nY:$centerY"
        }

        AutoClickService.onStateChanged = { running ->
            playPauseBtn.text = if (running) "⏹ STOPP" else "▶ START"
            (playPauseBtn.background as GradientDrawable).setColor(
                Color.parseColor(if (running) "#EF4444" else "#10B981")
            )
            setCrosshairTouchable(!running, targetParams)
        }

        windowManager.addView(targetCrosshair, targetParams)
        windowManager.addView(controlBar, barParams)
    }

    private fun setCrosshairTouchable(touchable: Boolean, params: WindowManager.LayoutParams) {
        params.flags = if (touchable) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        targetCrosshair.alpha = if (touchable) 1.0f else 0.45f
        windowManager.updateViewLayout(targetCrosshair, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableDrag(
        view: View,
        params: WindowManager.LayoutParams,
        onMoved: (Int, Int) -> Unit
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    onMoved(params.x, params.y)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        ConfigManager.isOverlayActive = false
        AutoClickService.onStateChanged = null
        if (::targetCrosshair.isInitialized) windowManager.removeView(targetCrosshair)
        if (::controlBar.isInitialized) windowManager.removeView(controlBar)
        super.onDestroy()
    }
}