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
import com.sanmen.bluesky.subway.Constant
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECT_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_ALARM_DEVICE_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_PLATFORM_DEVICE_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_STARTED
import com.sanmen.bluesky.subway.data.bean.NotifyMessage
import com.sanmen.bluesky.subway.utils.AppExecutors
import org.greenrobot.eventbus.EventBus


/**
 * 蓝牙串口服务UUID--SPP
 */
private const val MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"

private const val TAG = ".BluetoothService"


class BluetoothService : Service() {

    private var mBluetoothGatt: BluetoothGatt? = null

    private var mTargetDevice: BluetoothDevice? = null

    private val alarmDeviceName = "Lsensor"//MEIZU EP52 Lite,Lsensor

    private val platformDeviceName = "MEIZU EP52 Lite"//Lsensor-1

    private var searchAlarmDevice = false

    /**
     * 设备列表Address集合
     */
    private var deviceList = mutableListOf<BluetoothDevice>()

    /**
     * BluetoothSocket集合
     */
    private var socketList = mutableListOf<BluetoothSocket>()

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
        //注册Eventbus
//        EventBus.getDefault().register(this)
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
        EventBus.getDefault().post(NotifyMessage().setCode(ACTION_SEARCH_STARTED))
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
        val connectThread = ConnectThread(remoteDevice,platformDeviceName)

        connectThread.start()
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
        val alarmThread = AlarmThread(remoteDevice,alarmDeviceName)
        alarmThread.start()
        //提交执行任务：任务二：接收报警灯蓝牙数据
//        appExecutors.connectThread().execute(connectThread)
    }

    fun bondDevice(device: BluetoothDevice){

        //未配对设备发起配对
        if (device.bondState==BluetoothDevice.BOND_NONE){
            device.createBond()
        }
    }

    private fun close() {

        if (mBluetoothGatt == null) return

        disConnect()
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    /**
     * 断开连接
     */
    fun disConnect(){
        //广播当前状态为连接已断开
//        isConnected = false

//        mTargetDevice = null
//        connectThread.interrupt()
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
                    addDevice2List(device)
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED->{//搜索结束

                    val notifyMessage = NotifyMessage()

                    //搜索结束
                    when(deviceList.size) {
                        0 -> {//搜索失败
                            EventBus.getDefault().post(notifyMessage.setCode(ACTION_SEARCH_FAILED))
                        }
                        1 -> {//等待列车进站，搜索站台蓝牙
                            searchTargetDevice()
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
    private fun addDevice2List(device: BluetoothDevice?) {

        if (!searchAlarmDevice&&device?.name==alarmDeviceName){//报警灯蓝牙
            searchAlarmDevice = true
            //清空列表
            deviceList.clear()
            //提示--搜索到报警灯蓝牙
            deviceList.add(device)
//            mBluetoothAdapter.cancelDiscovery()
            EventBus.getDefault().post(NotifyMessage().setCode(ACTION_SEARCH_ALARM_DEVICE_SUCCESS))

        }
        if (searchAlarmDevice&&device?.name==platformDeviceName){//站台蓝牙
            deviceList.add(device)
            //立即取消搜索
            mBluetoothAdapter.cancelDiscovery()
            //搜索到站台蓝牙
            EventBus.getDefault().post(NotifyMessage().setCode(ACTION_SEARCH_PLATFORM_DEVICE_SUCCESS))

        }
    }

    override fun onBind(intent: Intent?): IBinder? =mBinder

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
//        EventBus.getDefault().unregister(this)
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
     * 连接站台蓝牙
     */
    class ConnectThread(remoteDevice: BluetoothDevice, tag: Any): Thread() {

        private var bluetoothSocket: BluetoothSocket? = null
        private var tag: Any? = null
        private var isConnected: Boolean = false
        init {
            bluetoothSocket = remoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
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

                    val count = inputStream.available()
                    if (count!=0){
                        val buffer = ByteArray(count)
                        val bytes =inputStream.read(buffer,0,count)
                        if (bytes>0){
                            System.arraycopy(buffer, 0, data, 0, bytes)
                        }

                    }
                }catch (e:IOException){

                }
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
            this.close()
            if (!isConnected) run { Log.d(TAG, "ConnectedThread END since user cancel.") }
            else run { Log.d(TAG, "ConnectedThread END.") }

        }

        fun close(){
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
