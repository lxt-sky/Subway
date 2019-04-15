package com.s

import android.content.Context
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.sanmen.bluesky.subway.R
import com.sanmen.bluesky.subway.data.bean.DriveRecord


/**
 * @author lxt_bluesky
 * @date 2019/3/27
 * @description
 */
class DriveAdapter(context: Context, data: MutableList<DriveRecord>?) :
    BaseQuickAdapter<DriveRecord, BaseViewHolder>(R.layout.item_drive, data) {

    override fun convert(helper: BaseViewHolder?, item: DriveRecord?) {
        helper?:return
        item?:return
        helper.setText(R.id.tvDriveName, item.driveName)
            .setText(R.id.tvDriveTime,item.driveBeginTime)
//            .addOnClickListener(R.id.llDriveLayout)

    }


}