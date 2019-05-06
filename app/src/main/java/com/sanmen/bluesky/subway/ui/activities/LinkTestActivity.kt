package com.sanmen.bluesky.subway.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sanmen.bluesky.subway.R
import kotlinx.android.synthetic.main.activity_about.*

class LinkTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_link_test)

        setSupportActionBar(toolBar)

        toolBar.setNavigationOnClickListener {
            onBackPressed()
        }


    }

}
