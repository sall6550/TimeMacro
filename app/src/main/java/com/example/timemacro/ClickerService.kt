package com.example.timemacro

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Context
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
    private var rapidClickButton: Button? = null

    // 상태 관리
    private var isAutoClicking = false
    private val handler = Handler(Looper.getMainLooper())
    private val PREFS_NAME = "ClickerPrefs"
    private val KEY_TARGET_X = "target_x"
    private val KEY_TARGET_Y = "target_y"
    private val KEY_SELECTED_MILLISECONDS = "selected_milliseconds"
    private var selectedMilliseconds: Int = 600  // 기본값 600

    // 윈도우 매니저 파라미터 설정
    private val params = createDefaultLayoutParams()
    private val targetParams = createTargetLayoutParams()

    override fun onBind(intent: Intent): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        selectedMilliseconds = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SELECTED_MILLISECONDS, 600)
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
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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
        initializeRapidClickButton()
    }

    /**
     * 오버레이 버튼 초기화
     */
    private fun initializeOverlayButton() {
        overlayButton = Button(this).apply {
            text = "1번 클릭(테스트용)"
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
            text = "좌표 설정"
            setOnClickListener { showTargetView() }
        }
    }

    /**
     * 자동 클릭 버튼 초기화
     */
    private fun initializeAutoClickButton() {
        autoClickButton = Button(this).apply {
            text = "59.x초에 연속클릭 수행"
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
                saveTargetPosition()
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
            text = "선택된 시간: ${selectedMilliseconds}ms"  // 초기값 표시
            setOnClickListener { showNumberPickerDialog() }
        }
    }

    /**
     * 빠른 연속 클릭 버튼 초기화
     */
    private fun initializeRapidClickButton() {
        rapidClickButton = Button(this).apply {
            text = "3초 연속 클릭"
            setOnClickListener {
                performRapidClicks(targetParams.x.toFloat(), targetParams.y.toFloat())
            }
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
        addViewWithPosition(rapidClickButton, buttonParams, 0, 750)  // 추가

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
            .setTitle("Choose a number (0-9)")
            .setItems(numbers) { _, which ->
                selectedMilliseconds = which * 100
                Log.d("ClickerService", "Selected milliseconds: $selectedMilliseconds")
                
                // 선택된 값을 SharedPreferences에 저장
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_SELECTED_MILLISECONDS, selectedMilliseconds)
                    .apply()

                numberPickerButton?.text = "선택된 시간: ${selectedMilliseconds}ms"
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
                layoutParams = WindowManager.LayoutParams(100, 100)
                setColorFilter(android.graphics.Color.rgb(0, 0, 139))
                setOnTouchListener(createTargetViewTouchListener())
            }

            targetParams.apply {
                gravity = Gravity.TOP or Gravity.START
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                x = prefs.getInt(KEY_TARGET_X, 500)  // 기본값 500
                y = prefs.getInt(KEY_TARGET_Y, 500)  // 기본값 500
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

                    // selectedMilliseconds 값을 사용하여 체크
                    if (seconds == 59 && milliseconds >= selectedMilliseconds) {
                        performRapidClicks(targetParams.x.toFloat(), targetParams.y.toFloat())
                        return
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

    private fun performRapidClicks(x: Float, y: Float, clickIntervalMs: Long = 100) {
        var clickCount = 0
        val startTime = System.currentTimeMillis()
        val duration = 3000 // 3초

        val rapidClickRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTime

                if (elapsedTime < duration) {
                    // 클릭 수행
                    performClickAtPosition(x, y)
                    clickCount++

                    // 로그 출력
                    Log.d("ClickerService", "빠른 클릭 수행: $clickCount, 경과 시간: ${elapsedTime}ms")

                    // 다음 클릭 예약
                    handler.postDelayed(this, clickIntervalMs)
                } else {
                    // 3초가 지나면 종료
                    Log.d("ClickerService", "빠른 클릭 종료 - 총 클릭 횟수: $clickCount")
                }
            }
        }

        // 첫 클릭 시작
        handler.post(rapidClickRunnable)
    }

    /**
     * 현재 타겟 위치 저장
     */
    private fun saveTargetPosition() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_TARGET_X, targetParams.x)
            putInt(KEY_TARGET_Y, targetParams.y)
            apply()
        }
        Log.d("ClickerService", "좌표 저장됨: x=${targetParams.x}, y=${targetParams.y}")
    }

    override fun onDestroy() {
        super.onDestroy()
        // 모든 뷰 제거
        listOf(overlayButton, coordinateButton, autoClickButton,
            closeButton, targetView, timeTextView, numberPickerButton,
            rapidClickButton)  // rapidClickButton 추가
            .forEach { it?.let { view -> windowManager?.removeView(view) } }
        stopAutoClick()
    }
}