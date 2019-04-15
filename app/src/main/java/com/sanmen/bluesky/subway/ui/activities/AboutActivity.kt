package com.sanmen.bluesky.subway.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sanmen.bluesky.subway.R
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.activity_about.toolBar
import kotlinx.android.synthetic.main.activity_alarm.*

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setSupportActionBar(toolBar)

        toolBar.setNavigationOnClickListener {
            onBackPressed()
        }

    }



}
