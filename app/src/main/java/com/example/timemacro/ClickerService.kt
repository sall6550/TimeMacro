package com.example.timemacro

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.util.Log

class ClickerService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayButton: Button
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        overlayButton = Button(this).apply {
            text = "Click Here"
            setOnClickListener {
                performClickAtPosition(500, 500)  // 예시 좌표, Int 형으로 변경
            }
        }

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        windowManager.addView(overlayButton, params)
    }

    // performClickAtPosition 함수 수정: Float 형 x, y를 Int 형으로 받도록 변경
    private fun performClickAtPosition(x: Int, y: Int) {
        ClickerAccessibilityService.instance?.let { service ->
            if (!service.performClick(x, y)) {
                Log.e("ClickerService", "Failed to perform click")
            }
        } ?: Log.e("ClickerService", "Accessibility Service not running")
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(overlayButton)
    }
}