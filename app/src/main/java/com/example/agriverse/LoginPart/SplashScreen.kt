package com.example.agriverse.LoginPart

import android.content.Intent
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.agriverse.R
import androidx.core.net.toUri

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        val videoView = findViewById<VideoView>(R.id.videoView)

        val videoUri = "android.resource://$packageName/${R.raw.farmerassest}".toUri()
        videoView.setVideoURI(videoUri)

        videoView.setOnCompletionListener {
            startActivity(Intent(this, Login_Activity::class.java))
            finish()
        }

        videoView.start()
    }
}
