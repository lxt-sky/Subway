package com.sanmen.bluesky.subway

import java.util.*

/**
 * @author lxt_bluesky
 * @date 2019/4/10
 * @description 静态属性类
 */
object Constant {

    /**
     * 搜索设备
     */
    const val ACTION_SEARCH_STARTED = "com.sanmen.bluesky.subway.ACTION_SEARCH_STARTED"

    const val ACTION_SEARCH_FAILED = "com.sanmen.bluesky.subway.ACTION_SEARCH_FAILED"

    /**
     * 蓝牙配对失败
     */
    const val BOND_FAILED = "com.sanmen.bluesky.subway.BOND_FAILED"

    /**
     * 蓝牙配对成功
     */
    const val BOND_SUCCESS = "com.sanmen.bluesky.subway.BOND_SUCCESS"
    /**
     * 连接成功
     */
    const val ACTION_CONNECTED = "com.sanmen.bluesky.subway.ACTION_CONNECTED"

    /**
     * 连接中
     */
    const val ACTION_CONNECTING = "com.sanmen.bluesky.subway.ACTION_CONNECTING"
    /**
     * 连接断开
     */
    const val ACTION_DISCONNECTED = "com.sanmen.bluesky.subway.ACTION_DISCONNECTED"

    /**
     * 连接失败
     */
    const val ACTION_CONNECT_FAILED = "com.sanmen.bluesky.subway.ACTION_CONNECT_FAILED"
    /**
     * 发现服务
     */
    const val ACTION_SERVICES_DISCOVERED = "com.sanmen.bluesky.subway.ACTION_SERVICES_DISCOVERED"
    /**
     * 无指定目标设备
     */
    const val ACTION_SEARCH_DEVICE_NONE = "com.sanmen.bluesky.subway.ACTION_SEARCH_DEVICE_NONE"
    /**
     * 取消搜索
     */
    const val ACTION_SEARCH_DEVICE_CANCELED = "com.sanmen.bluesky.subway.ACTION_SEARCH_DEVICE_CANCELED"
    /**
     * 获得数据
     */
    const val ACTION_DATA_AVAILABLE = "com.sanmen.bluesky.subway.ACTION_DATA_AVAILABLE "
    /**
     *  读取数据失败
     */
    const val ACTION_READ_DATA_FAILED = "com.sanmen.bluesky.subway.ACTION_READ_DATA_FAILED "
    /**
     * 读取数据成功
     */
    const val ACTION_READ_DATA_SUCCESS = "com.sanmen.bluesky.subway.ACTION_READ_DATA_SUCCESS "

    /**
     * 行车方向
     */
    const val DRIVE_DIRECTION = "DRIVE_DIRECTION"
    /**
     * 行车记录ID
     */
    const val RECORD_ID = "recordId"

    /**
     * 行车开始时间
     */
    const val DRIVE_BEGIN_TIME = "driveBeginTime"
    /**
     * 行车结束时间
     */
    const val DRIVE_END_TIME = "driveEndTime"
    /**
     * 光照临界值
     */
    const val LIGHT_THRESHOLD = "lightThreshold"
    /**
     * 光照强度
     */
    const val LIGHT_INTENSITY="lightIntensity"
    /**
     * 单元值
     */
    const val UNIT_VALUE = "unitValue"
    /**
     * 声音状态
     */
    const val SOUND_STATE = "SOUND_STATE"
    /**
     * 延迟时间
     */
    const val DELAY_TIME = "DELAY_TIME"
    /**
     * 光照数据
     */
    const val LIGHT_DATA = "LIGHT_DATA"

    /**
     * 提示文本
     */
    const val DEVICE_NOT_SUPPORT_BLUETOOTH = "当前设备不支持蓝牙"

    const val NOT_LOCATION_PERMISSION="app没有定位权限将无法搜索蓝牙设备!"




}