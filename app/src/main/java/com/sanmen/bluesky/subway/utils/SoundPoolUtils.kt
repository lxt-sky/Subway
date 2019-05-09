package com.sanmen.bluesky.subway.utils

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RawRes

/**
 * @author lxt_bluesky
 * @date 2019/4/10
 * @description 铃声和震动
 */

private const val  LEFT_VOLUME:Float = 1.0F
private const val  RIGHT_VOLUME:Float = 1.0F
private const val LOOP = 0
private const val RATE = 1.0f
private const val DEFAULT_PRIORITY = 1
private const val MAX_STREAMS = 2

class SoundPoolUtils private constructor(context: Context) {

    private var mContext:Context = context
    private var mSoundPool: SoundPool? = null
    private var mVibrator: Vibrator? = null
    private var load = 0
    private var videoId:Int = -1

    init {
        initSoundPool()
        initVibrator()
    }


    companion object {

        private var mSoundPoolUtils: SoundPoolUtils? = null

        fun getInstance(context: Context):SoundPoolUtils{
            return mSoundPoolUtils?: synchronized(SoundPoolUtils::class){
                mSoundPoolUtils?:build(context).also {
                    mSoundPoolUtils =it
                }
            }
        }

        private fun build(context: Context): SoundPoolUtils {

            return SoundPoolUtils(context)
        }
    }

    private fun initVibrator() {

        mVibrator = mContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
    }

    private fun initSoundPool() {
        //22
        mSoundPool = if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            SoundPool.Builder().setMaxStreams(MAX_STREAMS).build()
        }else{
            SoundPool(MAX_STREAMS,AudioManager.STREAM_SYSTEM,0)
        }

    }

    fun playVideo(@RawRes reid:Int,loop:Int){

        videoId = -1
        mSoundPool?:initSoundPool()
        //加载音乐
        load = mSoundPool!!.load(mContext,reid,0)
        mSoundPool!!.setOnLoadCompleteListener{soundPool, _, status ->
            //加载完成后播放
            if (status==0){
                videoId = soundPool.play(load, LEFT_VOLUME, RIGHT_VOLUME, DEFAULT_PRIORITY, loop, RATE)
            }
        }
    }

    /**
     * 取消声音和震动
     */
    fun cancelVideoAndVibrator(){
        if (videoId==-1) return

        mSoundPool?.stop(videoId)
        mVibrator?.cancel()
    }


    private fun startVibrator(seconds:Long){
        mVibrator?:initVibrator()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val vibrator = VibrationEffect.createOneShot(seconds,100)
            //设置手机震动
            mVibrator!!.vibrate(vibrator)
        }else{
            mVibrator!!.vibrate(1000)
        }
    }

    /**
     * 开始播放音频和震动
     * @param resId 音频源
     * @param seconds 震动时长
     * @param loop 循环次数
     */
    fun startVideoAndVibrator(@RawRes resId:Int,loop:Int, seconds: Long) {
        playVideo(resId,loop)
        startVibrator(seconds)
    }

    fun release(){
        if (mSoundPool!=null){
            mSoundPool!!.release()
            mSoundPool = null
        }

        if (mVibrator!=null){
            mVibrator!!.cancel()
            mVibrator = null
        }
    }

}