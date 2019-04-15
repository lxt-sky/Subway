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
class AlarmRecordAdapter(data: MutableList<AlarmInfo>?) :
    BaseQuickAdapter<AlarmInfo, BaseViewHolder>(R.layout.item_alarm_1, data) {
    override fun convert(helper: BaseViewHolder?, item: AlarmInfo?) {
        item?:return
        helper?:return
        helper.setText(R.id.tvAlarmText,item.alarmText)
            .setText(R.id.tvAlarmTime,item.alarmTime)
    }
}