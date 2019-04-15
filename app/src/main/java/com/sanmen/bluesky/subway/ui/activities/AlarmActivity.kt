package com.sanmen.bluesky.subway.ui.activities

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.gyf.barlibrary.ImmersionBar
import com.sanmen.bluesky.subway.Constant
import com.sanmen.bluesky.subway.R
import com.sanmen.bluesky.subway.adapters.AlarmAdapter
import com.sanmen.bluesky.subway.data.bean.AlarmInfo
import com.sanmen.bluesky.subway.data.dao.AlarmDao
import com.sanmen.bluesky.subway.data.database.DriveDatabase
import com.sanmen.bluesky.subway.data.repository.AlarmRepository
import com.sanmen.bluesky.subway.ui.base.BaseActivity
import com.sanmen.bluesky.subway.ui.fragments.TimeSelectDialog
import com.sanmen.bluesky.subway.utils.AppExecutors
import com.sanmen.bluesky.subway.utils.SoundPoolUtils
import com.sanmen.bluesky.subway.utils.TimeUtil
import kotlinx.android.synthetic.main.activity_alarm.*
import java.util.*

class AlarmActivity : BaseActivity(), TimeSelectDialog.OnDialogCloseListener {

    private var alarmData = mutableListOf<AlarmInfo>()

    private var isSound:Boolean = true

    private var delayTime = 0//延迟时间值0-100

    private var mRecordId = -1

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

        sharedPref.run {
            isSound = this.getBoolean(Constant.SOUND_STATE,false)
            delayTime = this.getInt(Constant.DELAY_TIME,0)
        }

        ivSound.isSelected = isSound
    }

    override fun initData() {
        super.initData()
        //获取当前行车记录ID
        mRecordId = intent.getIntExtra(Constant.RECORD_ID,-1)

        alarmRepository = AlarmRepository.getInstance(alarmDao,appExecutors)

    }

    /**
     * 在退出当前页面时，保存报警数据和行车记录
     */
    private fun saveData() {
        //正常情况是在产生一条报警数据后，便插入alarm数据表中


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
        btnAddData.setOnClickListener(onClickListener)

        toolBar.inflateMenu(R.menu.menu)
        toolBar.setOnMenuItemClickListener {
            startActivity(Intent(this,SettingActivity::class.java))
            return@setOnMenuItemClickListener false
        }
        toolBar.setNavigationOnClickListener {
            saveData()
            onBackPressed()
        }

    }

    private val onClickListener = View.OnClickListener {
        when(it.id){
            //静音按钮
            R.id.ivSound->{

                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

                isSound = !isSound
                it.isSelected = isSound
                audioManager.run {
                    this.ringerMode =if (isSound){
                        AudioManager.RINGER_MODE_NORMAL
                    }else{
                        AudioManager.RINGER_MODE_SILENT
                    }
                    this.getStreamVolume(AudioManager.STREAM_RING)

                }

            }
            //清空按钮
            R.id.btnClear->{
                alarmData.clear()
                alarmAdapter.notifyDataSetChanged()
            }
            //时间按钮
            R.id.ivAdjust->{

                dialog.run {
                    var bundle = Bundle()
                    bundle.putInt(Constant.DELAY_TIME,delayTime)
                    this.arguments= bundle
                    this.show(supportFragmentManager,"timeSelectDialog")
                }
            }
            R.id.btnAddData->{
                //测试数据添加
                var alarmInfo=AlarmInfo()

                alarmInfo.run {
                    this.alarmText="请确认出站信号"
                    this.alarmTime = TimeUtil.getTimeString(Date())
                    this.recordId = mRecordId
                }

                alarmRepository.insertAlarmItem(alarmInfo)

                alarmData.add(alarmInfo)
                alarmAdapter.notifyDataSetChanged()
                rvAlarmList.scrollToPosition(alarmData.size-1)
            }
        }
    }

    private val itemClickListener = BaseQuickAdapter.OnItemClickListener{adapter, view, position ->

        //开启声音和震动提示
        mSoundPoolUtils.startVideoAndVibrator(R.raw.ring,1000)
    }

    override fun onClick(progress: Int) {
        this.delayTime = progress

    }

    override fun onPause() {
        super.onPause()
        mSoundPoolUtils.release()
        sharedPref.edit().apply {
            this.putBoolean(Constant.SOUND_STATE,isSound)
            this.putInt(Constant.DELAY_TIME,delayTime)
            //异步提交配置信息
            this.apply()
        }
    }

}
