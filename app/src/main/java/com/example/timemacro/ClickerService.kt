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

/**
 * 오버레이 클릭 서비스
 * 화면 위에 떠있는 버튼을 통해 특정 위치에 자동 클릭을 수행합니다.
 */
class ClickerService : Service() {
    // UI 컴포넌트
    private var windowManager: WindowManager? = null
    private var overlayButton: Button? = null
    private var coordinateButton: Button? = null
    private var autoClickButton: Button? = null
    private var closeButton: Button? = null
    private var targetView: ImageView? = null
    private var timeTextView: TextView? = null
    private var numberPickerButton: Button? = null

    // 상태 관리
    private var isAutoClicking = false
    private val handler = Handler(Looper.getMainLooper())

    // 윈도우 매니저 파라미터 설정
    private val params = createDefaultLayoutParams()
    private val targetParams = createTargetLayoutParams()

    override fun onBind(intent: Intent): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        initializeViews()
        setupViewPositions()
        updateTime()
    }

    /**
     * 기본 레이아웃 파라미터 생성
     */
    private fun createDefaultLayoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    /**
     * 타겟 뷰를 위한 레이아웃 파라미터 생성
     */
    private fun createTargetLayoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    )

    /**
     * UI 컴포넌트 초기화
     */
    private fun initializeViews() {
        initializeOverlayButton()
        initializeCoordinateButton()
        initializeAutoClickButton()
        initializeCloseButton()
        initializeTimeTextView()
        initializeNumberPickerButton()
    }

    /**
     * 오버레이 버튼 초기화
     */
    private fun initializeOverlayButton() {
        overlayButton = Button(this).apply {
            text = "Click Here"
            setOnClickListener {
                performClickAtPosition(targetParams.x.toFloat(), targetParams.y.toFloat())
            }
        }
    }

    /**
     * 좌표 설정 버튼 초기화
     */
    private fun initializeCoordinateButton() {
        coordinateButton = Button(this).apply {
            text = "Set Coordinate"
            setOnClickListener { showTargetView() }
        }
    }

    /**
     * 자동 클릭 버튼 초기화
     */
    private fun initializeAutoClickButton() {
        autoClickButton = Button(this).apply {
            text = "Start Auto Click"
            setOnClickListener {
                if (!isAutoClicking) startAutoClick() else stopAutoClick()
            }
        }
    }

    /**
     * 닫기 버튼 초기화
     */
    private fun initializeCloseButton() {
        closeButton = Button(this).apply {
            text = "X"
            setOnClickListener {
                stopAutoClick()
                stopSelf()
            }
        }
    }

    /**
     * 시간 표시 텍스트뷰 초기화
     */
    private fun initializeTimeTextView() {
        timeTextView = TextView(this).apply {
            textSize = 20f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(10, 10, 10, 10)
            setBackgroundColor(android.graphics.Color.LTGRAY)
        }
    }

    /**
     * 숫자 선택 버튼 초기화
     */
    private fun initializeNumberPickerButton() {
        numberPickerButton = Button(this).apply {
            text = "Pick a Number"
            setOnClickListener { showNumberPickerDialog() }
        }
    }

    /**
     * 뷰 위치 설정
     */
    private fun setupViewPositions() {
        val buttonParams = createDefaultLayoutParams()
        buttonParams.gravity = Gravity.TOP or Gravity.START

        // 각 버튼 위치 설정
        addViewWithPosition(overlayButton, buttonParams, 0, 0)
        addViewWithPosition(coordinateButton, buttonParams, 0, 150)
        addViewWithPosition(autoClickButton, buttonParams, 0, 300)
        addViewWithPosition(closeButton, buttonParams, 0, 450)
        addViewWithPosition(numberPickerButton, buttonParams, 0, 600)

        // 시간 텍스트뷰 위치 설정
        val timeTextParams = createDefaultLayoutParams()
        timeTextParams.gravity = Gravity.TOP or Gravity.END
        addViewWithPosition(timeTextView, timeTextParams, 0, 0)
    }

    /**
     * 뷰를 특정 위치에 추가
     */
    private fun addViewWithPosition(view: View?, params: WindowManager.LayoutParams, x: Int, y: Int) {
        params.x = x
        params.y = y
        view?.let { windowManager?.addView(it, params) }
    }

    /**
     * 숫자 선택 다이얼로그 표시
     */
    private fun showNumberPickerDialog() {
        val numbers = (0..9).map { it.toString() }.toTypedArray()
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Choose a number")
            .setItems(numbers) { _, which ->
                Log.d("ClickerService", "Selected number: ${numbers[which]}")
            }
            .create()
            .apply {
                window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            }
            .show()
    }

    /**
     * 타겟 뷰 표시
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showTargetView() {
        if (targetView == null) {
            targetView = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_mylocation)
                setOnTouchListener(createTargetViewTouchListener())
            }

            targetParams.apply {
                gravity = Gravity.TOP or Gravity.START
                x = 500
                y = 500
            }
            windowManager?.addView(targetView, targetParams)
        }
    }

    /**
     * 타겟 뷰 터치 리스너 생성
     */
    private fun createTargetViewTouchListener() = View.OnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                targetParams.x = event.rawX.toInt() - view.width / 2
                targetParams.y = event.rawY.toInt() - view.height / 2
                windowManager?.updateViewLayout(view, targetParams)
            }
            MotionEvent.ACTION_UP -> view.performClick()
        }
        true
    }

    /**
     * 자동 클릭 시작
     */
    private fun startAutoClick() {
        isAutoClicking = true
        autoClickButton?.text = "Stop Auto Click"
        scheduleHourlyClick()
    }

    /**
     * 자동 클릭 중지
     */
    private fun stopAutoClick() {
        isAutoClicking = false
        autoClickButton?.text = "Start Auto Click"
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * 매시 59분 59.3초에 클릭 실행
     */
    private fun scheduleHourlyClick() {
        val scheduleClickRunnable = object : Runnable {
            override fun run() {
                if (isAutoClicking) {
                    val calendar = Calendar.getInstance()
                    val seconds = calendar[Calendar.SECOND]
                    val milliseconds = calendar[Calendar.MILLISECOND]

                    if (seconds == 59 && milliseconds >= 300) {
                        performClickAtPosition(targetParams.x.toFloat(), targetParams.y.toFloat())
                    }

                    handler.postDelayed(this, 10)
                }
            }
        }
        handler.post(scheduleClickRunnable)
    }

    /**
     * 현재 시간 업데이트
     */
    private fun updateTime() {
        val timeFormat = SimpleDateFormat("HH:mm:ss.S", Locale.getDefault())
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                timeTextView?.text = timeFormat.format(Calendar.getInstance().time)
                handler.postDelayed(this, 10)
            }
        }
        handler.post(updateTimeRunnable)
    }

    /**
     * 지정된 위치에서 클릭 수행
     */
    private fun performClickAtPosition(x: Float, y: Float) {
        Log.d("ClickerService", "클릭 시작: ${System.currentTimeMillis()}")
        ClickerAccessibilityService.instance?.performClick(x, y)
        Log.d("ClickerService", "클릭 종료: ${System.currentTimeMillis()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        // 모든 뷰 제거
        listOf(overlayButton, coordinateButton, autoClickButton,
            closeButton, targetView, timeTextView, numberPickerButton)
            .forEach { it?.let { view -> windowManager?.removeView(view) } }
        stopAutoClick()
    }
}