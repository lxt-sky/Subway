package com.sanmen.bluesky.subway.data.repository

import com.sanmen.bluesky.subway.data.bean.AlarmInfo
import com.sanmen.bluesky.subway.data.bean.DriveRecord
import com.sanmen.bluesky.subway.data.dao.AlarmDao
import com.sanmen.bluesky.subway.data.dao.DriveDao
import com.sanmen.bluesky.subway.utils.AppExecutors

/**
 * @author lxt_bluesky
 * @date 2019/4/4
 * @description
 */
class AlarmRepository private constructor(
    private val alarmDao: AlarmDao,
    private val appExecutors: AppExecutors
) {

    fun insertAlarmItem(alarm: AlarmInfo){
        appExecutors.diskIO().execute {
            alarmDao.insert(alarm)
        }
    }

    fun insertAlarmList(list: List<AlarmInfo>){
        appExecutors.diskIO().execute {
            alarmDao.insertAll(list)
        }
    }


    fun getAllAlarmInfo()=alarmDao.getAllAlarmInfo()

    fun getAlarmInfoByRecordId(recordId:Int) = alarmDao.getAlarmInfoByRecordId(recordId)



    companion object {
        //单例模式
        @Volatile private var INSTANCE: AlarmRepository? = null
        //静态方法
        fun getInstance(alarmDao: AlarmDao,appExecutors: AppExecutors)= INSTANCE?: synchronized(this){
            INSTANCE?:AlarmRepository(alarmDao,appExecutors).also {
                INSTANCE = it
            }
        }
    }
}