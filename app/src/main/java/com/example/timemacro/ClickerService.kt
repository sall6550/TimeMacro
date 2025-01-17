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
import java.util.Calendar

class ClickerService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayButton: Button? = null
    private var coordinateButton: Button? = null
    private var autoClickButton: Button? = null
    private var closeButton: Button? = null // X 버튼 추가
    private var targetView: ImageView? = null
    private var isAutoClicking = false // 자동 클릭 상태 추적 변수
    private val handler = Handler(Looper.getMainLooper()) // 메인 스레드 핸들러

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

        // X 버튼 추가 및 설정
        closeButton = Button(this).apply {
            text = "X"
            setOnClickListener {
                stopAutoClick() // 자동 클릭 중지
                stopSelf() // 서비스 종료
            }
        }

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        // 버튼 레이아웃 파라미터
        val buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        buttonParams.gravity = Gravity.TOP or Gravity.START

        // 각 버튼 위치 설정
        buttonParams.x = 0
        buttonParams.y = 0
        windowManager!!.addView(overlayButton, buttonParams)

        buttonParams.x = 0
        buttonParams.y = 150 // overlayButton 아래
        windowManager!!.addView(coordinateButton, buttonParams)

        buttonParams.x = 0
        buttonParams.y = 300 // coordinateButton 아래
        windowManager!!.addView(autoClickButton, buttonParams)

        buttonParams.x = 0
        buttonParams.y = 450 // autoClickButton 아래
        windowManager!!.addView(closeButton, buttonParams) // X 버튼 추가
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
        handler.removeCallbacksAndMessages(null) // 핸들러의 모든 콜백 및 메시지 제거
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
                            handler.postDelayed(this, 60 * 1000) // 60초마다 반복
                        }
                    }, delay)
                }
            }
        }

        handler.post(scheduleClickRunnable)
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
        // 모든 뷰 제거
        overlayButton?.let { windowManager!!.removeView(it) }
        coordinateButton?.let { windowManager!!.removeView(it) }
        autoClickButton?.let { windowManager!!.removeView(it) }
        closeButton?.let { windowManager!!.removeView(it) } // X 버튼 제거
        targetView?.let { windowManager!!.removeView(it) }

        // 핸들러 콜백 제거
        stopAutoClick()
    }
}