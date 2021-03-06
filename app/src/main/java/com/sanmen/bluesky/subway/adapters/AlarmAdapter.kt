package com.sanmen.bluesky.subway.adapters

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.sanmen.bluesky.subway.R
import com.sanmen.bluesky.subway.data.bean.AlarmInfo

/**
 * @author lxt_bluesky
 * @date 2019/3/29
 * @description
 */
class AlarmAdapter(data:MutableList<AlarmInfo>):
    BaseQuickAdapter<AlarmInfo, BaseViewHolder>(R.layout.item_alarm_2,data) {
    override fun convert(helper: BaseViewHolder?, item: AlarmInfo?) {
        item?:return
        helper?:return
        helper.setText(R.id.tvTitle,if (item.direction==0) "上行-报警信息" else "下行-报警信息")
        helper.setText(R.id.tvAlarmTime,item.alarmTime)
            .setText(R.id.tvAlarmText,item.alarmText)

    }
}