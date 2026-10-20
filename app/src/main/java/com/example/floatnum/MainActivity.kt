package com.example.floatnum

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        val info = TextView(this).apply {
            text = "نافذة صغيرة تفضل فوق التطبيقات: اكتب الرقم ودوس \"نسخ\"."
            textSize = 18f
            gravity = Gravity.CENTER
        }
        val start = Button(this).apply { text = "شغّل النافذة" }
        val stop = Button(this).apply { text = "اقفل النافذة" }
        start.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                startForegroundService(Intent(this, FloatingService::class.java))
                finish()
            }
        }
        stop.setOnClickListener { stopService(Intent(this, FloatingService::class.java)) }
        root.addView(info)
        root.addView(start)
        root.addView(stop)
        setContentView(root)
    }
}
