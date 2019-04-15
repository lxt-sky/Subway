package com.sanmen.bluesky.subway.ui.base

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sanmen.bluesky.subway.data.bean.DriveRecord

/**
 * @author lxt_bluesky
 * @date 2019/3/28
 * @description
 */
class BaseAdapter: RecyclerView.Adapter<BaseViewHolder> {
    var data: MutableList<Any>? = null
    set(value) {
        this.data!!.clear()
        if (value != null) {
            this.data!!.addAll(value)
        }
    }

    var context: Context? = null
    var listeners: MutableList<BaseListener>? = null

    constructor(listeners:BaseListener){
        //list集合初始化
        this.data= mutableListOf()
        this.listeners = mutableListOf()
        this.listeners!!.add(listeners)
    }

    fun addData(data:List<Any>) {

        this.data!!.addAll(data)

    }

    override fun getItemViewType(position: Int): Int {
        var obj= data!![position]
        for (index in listeners!!.indices){
            val listener = listeners!!.get(index)
            if (listener.isItemViewType(position,obj)){
                return index
            }
        }

        return 0
    }


    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): BaseViewHolder {
        return listeners!![p1].getViewHolder(p0)
    }

    override fun getItemCount(): Int {
        return data?.size?:0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, pos: Int) {
        val obj=data!![pos]
        holder.bindView(pos,obj)
    }
}

