package com.example.timemacro

import android.annotation.SuppressLint
import android.app.AlertDialog
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
    private var timeTextView: TextView? = null
    private var numberPickerButton: Button? = null // 숫자 선택 버튼 추가
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

        timeTextView = TextView(this).apply {
            textSize = 20f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(10, 10, 10, 10)
            setBackgroundColor(android.graphics.Color.LTGRAY)
        }

        // 숫자 선택 버튼 추가 및 설정
        numberPickerButton = Button(this).apply {
            text = "Pick a Number"
            setOnClickListener { showNumberPickerDialog() }
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

        buttonParams.x = 0
        buttonParams.y = 600 // closeButton 아래에 위치
        windowManager!!.addView(numberPickerButton, buttonParams) // 숫자 선택 버튼 추가

        val timeTextParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        timeTextParams.gravity = Gravity.TOP or Gravity.END
        timeTextParams.x = 0
        timeTextParams.y = 0
        windowManager!!.addView(timeTextView, timeTextParams)

        updateTime()
    }

    // 숫자 선택 다이얼로그를 표시하는 함수
    private fun showNumberPickerDialog() {
        val numbers = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert) // 테마 변경
            .setTitle("Choose a number")
            .setItems(numbers) { _, which ->
                Log.d("ClickerService", "Selected number: ${numbers[which]}")
            }
            .create().apply {
                // WindowManager.LayoutParams 설정
                window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            }
            .show()
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
                    val minutes = calendar[Calendar.MINUTE]
                    val seconds = calendar[Calendar.SECOND]
                    val milliseconds = calendar[Calendar.MILLISECOND]

                    // 59분 59.3초(59분 59300밀리초)일 때 클릭 실행
                    if (seconds == 59 && milliseconds >= 300) {
                        performClickAtPosition(targetParams.x.toFloat(), targetParams.y.toFloat())
                    }

                    // 10ms 간격으로 체크하여 더 정확한 타이밍 확보
                    handler.postDelayed(this, 10)
                }
            }
        }
        handler.post(scheduleClickRunnable)
    }

    private fun updateTime() {
        val timeFormat = SimpleDateFormat("HH:mm:ss.S", Locale.getDefault())
        val runnableCode = object : Runnable {
            override fun run() {
                val currentTime = timeFormat.format(Calendar.getInstance().time)
                timeTextView?.text = currentTime
                handler.postDelayed(this, 10)
            }
        }
        handler.post(runnableCode)
    }

    private fun performClickAtPosition(x: Float, y: Float) {
        Log.d("ClickerService", "클릭 시작: ${System.currentTimeMillis()}")
        ClickerAccessibilityService.instance?.performClick(x, y)
        Log.d("ClickerService", "클릭 종료: ${System.currentTimeMillis()}")

    }

    override fun onDestroy() {
        super.onDestroy()
        overlayButton?.let { windowManager!!.removeView(it) }
        coordinateButton?.let { windowManager!!.removeView(it) }
        autoClickButton?.let { windowManager!!.removeView(it) }
        closeButton?.let { windowManager!!.removeView(it) }
        targetView?.let { windowManager!!.removeView(it) }
        timeTextView?.let { windowManager!!.removeView(it) }
        numberPickerButton?.let { windowManager!!.removeView(it) } // 숫자 선택 버튼 제거
        stopAutoClick()
    }
}