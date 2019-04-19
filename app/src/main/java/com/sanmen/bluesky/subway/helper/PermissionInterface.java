package com.sanmen.bluesky.subway.helper;

/**
 * @author lxt_bluesky
 * @date 2018/10/30
 * @description
 */
public interface PermissionInterface {
    /**
     * 权限请求码
     * @return
     */
    int getPermissionRequestCode();

    /**
     * 需要请求的权限
     * @return
     */
    String[] getPermissions();

    /**
     * 请求权限成功回调
     */
    void requestPermissionSuccess();

    /**
     * 请求权限失败回调
     */
    void requestPermissionFail();
}
