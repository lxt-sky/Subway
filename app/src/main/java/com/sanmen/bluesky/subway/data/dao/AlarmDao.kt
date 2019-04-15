package com.sanmen.bluesky.subway.data.dao

import com.sanmen.bluesky.subway.data.bean.AlarmInfo
import androidx.room.*


/**
 * @author lxt_bluesky
 * @date 2019/3/29
 * @description  报警信息DAO
 */
@Dao
interface AlarmDao{

    @Query("SELECT * FROM ALARMS")
    fun getAllAlarmInfo():List<AlarmInfo>

    @Query("SELECT * FROM ALARMS WHERE recordId=:id")
    fun getAlarmInfoByRecordId(id:Int):List<AlarmInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(alarmInfo: AlarmInfo):Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<AlarmInfo>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg users: AlarmInfo): List<Long>

    @Delete
    fun deleteAll(vararg alarms: AlarmInfo): Int

    @Delete
    fun deleteAll(list: List<AlarmInfo>):Int

    @Delete
    fun delete(alarmInfo: AlarmInfo):Int
}