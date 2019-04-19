package com.sanmen.bluesky.subway.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sanmen.bluesky.subway.Constant

import com.sanmen.bluesky.subway.widget.KBubbleSeekBar
import kotlinx.android.synthetic.main.fragment_light.*
import com.sanmen.bluesky.subway.Constant.LIGHT_INTENSITY
import com.sanmen.bluesky.subway.Constant.LIGHT_THRESHOLD
import com.sanmen.bluesky.subway.Constant.UNIT_VALUE
import com.sanmen.bluesky.subway.R


class LightFragment : Fragment() {

    /**
     * 光照强度
     */
    private var lightIntensity = 0//0-4259244259
    /**
     * 光照临界值hi
     */
    private var light = 0//0-10

    /**
     * 单元值
     */
    private var unitValue = 10000



    private val sharedPref: SharedPreferences by lazy {
        activity!!.getSharedPreferences("sub_config", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref.run {
            light = this.getInt(LIGHT_THRESHOLD,4)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_light, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lightThreshold.run {
            lightThreshold.setProgress(light.toFloat())

            this.onProgressChangedListener = progressChangeListener
        }
        value.text = "(${light*unitValue})"
    }

    private val progressChangeListener = object :
        KBubbleSeekBar.OnProgressChangedListener{
        override fun onProgressChanged(
            bubbleSeekBar: KBubbleSeekBar?,
            progress: Int,
            progressFloat: Float,
            fromUser: Boolean
        ) {
            light = progress
            value.text = "(${light*unitValue})"

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

    override fun onPause() {
        super.onPause()

        //保存数据
         sharedPref.edit().apply {
             this.putInt(LIGHT_THRESHOLD,light)
             this.putInt(UNIT_VALUE,unitValue)
             this.apply()
         }
    }
}
