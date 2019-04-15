package com.sanmen.bluesky.subway.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * @author lxt_bluesky
 * @date 2019/3/28
 * @description
 */

abstract class BaseViewHolder(itemView:View) : RecyclerView.ViewHolder(itemView) {

    constructor(parent:ViewGroup,layoutRes: Int) : this(
        LayoutInflater.from(parent.context).inflate(layoutRes,parent,false)
    )

    fun bindView(position:Int,obj:Any) {
        bindViewCast(position, obj)
    }

    abstract fun bindViewCast(position:Int, d: Any)
}