package com.sanmen.bluesky.subway.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.sanmen.bluesky.subway.Constant
import com.sanmen.bluesky.subway.R

import com.sanmen.bluesky.subway.widget.KBubbleSeekBar

/**
 * @author lxt_bluesky
 * @date 2019/3/29
 * @description
 */
class TimeSelectDialog : DialogFragment(), KBubbleSeekBar.OnProgressChangedListener {



    private var delayTime = 0

    private var listener: OnDialogCloseListener? = null


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.listener = context as OnDialogCloseListener
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {


        if (arguments!=null){
            this.delayTime = arguments!!.getInt(Constant.DELAY_TIME,0)
        }
        val builder = AlertDialog.Builder(activity)
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_time_select,null)

        val dialog =builder.run {

            builder.setView(dialogView)
            builder.create()
        }
//        this.listener = activity as OnDialogCloseListener
        val seekBar =dialogView.findViewById<KBubbleSeekBar>(R.id.seekBar)
        seekBar.run {
            this.onProgressChangedListener = this@TimeSelectDialog
            this.setProgress(delayTime.toFloat())
        }

        return dialog

    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        if (listener!=null){
            listener!!.onClick(delayTime)
        }
    }


    public interface OnDialogCloseListener{
        fun onClick(progress: Int)
    }

    override fun onProgressChanged(
        bubbleSeekBar: KBubbleSeekBar?,
        progress: Int,
        progressFloat: Float,
        fromUser: Boolean
    ) {
        this.delayTime = progress


    }

    override fun getProgressOnActionUp(bubbleSeekBar: KBubbleSeekBar?, progress: Int, progressFloat: Float) {

    }

    override fun getProgressOnFinally(
        bubbleSeekBar: KBubbleSeekBar?,
        progress: Int,
        progressFloat: Float,
        fromUser: Boolean
    ) {

    }



}
