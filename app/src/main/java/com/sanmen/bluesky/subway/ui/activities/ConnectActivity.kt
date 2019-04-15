package com.sanmen.bluesky.subway.ui.activities

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter

import android.content.*
import android.content.pm.PackageManager

import android.os.IBinder

import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.gyf.barlibrary.ImmersionBar
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
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECT_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_DISCONNECTED
import com.sanmen.bluesky.subway.Constant.ACTION_FIND_DEVICE
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_DEVICE_CANCELED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_DEVICE_NONE
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
        const val PERMISSION_REQUEST_CODE = 2
    }

    private var driveRecord = mutableListOf<DriveRecord>()

    private var linkState = 0//0：未连接；1:正在连接，2:已连接

    private val appExecutors: AppExecutors by lazy {
        AppExecutors()
    }
    /**
     * 蓝牙状态
     */
    private var bluetoothState: Boolean = false

    private lateinit var repository:DriveRepository

    private lateinit var recordList:List<DriveRecord>

    private var mBluetoothService: BluetoothService? =null

    private val driveDir:Int by lazy {
        val mIntent = intent
        mIntent.getIntExtra("driveDir",-1)
    }

//    private val mConnectionManager:ConnectManager by lazy {
//        ConnectManager(this,mConnectionListener)
//    }


    private val bluetoothAdapter:BluetoothAdapter by lazy {
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


    private val itemClickListener =BaseQuickAdapter.OnItemClickListener{ _, _, position ->

        if (driveRecord.size!=0){
            val record =driveRecord[position]
            val intent=Intent(this,AlarmRecordActivity::class.java)
            intent.putExtra(Constant.RECORD_ID,record.id)
            intent.putExtra(Constant.DRIVE_BEGIN_TIME,record.driveBeginTime)
            intent.putExtra(Constant.DRIVE_END_TIME,record.driveEndTime)
            startActivity(intent)
        }

    }

    override fun getLayoutId(): Int= R.layout.activity_connect

    override fun initImmersionBar() {
        super.initImmersionBar()

        ImmersionBar.with(this).titleBar(R.id.toolBar)
            .navigationBarColor(R.color.centerColor )
            .init()
    }

    override fun onStart() {
        super.onStart()

        /**
         * 获取最新数据
         */
        getLatestData()

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),PERMISSION_REQUEST_CODE)
        }

        if (bluetoothAdapter!=null){
            btnLinked.isEnabled =true
            //是否开启蓝牙
            if (!bluetoothAdapter.isEnabled){
                this.turnBluetooth()
            }
        }else{
            btnLinked.isEnabled =false
            Toast.makeText(this,DEVICE_NOT_SUPPORT_BLUETOOTH,Toast.LENGTH_SHORT).show()
        }

    }


    override fun onResume() {
        super.onResume()
        //注册广播
        registerReceiver(mBroadcastReceiver,makeIntentFilter())
    }

    override fun initData() {
        super.initData()

        repository = DriveRepository.getInstance(driveDao,appExecutors)

        val intent = Intent(this,BluetoothService::class.java)
        bindService(intent,mServiceConnection,Context.BIND_AUTO_CREATE)

    }

    override fun initView() {

        btnLinked.setOnClickListener {

            loadView.isShowSubText(false)
            if(linkState==0){
                //执行搜索蓝牙设备，并在搜索成功后连接设备
                mBluetoothService!!.searchBluetoothDevice()
                loadView.playAnimation()
            }else if (linkState==2){
                //断开连接
                mBluetoothService!!.disConnect()
                loadView.cancelAnimation()
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
            this.setOnClickListener {
                if (linkState!=2) return@setOnClickListener
                val intent = Intent(this@ConnectActivity,AlarmActivity::class.java)

                if (!recordList.isEmpty()){
                    val record = recordList.last()
                    intent.putExtra(Constant.RECORD_ID,record.id)
                    startActivity(intent)
                }

            }
        }

    }

    //插入/更新一条行车记录
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
        if (!recordList.isEmpty()){
            var record = recordList.last()
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
            this.addAction(ACTION_SEARCH_STARTED)
            this.addAction(ACTION_CONNECTED)
            this.addAction(ACTION_DISCONNECTED)
            this.addAction(ACTION_CONNECT_FAILED)
            this.addAction(ACTION_SEARCH_DEVICE_NONE)
            this.addAction(ACTION_READ_DATA_SUCCESS)
            this.addAction(ACTION_READ_DATA_FAILED)
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
            mBluetoothService = myBinder.getService()
        }

    }

    private val mBroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                ACTION_SEARCH_STARTED->{//开始搜索设备
                    linkState = 1
//                    Toast.makeText(this@ConnectActivity,"开始搜索设备",Toast.LENGTH_SHORT).show()
                    loadView.setCenterText("搜索中")
                }
                ACTION_FIND_DEVICE->{
//                    Toast.makeText(this@ConnectActivity,"找到目标设备：Lsensor",Toast.LENGTH_SHORT).show()
                    loadView.setCenterText("开始连接")
                }

                ACTION_SEARCH_DEVICE_CANCELED->{
//                    Toast.makeText(this@ConnectActivity,"取消搜索设备",Toast.LENGTH_SHORT).show()
                }

                ACTION_CONNECTED->{//连接成功
                    linkState = 2
                    btnLinked.text = "断开连接"
                    btnLinked.setBackgroundResource(R.drawable.shape_btn_2)
                    Toast.makeText(this@ConnectActivity,"连接成功",Toast.LENGTH_SHORT).show()
                    loadView.setCenterText("上行")
                    loadView.isShowSubText(true)
                    loadView.cancelAnimation()
                    //插入或更新一条行车记录
                    insertDriveRecord()
                }
                ACTION_DISCONNECTED->{//连接断开
                    linkState = 0
                    btnLinked.text = "连接"
                    btnLinked.setBackgroundResource(R.drawable.shape_btn_1)
//                    Toast.makeText(this@ConnectActivity,"连接已断开",Toast.LENGTH_SHORT).show()
                    loadView.setCenterText("")
                    loadView.cancelAnimation()

                }
                ACTION_CONNECT_FAILED->{//连接失败
                    linkState = 0
                    btnLinked.text = "连接"
                    btnLinked.setBackgroundResource(R.drawable.shape_btn_1)
//                    Toast.makeText(this@ConnectActivity,"连接失败",Toast.LENGTH_SHORT).show()
                    loadView.setCenterText("连接失败")
                    loadView.cancelAnimation()
                }

                ACTION_READ_DATA_SUCCESS->{//读取数据成功
//                    Toast.makeText(this@ConnectActivity, "接收指令成功", Toast.LENGTH_SHORT).show()
                }
                ACTION_READ_DATA_FAILED->{//读取数据失败
//                    Toast.makeText(this@ConnectActivity, "接收指令失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE){
            Toast.makeText(this,NOT_LOCATION_PERMISSION,Toast.LENGTH_SHORT)
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
