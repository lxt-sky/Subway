package com.sanmen.bluesky.subway.helper;

import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.fragment.app.Fragment;
import com.sanmen.bluesky.subway.utils.PermissionUtil;

/**
 * @author lxt_bluesky
 * @date 2018/10/30
 * @description
 */
public class PermissionHelper {

    private Activity activity;

    private PermissionInterface permissionInterface;

    private Fragment fragment;

    public PermissionHelper(PermissionInterface permissionInterface, Activity activity) {
        this.permissionInterface = permissionInterface;
        this.activity = activity;
    }

    public PermissionHelper(PermissionInterface permissionInterface, Fragment fragment) {
        this.permissionInterface = permissionInterface;
        this.fragment=fragment;
        this.activity = fragment.getActivity();
    }

    /**
     * 请求权限
     */
    public void requestPermissions(){
        String[] deniedPermissions  = PermissionUtil.getDeniedPermissions(activity,permissionInterface.getPermissions());
        if (deniedPermissions!=null&&deniedPermissions.length>0){
            if (fragment!=null){
                PermissionUtil.requestPermissions(fragment,deniedPermissions,permissionInterface.getPermissionRequestCode());
            }else {
                PermissionUtil.requestPermissions(activity,deniedPermissions,permissionInterface.getPermissionRequestCode());
            }

        }else {
            permissionInterface.requestPermissionSuccess();
        }

    }

    public boolean requestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        if (requestCode==permissionInterface.getPermissionRequestCode()){
            boolean isAllGranted = true;
            for (int result:grantResults){
                if (result == PackageManager.PERMISSION_DENIED){
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted){
                //已全部授权
                permissionInterface.requestPermissionSuccess();
            }else {
                //权限有缺失
                permissionInterface.requestPermissionFail();
            }
            return true;
        }

        return true;
    }
}
