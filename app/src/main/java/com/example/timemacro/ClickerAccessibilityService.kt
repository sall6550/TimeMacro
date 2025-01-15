package com.example.timemacro

// ClickerAccessibilityService.kt
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class ClickerAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    // performClick 함수 수정: Float 형 x, y를 Int 형으로 받도록 변경
    fun performClick(x: Int, y: Int): Boolean {
        // Int 형 x, y를 Float 형으로 변환하여 Path 객체 생성에 사용
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d("ClickerService", "Click completed at x:$x, y:$y")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.e("ClickerService", "Click cancelled")
            }
        }, null)
    }

    companion object {
        // 서비스 인스턴스를 저장할 정적 변수
        var instance: ClickerAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("ClickerService", "Accessibility Service Connected")
    }
}
