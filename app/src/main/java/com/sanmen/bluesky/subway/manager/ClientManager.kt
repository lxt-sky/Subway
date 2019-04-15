package com.sanmen.bluesky.subway.manager

import com.inuker.bluetooth.library.BluetoothClient
import com.sanmen.bluesky.subway.MyApplication

/**
 * @author lxt_bluesky
 * @date 2019/4/9
 * @description
 */
object ClientManager {
    private var bluetoothClient: BluetoothClient? = null

    fun getClient(): BluetoothClient {
        if (bluetoothClient==null){
            synchronized(ClientManager::class){
                if (bluetoothClient==null){
                    bluetoothClient = BluetoothClient(MyApplication.getInstance())
                }
            }
        }
        return this.bluetoothClient!!
    }

}