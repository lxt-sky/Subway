package com.sanmen.bluesky.subway.ui.fragments

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.sanmen.bluesky.subway.R

import com.sanmen.bluesky.subway.data.bean.AlarmInfo
import com.sanmen.bluesky.subway.data.bean.DriveRecord
import com.sanmen.bluesky.subway.data.database.DriveDatabase
import com.sanmen.bluesky.subway.data.repository.AlarmRepository
import com.sanmen.bluesky.subway.data.repository.DriveRepository
import com.sanmen.bluesky.subway.helper.ExcelHelper
import com.sanmen.bluesky.subway.utils.AppExecutors
import com.sanmen.bluesky.subway.utils.DialogUtil
import kotlinx.android.synthetic.main.fragment_setting.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
/**
 * 待申请的权限数组
 */
private val PERMISSIONS_STORAGE:Array<String> = arrayOf(
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE)
/**
 * 请求码
 */
private const val REQUEST_PERMISSION_CODE =1

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SettingFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SettingFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SettingFragment : Fragment() {

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    private val data: MutableList<Any> = mutableListOf<Any>()

    private val database: DriveDatabase by lazy {
        DriveDatabase.getInstance(this.context!!)
    }

    private val appExecutors:AppExecutors by lazy {
        AppExecutors()
    }

    private lateinit var driveRepository: DriveRepository

    private lateinit var alarmRepository: AlarmRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        driveRepository = DriveRepository.getInstance(database.getDriveDao(),appExecutors)
        alarmRepository = AlarmRepository.getInstance(database.getAlarmDao(),appExecutors)

        llDataOutput.setOnClickListener(onClickListener)
        llAbout.setOnClickListener(onClickListener)
        llLight.setOnClickListener (onClickListener)
        llDataClear.setOnClickListener(onClickListener)

    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
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

    private val onClickListener = View.OnClickListener {
        when(it.id){
            R.id.llAbout->{
                NavHostFragment.findNavController(this).navigate(R.id.action_settingFragment_to_aboutActivity)
            }
            R.id.llLight->{
                //传递数据
                val bundle = Bundle()
                bundle.putInt("lightIntensity",0)//光照强度
                bundle.putInt("lightThreshold",0)//光照临界值
                NavHostFragment.findNavController(this).navigate(R.id.action_settingFragment_to_lightFragment,bundle)
            }
            R.id.llDataClear->{
                DialogUtil.getConfirmDialog(
                    this.context!!,resources.getString(R.string.dialog_data_clear),
                    DialogInterface.OnClickListener { _, _ ->
                        appExecutors.diskIO().execute {
//                            val driveList = driveRepository.getDriveRecordList()
//                            val num = driveRepository.deleteAllDriveRecord(driveList)
                            database.clearAllTables()

                            appExecutors.mainTread().execute {
//                                if (num!=-1){
                                    Toast.makeText(activity,"清除数据成功！",Toast.LENGTH_SHORT).show()
//                                }
                            }
                        }
                    }).show()
            }
            R.id.llDataOutput->{

                //检查权限
                if (Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP){
                    if (ActivityCompat.checkSelfPermission(activity!!.applicationContext,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(activity!!, PERMISSIONS_STORAGE,REQUEST_PERMISSION_CODE)
                    }else{
                        //弹窗提示
                        DialogUtil.getConfirmDialog(
                            this.context!!,resources.getString(R.string.dialog_data_output),
                            DialogInterface.OnClickListener { _, _ ->
                                dataOutput()
                            }).show()

                    }
                }

            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode== REQUEST_PERMISSION_CODE){
            for (item in grantResults){
                if (item==-1){
                    Toast.makeText(context,"缺少相应权限！",Toast.LENGTH_SHORT).show()
                    return
                }
            }

        }
    }

    /**
     * 数据导出
     */
    private fun dataOutput() {
        val result = "数据导出Excel成功,请前往/subway/tmp下查看！"
        //从数据库读取数据
        appExecutors.diskIO().execute {
            val alarmList:List<AlarmInfo> = alarmRepository.getAllAlarmInfo()
            val recordList:List<DriveRecord> = driveRepository.getDriveRecordList()
            data.clear()
            data.addAll(alarmList)
            data.addAll(recordList)
            appExecutors.mainTread().execute {
                val path = ExcelHelper.createExcel(data)
                if (path!=null){
                    //导出成功与否则用Toast提示
                    Toast.makeText(activity,result,Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
