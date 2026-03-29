package com.example.demo1

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // GitHub profile
        findViewById<android.widget.Button>(R.id.btn_github).setOnClickListener {
            openUrl("https://github.com/gulu43")
        }

        // Open source GitHub repo button
        findViewById<android.widget.Button>(R.id.btn_open_source).setOnClickListener {
            openUrl("https://github.com/gulu43/Notification_widget_ai")
        }

        // Chai4Me donation button
        findViewById<android.view.View>(R.id.btn_donate).setOnClickListener {
            openUrl("https://www.chai4.me/gulu")
        }

        // Chai link text with underline
        val chaiLink = findViewById<android.widget.TextView>(R.id.tv_about_chai_link)
        chaiLink.paintFlags = chaiLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        chaiLink.setOnClickListener {
            openUrl("https://www.chai4.me/gulu")
        }

        // Load Chai4Me wordmark image from URL
        com.squareup.picasso.Picasso.get()
            .load("https://chai4.me/icons/wordmark.png")
            .into(findViewById<android.widget.ImageView>(R.id.chai_wordmark_img))

        // Back button
        findViewById<android.widget.ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}