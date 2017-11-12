package com.xshengh.newband.scanner;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.xshengh.newband.models.DeviceInfo;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by xshengh on 17/7/9.
 */

public class RecycleManager {
    private static RecycleManager sRecycleManger = null;
    private Context mContext;
    private BleScanManager mScanManager;
    private DeviceInfo mCurrentDevice;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private RecycleCallback mCallback;
    private ConcurrentLinkedQueue<DeviceInfo> mDevices = new ConcurrentLinkedQueue<>();
//    private LinkedList<DeviceInfo> mDevices = new LinkedList<>();
    private boolean isStopped = false;


    private RecycleManager(Context context) {
        mContext = context.getApplicationContext();
        mScanManager = BleScanManager.getInstance(mContext);
    }

    public synchronized static RecycleManager getInstance(Context context) {
        if (sRecycleManger == null) {
            sRecycleManger = new RecycleManager(context.getApplicationContext());
        }
        return sRecycleManger;
    }

    public void setCallback(RecycleCallback callback) {
        mCallback = callback;
    }

    public void startRecycle(LinkedList<DeviceInfo> devices) {
        isStopped = false;
        System.out.println("------ size : " + devices.size() + ", list : " + devices);
        if (mCallback != null) {
            mCallback.onStart();
        }
        mDevices.addAll(devices);
        connectNext();
    }

    public void stopRecycle() {
        if (mScanManager != null) {
            mScanManager.closeConnect();
            isStopped = true;
        }
    }

    private void connectNext() {
        if (!isStopped) {
            mScanManager.setScanCallback(null);
            mScanManager.closeConnect();
            mScanManager.setScanCallback(new BleScanManager.Callback() {
                @Override
                public void onStartScan() {
                    System.out.println("----- onStartScan");
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCallback != null) {
                                mCallback.onStartConnect(mCurrentDevice);
                            }
                        }
                    });
                }

                @Override
                public void onScanning(ScanResult result) {
                    System.out.println("----- onScaning : " + result);
                }

                @Override
                public void onScanComplete() {
                    System.out.println("----- onScanComplete");
                }

                @Override
                public void onConnecting() {
                    System.out.println("----- onConnecting");
                }

                @Override
                public void onConnectFail() {
                    System.out.println("----- onConnectFail");
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCurrentDevice != null) {
                                if (mCallback != null) {
                                    mCallback.onRecycleFailed(mCurrentDevice);
                                }
                                connectNext();
                            }
                        }
                    });

                }

                @Override
                public void onConnectSuccess() {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCallback != null) {
                                mCallback.onConnectSuccess(mCurrentDevice);
                            }
                        }
                    });
                }

                @Override
                public void onDisConnected() {
                    System.out.println("----- onDisConnected");
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            connectNext();
                        }
                    }, 1500);
                }

                @Override
                public void onServicesDiscovered() {
                    System.out.println("----- onServicesDiscovered");
                    post(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onRecycling(mCurrentDevice);
                            measureHeartRate();
                        }
                    });
                }
            });
            if (!mDevices.isEmpty()) {
                mCurrentDevice = mDevices.poll();
                mScanManager.scanAndConnectByMac(mCurrentDevice.getMac(), 10 * 1000);
            } else {
                mCallback.onComplete();
            }
        }
    }

    private void measureHeartRate() {
        mScanManager.setNotifyOn(new BleScanManager.Callback3() {
            boolean rated = false;
            boolean stepd = false;

            @Override
            public void onStart() {
                System.out.println("----- setNotify OnStart");
                rated = false;
                stepd = false;
            }

            @Override
            public void onStartFetchRate() {
                rated = true;
            }

            @Override
            public void onStartFetchStep() {
                stepd = true;
            }

            @Override
            public boolean isStepFetching() {
                return stepd;
            }

            @Override
            public void onRateDataReceived(final byte[] data) {
                rated = true;
                if (mCallback != null) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onFetchRate(mCurrentDevice, data);
                        }
                    });
                }
            }

            @Override
            public void onStepDataReceived(final byte[] data1, final byte[] data2) {
                stepd = true;
                if (mCallback != null) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onFetchStepAndCal(mCurrentDevice, data1, data2);
                        }
                    });
                }
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onRecycleFinish(mCurrentDevice);
                        }
                        mScanManager.stopNotify();
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mScanManager.closeConnect();
                            }
                        }, 1000);
                    }
                });
            }

            @Override
            public void onReceiveFail(BleException exception) {
                mCallback.onRecycleFailed(mCurrentDevice);
                mScanManager.closeConnect();
            }

            @Override
            public void onAlarmSetup(int hour, int minute) {
                mCallback.onSetAlarm(mCurrentDevice);
            }

            @Override
            public void onAlarmCancel() {
            }
        });

        post(new Runnable() {
            @Override
            public void run() {
                mScanManager.notifyBandData(mCurrentDevice.getAlarm());
            }
        });
    }

    private void post(Runnable runnable) {
        mMainHandler.postDelayed(runnable, 200);
    }

    private void postDelayed(Runnable runnable, long delayMills) {
        mMainHandler.postDelayed(runnable, delayMills);
    }

    public interface RecycleCallback {
        void onStart();

        void onStartConnect(DeviceInfo device);

        void onConnectSuccess(DeviceInfo device);

        void onRecycling(DeviceInfo device);

        void onRecycleFailed(DeviceInfo device);

        void onSetAlarm(DeviceInfo device);

        void onFetchRate(DeviceInfo device, byte[] rate);

        void onFetchStepAndCal(DeviceInfo device, byte[] step, byte[] cal);

        void onRecycleFinish(DeviceInfo device);

        void onComplete();
    }
}
