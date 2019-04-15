package com.sanmen.bluesky.subway.utils

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface

/**
 * @author lxt_bluesky
 * @date 2019/3/29
 * @description 适用于简单对话场景
 */
object DialogUtil {

    private fun getDialogBuilder(context: Context):AlertDialog.Builder{
        return AlertDialog.Builder(context)
    }

    fun getConfirmDialog(context: Context,message:String,
                           onDetermineClickListener:DialogInterface.OnClickListener):AlertDialog.Builder{

        var builder = getDialogBuilder(context)
        builder.run {
            this.setMessage(message)
            this.setPositiveButton("确定",onDetermineClickListener)
            this.setNegativeButton("取消", null)

        }
        return builder
    }


}