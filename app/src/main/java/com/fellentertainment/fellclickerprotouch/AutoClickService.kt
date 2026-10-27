package com.fellentertainment.fellclickerprotouch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import kotlin.random.Random

class AutoClickService : AccessibilityService() {

    companion object {
        var instance: AutoClickService? = null
        var onStateChanged: ((Boolean) -> Unit)? = null
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var clickJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stopClicking() }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            if (ConfigManager.isClicking) {
                stopClicking()
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    fun startClicking() {
        if (ConfigManager.isClicking) return
        ConfigManager.isClicking = true
        onStateChanged?.invoke(true)

        val cfg = ConfigManager.currentConfig
        val startTime = System.currentTimeMillis()
        var clickCount = 0

        clickJob = serviceScope.launch {
            while (isActive && ConfigManager.isClicking) {
                if (cfg.maxTimeSeconds > 0) {
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
                    if (elapsedSec >= cfg.maxTimeSeconds) {
                        stopClicking()
                        break
                    }
                }

                if (cfg.maxClicks > 0 && clickCount >= cfg.maxClicks) {
                    stopClicking()
                    break
                }

                val jitterX = (cfg.targetX + Random.nextInt(-2, 3)).toFloat().coerceAtLeast(0f)
                val jitterY = (cfg.targetY + Random.nextInt(-2, 3)).toFloat().coerceAtLeast(0f)

                withContext(Dispatchers.Main) {
                    dispatchTap(jitterX, jitterY, cfg.holdDurationMs)
                }

                if (cfg.doubleClick) {
                    delay(45L)
                    withContext(Dispatchers.Main) {
                        dispatchTap(jitterX, jitterY, cfg.holdDurationMs)
                    }
                }

                clickCount++

                val randomDelta = if (cfg.randomOffsetMs > 0) {
                    Random.nextLong(-cfg.randomOffsetMs, cfg.randomOffsetMs + 1)
                } else 0L

                val finalDelay = (cfg.intervalMs + randomDelta).coerceAtLeast(20L)
                delay(finalDelay)
            }
        }
    }

    fun stopClicking() {
        ConfigManager.isClicking = false
        clickJob?.cancel()
        MainScope().launch { onStateChanged?.invoke(false) }
    }

    private fun dispatchTap(x: Float, y: Float, durationMs: Long) {
        val path = Path().apply { moveTo(x, y) }
        val safeDuration = durationMs.coerceIn(10L, 60000L)
        val stroke = GestureDescription.StrokeDescription(path, 0, safeDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onDestroy() {
        stopClicking()
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }
}