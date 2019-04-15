package com.sanmen.bluesky.subway.service

import android.app.IntentService
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.provider.Settings.Global.DEVICE_NAME
import android.widget.Toast
import com.inuker.bluetooth.library.BluetoothClient
import com.inuker.bluetooth.library.connect.options.BleConnectOptions
import com.inuker.bluetooth.library.connect.response.BleConnectResponse
import com.inuker.bluetooth.library.model.BleGattProfile
import com.inuker.bluetooth.library.search.SearchRequest
import com.inuker.bluetooth.library.search.SearchResult
import com.inuker.bluetooth.library.search.response.SearchResponse
import com.sanmen.bluesky.subway.manager.ClientManager
import java.util.*

import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.BleConnectManager.disconnect
import com.inuker.bluetooth.library.connect.response.BleReadResponse
import com.inuker.bluetooth.library.model.BleGattCharacter
import com.inuker.bluetooth.library.model.BleGattService
import com.inuker.bluetooth.library.utils.ByteUtils
import com.sanmen.bluesky.subway.Constant
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECTED
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECT_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_DISCONNECTED
import com.sanmen.bluesky.subway.Constant.ACTION_FIND_DEVICE
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_DEVICE_CANCELED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_DEVICE_NONE
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_STARTED
import com.sanmen.bluesky.subway.data.dao.AlarmDao
import com.sanmen.bluesky.subway.data.dao.DriveDao
import com.sanmen.bluesky.subway.data.database.DriveDatabase

/**
 * 蓝牙串口服务UUID
 */
private const val MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"


/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class BluetoothService : Service() {

    private var deviceList = mutableListOf<BluetoothDevice>()

    /**
     * 目标设备名称 Lsensor
     */
    private val mTargetDeviceName = "Mi Band 3"

    private var mDevice: BluetoothDevice? = null

    private var Uuid_Character: UUID? = null

    private var Uuid_Service: UUID? = null

    private val mBinder:MyBinder by lazy {
        MyBinder(this@BluetoothService)
    }

    private val mClient:BluetoothClient by lazy {
        ClientManager.getClient()
    }

    /**
     * 打开蓝牙
     */
    fun openBluetooth(){
        mClient.openBluetooth()
    }

    /**
     * 关闭蓝牙
     */
    fun closeBluetooth(){
        if (mDevice==null) return
        mClient.disconnect(mDevice!!.address)
        mClient.closeBluetooth()
    }

    fun getBlueDevice(): BluetoothDevice? =mDevice


    /**
     * 搜索蓝牙设备列表
     */
    fun searchBluetoothDevice(){
        if (!mClient.isBluetoothOpened){
            mClient.openBluetooth()
        }else{
            getBluetoothDevice()
        }
    }

    /**
     * 连接设备
     */
    fun connectBluetooth(){
        if (mDevice==null) return

        val options = BleConnectOptions.Builder()
            .setConnectRetry(3)
            .setConnectTimeout(20000)
            .setServiceDiscoverRetry(3)
            .setServiceDiscoverTimeout(10000)
            .build()

        mClient.connect(mDevice!!.address,options) { code, data ->
            //请求连接成功
            if (code==Constants.REQUEST_SUCCESS){
                //广播当前状态为连接成功
                broadcastUpdate(ACTION_CONNECTED)
                setGattProfile(data)
                readData()
            }else{
                //广播当前状态为连接失败
                broadcastUpdate(ACTION_CONNECT_FAILED)

            }
        }

    }

    /**
     * 断开连接
     */
    fun disConnect(){
        //广播当前状态为连接已断开
        broadcastUpdate(ACTION_DISCONNECTED)
        mClient.disconnect(mDevice!!.address)
        mDevice = null

    }

    /**
     * 获取蓝牙设备列表
     */
    private fun getBluetoothDevice() {
        val request:SearchRequest = SearchRequest.Builder()
            .searchBluetoothLeDevice(3000,3)
            .searchBluetoothClassicDevice(5000)
            .searchBluetoothLeDevice(2000)
            .build()
        mClient.search(request,response)
    }

    private val response = object :SearchResponse{
        override fun onSearchStopped() {

        }

        override fun onSearchStarted() {
            broadcastUpdate(ACTION_SEARCH_STARTED)
        }

        override fun onDeviceFounded(result: SearchResult?) {
            val device= result?.device
            if (device != null ) {

                if (mTargetDeviceName == device.name){
                    mDevice = device
                    //找到指定设备，停止扫描
                    mClient.stopSearch()
                    broadcastUpdate(ACTION_FIND_DEVICE)

                    //执行连接请求
                    connectBluetooth()
                }

            }else{
                //广播，无目标蓝牙设备
                broadcastUpdate(ACTION_SEARCH_DEVICE_NONE)
            }

        }

        override fun onSearchCanceled() {

            broadcastUpdate(ACTION_SEARCH_DEVICE_CANCELED)
        }

    }

    /**
     * 读取数据
     */
    private fun readData() {
        if (Uuid_Character!=null&&Uuid_Service!=null&&mDevice!=null){
            mClient.read(mDevice!!.address,Uuid_Service,Uuid_Character,mReadRsp)
        }

    }

    /**
     * 提起配置信息
     */
    private fun setGattProfile(data: BleGattProfile?) {
        //遍历服务列表，还可以根据UUID获取指定服务data.getService(UUID)
        val services: MutableList<BleGattService>? = data?.services ?: return
        if (services != null) {
            for (service in services){
                val characters:List<BleGattCharacter> = service.characters
                for (character in characters){
                    if (BluetoothGattCharacteristic.PROPERTY_READ == character.property) {
                        Uuid_Character = character.uuid
                        Uuid_Service = service.uuid
                        return
                    }
                }
            }
        }
    }

    /**
     * 读取数据
     */
    private val mReadRsp: BleReadResponse = BleReadResponse { code, data ->
        if (code == Constants.REQUEST_SUCCESS) {

            //解析指令,完成后续任务.
            //广播当前状态
            broadcastUpdate(ACTION_READ_DATA_SUCCESS)

            toParseInstruction(ByteUtils.byteToString(data))
        } else {

            //广播当前状态
            broadcastUpdate(ACTION_READ_DATA_FAILED)
        }
    }

    /**
     * 解析指令
     */
    private fun toParseInstruction(data:String){
        //在此进行数据的处理并返回至Activity当中


    }

    /**
     * 发送广播
     */
    private fun broadcastUpdate(intentAction: String) {

        val intent = Intent(intentAction)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? =mBinder

    override fun onUnbind(intent: Intent?): Boolean {

        return super.onUnbind(intent)
    }

    class MyBinder(private val service: BluetoothService):Binder(){

        fun getService():BluetoothService{
            return service
        }
    }





}
