package com.example.timemacro

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClickerService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayButton: Button? = null
    private var coordinateButton: Button? = null
    private var autoClickButton: Button? = null
    private var closeButton: Button? = null
    private var targetView: ImageView? = null
    private var timeTextView: TextView? = null // 시간 표시를 위한 TextView
    private var isAutoClicking = false
    private val handler = Handler(Looper.getMainLooper())

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

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

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
            setOnClickListener { showTargetView() }
        }

        autoClickButton = Button(this).apply {
            text = "Start Auto Click"
            setOnClickListener {
                if (!isAutoClicking) {
                    startAutoClick()
                } else {
                    stopAutoClick()
                }
            }
        }

        closeButton = Button(this).apply {
            text = "X"
            setOnClickListener {
                stopAutoClick()
                stopSelf()
            }
        }

        // TextView 초기화 및 설정
        timeTextView = TextView(this).apply {
            textSize = 20f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(10, 10, 10, 10)
            setBackgroundColor(android.graphics.Color.LTGRAY)
        }

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

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
        windowManager!!.addView(overlayButton, buttonParams)

        buttonParams.x = 0
        buttonParams.y = 150
        windowManager!!.addView(coordinateButton, buttonParams)

        buttonParams.x = 0
        buttonParams.y = 300
        windowManager!!.addView(autoClickButton, buttonParams)

        buttonParams.x = 0
        buttonParams.y = 450
        windowManager!!.addView(closeButton, buttonParams)

        // TextView 위치 설정
        val timeTextParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        timeTextParams.gravity = Gravity.TOP or Gravity.END // 오른쪽 상단 정렬
        timeTextParams.x = 0
        timeTextParams.y = 0
        windowManager!!.addView(timeTextView, timeTextParams)

        // 시간 업데이트 시작
        updateTime()
    }

    private fun showTargetView() {
        if (targetView == null) {
            targetView = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_mylocation)
            }

            targetParams.gravity = Gravity.TOP or Gravity.START
            targetParams.x = 500
            targetParams.y = 500
            windowManager!!.addView(targetView, targetParams)

            targetView!!.setOnTouchListener { view: View, event: MotionEvent ->
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        targetParams.x = event.rawX.toInt() - view.width / 2
                        targetParams.y = event.rawY.toInt() - view.height / 2
                        windowManager!!.updateViewLayout(view, targetParams)
                    }
                    MotionEvent.ACTION_UP -> view.performClick()
                }
                true
            }
        }
    }

    private fun startAutoClick() {
        isAutoClicking = true
        autoClickButton?.text = "Stop Auto Click"
        scheduleHourlyClick()
    }

    private fun stopAutoClick() {
        isAutoClicking = false
        autoClickButton?.text = "Start Auto Click"
        handler.removeCallbacksAndMessages(null)
    }

    private fun scheduleHourlyClick() {
        val scheduleClickRunnable = object : Runnable {
            override fun run() {
                if (isAutoClicking) {
                    val calendar = Calendar.getInstance()
                    val seconds = calendar[Calendar.SECOND]
                    val milliseconds = calendar[Calendar.MILLISECOND]

                    var delay = ((59 * 1000 + 300) - (seconds * 1000 + milliseconds)).toLong()
                    if (delay < 0) {
                        delay += (60 * 1000).toLong()
                    }

                    handler.postDelayed({
                        if (isAutoClicking) {
                            performClickAtPosition(targetParams.x.toFloat(), targetParams.y.toFloat())
                            handler.postDelayed(this, 60 * 1000)
                        }
                    }, delay)
                }
            }
        }

        handler.post(scheduleClickRunnable)
    }

    // 시간 업데이트 함수
    private fun updateTime() {
        val timeFormat = SimpleDateFormat("HH:mm:ss.S", Locale.getDefault())
        val runnableCode = object : Runnable {
            override fun run() {
                val currentTime = timeFormat.format(Calendar.getInstance().time)
                timeTextView?.text = currentTime
                handler.postDelayed(this, 100) // 0.1초마다 업데이트
            }
        }
        handler.post(runnableCode)
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
        overlayButton?.let { windowManager!!.removeView(it) }
        coordinateButton?.let { windowManager!!.removeView(it) }
        autoClickButton?.let { windowManager!!.removeView(it) }
        closeButton?.let { windowManager!!.removeView(it) }
        targetView?.let { windowManager!!.removeView(it) }
        timeTextView?.let { windowManager!!.removeView(it) } // TextView 제거
        stopAutoClick()
    }
}