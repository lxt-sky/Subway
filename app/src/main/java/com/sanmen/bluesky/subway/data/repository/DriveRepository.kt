package com.sanmen.bluesky.subway.data.repository

import com.sanmen.bluesky.subway.data.bean.DriveRecord
import com.sanmen.bluesky.subway.data.dao.DriveDao
import com.sanmen.bluesky.subway.utils.AppExecutors

/**
 * @author lxt_bluesky
 * @date 2019/4/4
 * @description
 */
class DriveRepository(
    private val driveDao: DriveDao,
    private val appExecutors: AppExecutors
) {

    fun insertDriveRecord(record: DriveRecord){
        synchronized(DriveRepository){
            appExecutors.diskIO().execute {
                driveDao.insert(record)
            }
        }

    }

    fun insertDriveRecordList(list: List<DriveRecord>){
        synchronized(DriveRepository){
            appExecutors.diskIO().execute {
                driveDao.insertAll(list)
            }
        }

    }

    fun updateDriveRecord(record: DriveRecord){
        synchronized(DriveRepository){
            appExecutors.diskIO().execute {
                driveDao.update(record)
            }
        }

    }

    fun deleteAllDriveRecord(list: List<DriveRecord>) = driveDao.deleteAll(list)

    fun getDriveRecordList()= synchronized(DriveRepository){driveDao.getAllRecord()}

    fun getLatestRecord() = driveDao.getLatestRecord()

    companion object {
        //单例模式
        @Volatile private var INSTANCE: DriveRepository? = null
        //静态方法
        fun getInstance(driveDao: DriveDao,appExecutors: AppExecutors)= INSTANCE?: synchronized(this){
            INSTANCE?:DriveRepository(driveDao,appExecutors).also {
                INSTANCE = it
            }
        }
    }

}