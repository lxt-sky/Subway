package com.sanmen.bluesky.subway.ui.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.gyf.barlibrary.ImmersionBar

public abstract class BaseActivity : AppCompatActivity() {

    public val sharedPref:SharedPreferences by lazy {
        getSharedPreferences("sub_config",Context.MODE_PRIVATE)
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())

        initImmersionBar()
        initData()
        initView()
    }

    /**
     * 状态栏初始化
     */
    open fun initImmersionBar() {
        //启动沉浸式状态栏
        ImmersionBar.with(this).init()

    }

    /**
     * 获取子类布局ID
     */
    @LayoutRes
    abstract fun getLayoutId():Int

    /**
     * 数据初始化工作
     */
    open fun initData() {}

    /**
     * 页面初始化工作
     */
    open fun initView() {}

    override fun onPause() {
        super.onPause()


    }

    override fun onDestroy() {
        super.onDestroy()
        //防止内存泄漏
        ImmersionBar.with(this).destroy()
    }

}
