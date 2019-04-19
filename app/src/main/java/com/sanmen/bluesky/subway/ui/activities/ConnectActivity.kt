package com.sanmen.bluesky.subway.ui.activities

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice

import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler

import android.os.IBinder
import android.util.Log

import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.gyf.barlibrary.ImmersionBar
import com.inuker.bluetooth.library.utils.BluetoothUtils.sendBroadcast
import com.inuker.bluetooth.library.utils.BluetoothUtils.unregisterReceiver

import com.s.DriveAdapter
import com.sanmen.bluesky.subway.Constant

import com.sanmen.bluesky.subway.data.bean.DriveRecord
import com.sanmen.bluesky.subway.data.dao.DriveDao
import com.sanmen.bluesky.subway.data.database.DriveDatabase
import com.sanmen.bluesky.subway.data.repository.DriveRepository

import com.sanmen.bluesky.subway.service.*
import com.sanmen.bluesky.subway.ui.base.BaseActivity
import com.sanmen.bluesky.subway.utils.TimeUtil
import kotlinx.android.synthetic.main.activity_connect.*
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECTED
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECTING
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECT_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_DATA_AVAILABLE
import com.sanmen.bluesky.subway.Constant.ACTION_DISCONNECTED
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_STARTED
import com.sanmen.bluesky.subway.Constant.DEVICE_NOT_SUPPORT_BLUETOOTH
import com.sanmen.bluesky.subway.Constant.NOT_LOCATION_PERMISSION
import com.sanmen.bluesky.subway.R

import com.sanmen.bluesky.subway.utils.AppExecutors
import java.util.*


class ConnectActivity : BaseActivity() {

    companion object {
        const val DRIVE_INFO="报警信息"
        const val BLUETOOTH_DISCOVERABLE_DURATION = 0
        const val REQUEST_CODE = 1
    }

    private val mTargetDeviceName = "Lsensor"

    private lateinit var mTargetDevice: BluetoothDevice

    private var driveRecord = mutableListOf<DriveRecord>()

    private var linkState = 0//0：未连接；1:正在连接，2:已连接
    /**
     * 蓝牙状态
     */
    private var bluetoothState: Boolean = false

    private var isHaveTarget: Boolean = false

    private var mBluetoothService: BluetoothService? =null

    private lateinit var repository:DriveRepository

    private lateinit var recordList:List<DriveRecord>

//    private lateinit var mConnectManager:ConnectManager

    private val mHandler:Handler by lazy {
        Handler()
    }

    private val appExecutors: AppExecutors by lazy {
        AppExecutors()
    }
    /**
     * 运行方向，0:上行，1:下行
     */
    private val driveDir:Int by lazy {
        intent.getIntExtra(Constant.DRIVE_DIRECTION,-1)
    }

    private val mBluetoothAdapter:BluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private val driveDao:DriveDao by lazy {
        DriveDatabase.getInstance(applicationContext).getDriveDao()
    }

    private val driveAdapter:DriveAdapter by lazy {
        DriveAdapter(this,driveRecord)
    }

    private val linearLayoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(this)
    }

    override fun getLayoutId(): Int= R.layout.activity_connect

    override fun initImmersionBar() {
        super.initImmersionBar()

        ImmersionBar.with(this).titleBar(R.id.toolBar)
            .navigationBarColor(R.color.centerColor )
            .init()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //注册广播
        registerReceiver(mBroadcastReceiver,makeIntentFilter())
    }

    override fun onStart() {
        super.onStart()

        /**
         * 获取最新数据
         */
        getLatestData()

        //申请模糊定位权限
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                2
            )
        }

        if (mBluetoothAdapter!=null){
            btnLinked.isEnabled =true
            //是否开启蓝牙
            if (!mBluetoothAdapter.isEnabled){
                this.turnBluetooth()
            }
        }else{
            //本设备不支持蓝牙
            btnLinked.isEnabled =false
            Toast.makeText(this,DEVICE_NOT_SUPPORT_BLUETOOTH,Toast.LENGTH_SHORT).show()
        }
    }

    override fun initData() {
        super.initData()

        repository = DriveRepository.getInstance(driveDao,appExecutors)

//        mConnectManager = ConnectManager(this,connectionListener)

        val intent = Intent(this,BluetoothService::class.java)
        bindService(intent,mServiceConnection,Context.BIND_AUTO_CREATE)
    }

    override fun initView() {

        btnLinked.setOnClickListener {

            loadView.isShowSubText(false)
            if(linkState==0){
                //搜索目标设备
                searchTargetDevice()
                loadView.startSpinning()
            }else if (linkState==2){
                //断开连接
                mBluetoothService!!.disConnect()
                loadView.stopSpinning()
            }

        }

        driveAdapter.run {
            bindToRecyclerView(rvDriveRecord)
            onItemClickListener = itemClickListener
        }

        rvDriveRecord.run {
            adapter = driveAdapter
            layoutManager = linearLayoutManager
        }

        toolBar.run {
            this.inflateMenu(R.menu.menu)

            this.setOnMenuItemClickListener {
                startActivity(Intent(this@ConnectActivity,SettingActivity::class.java))
                return@setOnMenuItemClickListener false
            }

            this.setNavigationOnClickListener {
                onBackPressed()
            }
        }

        loadView.run {
            this.nextText("未连接",0)
            this.setOnClickListener {
                if (linkState!=2) return@setOnClickListener
                val intent = Intent(this@ConnectActivity, AlarmActivity::class.java)

                if (recordList.isNotEmpty()){
                    val record = recordList.last()
                    intent.putExtra(Constant.RECORD_ID,record.recordId)
                    startActivity(intent)
                }

            }
        }

    }

    /**
     * 搜索目标设备
     */
    private fun searchTargetDevice() {

        if (mBluetoothAdapter.isDiscovering) {
            mBluetoothAdapter.cancelDiscovery()
        }
        //开始搜索
        sendBroadcast(Intent(ACTION_SEARCH_STARTED))
        mBluetoothAdapter.startDiscovery()

        isHaveTarget = false
        mHandler.postDelayed({
            mBluetoothAdapter.cancelDiscovery()
            sendBroadcast(Intent(ACTION_SEARCH_FAILED))
        },1000*10)

    }

    /**
     * 插入/更新一条行车记录
     */
    private fun insertDriveRecord(){

        val date = Date()
        val dateStr = TimeUtil.getTimeString(date)
        val newRecord =DriveRecord()
        newRecord.run {
            this.driveBeginTime =dateStr
            this.driveEndTime = dateStr
            this.driveName=DRIVE_INFO
        }

        //获取最新一条行车记录
        if (recordList.isNotEmpty()){
            val record = recordList.last()
            //结束时间
            val endDate = TimeUtil.getDateString(record.driveEndTime)
            val newDate = TimeUtil.getDateString(dateStr)
            if (endDate == newDate){
                //结束时间为当天则更新当前一条记录，否则新增一条行车记录
                record.driveEndTime = TimeUtil.getTimeString(date)
                repository.updateDriveRecord(record)
            }else{
                repository.insertDriveRecord(newRecord)
            }
        }else{
            repository.insertDriveRecord(newRecord)
        }
        getLatestData()
    }

    private fun getLatestData() {

        appExecutors.diskIO().execute {
            recordList =  repository.getDriveRecordList()

            appExecutors.mainTread().execute{
                //清空行车记录列表
                driveRecord.clear()
                driveRecord.addAll(recordList)
                driveRecord.reverse()
                driveAdapter.notifyDataSetChanged()
            }
        }
    }

    /**
     *打开蓝牙
     */
    private fun turnBluetooth() {

        //请求打开蓝牙
        val requestBluetoothOn  = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestBluetoothOn.run {
            //设置Bluetooth设备可被发现
            this.action = BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE
            //设备可见时间
            this.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BLUETOOTH_DISCOVERABLE_DURATION)
            startActivityForResult(this,REQUEST_CODE)
        }
    }

    /**
     * 过滤器
     */
    private fun makeIntentFilter():IntentFilter{

        return IntentFilter().apply {
            this.addAction(BluetoothDevice.ACTION_FOUND)//发现设备
            this.addAction(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION)//发现设备
            this.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)//搜索结束
            this.addAction(ACTION_SEARCH_STARTED)//开始搜索
            this.addAction(ACTION_SEARCH_FAILED)//搜索失败
            this.addAction(ACTION_CONNECTED)
            this.addAction(ACTION_DISCONNECTED)
            this.addAction(ACTION_CONNECT_FAILED)
            this.addAction(ACTION_READ_DATA_SUCCESS)
            this.addAction(ACTION_READ_DATA_FAILED)
            this.addAction(ACTION_DATA_AVAILABLE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode== REQUEST_CODE){
            when(resultCode){
                //已开启蓝牙
                Activity.RESULT_OK->{
                    bluetoothState = true
                }
                //拒绝开启蓝牙
                Activity.RESULT_CANCELED->{
                    bluetoothState = false
                }
            }
        }
    }

    /**
     * Service连接回调
     */
    private val mServiceConnection = object :ServiceConnection{
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val myBinder = service as BluetoothService.MyBinder
            mBluetoothService = myBinder.getService(this@ConnectActivity)

        }
    }

    /**
     * 广播接收
     */
    private val mBroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){

                ACTION_SEARCH_STARTED->{//开始搜索设备
                    linkState = 1
                    loadView.nextText("搜索中",1)
                }
                ACTION_SEARCH_FAILED->{//搜索失败
                    if (!isHaveTarget){
                        linkState = 0
                        loadView.nextText("搜索失败",1)
                        loadView.stopSpinning()
                    }

                }

                BluetoothDevice.ACTION_FOUND->{//发现设备

                    val device:BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device?.name==mTargetDeviceName){
                        isHaveTarget = true
                        mTargetDevice = device
                        //立即取消搜索
                        mBluetoothAdapter.cancelDiscovery()
                        //开始连接
                        sendBroadcast(Intent(ACTION_CONNECTING))
                        //发起配对，配对成功连接
                        mBluetoothService!!.connectDevice(mTargetDevice.address)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED->{//搜索结束

                }
                ACTION_CONNECTING->{
                    Toast.makeText(this@ConnectActivity,"找到目标设备：$mTargetDeviceName",Toast.LENGTH_SHORT).show()
                    loadView.nextText("连接中",1)
                }

                ACTION_CONNECTED->{//连接成功
                    linkState = 2
                    btnLinked.text = "断开连接"
                    btnLinked.setBackgroundResource(R.drawable.shape_btn_2)
                    Toast.makeText(this@ConnectActivity,"连接成功",Toast.LENGTH_SHORT).show()

                    loadView.nextText(if (driveDir==0) "上行" else "下行",1)
//                    loadView.setCenterText(if (driveDir==0) "上行" else "下行")
                    loadView.isShowSubText(true)
                    loadView.stopSpinning()
                    //插入或更新一条行车记录
                    insertDriveRecord()
                }
                ACTION_DISCONNECTED->{//连接断开
                    linkState = 0
                    btnLinked.text = "连接"
                    btnLinked.setBackgroundResource(R.drawable.shape_btn_1)
                    loadView.nextText("未连接",1)
                    loadView.stopSpinning()

                }
                ACTION_CONNECT_FAILED->{//失败
                    linkState = 0
                    btnLinked.text = "连接"
                    btnLinked.setBackgroundResource(R.drawable.shape_btn_1)
                    loadView.nextText("连接失败",1)
//                    loadView.setCenterText("")
                    loadView.stopSpinning()

                }

                ACTION_READ_DATA_FAILED->{//读取数据失败
//                    Toast.makeText(this@ConnectActivity, "接收指令失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toParseCommand(command: String) {
        Log.e(".ConnectActivity","报警信息为：$command")
        if (command.equals("W", ignoreCase = true)) {
            //W为报警指令
//            toAlarm()

        }

    }

    /**
     * 列表Item点击事件处理
     */
    private val itemClickListener =BaseQuickAdapter.OnItemClickListener{ _, _, position ->

        if (driveRecord.size!=0){
            val record =driveRecord[position]
            val intent=Intent(this,AlarmRecordActivity::class.java)
            intent.putExtra(Constant.RECORD_ID,record.recordId)
            intent.putExtra(Constant.DRIVE_BEGIN_TIME,record.driveBeginTime)
            intent.putExtra(Constant.DRIVE_END_TIME,record.driveEndTime)
            startActivity(intent)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.contains(-1)){
            Toast.makeText(this,NOT_LOCATION_PERMISSION,Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        //注销广播
        unregisterReceiver(mBroadcastReceiver)
        if (mBluetoothService!=null){
            unbindService(mServiceConnection)
            mBluetoothService = null
        }

    }

}
