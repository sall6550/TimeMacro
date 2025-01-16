package com.example.timemacro

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView

class ClickerService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayButton: Button
    private lateinit var coordinateButton: Button
    private var targetView: ImageView? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )
    private val targetParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    )

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        overlayButton = Button(this).apply {
            text = "Click Here"
            setOnClickListener {
                performClickAtPosition(targetParams.x.toFloat(), targetParams.y.toFloat())
            }
        }

        coordinateButton = Button(this).apply {
            text = "Set Coordinate"
            setOnClickListener {
                showTargetView()
            }
        }

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        // Add both buttons to the window
        val buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        buttonParams.gravity = Gravity.TOP or Gravity.START
        buttonParams.x = 0
        buttonParams.y = 0
        windowManager.addView(overlayButton, buttonParams)

        buttonParams.x = 0 // Adjust position as needed
        buttonParams.y = 100 // Place it below the overlayButton
        windowManager.addView(coordinateButton, buttonParams)
    }

    private fun showTargetView() {
        if (targetView == null) {
            targetView = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_mylocation) // Use a suitable icon
//                alpha = 0.5f // Make it semi-transparent
            }

            targetParams.gravity = Gravity.TOP or Gravity.START
            targetParams.x = 500 // Initial position
            targetParams.y = 500
            windowManager.addView(targetView, targetParams)

            targetView?.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        targetParams.x = event.rawX.toInt() - view.width / 2
                        targetParams.y = event.rawY.toInt() - view.height / 2
                        windowManager.updateViewLayout(view, targetParams)
                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
                true
            }
        }
    }

    private fun performClickAtPosition(x: Float, y: Float) {
        ClickerAccessibilityService.instance?.let { service ->
            if (!service.performClick(x, y)) {
                Log.e("ClickerService", "Failed to perform click")
            }
        } ?: Log.e("ClickerService", "Accessibility Service not running")
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(overlayButton)
        windowManager.removeView(coordinateButton)
        targetView?.let { windowManager.removeView(it) }
    }
}