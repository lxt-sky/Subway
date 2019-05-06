package com.sanmen.bluesky.subway.adapters

import android.bluetooth.BluetoothDevice
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.sanmen.bluesky.subway.R

/**
 * @author lxt_bluesky
 * @date 2019/5/6
 * @description
 */
class DeviceAdapter(data:MutableList<BluetoothDevice>):
    BaseQuickAdapter<BluetoothDevice, BaseViewHolder>(R.layout.item_device, data) {
    override fun convert(helper: BaseViewHolder?, item: BluetoothDevice?) {

        item?:return
        helper?:return
        helper.setText(R.id.tvDeviceTitle,if (item.name==null) "Null" else item.name)
        helper.setText(R.id.tvDeviceAddr,item.address)
    }
}