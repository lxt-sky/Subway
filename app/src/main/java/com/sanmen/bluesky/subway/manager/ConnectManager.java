package com.sanmen.bluesky.subway.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * @author lxt_bluesky
 * @date 2018/11/7
 * @description
 */
public class ConnectManager {

    private static final String TAG = ".ConnectManager";
    public static final int CONNECT_STATE_IDLE = 0;
    public static final int CONNECT_STATE_CONNECTING = 1;
    public static final int CONNECT_STATE_CONNECTED = 2;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private final BluetoothAdapter mBluetoothAdapter;

    private Context context;

    private ConnectedThread mConnectedThread;

    private ConnectionListener mConnectionListener;

    private int mConnectState = CONNECT_STATE_IDLE;


    public interface ConnectionListener {
        /**
         * 蓝牙连接状态改变
         * @param oldState 旧状态
         * @param state 新状态
         */
        public void onConnectStateChange(int oldState, int state);

        /**
         * 读取数据
         * @param data 数据
         */
        public void onReadData(byte[] data);
    }

    public ConnectManager(Context context, ConnectionListener listener) {
        mConnectionListener = listener;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.context = context;

    }

    /**
     * 建立连接
     * @param address
     */
    public synchronized void connect(String address){
        //在建立连接之前,将当前通信线程取消
        if(mConnectedThread != null) {
            mConnectedThread.cancel();
        }
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        try {
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            connected(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 建立连接成功,进行通信
     * @param socket
     */
    private synchronized void connected(BluetoothSocket socket) {

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    /**
     * 断开连接
     */
    public void disconnect() {

        Log.d(TAG, "ConnectionManager disconnect connection");

        if(mConnectedThread != null) {
            mConnectedThread.cancel();
        }
    }

    private void setConnectState(int state) {
        if (mConnectState==state){
            return;
        }
        int oldState = mConnectState;
        mConnectState = state;
        if (mConnectionListener!=null){
            mConnectionListener.onConnectStateChange(oldState,mConnectState);
        }
    }

    private class ConnectedThread extends Thread{

        private BluetoothSocket mSocket;
        private static final int MAX_BUFFER_SIZE = 1024;
        private boolean isConnected;
        private InputStream mInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mSocket = socket;
            this.isConnected =true;
        }

        @Override
        public void run() {
            super.run();
            setConnectState(CONNECT_STATE_CONNECTING);
            if (isConnected){
                try {
                    mSocket.connect();
                } catch (IOException e) {
                    //连接失败
                    setConnectState(CONNECT_STATE_IDLE);
                    mSocket = null;
                    mConnectedThread = null;
                    return;
                }
            }

            InputStream tmpIn=null;
            try {
                tmpIn = mSocket.getInputStream();
            } catch (IOException e) {
                //连接失败
                setConnectState(CONNECT_STATE_IDLE);
                mSocket = null;
                mConnectedThread = null;
                return;
            }
            mInStream = tmpIn;
            //连接成功
            setConnectState(CONNECT_STATE_CONNECTED);

            byte[] buffer = new byte[MAX_BUFFER_SIZE];
            int bytes;

            while (isConnected){
                try {
                    bytes = mInStream.read(buffer);
                    if(mConnectionListener != null && bytes > 0) {
                        byte [] data = new byte[bytes];
                        //值拷贝,浅拷贝,引用复制
                        System.arraycopy(buffer, 0, data, 0, bytes);
                        mConnectionListener.onReadData(data);
                    }
                } catch (IOException e) {
                    break;
                }
            }

            mSocket = null;
            mConnectedThread = null;
            setConnectState(CONNECT_STATE_IDLE);

            if(isConnected == false) {
                Log.d(TAG, "ConnectedThread END since user cancel.");
            }
            else {
                Log.d(TAG, "ConnectedThread END");
            }
        }

        public void cancel(){
            Log.d(TAG, "ConnectedThread cancel START");

            try {
                isConnected = false;
                if (mSocket!=null){
                    mSocket.close();
                }

            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread cancel failed", e);
            }
            Log.d(TAG, "ConnectedThread cancel END");

        }

    }


}
