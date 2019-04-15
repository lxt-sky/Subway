package com.sanmen.bluesky.subway.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * @author lxt_bluesky
 * @date 2019/4/3
 * @description
 */
object TimeUtil {

    /**
     * 获取当前时间 yyyy/MM/dd HH:mm:ss
     * @return
     */
    private val format1 = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    private val format2 = SimpleDateFormat("yyyy/MM/dd")

    fun getTimeString(date:Date):String{
        //字符串格式
        return format1.format(date)
    }

    fun getTimeString(date:String):String{
        //字符串格式
        val time = format2.parse(date)
        return format1.format(time)
    }

    /**
     * 获取当前日期 yyyy/MM/dd
     * @return
     */
    fun getDateString(date:Date):String{
        //字符串格式
        return format2.format(date)
    }

    fun getDateString(time:String):String{
        val date = format1.parse(time)
        return format2.format(date)
    }






}