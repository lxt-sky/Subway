package com.sanmen.bluesky.subway.service

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.content.Context
import android.os.Binder
import android.os.IBinder
import com.inuker.bluetooth.library.BluetoothClient
import com.sanmen.bluesky.subway.manager.ClientManager
import java.util.*

import com.inuker.bluetooth.library.receiver.listener.BluetoothBondListener
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECTED
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECTING
import com.sanmen.bluesky.subway.Constant.ACTION_DISCONNECTED
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_SUCCESS
import com.sanmen.bluesky.subway.Constant.BOND_FAILED
import com.sanmen.bluesky.subway.Constant.BOND_SUCCESS
import java.io.IOException
import java.io.InputStream
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.util.Log
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECT_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_ALARM_DEVICE_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_PLATFORM_DEVICE_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_PLATFORM_DEVICE_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_STARTED
import com.sanmen.bluesky.subway.data.bean.NotifyMessage
import com.sanmen.bluesky.subway.utils.AppExecutors
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus


/**
 * 蓝牙串口服务UUID--SPP
 */
private const val MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"

private const val TAG = ".BluetoothService"


class BluetoothService : Service() {

    private val alarmDeviceName = "HC-05"//MEIZU EP52 Lite,Lsensor

    private val platformDeviceName = "BT20"//Lsensor-1

    private var mTargetDevice: BluetoothDevice? = null

    private var latestPlatformDevice:BluetoothDevice? = null

    private var connectThread: ConnectThread? = null

    private var communicateThread:CommunicateThread?=null

    /**
     * 处在连接状态
     */
    private var isLinking: Boolean=false

    /**
     * 已连接到报警灯设备
     */
    private var linkAlarmDevice = false
    /**
     * 列车是否到站
     */
    private var isArrived: Boolean = false

    private var trainState: Int = 0//0未上线，1到站，2出站

    /**
     * 设备匹配次数，在不能搜索到站台设备时开始计数，大于30则认定列车出站
     */
    private var matchDeviceCount = 0

    /**
     * 设备列表Address集合
     */
    private var deviceList = mutableListOf<BluetoothDevice>()

    /**
     * BluetoothSocket集合
     */
    private var socketList = mutableListOf<BluetoothSocket>()

    /**
     * 范围内设备MAC地址集合
     */
    private var deviceMacSet = mutableSetOf<String>()


    private val mBinder:MyBinder by lazy {
        MyBinder(this@BluetoothService)
    }

    private val mClient:BluetoothClient by lazy {
        ClientManager.getClient()
    }

    private val mBluetoothAdapter:BluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }


    override fun onCreate() {
        super.onCreate()
        mClient.registerBluetoothBondListener(mBluetoothBondListener)
        IntentFilter().apply {
            this.addAction(BluetoothDevice.ACTION_FOUND)
            this.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            registerReceiver(mBroadcastReceiver,this)
        }
    }
    /**
     * 是否支持蓝牙
     */
    fun isBluetoothSupported():Boolean{
        if (mBluetoothAdapter==null){
            return false
        }
        return true
    }

    /**
     * 打开蓝牙
     */
    fun openBluetooth(){
        if (!mBluetoothAdapter.isEnabled){
            mBluetoothAdapter.enable()
        }
    }

    /**
     * 关闭蓝牙
     */
    fun closeBluetooth(){

        if (mBluetoothAdapter==null) return

        if (mTargetDevice==null) return
        mBluetoothAdapter.disable()
    }


    /**
     * 搜索设备
     */
    fun searchTargetDevice() {

        if (mBluetoothAdapter.isDiscovering) {
            mBluetoothAdapter.cancelDiscovery()
        }
        //开始搜索
        EventBus.getDefault().post(NotifyMessage().setCode(ACTION_SEARCH_STARTED).setData(trainState))
        trainState = 2
        isLinking = true
        latestPlatformDevice = null

        mBluetoothAdapter.startDiscovery()
        //开始搜索后8秒取消搜索
        GlobalScope.launch {
            delay(8000)
            if(mBluetoothAdapter.isDiscovering){
                mBluetoothAdapter.cancelDiscovery()
            }
        }
    }

    /**
     * 连接报警灯蓝牙
     */
    fun connectAlarmDevice(){
        if (deviceList.size==0) return

        val remoteDevice = mBluetoothAdapter.getRemoteDevice(deviceList[0].address)
        mTargetDevice = remoteDevice

        //设备配对
        bondDevice(remoteDevice)
        //执行蓝牙连接
        if (addSocket(remoteDevice)) {
            connected(socketList[0],alarmDeviceName)
        }
    }

    /**
     * 获取设备BluetoothSocket
     */
    fun addSocket(remoteDevice: BluetoothDevice?):Boolean {

        val socket: BluetoothSocket?
        try {
            socket = remoteDevice!!.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
        }catch (e:IOException){
            //建立连接通道失败
            EventBus.getDefault().post(NotifyMessage.newInstance().setCode(ACTION_CONNECT_FAILED).setData(e.toString()))
            return false
        }
        socketList.add(socket)
        return true
    }

    /**
     * 设备配对
     * @param device 目标设备
     */
    fun bondDevice(device: BluetoothDevice){

        //未配对设备发起配对
        if (device.bondState==BluetoothDevice.BOND_NONE){
            device.createBond()
        }
    }

    /**
     * 断开连接
     */
    fun disConnect(){
        //广播当前状态为连接已断开
        //参数重置
        isLinking = false
        linkAlarmDevice = false
        isArrived = false
        mTargetDevice = null
        //关闭线程
        communicateThread?.cancel()
        connectThread?.cancel()
        //清空列表
        deviceList.clear()
        socketList.clear()
        //取消搜索
        mBluetoothAdapter.cancelDiscovery()

        EventBus.getDefault().post(NotifyMessage().setCode(ACTION_DISCONNECTED))

    }

    /**
     * 设备配对监听
     */
    private val mBluetoothBondListener = object : BluetoothBondListener() {
        override fun onBondStateChanged(mac: String?, bondState: Int) {

            val notifyMessage = NotifyMessage()
            if (bondState==BluetoothDevice.BOND_BONDED){
                //广播配对成功
                EventBus.getDefault().post(notifyMessage.setCode(BOND_SUCCESS))

                return
            }else{
                //配对失败，广播连接失败
                EventBus.getDefault().post(notifyMessage.setCode(BOND_FAILED))
            }

        }
    }

    /**
     * 蓝牙搜索广播接收
     */
    private val mBroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                BluetoothDevice.ACTION_FOUND->{//发现设备

                    val device:BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    if (device != null) {
                        addDevice2List(device)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED->{//搜索结束

                    if (!isLinking) return

                    if (linkAlarmDevice){

                        if (isArrived){

                            when (trainState) {
                                0 -> //列车上线
                                    searchTargetDevice()
                                1 -> //列车到站
                                    searchTargetDevice()
                                2 -> {//列车出站
                                    //列车出站
                                    EventBus.getDefault().post(NotifyMessage.newInstance().setCode(ACTION_SEARCH_PLATFORM_DEVICE_FAILED))
                                    //立即取消搜索
                                    mBluetoothAdapter.cancelDiscovery()
                                    //结束接收光照数据
                                    isGettingDeviceData(false)
                                    isArrived = false
                                    //等待10秒后重新搜索
                                    GlobalScope.launch {
                                        delay(10000)

                                        searchTargetDevice()
                                    }
                                }
                            }

                        }else{//搜索站点失败
                            searchTargetDevice()
                        }

                    }else{//连接报警灯失败
                    }

                    if (deviceList.size==0){
                        EventBus.getDefault().post(NotifyMessage.newInstance().setCode(ACTION_SEARCH_FAILED))
                        mBluetoothAdapter.cancelDiscovery()
                    }

                }
            }
        }
    }

    /**
     * 添加蓝牙设备至列表
     */
    private fun addDevice2List(device: BluetoothDevice) {

        //添加设备至集合
        if (!deviceMacSet.contains(device.address)){
            deviceMacSet.add(device.address)
        }

        //搜到报警灯-列车上线
        if (!linkAlarmDevice){//未连接到报警灯蓝牙，报警灯蓝牙
            if (device.name ==alarmDeviceName){
                trainState = 0//列车刚上线
                //清空列表
                deviceList.clear()
                //提示--搜索到报警灯蓝牙
                deviceList.add(device)
                EventBus.getDefault().post(NotifyMessage().setCode(ACTION_SEARCH_ALARM_DEVICE_SUCCESS))
            }else{
                return
            }
        }

        if (linkAlarmDevice){//已连接到报警灯蓝牙，站台蓝牙

            if (device.name ==platformDeviceName){
                //列车进站
                latestPlatformDevice = device
                trainState = 1

                if (!isArrived){
                    //搜索到站台蓝牙
                    EventBus.getDefault().post(NotifyMessage().setCode(ACTION_SEARCH_PLATFORM_DEVICE_SUCCESS))

                    isArrived = true
                    //开始接收光照数据
                    isGettingDeviceData(true)

                }
            }

        }

    }

    /**
     * 是否获取设备数据
     */
    fun isGettingDeviceData(state: Boolean){

        if (communicateThread!=null){
            communicateThread?.isGetData(state)
        }else{
            communicated(state)
        }

    }

    fun testDeviceConnection(remoteDevice: BluetoothDevice?){

        if (addSocket(remoteDevice)) {
            connected(socketList[0],remoteDevice!!.name)
        }

    }

    /**
     * 执行设备连接请求
     */
    private fun connected(socket: BluetoothSocket?, tag: Any?) {

        try {
            connectThread?.run{
                cancel()
            }
            //可以考虑设置tag
            this.connectThread = ConnectThread(socket,tag)
            this.connectThread!!.start()
        }finally {

        }

    }

    private fun communicated(state: Boolean) {
        try {
            communicateThread?.run{
                cancel()
            }

            if (connectThread!=null){
                //可以考虑设置tag
                communicateThread = CommunicateThread(socketList[0],alarmDeviceName,state)
                communicateThread!!.start()
            }else{
                EventBus.getDefault().post(NotifyMessage.newInstance().setCode(ACTION_DISCONNECTED))
            }



        }finally {

        }
    }

    override fun onBind(intent: Intent?): IBinder? =mBinder

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
        mClient.unregisterBluetoothBondListener(mBluetoothBondListener)
    }

    class MyBinder(private val service: BluetoothService):Binder(){
        var context: Context? =null

        fun getService(context: Context):BluetoothService{

            this.context = context
            return service
        }
    }

    /**
     * 连接蓝牙线程
     */
    inner class ConnectThread(socket: BluetoothSocket?,tag: Any?): Thread() {

        private var mBluetoothSocket: BluetoothSocket? = null
        private val notifyMessage = NotifyMessage()
        private var tag:Any?=null
        init {

            if (socket!=null){
                mBluetoothSocket = socket
                //连接中
                EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECTING))
            }
            this.tag = tag.toString()
        }

        override fun run() {
            super.run()

            try {
                //执行连接
                mBluetoothSocket?.connect()
            }catch (e: IOException){
                //连接失败
                EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECT_FAILED).setData(e.toString()))
                mBluetoothAdapter.cancelDiscovery()
                cancel()
                return
            }
            //连接成功
            EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECTED))
            //列车已上线
            linkAlarmDevice = true

        }

        /**
         * 关闭线程
         */
        fun cancel(){
            try {
                this.mBluetoothSocket?.close()
                connectThread = null
                return
            }catch (e:IOException){
                //关闭蓝牙连接失败
            }
        }
    }

    /**
     * 通信线程
     */
    inner class CommunicateThread(socket: BluetoothSocket?, tag: Any?, state: Boolean): Thread() {

        private var isBeginGetByte: Boolean = false
        private var isGetData: Boolean = false
        private var mBluetoothSocket: BluetoothSocket? = null
        private val notifyMessage = NotifyMessage()
        private var tag:Any?=null
        private var isConnected = true
        private var mInputStream: InputStream? = null

        var begin = 0
        var end = 0

        init {

            if (socket!=null){
                mBluetoothSocket = socket
                this.tag = tag.toString()
                isGetData = state
            }

            try {
                mInputStream = mBluetoothSocket!!.inputStream
            }catch (e:IOException){
                //连接失败
                EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECT_FAILED).setData(e.toString()))
                mBluetoothAdapter.cancelDiscovery()
                cancel()
            }
        }

        override fun run() {
            super.run()

//            val data = ByteArray(1024)
            var i=0
            val data = ByteArray(1024)

            try {

                while (true){
                    if (isGetData){
                        if (mInputStream==null) return

//                        val count  = mInputStream!!.available()
//
//                        if (count>0){
//                            mInputStream!!.read(data)
//                            for (i in data.indices){
//                                if (data[i]=='\n'.toByte()){
//                                    isBeginGetByte= true
//                                    begin = i
//                                    continue
//                                } else if (isBeginGetByte&&data[i]== '\r'.toByte()){//13,10---\r\n
//                                    end = i
//                                    val size = end-begin-1
//                                    val reData = ByteArray(size)
//                                    isBeginGetByte = false
//                                    if (size<3){//无效数据
//
//                                    }else{
//                                        System.arraycopy(data, begin+1, reData, 0, size)
//                                        //读取数据成功
//                                        EventBus.getDefault().post(notifyMessage.setCode(ACTION_READ_DATA_SUCCESS).setData(String(reData)).setTag(tag))
//                                        Log.e(".service",String(reData))
//                                        break
//                                    }
//                                }
//                            }
//                        }else{
////                            EventBus.getDefault().post(notifyMessage.setCode(ACTION_READ_DATA_FAILED).setData("字节流：$count"))
////                            cancel()
//                        }


                        val byte = mInputStream!!.read().toByte()
                        if (byte== '\r'.toByte()){//13,10---\r\n
                            val reData = ByteArray(i)
                            System.arraycopy(data, 0, reData, 0, i)
                            //读取数据成功
                            EventBus.getDefault().post(notifyMessage.setCode(ACTION_READ_DATA_SUCCESS).setData(String(reData)).setTag(tag))
                            i=0
                        }
                        else if (byte=='\n'.toByte()){
                            isBeginGetByte= true
                            continue
                        }else if (isBeginGetByte){
                            data[i++]=byte
                        }

                    }else{
                        isBeginGetByte= false
                    }

                }
                return

            }catch (e:IOException){
                //读取数据失败
                disConnect()
                EventBus.getDefault().post(notifyMessage.setCode(ACTION_READ_DATA_FAILED).setData(e.toString()))
                cancel()
                return
            }

        }

        /**
         * 是否接收数据
         */
        fun isGetData(state: Boolean){
            this.isGetData = state
        }

        /**
         * 关闭线程
         */
        fun cancel(){
            try {
                isConnected = false
                communicateThread=null
                return
            }catch (e:IOException){
                //关闭蓝牙连接失败
            }
        }
    }

}
