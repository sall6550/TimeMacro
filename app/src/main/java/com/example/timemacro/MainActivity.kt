package com.example.timemacro

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    private val OVERLAY_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 권한 요청 버튼
        findViewById<Button>(R.id.startButton).setOnClickListener {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE)
        } else {
            Log.d("MainActivity", "Overlay permission already granted")
            checkAccessibilityAndStartService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Settings.canDrawOverlays(this)) {
                checkAccessibilityAndStartService()
            }
        }
    }

    private fun checkAccessibilityAndStartService() {
        if (!checkAccessibilityPermission(this)) {
            showAccessibilityPermissionDialog(this)
        } else {
            startClickerService()
        }
    }

    private fun startClickerService() {
        try {
            val serviceIntent = Intent(this, ClickerService::class.java)
            startService(serviceIntent)
            Log.d("MainActivity", "Service started successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting service: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun checkAccessibilityPermission(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        for (enabledService in enabledServices) {
            val serviceInfo = enabledService.resolveInfo.serviceInfo
            if (serviceInfo.packageName == context.packageName && serviceInfo.name == ClickerAccessibilityService::class.java.name) {
                return true
            }
        }
        return false
    }

    private fun showAccessibilityPermissionDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("접근성 권한 필요")
            .setMessage("앱을 사용하기 위해 접근성 권한이 필요합니다. 설정 화면으로 이동하시겠습니까?")
            .setPositiveButton("이동") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }
}