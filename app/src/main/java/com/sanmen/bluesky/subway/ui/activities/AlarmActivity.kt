package com.sanmen.bluesky.subway.ui.activities

import android.app.NotificationManager
import android.content.*
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.gyf.barlibrary.ImmersionBar
import com.sanmen.bluesky.subway.Constant
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_PLATFORM_DEVICE_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_PLATFORM_DEVICE_SUCCESS
import com.sanmen.bluesky.subway.Constant.ACTION_SEARCH_STARTED
import com.sanmen.bluesky.subway.R
import com.sanmen.bluesky.subway.adapters.AlarmAdapter
import com.sanmen.bluesky.subway.data.bean.AlarmInfo
import com.sanmen.bluesky.subway.data.bean.NotifyMessage
import com.sanmen.bluesky.subway.data.dao.AlarmDao
import com.sanmen.bluesky.subway.data.database.DriveDatabase
import com.sanmen.bluesky.subway.data.repository.AlarmRepository
import com.sanmen.bluesky.subway.service.BluetoothService
import com.sanmen.bluesky.subway.ui.base.BaseActivity
import com.sanmen.bluesky.subway.ui.fragments.TimeSelectDialog
import com.sanmen.bluesky.subway.utils.AppExecutors
import com.sanmen.bluesky.subway.utils.SoundPoolUtils
import com.sanmen.bluesky.subway.utils.TimeUtil
import kotlinx.android.synthetic.main.activity_alarm.*
import kotlinx.android.synthetic.main.activity_alarm.toolBar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class AlarmActivity : BaseActivity(), TimeSelectDialog.OnDialogCloseListener {

    private var isOpenTimeout: Boolean = false//开门时间超时
    private var isDoorOpen: Boolean = false
    private var isBeginDelay2 = false//延迟

    private var isAlarmOver: Boolean = false//是否产生已经报警，在出站时重置状态

    private var isAlarmOver2: Boolean = false//是否产生已经报警，在出站时重置状态

    private var isAlarming: Boolean=false

    private var isBeginDelay:Boolean = false

    private var isTrainInto:Boolean = false //列车是否进站

    private var alarmData = mutableListOf<AlarmInfo>()

    private var isSound:Boolean = true

    private var delayTime:Int = 0//延迟时间值0-10

    private var intervalTime:Int = 0

    private var mRecordId = -1

    private var driveDir = -1

    private var lightThreshold:Int=0

    private var unitValue:Int = 0

    private var mBluetoothService: BluetoothService? =null


    private val mHandler: Handler by lazy {
        Handler()
    }
    private val mHandler2: Handler by lazy {
        Handler()
    }

    private val appExecutors: AppExecutors by lazy {
        AppExecutors()
    }

    private val dialog:TimeSelectDialog by lazy {
        TimeSelectDialog()
    }

    private val mSoundPoolUtils:SoundPoolUtils by lazy {
        SoundPoolUtils.getInstance(this)
    }

    private lateinit var alarmRepository: AlarmRepository

    private val alarmDao:AlarmDao by lazy {
        DriveDatabase.getInstance(this).getAlarmDao()
    }

    private val alarmAdapter:AlarmAdapter by lazy {
        AlarmAdapter(alarmData)
    }

    private val linearLayoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(this)
    }


    override fun getLayoutId(): Int= R.layout.activity_alarm

    override fun initImmersionBar() {
        super.initImmersionBar()
        ImmersionBar.with(this).titleBar(R.id.toolBar)
            .navigationBarColor(R.color.colorPrimary).init()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sharedPref.run {
            isSound = this.getBoolean(Constant.SOUND_STATE,false)
            delayTime = this.getInt(Constant.DELAY_TIME,0)
            intervalTime = this.getInt(Constant.INTERVAL_TIME,0)
            lightThreshold = this.getInt(Constant.LIGHT_THRESHOLD,4)
            unitValue = this.getInt(Constant.UNIT_VALUE,5000)
        }

        ivSound.isSelected = isSound

        //绑定服务
        val intent = Intent(this,BluetoothService::class.java)
        bindService(intent,mServiceConnection,Context.BIND_AUTO_CREATE)
    }

    override fun initData() {
        super.initData()
        //获取当前行车记录ID
        intent.run {
            mRecordId = this.getIntExtra(Constant.RECORD_ID,-1)
            driveDir = this.getIntExtra(Constant.DRIVE_DIRECTION,-1)
        }

        alarmRepository = AlarmRepository.getInstance(alarmDao,appExecutors)

        EventBus.getDefault().register(this)
    }

    override fun initView() {
        super.initView()

        alarmAdapter.run {
            onItemClickListener = itemClickListener
        }

        rvAlarmList.run {
            this.layoutManager = linearLayoutManager
            this.adapter = alarmAdapter
        }

        btnClear.setOnClickListener(onClickListener)
        ivSound.setOnClickListener(onClickListener)
        ivAdjust.setOnClickListener(onClickListener)

        toolBar.inflateMenu(R.menu.menu)
        toolBar.setOnMenuItemClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
            return@setOnMenuItemClickListener false
        }
        toolBar.setNavigationOnClickListener {
            onBackPressed()
        }

    }

    private val onClickListener = View.OnClickListener {
        when(it.id){
            //静音按钮
            R.id.ivSound->{
//                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
//                    && !notificationManager.isNotificationPolicyAccessGranted
//                ) {
//                    val intent =Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
//                    applicationContext.startActivity(intent)
//                }else{
//                    isSound = !isSound
//                    it.isSelected = isSound
//                    audioManager.run {
//                        this.ringerMode =if (isSound){
//                            AudioManager.RINGER_MODE_NORMAL
//                        }else{
//                            AudioManager.RINGER_MODE_SILENT
//                        }
//                        this.getStreamVolume(AudioManager.STREAM_RING)
//
//                    }
//                }

                //取消当前告警
                mSoundPoolUtils.cancelVideoAndVibrator()
                //手动判定车门已开启


            }
            //清空按钮
            R.id.btnClear->{
                alarmData.clear()
                alarmAdapter.notifyDataSetChanged()
            }
            //时间按钮
            R.id.ivAdjust->{

                dialog.run {
                    val bundle = Bundle()
                    bundle.putInt(Constant.DELAY_TIME,delayTime)
                    bundle.putInt(Constant.INTERVAL_TIME,intervalTime)
                    this.arguments= bundle
                    this.show(supportFragmentManager,"timeSelectDialog")
                }
            }
        }
    }

    private val itemClickListener = BaseQuickAdapter.OnItemClickListener{ _, _, _ ->

        //开启声音和震动提示
        mSoundPoolUtils.startVideoAndVibrator(R.raw.voice_no_door_open,0,1000)
    }

    override fun onClick(progress1: Int,progress2: Int) {
        this.delayTime = progress1
        this.intervalTime = progress2

    }

    override fun onPause() {
        super.onPause()
        mSoundPoolUtils.release()
        sharedPref.edit().apply {
            this.putBoolean(Constant.SOUND_STATE,isSound)
            this.putInt(Constant.DELAY_TIME,delayTime)
            this.putInt(Constant.INTERVAL_TIME,intervalTime)
            //异步提交配置信息
            this.apply()
        }
    }

    //EventBus事件接收
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(msg: NotifyMessage){
        when(msg.code){
            ACTION_SEARCH_STARTED ->{//开始搜索设备
                if (msg.getData() as Int!=1){//列车已进站
                    setAlarmTitle("搜索中")
                }
            }
            ACTION_SEARCH_PLATFORM_DEVICE_SUCCESS ->{//搜索到站台蓝牙
                setAlarmTitle("列车已进站")
                isTrainInto = true


            }
            ACTION_SEARCH_PLATFORM_DEVICE_FAILED ->{//列车已出站
                setAlarmTitle("列车已出站")
                isTrainInto = false
                setDefaultState()
            }
            ACTION_READ_DATA_SUCCESS ->{//读取数据成功
                val lightData = msg.getData<String>()
                toParseCommand(lightData)
            }
            ACTION_READ_DATA_FAILED->{
                Toast.makeText(this@AlarmActivity,"读取数据失败！",Toast.LENGTH_SHORT).show()
                Log.e(".AlarmActivity",msg.getData())
            }
        }
    }

    private fun setDefaultState() {

        isBeginDelay2 = false//延迟
        isAlarmOver = false//是否产生已经报警，在出站时重置状态
        isAlarming=false
        isBeginDelay = false
        isOpenTimeout = false//列车开门超时
        isDoorOpen = false

    }

    /**
     * 设置标题
     */
    private fun setAlarmTitle(title: String) {
        alarmTitle.text = title
        Toast.makeText(this,"列车状态已更新，$title",Toast.LENGTH_SHORT).show()
    }

    /**
     * 根据报警灯状态处理告警信息
     * @param state 报警灯状态
     */
    private fun addOneAlarmInfo(info:String,state:Boolean){
        val alarmInfo=AlarmInfo()

        alarmInfo.run {
            this.direction = driveDir
            this.alarmText=info
            this.alarmTime = TimeUtil.getTimeString(Date())
            this.recordId = mRecordId
        }

        alarmRepository.insertAlarmItem(alarmInfo)

        alarmData.add(alarmInfo)
        alarmAdapter.notifyDataSetChanged()
        rvAlarmList.scrollToPosition(alarmData.size-1)

        if(state){//灯亮，到站未开门的情况下
            //开启振动和声音
            mSoundPoolUtils.cancelVideoAndVibrator()
            mSoundPoolUtils.startVideoAndVibrator(R.raw.voice_no_door_open,-1,1000)
        }else{
            mSoundPoolUtils.cancelVideoAndVibrator()
            mSoundPoolUtils.playVideo(R.raw.voice_alarm,1)
        }

//        isAlarmOver = true
    }

    /**
     * 光照数据处理
     */
    private fun toParseCommand(lightData: String?) {

        var lightValue = 0
        if (lightData != null&&lightData!="") {
            if (lightData.indexOf(',')!=-1){//双灯,取较小的灯光照进行比较
                var array = lightData.split(',')
                if (array.isNotEmpty()){
                    var temp = array[0].toInt()
                    lightValue =if (temp<array[1].toInt()) temp else array[1].toInt()
                }
            }else{//单灯
                lightValue = lightData.toInt()
            }
        }

        if (isTrainInto&&!isAlarming){//如果未报警,前提是处于进站状态
            if (getAlarmState(lightValue)){//灯亮
                if (!isBeginDelay){//是否开始计时
                    mHandler.postDelayed({//根据指定延迟时间，开始判定开门
                        isAlarming = false
                        isDoorOpen = false
                        isBeginDelay = true
                        isAlarmOver = false
                    },(1000*delayTime).toLong())
                    isAlarming = true//已捕捉到报警信号

                }else{//计时结束，开始判定开门
                    if (!isAlarmOver&&!isDoorOpen){//如果未报过警，且未开门，执行报警提示，防止多次报警;
                        addOneAlarmInfo("车门未开启，请注意！！",true)
                        isAlarmOver = true
                    }
                    if (isDoorOpen){//如果门由开到关，执行嘟嘟报警声提示
                        mSoundPoolUtils.cancelVideoAndVibrator()
                        mSoundPoolUtils.playVideo(R.raw.voice_door_close_over,0)//播放一次，嘟嘟声
                        isDoorOpen = false
                    }
                }
                isBeginDelay2 = false//重置为未报警
            }else{
                //列车门为打开状态，灯灭，需要避免一开始为灯灭的情况
                if (!isBeginDelay2){//已报警
                    mHandler.postDelayed({//开门后等待3秒，判定关门
                        isAlarmOver2 = false
                        isDoorOpen = true
                    },(1000*3).toLong())

                    mHandler2.postDelayed({//开门后等待10秒，判定关门.告警提示
                        isOpenTimeout = true
                    },(1000*10).toLong())

                    isBeginDelay2 = true
                    isAlarmOver = true
                    //关闭报警提示
                    mSoundPoolUtils.cancelVideoAndVibrator()
                }else{
                    if (!isAlarmOver2&&isOpenTimeout){//车门打开超时提示,滴滴声
                        addOneAlarmInfo("列车未关门，请注意！！",false)
                        isOpenTimeout = false
                        isAlarmOver2 = true
                    }
                }

//                if (isOpenTimeout){//车门打开超时提示,滴滴声
//
//                }
            }
        }

    }

    private fun getAlarmState(lightValue:Int):Boolean=lightValue>lightThreshold*unitValue //光照阈值*单元值,例如:4*5000=20000

    /**
     * Service连接回调
     */
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val myBinder = service as BluetoothService.MyBinder
            mBluetoothService = myBinder.getService(this@AlarmActivity)

        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mBluetoothService!=null){
            unbindService(mServiceConnection)
            mBluetoothService = null
        }
        EventBus.getDefault().unregister(this)
    }

}
