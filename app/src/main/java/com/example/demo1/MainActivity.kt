package com.example.demo1

import android.os.Bundle
import android.widget.TextView
import android.view.Gravity
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this).apply {
            text = "Ringer Widget Installed!\n\nTo use it:\n1. Long press on your home screen\n2. Select Widgets\n3. Find 'DEmo1' and drag the 'Ringer Mode Widget' to your screen."
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            textSize = 18f
        }
        setContentView(textView)
    }
}