package com.xshengh.newband.scanner;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.xshengh.newband.models.DeviceInfo;
import com.xshengh.newband.scanner.Commander.CommandCallback;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by xshengh on 17/7/9.
 * 轮询单例类
 */

public class RecycleManager {
    private static RecycleManager sRecycleManger = null;
    private Context mContext;
    private BleManager mScanManager;
    private DeviceInfo mCurrentDevice;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private RecycleCallback mCallback;
    private ConcurrentLinkedQueue<DeviceInfo> mDevices = new ConcurrentLinkedQueue<>();
    private boolean isStopped = false;
    private Commander mCommander;
    private MessageSender mMessageSender;;
    private Runnable mRateTimeout = new Runnable() {
        @Override
        public void run() {
            System.out.println("------- rate timeout");
            closeConnect();
        }
    };
    private Runnable mStepTimeout = new Runnable() {
        @Override
        public void run() {
            System.out.println("---- step time out");
            sendMessage();
//            fetchHeartRate();
        }
    };


    private RecycleManager(Context context) {
        mContext = context.getApplicationContext();
        mScanManager = new BleManager(mContext);
        mCommander = new Commander(mScanManager);
        mMessageSender = new MessageSender(mCommander);
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
        System.out.println("-------stopRecycle------");
        if (mScanManager != null) {
            closeConnect();
            isStopped = true;
        }
    }

    private void connectNext() {
        if (!isStopped) {
            if (!mDevices.isEmpty()) {
                mCurrentDevice = mDevices.poll();
                System.out.println("----- onStartScan");
                if (mCallback != null) {
                    mCallback.onStartConnect(mCurrentDevice);
                }
                mScanManager.scanMacAndConnect(mCurrentDevice.getMac(), 10 * 1000, false, new BleGattCallback() {

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {
                    }

                    @Override
                    public void onConnecting(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onConnectError(BleException exception) {
                        System.out.println("----- onConnectFail");
                        if (mCurrentDevice != null) {
                            if (mCallback != null) {
                                mCallback.onRecycleFailed(mCurrentDevice);
                            }
                            connectNext();
                        }
                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        if (mCallback != null) {
                            mCallback.onConnectSuccess(mCurrentDevice);
                        }
                    }

                    @Override
                    public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {
                        System.out.println("----- onDisConnected");
                        connectNext();
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        System.out.println("----- onServicesDiscovered");
                        if (mCallback != null) {
                            mCallback.onRecycling(mCurrentDevice);
                        }
                        startWork();
                    }
                });
            } else if (mCallback != null) {
                mCallback.onComplete();
            }
        }
    }

    private void startWork() {
        post(new Runnable() {
            @Override
            public void run() {
                mCommander.init(new CommandCallbackAdapter() {
                    @Override
                    public void onSuccess(byte[]... b) {
                        correctTime();
                    }
                });
            }
        });
    }

    private void correctTime() {
        post(new Runnable() {
            @Override
            public void run() {
                mCommander.correctTime(new CommandCallbackAdapter() {
                    @Override
                    public void onSuccess(byte[]... b) {
                        int onOff = mCurrentDevice.getExerciseOnOff();
                        if (onOff == 1) {
                            openExerciseMode();
                        } else if (onOff == 0) {
                            exitExerciseMode();
                        } else if (onOff == 2) {
                            if (mCurrentDevice.getAlarm() != null) {
                                setupAlarm();
                            } else {
                                fetchStepRecord();
                            }
                        } else {
                            closeConnect();
                        }
                    }
                });
            }
        });

    }

    private void setupAlarm() {
        post(new Runnable() {
            @Override
            public void run() {
                byte[] b = mCurrentDevice.getAlarm();
                boolean open = b[0] == 1;
                int minute = -1;
                int hour = -1;
                if (open) {
                    minute = b[1];
                    hour = b[2];
                }
                System.out.println("-------- Alarm open : " + open + ", hour : " + hour + ", minute : " + minute);
                mCommander.setupAlarm(b, new CommandCallbackAdapter() {
                    @Override
                    public void onSuccess(byte[]... b) {
                        if (mCallback != null) {
                            mCallback.onSetAlarm(mCurrentDevice);
                        }
                        fetchStepRecord();
                    }
                });
            }
        });
    }

    private void openExerciseMode() {
        post(new Runnable() {
            @Override
            public void run() {
                mCommander.openExerciseMode(new CommandCallbackAdapter() {
                    @Override
                    public void onSuccess(byte[]... b) {
                        closeConnect();
                    }
                });
            }
        });
    }

    private void exitExerciseMode() {
        post(new Runnable() {
            @Override
            public void run() {
                mCommander.exitExerciseMode(new CommandCallbackAdapter() {
                    @Override
                    public void onSuccess(byte[]... b) {
                        closeConnect();
                    }
                });
            }
        });
    }

    private void fetchHeartRate() {
        post(new Runnable() {
            @Override
            public void run() {
                mCommander.fetchHeartRate(new CommandCallbackAdapter() {
                    @Override
                    public void onSuccess(byte[]... b) {
                        mMainHandler.removeCallbacks(mRateTimeout);
                        if (mCallback != null && b != null && b.length > 0) {
                            mCallback.onFetchRate(mCurrentDevice, b[0]);
                            mCallback.onRecycleFinish(mCurrentDevice);
                        }
//                        closeConnect();
                        sendMessage();
                    }
                });
            }
        });
        postDelayed(mRateTimeout, 5000);
    }

    private void sendMessage() {
        post(new Runnable() {
            @Override
            public void run() {
                mMessageSender.sendMessage("hello world\0".getBytes(), new CommandCallbackAdapter() {
                    @Override
                    public void onSuccess(byte[]... b) {
                    }
                });
            }
        });
    }
    private void fetchStepRecord() {
        post(new Runnable() {
            @Override
            public void run() {
                mCommander.fetchStepRecord(new CommandCallbackAdapter() {
                    @Override
                    public void onSuccess(byte[]... b) {
                        mMainHandler.removeCallbacks(mStepTimeout);
                        if (mCallback != null && b != null && b.length > 1) {
                            mCallback.onFetchStepAndCal(mCurrentDevice, b[0], b[1]);
                        }
                        sendMessage();
//                        fetchHeartRate();
                    }
                });
            }
        });
        postDelayed(mStepTimeout, 2000);
    }

    private void closeConnect() {
        post(new Runnable() {
            @Override
            public void run() {
                mCommander.closeConnect();
            }
        });
    }

    private void post(Runnable runnable) {
        mMainHandler.postDelayed(runnable, 500);
    }

    private void postDelayed(Runnable runnable, long delay) {
        mMainHandler.postDelayed(runnable, 500 + delay);
    }

    private abstract class CommandCallbackAdapter implements CommandCallback {
        @Override
        public void onFailure(BleException exception) {
            System.out.println("------- onFailure exception : " + exception);
            if (mCallback != null) {
                mCallback.onRecycleFailed(mCurrentDevice);
            }
            closeConnect();
        }
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
