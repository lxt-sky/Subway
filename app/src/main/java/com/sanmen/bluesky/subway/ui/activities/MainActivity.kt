package com.sanmen.bluesky.subway.ui.activities

import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.gyf.barlibrary.ImmersionBar
import com.sanmen.bluesky.subway.Constant
import com.sanmen.bluesky.subway.R
import com.sanmen.bluesky.subway.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_main.*



class MainActivity : BaseActivity(), View.OnClickListener {

    private var exitTime: Long = 0

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun initImmersionBar() {
        super.initImmersionBar()

//        supportActionBar!!.hide()
        ImmersionBar.with(this).statusBarColor(R.color.whiteStatusBar).statusBarDarkFont(true).init()
    }

    override fun initData() {

    }

    override fun initView() {

        btnUpStream.setOnClickListener(this)
        btnDownStream.setOnClickListener(this)
        ivSetting.setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        val intent = Intent()
        when(v!!.id){
            R.id.btnUpStream->{
                intent.run {
                    this.setClass(this@MainActivity,ConnectActivity::class.java)
                    this.putExtra(Constant.DRIVE_DIRECTION,0)

                }
            }
            R.id.btnDownStream->{
                intent.run {
                    this.setClass(this@MainActivity,ConnectActivity::class.java)
                    this.putExtra(Constant.DRIVE_DIRECTION,1)
                }
            }
            R.id.ivSetting->{
                intent.run {
                    this.setClass(this@MainActivity,SettingActivity::class.java)

                }
            }
        }
        startActivity(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - exitTime > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show()
                exitTime = System.currentTimeMillis()
            } else {
                System.exit(0)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


}

