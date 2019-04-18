package com.sanmen.bluesky.subway.data.bean

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author lxt_bluesky
 * @date 2019/3/27
 * @description 行车记录数据类
 */
@Entity(tableName = "drives")
class DriveRecord {
    //ID自增长
    @PrimaryKey(autoGenerate = true)
    var recordId:Int = 0

    var driveName:String = ""

    var driveBeginTime:String = ""

    var driveEndTime:String=""
}