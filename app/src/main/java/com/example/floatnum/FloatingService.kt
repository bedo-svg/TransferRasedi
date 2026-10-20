package com.example.floatnum

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var lp: WindowManager.LayoutParams
    private var root: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("float", "النافذة العائمة", NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(
            1,
            Notification.Builder(this, "float")
                .setContentTitle("نافذة النسخ شغالة")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .build()
        )
        if (root == null) addWindow()
        return START_STICKY
    }

    private fun setFocusable(on: Boolean) {
        lp.flags = if (on) lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        else lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        root?.let { wm.updateViewLayout(it, lp) }
    }

    private fun normalize(raw: String): String {
        val ar = "٠١٢٣٤٥٦٧٨٩"
        var d = raw.map { c -> val i = ar.indexOf(c); if (i >= 0) ('0' + i) else c }
            .filter { it.isDigit() }.joinToString("")
        if (d.length == 12 && d.startsWith("20")) d = "0" + d.substring(2)
        return d
    }

    private fun addWindow() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        lp = WindowManager.LayoutParams(
            px(260), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = px(16); y = px(200) }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(10), px(6), px(10), px(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E61B2420"))
                cornerRadius = px(14).toFloat()
            }
        }

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val handle = TextView(this).apply {
            text = "☰  اسحب من هنا"
            setTextColor(Color.parseColor("#9FB3A8"))
            textSize = 12f
            setPadding(0, px(6), 0, px(6))
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(px(12), px(2), px(4), px(2))
            setOnClickListener { stopSelf() }
        }
        bar.addView(handle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(close)

        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0
        handle.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = ox + (e.rawX - sx).toInt()
                    lp.y = oy + (e.rawY - sy).toInt()
                    wm.updateViewLayout(box, lp)
                }
            }
            true
        }

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val et = EditText(this).apply {
            hint = "الرقم"
            setHintTextColor(Color.parseColor("#7C8F85"))
            setTextColor(Color.WHITE)
            textSize = 18f
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine()
        }
        val copy = Button(this).apply { text = "نسخ" }
        row.addView(et, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(copy)

        et.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_DOWN) {
                setFocusable(true)
                et.post {
                    et.requestFocus()
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
                }
            }
            false
        }

        copy.setOnClickListener {
            val num = normalize(et.text.toString())
            if (num.isEmpty()) {
                Toast.makeText(this, "اكتب الرقم الأول", Toast.LENGTH_SHORT).show()
            } else {
                getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("number", num))
                Toast.makeText(this, "اتنسخ: $num", Toast.LENGTH_SHORT).show()
                et.setText("")
            }
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(et.windowToken, 0)
            et.clearFocus()
            setFocusable(false)
        }

        box.addView(bar)
        box.addView(row)
        root = box
        wm.addView(box, lp)
    }

    override fun onDestroy() {
        root?.let { wm.removeView(it) }
        root = null
        super.onDestroy()
    }
}
