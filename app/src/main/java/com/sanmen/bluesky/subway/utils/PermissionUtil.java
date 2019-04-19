package com.sanmen.bluesky.subway.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

/**
 * @author lxt_bluesky
 * @date 2018/10/30
 * @description 动态权限工具类
 */
public class PermissionUtil {
    /**
     * 检查是否拥有指定权限
     * @param context
     * @param permission
     * @return
     */
    public static boolean hasPermission(Context context,String permission){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    public static void requestPermissions(Activity activity, String[] permissions,int requestCode){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(permissions,requestCode);
        }

    }

    public static void requestPermissions(Fragment fragment, String[] permissions, int requestCode){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fragment.requestPermissions(permissions,requestCode);
        }

    }

    public static String[] getDeniedPermissions(Context context,String[] permissions){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            ArrayList<String> deniedPermissionList = new ArrayList<>();
            for (String permission : permissions) {
                if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissionList.add(permission);
                }
            }
            int size = deniedPermissionList.size();
            if (size > 0) {
                return deniedPermissionList.toArray(new String[deniedPermissionList.size()]);
            }

        }

        return null;
    }
}
