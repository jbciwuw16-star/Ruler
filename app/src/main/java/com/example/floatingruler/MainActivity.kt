package com.example.floatingruler

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }
        val info = TextView(this).apply {
            text = "Penggaris Floating\n\n" +
                "• Geser badan garis = pindahin\n" +
                "• Tarik bulatan merah di ujung = ubah arah & panjang\n" +
                "• Matiin lewat tombol Stop di notifikasi"
            textSize = 16f
            setPadding(0, 0, 0, pad)
        }
        val start = Button(this).apply {
            text = "Mulai Penggaris"
            setOnClickListener { startRuler() }
        }
        val stop = Button(this).apply {
            text = "Stop Penggaris"
            setOnClickListener { stopService(Intent(this@MainActivity, RulerService::class.java)) }
        }
        root.addView(info)
        root.addView(start)
        root.addView(stop)
        setContentView(root)
    }

    private fun startRuler() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Aktifkan izin 'Tampil di atas aplikasi lain' dulu", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        startForegroundService(Intent(this, RulerService::class.java))
        finish() // tutup activity, penggaris tetap floating
    }
}
