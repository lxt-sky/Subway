package com.sanmen.bluesky.subway

import android.app.Application
import com.inuker.bluetooth.library.BluetoothContext

/**
 * @author lxt_bluesky
 * @date 2019/4/9
 * @description
 */
class MyApplication: Application() {

    companion object {
        private var instance: MyApplication? = null

        fun getInstance(): MyApplication? {return instance}
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        BluetoothContext.set(this)


    }


}