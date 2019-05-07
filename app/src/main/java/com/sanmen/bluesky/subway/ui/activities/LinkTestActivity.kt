package com.sanmen.bluesky.subway.ui.activities

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.gyf.barlibrary.ImmersionBar
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECTED
import com.sanmen.bluesky.subway.Constant.ACTION_CONNECT_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_DISCONNECTED
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_FAILED
import com.sanmen.bluesky.subway.Constant.ACTION_READ_DATA_SUCCESS
import com.sanmen.bluesky.subway.Constant.MAC_ADDRESS
import com.sanmen.bluesky.subway.R
import com.sanmen.bluesky.subway.adapters.LightDataAdapter
import com.sanmen.bluesky.subway.data.bean.NotifyMessage
import com.sanmen.bluesky.subway.service.BluetoothService
import com.sanmen.bluesky.subway.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_link_test.*
import kotlinx.android.synthetic.main.activity_link_test.toolBar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class LinkTestActivity : BaseActivity() {

    private var deviceMac: String? = null

    private var mBluetoothService: BluetoothService? =null

    private var lightData = mutableListOf<String>()

    private val linearLayoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(this)
    }

    private val adapter:LightDataAdapter by lazy {
        LightDataAdapter(lightData)
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_link_test
    }

    override fun initImmersionBar() {
        super.initImmersionBar()
        ImmersionBar.with(this).titleBar(toolBar).init()
    }

    override fun initData() {
        super.initData()

        intent.getStringExtra(MAC_ADDRESS)?.let {
            deviceMac =it
        }

        //绑定服务
        val intent = Intent(this,BluetoothService::class.java)
        bindService(intent,mServiceConnection, Context.BIND_AUTO_CREATE)

        EventBus.getDefault().register(this)

    }

    override fun initView() {
        super.initView()

        toolBar.run {
            this.setNavigationOnClickListener {
                onBackPressed()
            }
        }

        rvLightData.let {
            it.adapter=adapter
            it.layoutManager =linearLayoutManager
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBluetoothService!=null){
            unbindService(mServiceConnection)
            mBluetoothService!!.disConnect()
            mBluetoothService = null
        }

        EventBus.getDefault().unregister(this)
    }

    //EventBus事件接收
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(msg: NotifyMessage){
        when(msg.code){
            ACTION_CONNECTED -> {//连接成功
                mBluetoothService!!.isGettingDeviceData(true)
            }

            ACTION_DISCONNECTED->{//设备连接断开
                Toast.makeText(this,"连接已断开!",Toast.LENGTH_SHORT).show()
            }

            ACTION_CONNECT_FAILED->{//设备连接失败
                Toast.makeText(this,"设备连接失败!",Toast.LENGTH_SHORT).show()
            }

            ACTION_READ_DATA_SUCCESS ->{//读取数据成功
                val data = msg.getData<String>()

                lightData.add(data)
                adapter.notifyDataSetChanged()
            }
            ACTION_READ_DATA_FAILED ->{
                Toast.makeText(this,"读取数据失败！", Toast.LENGTH_SHORT).show()
                Log.e(".AlarmActivity",msg.getData())
            }
        }
    }

    /**
     * Service连接回调
     */
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val myBinder = service as BluetoothService.MyBinder
            mBluetoothService = myBinder.getService(this@LinkTestActivity)

            if (deviceMac!=null){
                val remote = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceMac)

                mBluetoothService!!.bondDevice(remote)
                mBluetoothService!!.testDeviceConnection(remote)

            }

        }
    }

}
