package com.takusemba.exobook.extension

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.takusemba.exobook.R

class LeanbackSampleActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leanback)
    }
}