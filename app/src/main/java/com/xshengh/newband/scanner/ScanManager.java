package com.xshengh.newband.scanner;

import android.content.Context;
import android.support.annotation.NonNull;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.scan.ListScanCallback;

import java.util.concurrent.Executors;

/**
 * Created by xshengh on 18/1/27.
 * 扫描单例类
 */

public class ScanManager {
    private static final int SCAN_TIMEOUT = 10000;
    private static ScanManager sScanManager;
    private ScanCallback mCallback;
    private BleManager mBleManager;

    private ScanManager(Context context) {
        mBleManager = new BleManager(context.getApplicationContext());
        if (!mBleManager.isBlueEnable()) {
            mBleManager.enableBluetooth();
        }
    }

    public void setScanCallback(ScanCallback callback) {
        mCallback = callback;
    }

    public synchronized static ScanManager getInstance(@NonNull Context context) {
        if (sScanManager == null) {
            sScanManager = new ScanManager(context.getApplicationContext());
        }
        return sScanManager;
    }

    public void scanDevice() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onStartScan();
                }
                boolean b = mBleManager.scanDevice(new ListScanCallback(SCAN_TIMEOUT) {
                    @Override
                    public void onScanning(final ScanResult result) {
                        if (mCallback != null) {
                            mCallback.onScanning(result);
                        }
                    }
                    @Override
                    public void onScanComplete(final ScanResult[] results) {
                        if (mCallback != null) {
                            mCallback.onScanComplete();
                        }
                    }
                });
                if (!b) {
                    if (mCallback != null) {
                        mCallback.onScanComplete();
                    }
                }
            }
        });
    }

    public void stop() {
        mCallback = null;
    }
    public interface ScanCallback {

        void onStartScan();

        void onScanning(ScanResult scanResult);

        void onScanComplete();

    }
}
