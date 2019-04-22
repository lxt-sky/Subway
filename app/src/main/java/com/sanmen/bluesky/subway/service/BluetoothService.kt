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
import android.os.Build
import com.inuker.bluetooth.library.utils.BluetoothUtils.sendBroadcast


/**
 * 蓝牙串口服务UUID--SPP
 */
private const val MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"

private const val TAG = ".BluetoothService"


class BluetoothService : Service() {

    private var mBluetoothGatt: BluetoothGatt? = null
    private var isConnected: Boolean = false

    private var mTargetDevice: BluetoothDevice? = null

    private var deviceList = mutableListOf<BluetoothDevice>()

    private lateinit var connectThread:Thread

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


    fun connectDevice(devices:List<BluetoothDevice>){
//        for (item in devices){
//            deviceList.add(item)
//            connectDevice(item)
//        }

    }

    /**
     * 连接目标设备
     * @param address 设备地址
     * @param tag 自定义标识
     */
    fun connectDevice(address: String){

        if (mBluetoothGatt!=null){
            mBluetoothGatt?.close()
            mBluetoothGatt = null
        }


        val remoteDevice = mBluetoothAdapter.getRemoteDevice(address)
        deviceList.add(remoteDevice)
        mTargetDevice = remoteDevice

        mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            remoteDevice.connectGatt(this,false,blueGattCallback,BluetoothDevice.TRANSPORT_LE)
        }else{
            remoteDevice.connectGatt(this,false,blueGattCallback)
        }


//        //获取目标设备
//        mTargetDevice = mTargetDevice?:mBluetoothAdapter.getRemoteDevice(address)
//
//        //未配对设备发起配对
//        if (mTargetDevice!!.bondState==BluetoothDevice.BOND_NONE){
//            mTargetDevice!!.createBond()
//        }else{
//            //配对成功直接连接
//            connect()
//        }
    }

    private val blueGattCallback: BluetoothGattCallback? = object : BluetoothGattCallback() {
        /**
         * 连接状态变化
         */
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (status==BluetoothGatt.GATT_SUCCESS){

            }else{

                mBluetoothGatt?.disconnect()

                connectDevice(mTargetDevice!!.address)
            }

        }

        /**
         * 发现服务
         */
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
        }

        /**
         * 读操作回调
         */
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
        }

        /**
         * 数据返回回调
         */
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
        }
    }

    /**
     * 发起蓝牙连接
     */
    fun connect(){
        if (mTargetDevice == null) {
            return
        }
        //socket连接-spp协议连接
        val bluetoothSocket = mTargetDevice!!.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))

        //开启线程
        connectThread=thread(start = true,name = "ConnectThread"){
            isConnected=true
            //连接中
            broadcastUpdate(ACTION_CONNECTING)
                if (isConnected){
                try {
                    //执行连接
                    bluetoothSocket.connect()
                }catch (e: IOException){
                    //连接失败
                    broadcastUpdate(ACTION_DISCONNECTED)
                    return@thread
                }
            }
            val inputStream:InputStream
            try {
                inputStream = bluetoothSocket.inputStream
            }catch (e:IOException){
                broadcastUpdate(ACTION_DISCONNECTED)
                return@thread
            }

            //连接成功
            broadcastUpdate(ACTION_CONNECTED)

            val intent = Intent(ACTION_READ_DATA_SUCCESS)
            var i=0
            val data = ByteArray(1024)
            while (isConnected){
                try {
                    val byte = inputStream.read().toByte()
                    if (byte== '\r'.toByte()){//13,10---\r\n

                        val reData = ByteArray(i)
                        System.arraycopy(data, 0, reData, 0, i)

                        intent.putExtra(LIGHT_DATA, String(reData))
                        sendBroadcast(intent)
                        i=0
                    }
                    else if (byte=='\n'.toByte()){
                        continue
                    }else{
                        data[i++]=byte
                    }
                }catch (e:IOException){
                    broadcastUpdate(ACTION_READ_DATA_FAILED)
                    break
                }
            }
            bluetoothSocket.close()
            if (!isConnected) run { Log.d(TAG, "ConnectedThread END since user cancel.") }
            else run { Log.d(TAG, "ConnectedThread END.") }
        }
    }

    /**
     * 断开连接
     */
    fun disConnect(){
        //广播当前状态为连接已断开
        isConnected = false
        broadcastUpdate(ACTION_DISCONNECTED)
        mTargetDevice = null
        connectThread.interrupt()

    }

    /**
     * 设备配对监听
     */
    private val mBluetoothBondListener = object : BluetoothBondListener() {
        override fun onBondStateChanged(mac: String?, bondState: Int) {

            if (bondState==BluetoothDevice.BOND_BONDED){
                connect()
                //广播配对成功
                broadcastUpdate(BOND_SUCCESS)
                return
            }
            //配对失败，广播连接失败
            broadcastUpdate(BOND_FAILED)
        }
    }

    /**
     * 发送广播
     */
    private fun broadcastUpdate(intentAction: String) {

        val intent = Intent(intentAction)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? =mBinder

    override fun onDestroy() {
        super.onDestroy()

        mClient.unregisterBluetoothBondListener(mBluetoothBondListener)
    }

    class MyBinder(private val service: BluetoothService):Binder(){
        var context: Context? =null

        fun getService(context: Context):BluetoothService{

            this.context = context
            return service
        }
    }





}
