package com.sanmen.bluesky.subway.ui.activities

import com.gyf.barlibrary.ImmersionBar
import com.sanmen.bluesky.subway.R
import com.sanmen.bluesky.subway.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_about.toolBar

class AboutActivity : BaseActivity() {
    override fun getLayoutId(): Int {
        return R.layout.activity_about
    }

    override fun initImmersionBar() {
        super.initImmersionBar()

        ImmersionBar.with(this).titleBar(toolBar).init()

    }


    override fun initView() {
        super.initView()

        toolBar.run {
            this.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }


}
