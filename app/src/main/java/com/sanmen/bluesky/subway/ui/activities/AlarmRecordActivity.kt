package com.sanmen.bluesky.subway.ui.activities

import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import com.gyf.barlibrary.ImmersionBar
import com.sanmen.bluesky.subway.Constant
import com.sanmen.bluesky.subway.R
import com.sanmen.bluesky.subway.adapters.AlarmRecordAdapter
import com.sanmen.bluesky.subway.data.bean.AlarmInfo
import com.sanmen.bluesky.subway.data.dao.AlarmDao
import com.sanmen.bluesky.subway.data.database.DriveDatabase
import com.sanmen.bluesky.subway.data.repository.AlarmRepository
import com.sanmen.bluesky.subway.ui.base.BaseActivity
import com.sanmen.bluesky.subway.utils.AppExecutors
import kotlinx.android.synthetic.main.activity_alarm_record.*

class AlarmRecordActivity : BaseActivity() {

    var alarmData = mutableListOf<AlarmInfo>()

    var mRecordId:Int = -1

    lateinit var beginTime:String

    lateinit var endTime:String

    lateinit var alarmList:List<AlarmInfo>

    private lateinit var alarmRepository: AlarmRepository

    private val appExecutors:AppExecutors by lazy {
        AppExecutors()
    }

    private val alarDao:AlarmDao by lazy {
        DriveDatabase.getInstance(this).getAlarmDao()
    }

    private val alarmRecordAdapter:AlarmRecordAdapter by lazy {
        AlarmRecordAdapter(alarmData)
    }

    private val linearLayoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(this)
    }

    override fun getLayoutId(): Int= R.layout.activity_alarm_record


    override fun initImmersionBar() {
        super.initImmersionBar()
        ImmersionBar.with(this).titleBar(R.id.toolBar).init()
    }

    override fun initData() {
        super.initData()

        mRecordId=intent.getIntExtra(Constant.RECORD_ID,-1)
        beginTime=intent.getStringExtra(Constant.DRIVE_BEGIN_TIME)
        endTime=intent.getStringExtra(Constant.DRIVE_END_TIME)

        alarmRepository = AlarmRepository.getInstance(alarDao,appExecutors)

        appExecutors.diskIO().execute {
            alarmList = alarmRepository.getAlarmInfoByRecordId(mRecordId)

            alarmData.clear()
            alarmData.addAll(alarmList)
        }


    }

    override fun initView() {
        super.initView()

        tvStartTime.text = beginTime
        tvEndTime.text = endTime

        rvAlarmList.run {
            adapter = alarmRecordAdapter
            layoutManager = linearLayoutManager
        }

        toolBar.run {
            toolBar.inflateMenu(R.menu.menu)
            toolBar.setNavigationOnClickListener {
                onBackPressed()
            }
            toolBar.setOnMenuItemClickListener {
                startActivity(Intent(this@AlarmRecordActivity,SettingActivity::class.java))
                return@setOnMenuItemClickListener false
            }
        }


    }
}
