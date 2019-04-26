package com.sanmen.bluesky.subway.service

import android.app.IntentService
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.util.Log
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
import com.sanmen.bluesky.subway.Constant.LIGHT_DATA
import java.io.IOException
import java.io.InputStream
import kotlin.concurrent.thread
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.nfc.Tag
import android.os.Build
import android.system.Os.close
import com.inuker.bluetooth.library.utils.BluetoothUtils.sendBroadcast
import com.inuker.bluetooth.library.utils.BluetoothUtils.unregisterReceiver
import com.sanmen.bluesky.subway.Constant
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECT_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_ALARM_DEVICE_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_PLATFORM_DEVICE_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_PLATFORM_DEVICE_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_STARTED
import com.sanmen.bluesky.subway.data.bean.NotifyMessage
import com.sanmen.bluesky.subway.utils.AppExecutors
import io.reactivex.internal.subscriptions.SubscriptionHelper.cancel
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

    private val alarmDeviceName = "Lsensor"//MEIZU EP52 Lite,Lsensor

    private val platformDeviceName = "BT20"//Lsensor-1

    private var mTargetDevice: BluetoothDevice? = null

    private var latestPlatformDevice:BluetoothDevice? = null

    private var connectThread: ConnectThread? = null

    /**
     * 断开连接
     */
    private var isLinkCancel: Boolean=false

    /**
     * 是否扫描到报警灯设备
     */
    private var searchAlarmDevice = false
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

    private val appExecutors: AppExecutors by lazy {
        AppExecutors()
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

        isLinkCancel = false
        latestPlatformDevice = null
        //开始搜索
        EventBus.getDefault().post(NotifyMessage().setCode(ACTION_SEARCH_STARTED).setData(trainState))
        mBluetoothAdapter.startDiscovery()

    }

    /**
     * 连接站台蓝牙设备
     */
    fun connectPlatformDevice(){

        if (deviceList.size==0) return

        val remoteDevice = mBluetoothAdapter.getRemoteDevice(deviceList[1].address)
        mTargetDevice = remoteDevice

        //设备配对
        bondDevice(remoteDevice)


        //当搜到站台设备时，开始准备和站台蓝牙通信

        //提交执行任务：任务一：接收站台蓝牙数据
//        appExecutors.connectThread().execute(connectThread)

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

    private fun addSocket(remoteDevice: BluetoothDevice?):Boolean {

        var socket: BluetoothSocket? = null
        try {
            socket = remoteDevice!!.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
        }catch (e:IOException){
            //建立连接通道失败

        }
        if (socket != null) {
            socketList.add(socket)
            return true
        }
        return false
    }


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
        isLinkCancel = true
        searchAlarmDevice = false//查找报警灯设备置为初值
        isArrived = false
        mTargetDevice = null
        connectThread?.cancel()
        deviceList.clear()
        socketList.clear()
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

                    val notifyMessage = NotifyMessage()//搜索完成

                    if (isLinkCancel){
                        return
                    }
                    if (searchAlarmDevice){

                        if (isArrived){

                            if (trainState==0){//列车上线
                                searchTargetDevice()
                            }else if (trainState==1){//列车到站
                                searchTargetDevice()
                            }else if (trainState == 2){//列车出站
                                //列车出站
                                EventBus.getDefault().post(NotifyMessage().setCode(ACTION_SEARCH_PLATFORM_DEVICE_FAILED))
                                //立即取消搜索
                                mBluetoothAdapter.cancelDiscovery()
                                //结束接收光照数据
                                isGettingDeviceData(false)
                                isArrived = false

                                GlobalScope.launch {
                                    delay(10000)

                                    searchTargetDevice()
                                }
                            }

                        }else{//搜索站点失败
                            searchTargetDevice()
                        }

                    }else{//搜索报警灯失败

                    }

                    //搜索结束
                    when(deviceList.size) {
                        0 -> {//搜索失败
                            mBluetoothAdapter.cancelDiscovery()
                            EventBus.getDefault().post(notifyMessage.setCode(ACTION_SEARCH_FAILED))

                        }
                        1 -> {//等待列车进站，搜索站台蓝牙
//                            if(!isSearchCancel){
//                                searchTargetDevice()
//                            }
                        }
                        2 -> {//搜索完成

                        }
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
        if (!searchAlarmDevice){//报警灯蓝牙
            if (device.name ==alarmDeviceName){
                trainState = 0//列车刚上线
                //清空列表
                deviceList.clear()
                //提示--搜索到报警灯蓝牙
                deviceList.add(device)
//            mBluetoothAdapter.cancelDiscovery()
                EventBus.getDefault().post(NotifyMessage().setCode(ACTION_SEARCH_ALARM_DEVICE_SUCCESS))
            }else{
//                return
            }
        }

        if (searchAlarmDevice){//站台蓝牙

            if (device.name ==platformDeviceName){
                //列车进站
                latestPlatformDevice = device

                if (!isArrived){
                    //开始接收光照数据
                    isGettingDeviceData(true)
                    //搜索到站台蓝牙
                    EventBus.getDefault().post(NotifyMessage().setCode(ACTION_SEARCH_PLATFORM_DEVICE_SUCCESS))
                    isArrived = true
                }
            }

        }

        if (latestPlatformDevice!=null){
            if (device.address == latestPlatformDevice!!.address&&deviceMacSet.contains(device.address)){//还在站内
                trainState = 1 //一次循环有目标设备，在站内
            }
        }else{
            trainState=2 //一次循环无目标设备，在站外
        }

    }

    /**
     * 是否获取设备数据
     */
    private fun isGettingDeviceData(state: Boolean){

        if (connectThread!=null&& connectThread?.isAlive!!){
            connectThread?.isGetData(state)
        }else{
            connected(socketList[0],alarmDeviceName)
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
     * 执行设备连接请求
     */
    private fun connected(socket: BluetoothSocket?, tag: Any?) {

        try {
//            connectedThread?.run {
//                cancel()
//                connectedThread = null
//            }
//            alarmThread?.run{
//                cancel()
//                alarmThread = null
//            }
            connectThread?.run{
                cancel()
                connectThread = null
            }

            //可以考虑设置tag
            this.connectThread = ConnectThread(socket,tag)
            this.connectThread!!.start()
        }finally {

        }

    }


    /**
     * 连接蓝牙线程
     */
    inner class ConnectThread(socket: BluetoothSocket?,tag: Any?): Thread() {

        private var isGetData: Boolean = false
        private var mBluetoothSocket: BluetoothSocket? = null
        private val notifyMessage = NotifyMessage()
        private var tag:Any?=null
        private var isConnected = false
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
                return
            }

            isConnected = true

            //连接成功
            EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECTED))
            //列车已上线
            searchAlarmDevice = true

            var inputStream: InputStream?
            try {
                inputStream = mBluetoothSocket!!.inputStream
            }catch (e:IOException){
                //连接失败
                EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECT_FAILED).setData(e.toString()))
                mBluetoothAdapter.cancelDiscovery()
                return
            }

            var i=0
            val data = ByteArray(1024)

            while (isConnected){

                if (isGetData){
                    try {

                        if (inputStream==null) return

                        val byte = inputStream.read().toByte()
                        if (byte== '\r'.toByte()){//13,10---\r\n
                            val reData = ByteArray(i)
                            System.arraycopy(data, 0, reData, 0, i)
                            //读取数据成功
                            EventBus.getDefault().post(notifyMessage.setCode(ACTION_READ_DATA_SUCCESS).setData(String(reData)).setTag(tag))
                            i=0
                        }
                        else if (byte=='\n'.toByte()){
                            continue
                        }else{
                            data[i++]=byte
                        }
                    }catch (e:IOException){
                        //读取数据失败
                        EventBus.getDefault().post(notifyMessage.setCode(ACTION_READ_DATA_FAILED).setData(e.toString()))
                        disConnect()
                        return
                    }

                }

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
                this.mBluetoothSocket?.close()
                return
            }catch (e:IOException){
                //关闭蓝牙连接失败
            }
        }
    }

    /**
     * 连接成功行线程
     */
    class ConnectedThread(bluetoothSocket: BluetoothSocket?,tag: Any?):Thread(){
        private var isGetData: Boolean = true
        private var mInputStream: InputStream? = null

        private var mBluetoothSocket:BluetoothSocket?=null

        private val notifyMessage = NotifyMessage()

        private var tag:Any?=null


        var isConnected = false

        init {
            if (bluetoothSocket!=null){
                this.mBluetoothSocket = bluetoothSocket

                //连接成功
                EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECTED))

                var inputStream: InputStream? =null
                try {
                    inputStream = mBluetoothSocket!!.inputStream
                }catch (e:IOException){
                    //连接失败
                    EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECT_FAILED))
                }
                this.mInputStream = inputStream
                this.tag = tag
                isConnected = true
            }

        }

        override fun run() {
            super.run()

            var i=0
            val data = ByteArray(1024)

            do {
                if (isGetData){
                    try {

                        if (mInputStream==null) return

                        val byte = mInputStream!!.read().toByte()

                        if (byte== '\r'.toByte()){//13,10---\r\n
                            val reData = ByteArray(i)
                            System.arraycopy(data, 0, reData, 0, i)
                            //读取数据成功
                            EventBus.getDefault().post(notifyMessage.setCode(ACTION_READ_DATA_SUCCESS).setData(String(reData)).setTag(tag))
                            i=0
                        }
                        else if (byte=='\n'.toByte()){
                            continue
                        }else{
                            data[i++]=byte
                        }
                    }catch (e:IOException){
                        //读取数据失败
                        EventBus.getDefault().post(notifyMessage.setCode(ACTION_READ_DATA_FAILED))
                        break
                    }
                }
            }while (isConnected)
        }

        fun cancel(){
            try {
                this.mBluetoothSocket?.close()
                return
            }catch (e:IOException){
                //关闭蓝牙连接失败
            }
        }
    }

    /**
     * 子线程：连接报警灯蓝牙
     */
    class AlarmThread(device: BluetoothDevice,tag:Any):Thread(){
        private var bluetoothSocket: BluetoothSocket? = null
        private var tag: Any? = null
        private var isConnected: Boolean = false

        init {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
            this.tag = tag.toString()
        }

        override fun run() {
            super.run()

            isConnected=true

            val notifyMessage = NotifyMessage()
            notifyMessage.tag = tag
            //连接中
            EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECTING))
            if (isConnected){
                try {
                    //执行连接
                    bluetoothSocket!!.connect()
                }catch (e: IOException){
                    //连接失败
                    EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECT_FAILED))
                    return
                }
            }
            val inputStream:InputStream
            try {
                inputStream = bluetoothSocket!!.inputStream
            }catch (e:IOException){
                //连接失败
                EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECT_FAILED))
                return
            }

            //连接成功
            EventBus.getDefault().post(notifyMessage.setCode(ACTION_CONNECTED))

            var i=0
            val data = ByteArray(1024)

            while (isConnected){
                try {
                    val byte = inputStream.read().toByte()

                    if (byte== '\r'.toByte()){//13,10---\r\n

                        val reData = ByteArray(i)
                        System.arraycopy(data, 0, reData, 0, i)
                        //读取数据成功
                        EventBus.getDefault().post(notifyMessage.setCode(ACTION_READ_DATA_SUCCESS).setData(String(reData)))
                        i=0
                    }
                    else if (byte=='\n'.toByte()){
                        continue
                    }else{
                        data[i++]=byte
                    }
                }catch (e:IOException){
                    //读取数据失败
                    EventBus.getDefault().post(notifyMessage.setCode(ACTION_READ_DATA_FAILED))
                    break
                }
            }
            bluetoothSocket = null
            this.cancel()
            if (!isConnected) run { Log.d(TAG, "ConnectedThread END since user cancel.") }
            else run { Log.d(TAG, "ConnectedThread END.") }

        }

        fun cancel(){
            try {
                isConnected =false
                if (bluetoothSocket!=null){
                    bluetoothSocket!!.close()
                }
            }catch (e:IOException){

            }

        }
    }

}
