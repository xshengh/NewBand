package com.xshengh.newband.recycle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * Created by xshengh on 17/4/2.
 */

public class BLEServiceManager {
    private final String TAG = "BLEServiceManager";
    private static BLEServiceManager sBLEServiceManager = null;

    private static final LinkedList<DeviceInfo> DEVICE_LIST = new LinkedList<>();
    private static final Hashtable<String, ArrayList<DeviceInfo>> DEVICE_RESULT = new Hashtable<>();
    private static final int SCAN_TIME = 5000;
    private static final int OVER_TIME = 60 * 1000;

    private Context mContext;
    private SharedPreferences mSettingPreference;
    private DeviceInfo mCurDevice;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private BleManager mBleManager;

    private BLEServiceManager(Context context) {
        mContext = context;
        initBleService(context);
    }

    private void initHandlerThread() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MessageCode.START_SCAN_DEVICE:
                        startScanDevices();
                        break;
                    case MessageCode.STOP_SCAN_DEVICE:
                        stopScanDevices();
                        break;
                    case MessageCode.CONNECT_DEVICE:
                        connect();
                        break;
                    case MessageCode.COLLECT_DATA:
                        uploadData();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void initBleService(Context context) {
        mBleManager = new BleManager(context);
    }

    public static synchronized BLEServiceManager getInstance(Context context) {
        if (sBLEServiceManager == null) {
            sBLEServiceManager = new BLEServiceManager(context);
        }
        return sBLEServiceManager;
    }

    private void startScanDevices() {
        mBleManager.enableBluetooth();
        mBleManager.scanDevice(new ListScanCallback(SCAN_TIME) {
            @Override
            public void onScanning(ScanResult result) {
            }

            @Override
            public void onScanComplete(ScanResult[] results) {
            }
        });
    }


    private void stopScanDevices() {
        mHandler.obtainMessage(MessageCode.CONNECT_DEVICE).sendToTarget();
    }

    private void connect() {
        if (DEVICE_LIST.isEmpty()) {
            mCurDevice = null;
            mHandler.obtainMessage(MessageCode.START_SCAN_DEVICE).sendToTarget();
        } else {
            mCurDevice = DEVICE_LIST.removeFirst();
            mBleManager.connectDevice(mCurDevice, true, new BleGattCallback() {
                @Override
                public void onFoundDevice(ScanResult scanResult) {

                }

                @Override
                public void onConnectError(BleException exception) {

                }

                @Override
                public void onConnectSuccess(BluetoothGatt gatt, int status) {
                    gatt.discoverServices();
                }

                @Override
                public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {

                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                }
            });
        }
    }

    public void startTask() {
        mHandler.obtainMessage(MessageCode.START_SCAN_DEVICE).sendToTarget();
        mHandler.sendEmptyMessageDelayed(MessageCode.COLLECT_DATA, OVER_TIME);
    }

    private void uploadData() {
        Hashtable<String, ArrayList<DeviceInfo>> deviceList = new Hashtable<>(DEVICE_RESULT);
        DEVICE_RESULT.clear();
        DataUploader.getInstance(mContext).writeToFile(deviceList, new DataUploader.UploaderListener() {
            @Override
            public void onUploadSuccess() {
                System.out.println("------upload success");
                mHandler.sendEmptyMessageDelayed(MessageCode.COLLECT_DATA, OVER_TIME);
            }

            @Override
            public void onUploadFail(Exception e) {
                System.out.println("------upload fail");
                mHandler.sendEmptyMessageDelayed(MessageCode.COLLECT_DATA, OVER_TIME);
                e.printStackTrace();
            }
        });
    }

    public void stopTask() {
        mHandlerThread.quit();
        mHandler.removeCallbacksAndMessages(null);
    }
}
