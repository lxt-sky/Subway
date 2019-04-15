package com.sanmen.bluesky.subway.ui.base

import android.view.ViewGroup


/**
 * @author lxt_bluesky
 * @date 2019/3/28
 * @description 监听器接口
 */
interface BaseListener {

    fun isItemViewType(position:Int,obj: Any):Boolean

    fun getViewHolder(parent:ViewGroup):BaseViewHolder
}