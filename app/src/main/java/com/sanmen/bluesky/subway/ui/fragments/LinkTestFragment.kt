package com.sanmen.bluesky.subway.ui.fragments

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.inuker.bluetooth.library.utils.BluetoothUtils.registerReceiver
import com.inuker.bluetooth.library.utils.BluetoothUtils.unregisterReceiver
import com.sanmen.bluesky.subway.Constant
import com.sanmen.bluesky.subway.Constant.MAC_ADDRESS

import com.sanmen.bluesky.subway.R
import com.sanmen.bluesky.subway.adapters.DeviceAdapter
import com.sanmen.bluesky.subway.data.bean.NotifyMessage
import com.sanmen.bluesky.subway.ui.activities.ConnectActivity
import kotlinx.android.synthetic.main.fragment_link_test.*
import org.greenrobot.eventbus.EventBus

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LinkTestFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LinkTestFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class LinkTestFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: SettingFragment.OnFragmentInteractionListener? = null

    private var deviceList = mutableListOf<BluetoothDevice>()

    private val rvAdapter :DeviceAdapter by lazy {
        DeviceAdapter(deviceList)
    }

    private val linearLayoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(context)
    }

    private val mBluetoothAdapter: BluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        IntentFilter().apply {
            this.addAction(BluetoothDevice.ACTION_FOUND)
            this.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            registerReceiver(mBroadcastReceiver,this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_link_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSearch.let {
            it.setOnClickListener {
                //检测是否支持蓝牙
                //检测蓝牙是否打开
                //执行蓝牙搜索
                doDiscovery()
            }
        }

        rvDeviceList.let {
            it.adapter = rvAdapter
            it.layoutManager = linearLayoutManager

        }

        rvAdapter.let {
            it.onItemClickListener = itemClickListener
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SettingFragment.OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null

    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterReceiver(mBroadcastReceiver)
    }

    private val itemClickListener = BaseQuickAdapter.OnItemClickListener{ _, _, position ->

        mBluetoothAdapter.cancelDiscovery()
//        Toast.makeText(context,"this is item $position",Toast.LENGTH_SHORT).show()

        NavHostFragment.findNavController(this@LinkTestFragment)
            .navigate(R.id.action_linkTestFragment_to_linkTestActivity, Bundle().apply { putString(MAC_ADDRESS,deviceList[position].address) })
    }


    private val mBroadcastReceiver =  object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action) {
                BluetoothDevice.ACTION_FOUND -> {//发现设备

                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    if (device != null) {
                        addDevice2List(device)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {//搜索结束
                    Toast.makeText(context,"搜索结束!",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 搜索蓝牙设备
     */
    private fun doDiscovery() {

        if (!mBluetoothAdapter.isEnabled){
            turnBluetooth()
            return
        }
        if (mBluetoothAdapter.isDiscovering) {
            mBluetoothAdapter.cancelDiscovery()
        }
        deviceList.clear()
        //开始搜索
        mBluetoothAdapter.startDiscovery()

    }

    /**
     *打开蓝牙
     */
    private fun turnBluetooth() {

        //请求打开蓝牙
        val requestBluetoothOn  = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestBluetoothOn.run {
            startActivityForResult(this, ConnectActivity.REQUEST_CODE)
        }
    }

    /**
     * 添加设备至列表
     */
    private fun addDevice2List(device: BluetoothDevice) {

        deviceList.add(device)
        rvAdapter.notifyDataSetChanged()

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LinkTestFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LinkTestFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}


