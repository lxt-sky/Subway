package com.sanmen.bluesky.subway.ui.activities

import android.net.Uri
import com.gyf.barlibrary.ImmersionBar
import com.sanmen.bluesky.subway.R
import com.sanmen.bluesky.subway.ui.base.BaseActivity
import com.sanmen.bluesky.subway.ui.fragments.SettingFragment
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : BaseActivity(),SettingFragment.OnFragmentInteractionListener {

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLayoutId(): Int {

        return R.layout.activity_setting
    }

    override fun initImmersionBar() {
        super.initImmersionBar()
        ImmersionBar.with(this).titleBar(R.id.toolBar).init()
    }

    override fun initData() {
        super.initData()
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
