package com.sanmen.bluesky.subway.adapters

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.sanmen.bluesky.subway.R

/**
 * @author lxt_bluesky
 * @date 2019/5/7
 * @description
 */
class LightDataAdapter(lightData:List<String>):
    BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_data,lightData) {
    override fun convert(helper: BaseViewHolder?, item: String?) {

        item?:return
        helper?:return
        helper.setText(R.id.tvLightData,item.toString())

    }

}