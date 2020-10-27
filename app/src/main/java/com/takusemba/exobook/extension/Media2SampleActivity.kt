package com.takusemba.exobook.extension

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.takusemba.exobook.R

class Media2SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_button)

        val button = findViewById<Button>(R.id.button)
        button.text = getString(R.string.button_start_background_playback)
        button.setOnClickListener {
            val intent = Intent(this, Media2SampleService::class.java)
            startService(intent)
        }
    }
}