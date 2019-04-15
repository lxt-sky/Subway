package com.sanmen.bluesky.subway.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sanmen.bluesky.subway.data.bean.AlarmInfo
import com.sanmen.bluesky.subway.data.bean.DriveRecord
import com.sanmen.bluesky.subway.data.dao.AlarmDao
import com.sanmen.bluesky.subway.data.dao.DriveDao

/**
 * @author lxt_bluesky
 * @date 2019/3/27
 * @description 行车数据库类
 */
@Database(entities = [DriveRecord::class,AlarmInfo::class],version = 1,exportSchema = false)
abstract class DriveDatabase: RoomDatabase() {


    abstract fun getDriveDao():DriveDao

    abstract fun getAlarmDao():AlarmDao

    companion object {
        private const val DB_NAME:String="drive.db"
        @Volatile
        private var INSTANCE: DriveDatabase? = null

        fun getInstance(context:Context):DriveDatabase {

            return INSTANCE?: synchronized(this){
                INSTANCE?:buildDatabase(context).also {
                    INSTANCE=it
                }
            }
        }

        private fun buildDatabase(context: Context):DriveDatabase{
            return Room.databaseBuilder(context.applicationContext,DriveDatabase::class.java,DB_NAME).build()
        }
    }


}