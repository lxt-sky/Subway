package com.sanmen.bluesky.subway.data.dao

import androidx.room.*
import com.sanmen.bluesky.subway.data.bean.DriveRecord

/**
 * @author lxt_bluesky
 * @date 2019/3/27
 * @description 行车记录DAO
 */
@Dao
interface DriveDao {

    @Query("SELECT * FROM DRIVES")
    fun getAllRecord():List<DriveRecord>

    @Query("SELECT * FROM DRIVES ORDER BY recordId DESC LIMIT 1")
    fun getLatestRecord():DriveRecord

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: DriveRecord):Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(records: List<DriveRecord>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg records: DriveRecord): List<Long>

    @Update
    fun update(record: DriveRecord):Int

    @Delete
    fun deleteAll(vararg alarms: DriveRecord): Int

    @Delete
    fun deleteAll(list: List<DriveRecord>):Int

}