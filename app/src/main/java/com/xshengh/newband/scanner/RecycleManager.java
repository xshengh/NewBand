
package com.xshengh.newband.scanner;

import static com.xshengh.newband.utils.Constants.CODE_CORRECT_TIME;
import static com.xshengh.newband.utils.Constants.CODE_DISCONNECT;
import static com.xshengh.newband.utils.Constants.CODE_EXERCISE_OFF;
import static com.xshengh.newband.utils.Constants.CODE_EXERCISE_ON;
import static com.xshengh.newband.utils.Constants.CODE_FETCH_STATISTIC;
import static com.xshengh.newband.utils.Constants.CODE_INIT;
import static com.xshengh.newband.utils.Constants.CODE_SETUP_ALARM;

import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.xshengh.newband.models.DeviceInfo;
import com.xshengh.newband.scanner.Commander.CommandCallback;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by xshengh on 17/7/9. 轮询单例类
 */

public class RecycleManager {
    private static RecycleManager sRecycleManger = new RecycleManager();
    private BleManager mBleManager = BleManager.getInstance();
    private DeviceInfo mCurrentDevice;
    private RecycleCallback mCallback;
    private ConcurrentLinkedQueue<DeviceInfo> mDevices = new ConcurrentLinkedQueue<>();
    private volatile boolean isStopped = false;
    private Commander mCommander;
    private MessageSender mMessageSender;
    private Runnable mStatisticTimeout = new Runnable() {
        @Override
        public void run() {
            System.out.println("----Statistic time out");
            sendMessageDelay500(mMainHandler.obtainMessage(CODE_DISCONNECT));
        }
    };
    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CODE_INIT: {
                    if (msg.obj instanceof BleDevice) {
                        startWork((BleDevice) msg.obj);
                    }
                    break;
                }
                case CODE_CORRECT_TIME: {
                    correctTime();
                    break;
                }
                case CODE_EXERCISE_ON: {
                    openExerciseMode();
                    break;
                }
                case CODE_EXERCISE_OFF: {
                    exitExerciseMode();
                    break;
                }
                case CODE_SETUP_ALARM: {
                    setupAlarm();
                    break;
                }
                case CODE_FETCH_STATISTIC: {
                    fetchStatistic();
                    break;
                }
                case CODE_DISCONNECT: {
                    closeConnect();
                    break;
                }
                default:
                    break;
            }
        }
    };

    private RecycleManager() {
        mCommander = new Commander();
        mMessageSender = new MessageSender(mCommander);
    }

    public static RecycleManager getInstance() {
        return sRecycleManger;
    }

    public void setCallback(RecycleCallback callback) {
        mCallback = callback;
    }

    public void startRecycle(LinkedList<DeviceInfo> devices) {
        isStopped = false;
        System.out.println("----StartRecycle list : " + devices);
        if (mCallback != null) {
            mCallback.onStart();
        }
        mDevices.addAll(devices);
        connectNext();
    }

    public void stopRecycle() {
        System.out.println("----StopRecycle");
        closeConnect();
        isStopped = true;
    }

    public boolean isStopped() {
        return isStopped;
    }

    private void connectNext() {
        if (!isStopped) {
            if (!mDevices.isEmpty()) {
                mCurrentDevice = mDevices.poll();
                System.out.println("----StartConnect");
                if (mCallback != null) {
                    mCallback.onStartConnect(mCurrentDevice);
                }
                BleScanRuleConfig config = new BleScanRuleConfig.Builder().setScanTimeOut(7000)
                        .setDeviceMac(mCurrentDevice.getMac()).build();
                mBleManager.initScanRule(config);
                mBleManager.scanAndConnect(new BleScanAndConnectCallback() {
                    @Override
                    public void onScanFinished(BleDevice scanResult) {
                        if (scanResult == null) {
                            connectNext();
                        }
                    }

                    @Override
                    public void onStartConnect() {
                    }

                    @Override
                    public void onConnectFail(BleDevice bleDevice, BleException exception) {
                        System.out.println("----OnConnectFail : " + exception);
                        if (mCallback != null) {
                            mCallback.onRecycleFailed(mCurrentDevice);
                        }
                        connectNext();
                    }

                    @Override
                    public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt,
                            int status) {
                        if (mCallback != null) {
                            mCallback.onConnectSuccess(mCurrentDevice);
                        }
                        mCurrentDevice.setDevice(bleDevice);
                        sendMessageDelay500(mMainHandler.obtainMessage(CODE_INIT, bleDevice));
                    }

                    @Override
                    public void onDisConnected(boolean isActiveDisConnected, BleDevice device,
                            BluetoothGatt gatt, int status) {
                        System.out.println("----OnDisConnected");
                        connectNext();
                    }

                    @Override
                    public void onScanStarted(boolean success) {
                        if (!success) {
                            connectNext();
                        }
                    }

                    @Override
                    public void onScanning(BleDevice bleDevice) {
                    }
                });
            } else if (mCallback != null) {
                isStopped = true;
                mCallback.onComplete();
            }
        }
    }

    private void startWork(BleDevice bleDevice) {
        mCommander.init(bleDevice, new CommandCallbackAdapter() {
            @Override
            public void onSuccess(byte[]... b) {
                Message msg = mMainHandler.obtainMessage(CODE_CORRECT_TIME);
                sendMessageDelay500(msg);
            }
        });
    }

    private void correctTime() {
        mCommander.correctTime(new CommandCallbackAdapter() {
            @Override
            public void onSuccess(byte[]... b) {
                int onOff = mCurrentDevice.getExerciseOnOff();
                Message msg;
                if (onOff == 1) {
                    msg = mMainHandler.obtainMessage(CODE_EXERCISE_ON);
                } else if (onOff == 0) {
                    msg = mMainHandler.obtainMessage(CODE_EXERCISE_OFF);
                } else if (onOff == 2) {
                    if (mCurrentDevice.getAlarm() != null) {
                        msg = mMainHandler.obtainMessage(CODE_SETUP_ALARM);
                    } else {
                        msg = mMainHandler.obtainMessage(CODE_FETCH_STATISTIC);
                    }
                } else {
                    msg = mMainHandler.obtainMessage(CODE_DISCONNECT);
                }
                sendMessageDelay500(msg);
            }
        });
    }

    private void setupAlarm() {
        byte[] b = mCurrentDevice.getAlarm();
        boolean open = b[0] == 1;
        int minute = -1;
        int hour = -1;
        if (open) {
            minute = b[1];
            hour = b[2];
            mCommander.setupAlarm(b, new CommandCallbackAdapter() {
                @Override
                public void onSuccess(byte[]... b) {
                    if (mCallback != null) {
                        mCallback.onSetAlarm(mCurrentDevice);
                    }
                    sendMessageDelay500(mMainHandler.obtainMessage(CODE_FETCH_STATISTIC));
                }
            });
        } else {
            sendMessageDelay500(mMainHandler.obtainMessage(CODE_FETCH_STATISTIC));
        }
        System.out
                .println("----Alarm open : " + open + ", hour : " + hour + ", minute : " + minute);
    }

    private void openExerciseMode() {
        mCommander.openExerciseMode(new CommandCallbackAdapter() {
            @Override
            public void onSuccess(byte[]... b) {
                sendMessageDelay500(mMainHandler.obtainMessage(CODE_DISCONNECT));
            }
        });
    }

    private void exitExerciseMode() {
        mCommander.exitExerciseMode(new CommandCallbackAdapter() {
            @Override
            public void onSuccess(byte[]... b) {
                sendMessageDelay500(mMainHandler.obtainMessage(CODE_DISCONNECT));
            }
        });
    }

    private void sendMessage() {
        post(new Runnable() {
            @Override
            public void run() {
                mMessageSender.sendMessage("hello world\0".getBytes(),
                        new CommandCallbackAdapter() {
                            @Override
                            public void onSuccess(byte[]... b) {
                            }
                        });
            }
        });
    }

    private void fetchStatistic() {
        mCommander.fetchStatistic(new CommandCallbackAdapter() {
            @Override
            public void onSuccess(byte[]... b) {
                mMainHandler.removeCallbacks(mStatisticTimeout);
                if (mCallback != null && b != null && b.length > 2) {
                    mCallback.onFetchStatistic(mCurrentDevice, b[0], b[1], b[2]);
                    mCallback.onRecycleFinish(mCurrentDevice);
                }
                sendMessageDelay500(mMainHandler.obtainMessage(CODE_DISCONNECT));
            }
        });
        postDelayed(mStatisticTimeout, 7000);
    }

    private void closeConnect() {
        // mBleManager.disconnect(mCurrentDevice.getDevice());
        mCommander.closeConnect();
        mBleManager.clearCharacterCallback(mCurrentDevice.getDevice());
    }

    private void sendMessageDelay500(Message msg) {
        mMainHandler.sendMessageDelayed(msg, 500);
    }

    private void post(Runnable runnable) {
        mMainHandler.postDelayed(runnable, 500);
    }

    private void postDelayed(Runnable runnable, long delay) {
        mMainHandler.postDelayed(runnable, 500 + delay);
    }

    public interface RecycleCallback {
        void onStart();

        void onStartConnect(DeviceInfo device);

        void onConnectSuccess(DeviceInfo device);

        void onRecycleFailed(DeviceInfo device);

        void onSetAlarm(DeviceInfo device);

        void onFetchStatistic(DeviceInfo device, byte[] rate, byte[] step, byte[] cal);

        void onRecycleFinish(DeviceInfo device);

        void onComplete();
    }

    private abstract class CommandCallbackAdapter implements CommandCallback {
        @Override
        public void onFailure(BleException exception) {
            System.out.println("----OnFailure exception : " + exception);
            if (mCallback != null) {
                mCallback.onRecycleFailed(mCurrentDevice);
            }
            sendMessageDelay500(mMainHandler.obtainMessage(CODE_DISCONNECT));
        }
    }
}
