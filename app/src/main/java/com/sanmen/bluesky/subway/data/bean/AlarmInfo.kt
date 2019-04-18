package com.sanmen.bluesky.subway.data.bean

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey

/**
 * @author lxt_bluesky
 * @date 2019/3/29
 * @description 报警信息实体类
 */
@Entity(tableName = "alarms")
@ForeignKey(entity = DriveRecord::class,parentColumns = ["recordId"],childColumns = ["recordId"],onDelete = CASCADE)
class AlarmInfo{
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "alarmId")
    var id:Int=0


    var recordId:Int = 0

    var alarmText:String=""

    var alarmTime:String=""
}