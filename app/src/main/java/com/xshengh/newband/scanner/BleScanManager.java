package com.xshengh.newband.scanner;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;
import com.xshengh.newband.utils.Constants;

import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.Executors;

public class BleScanManager {

    private static final int SCAN_TIMEOUT = 7000;
    private BleManager bleManager;
    private static BleScanManager sBleScanManager;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Callback mCallback = null;
    private Callback2 mCallback2 = null;

    private Callback3 mCallback3;
    private String mCurrentWriteCmd;
    private Runnable mRateTimeout = new Runnable() {
        @Override
        public void run() {
            System.out.println("------- rate timeout");
            fetchStepRecord();
        }
    };
    private Runnable mStepTimeout = new Runnable() {
        @Override
        public void run() {
            System.out.println("---- step time out");
            closeConnect();
        }
    };

    public BleScanManager(Context context) {
        bleManager = new BleManager(context.getApplicationContext());
        if (!bleManager.isBlueEnable()) {
            bleManager.enableBluetooth();
        }
    }

    public static synchronized BleScanManager getInstance(Context context) {
        if (sBleScanManager == null) {
            sBleScanManager = new BleScanManager(context.getApplicationContext());
        }
        return sBleScanManager;
    }

    public void stop() {
        bleManager.closeBluetoothGatt();
        bleManager = null;
        mCallback = null;
        mCallback2 = null;
    }

    public void setScanCallback(Callback callback) {
        mCallback = callback;
    }

    public void setCollectDataCallback(final Callback3 callback) {
        mCallback3 = callback;
    }

    private void writeBTDevice(String command, BleCharacterCallback callback) {
        mCurrentWriteCmd = command;
        write(Constants.UUID_SERVICE, Constants.UUID_WRITE, command, callback);
    }

    private void notifyBTDevice(BleCharacterCallback callback) {
        notify(Constants.UUID_SERVICE, Constants.UUID_READ_NOTIFY, callback);
    }

    public void notifyBandData(final int exerciseOnoff, final byte[] alarmCommand) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (mCallback3 != null) {
                    mCallback3.onStart();
                }
            }
        });
        notifyBTDevice(new BleCharacterCallback() {
            @Override
            public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (characteristic != null) {
                            final byte[] res = characteristic.getValue();
                            System.out.println("return value : " + HexUtil.encodeHexStr(res) + ", length :" + res.length);
                            if (res.length >= 1) {
                                String prefix = HexUtil.extractData(res, 0);
                                System.out.println("------- cmd : " + mCurrentWriteCmd + ", prefix : " + prefix);
                                if (Constants.COMMAND_ACK2.equalsIgnoreCase(prefix)) {
                                    if (Constants.COMMAND_EXERCISE_MODE_ON.equalsIgnoreCase(mCurrentWriteCmd) || Constants.COMMAND_EXERCISE_MODE_OFF.equalsIgnoreCase(mCurrentWriteCmd)) {
                                        closeConnect();
                                    }
                                } else if (Constants.COMMAND_ACK1.equalsIgnoreCase(prefix)) {
                                    if (mCurrentWriteCmd != null) {
                                        if (mCurrentWriteCmd.startsWith(Constants.COMMAND_PREFIX_SET_TIME)) {
                                            if (exerciseOnoff == 1) {
                                                openExerciseMode();
                                            } else if (exerciseOnoff == 0) {
                                                exitExerciseMode();
                                            } else if (exerciseOnoff == 2) {
                                                if (alarmCommand != null) {
                                                    setAlarm(alarmCommand);
                                                } else {
                                                    fetchHeartRate();
                                                }
                                            } else {
                                                closeConnect();
                                            }
                                        } else if (mCurrentWriteCmd.startsWith(Constants.COMMAND_PREFIX_ALARM)) {
                                            if (mCallback3 != null) {
                                                boolean open = alarmCommand[0] == 1;
                                                if (open) {
                                                    int minute = alarmCommand[1];
                                                    int hour = alarmCommand[2];
                                                    mCallback3.onAlarmSetup(hour, minute);
                                                } else {
                                                    mCallback3.onAlarmCancel();
                                                }
                                            }
                                            fetchHeartRate();
                                        }
                                    }
                                } else if (Constants.RETURN_RATE_PREFIX.equalsIgnoreCase(prefix) && res.length == 16) {
                                    if (mCallback3 != null && !mCallback3.isRateFetched()) {
                                        if (!mCallback3.isStepFetched()) {
                                            mMainHandler.removeCallbacks(mRateTimeout);
                                        }
                                        mCallback3.onRateDataReceived(Arrays.copyOfRange(res, 13, 14));
                                        fetchStepRecord();
                                    }
                                } else if (Constants.COMMAND_RECEIVE_STEP_PEEFIX.equalsIgnoreCase(prefix) && res.length == 19) {
                                    if (mCallback3 != null) {
                                        mMainHandler.removeCallbacks(mStepTimeout);
                                        mCallback3.onStepDataReceived(Arrays.copyOfRange(res, 7, 11), Arrays.copyOfRange(res, 11, 13));
                                        System.out.println("-------onStepDataReceived-------");
                                        closeConnect();
                                    }
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onFailure(BleException exception) {
                System.out.println("----- notifyBTDevice exception : " + exception);
                notifyInitError(exception);
            }

            @Override
            public void onInitiatedResult(boolean result) {
                System.out.println("----- init result : " + result);
                if (result) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            setTime();
                        }
                    });
                } else {
                    notifyInitError(null);
                }
            }
        });
    }

    private void notifyInitError(BleException e) {
        if (mCallback3 != null) {
            mCallback3.onReceiveFail(e);
            System.out.println("----- init failed");
        }
    }
    private byte[] getCurrentTimeByteArr() {
        Calendar cal = Calendar.getInstance();
        int sec = cal.get(Calendar.SECOND);
        int min = cal.get(Calendar.MINUTE);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int date = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        System.out.println("sec : " + sec + ", min : " + min + ", hour : " + hour + ", date : " + date + ", month : " + month + ", year : " + year);
        byte[] time = new byte[6];
        time[0] = (byte) sec;
        time[1] = (byte) min;
        time[2] = (byte) hour;
        time[3] = (byte) date;
        time[4] = (byte) month;
        time[5] = (byte) year;
        return time;
    }

    private void setTime() {
        writeBTDevice(Constants.COMMAND_PREFIX_SET_TIME + HexUtil.encodeHexStr(getCurrentTimeByteArr()), new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                System.out.println("------ settime success");
            }

            @Override
            public void onFailure(BleException exception) {
                System.out.println("----- settime exception : " + exception);
                notifyInitError(exception);
            }

            @Override
            public void onInitiatedResult(boolean result) {
                System.out.println("----- settime result : " + result);
                if (!result) {
                    notifyInitError(null);
                }
            }
        });
    }

    private void openExerciseMode() {
        writeBTDevice(Constants.COMMAND_EXERCISE_MODE_ON, new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                System.out.println("------ write exercise mode on success");
            }

            @Override
            public void onFailure(BleException exception) {
                System.out.println("----- openExerciseMode write exception : " + exception);
                notifyInitError(exception);
            }

            @Override
            public void onInitiatedResult(boolean result) {
                System.out.println("----- openExerciseMode init result : " + result);
                if(!result) {
                    notifyInitError(null);
                }
            }
        });
    }

    private void exitExerciseMode() {
        writeBTDevice(Constants.COMMAND_EXERCISE_MODE_OFF, new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                System.out.println("------ write exercise mode off success");
            }

            @Override
            public void onFailure(BleException exception) {
                System.out.println("----- exitExerciseMode write exception : " + exception);
                notifyInitError(exception);
            }

            @Override
            public void onInitiatedResult(boolean result) {
                System.out.println("----- exitExerciseMode init result : " + result);
                if(!result) {
                    notifyInitError(null);
                }
            }
        });
    }

    private void fetchHeartRate() {
        if (mCallback3 != null) {
            mCallback3.onStartFetchRate();
        }
        writeBTDevice(Constants.COMMAND_MANUAL_HR, new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                System.out.println("------ write manual heart rate success");
            }

            @Override
            public void onFailure(BleException exception) {
                System.out.println("----- fetchHeartRate exception : " + exception);
                notifyInitError(exception);
            }

            @Override
            public void onInitiatedResult(boolean result) {
                if(!result) {
                    notifyInitError(null);
                }
            }
        });
        System.out.println("------ start time : " + System.currentTimeMillis());
        postDelayed(mRateTimeout, 5000);
    }

    private void fetchStepRecord() {
        if (mCallback3 != null) {
            mCallback3.onStartFetchStep();
        }
        writeBTDevice(Constants.COMMAND_SEND_STEP, new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                System.out.println("------ write step command success");
            }

            @Override
            public void onFailure(BleException exception) {
                System.out.println("----- fetchStepRecord exception : " + exception);
                notifyInitError(exception);
            }

            @Override
            public void onInitiatedResult(boolean result) {
                if(!result) {
                    notifyInitError(null);
                }
            }
        });
        postDelayed(mStepTimeout, 2000);
    }

    private void setAlarm(final byte[] command) {
        String c = Constants.COMMAND_PREFIX_ALARM + HexUtil.encodeHexStr(command) + Constants.COMMAND_POSTFIX_ALARM;
        System.out.println("-------- alarm : " + c);
        writeBTDevice(c, new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                System.out.println("------- set alarm success");
            }

            @Override
            public void onFailure(BleException exception) {
                System.out.println("----- setAlarm exception : " + exception);
                notifyInitError(exception);
            }

            @Override
            public void onInitiatedResult(boolean result) {
                System.out.println("----- setAlarm init result : " + result);
                if(!result) {
                    notifyInitError(null);
                }
            }
        });
    }

    public void setAlarm(final int hour, final int minute) {
        writeBTDevice(createAlarmCommand(hour, minute), new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                if (mCallback3 != null) {
                    mCallback3.onAlarmSetup(hour, minute);
                }
                System.out.println("------- set alarm success");
            }

            @Override
            public void onFailure(BleException exception) {
                System.out.println("----- setAlarm exception : " + exception);
                notifyInitError(exception);
            }

            @Override
            public void onInitiatedResult(boolean result) {
                if(!result) {
                    notifyInitError(null);
                }
            }
        });
    }

    public void cancelAlarm() {
        if (mCallback3 != null) {
            mCallback3.onStart();
        }
        writeBTDevice(Constants.COMMAND_DISABLE_ALARM, new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                if (mCallback3 != null) {
                    mCallback3.onAlarmCancel();
                }
                System.out.println("------- cancel alarm success");
            }

            @Override
            public void onFailure(BleException exception) {
                System.out.println("----- cancelAlarm exception : " + exception);
                notifyInitError(exception);
            }

            @Override
            public void onInitiatedResult(boolean result) {
                if(!result) {
                    notifyInitError(null);
                }
            }
        });
    }

    private String createAlarmCommand(int hour, int minute) {
        String hourHex = String.format("%02X", hour);
        String minuteHex = String.format("%02X", minute);
        System.out.println("-------- hour hex : " + hourHex);
        System.out.println("-------- minute hex : " + minuteHex);
        String command = Constants.COMMAND_PREFIX_ENABLE_ALARM + minuteHex + hourHex + Constants.COMMAND_POSTFIX_ALARM;
        System.out.println("-------- command " + command);
        return command;
    }

    public void setConnectCallback(Callback2 callback) {
        mCallback2 = callback;
    }

    public interface Callback {

        void onStartScan();

        void onScanning(ScanResult scanResult);

        void onScanComplete();

        void onConnecting();

        void onConnectFail();

        void onConnectSuccess();

        void onDisConnected();

        void onServicesDiscovered();
    }

    public interface Callback2 {
        void onDisConnected();
    }

    public interface Callback3 {
        void onStart();

        void onStartFetchRate();

        void onStartFetchStep();

        boolean isRateFetched();

        boolean isStepFetched();

        void onRateDataReceived(byte[] rate);

        void onStepDataReceived(byte[] steps, byte[] cal);

        void onReceiveFail(BleException exception);

        void onAlarmSetup(int hour, int minute);

        void onAlarmCancel();
    }

    public void scanDevice() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onStartScan();
                }

                boolean b = bleManager.scanDevice(new ListScanCallback(SCAN_TIMEOUT) {

                    @Override
                    public void onScanning(final ScanResult result) {
                        runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mCallback != null) {
                                    mCallback.onScanning(result);
                                }
                            }
                        });
                    }

                    @Override
                    public void onScanComplete(final ScanResult[] results) {
                        runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mCallback != null) {
                                    mCallback.onScanComplete();
                                }
                            }
                        });
                    }
                });
                if (!b) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mCallback != null) {
                                mCallback.onScanComplete();
                            }
                        }
                    });
                }
            }
        });
    }

    public void cancelScan() {
        bleManager.cancelScan();
    }

    public void connectDevice(final ScanResult scanResult) {
        if (mCallback != null) {
            mCallback.onConnecting();
        }

        bleManager.connectDevice(scanResult, false, new BleGattCallback() {
            @Override
            public void onFoundDevice(ScanResult scanResult) {
            }

            public void onConnecting(BluetoothGatt gatt, int status) {
            }

            @Override
            public void onConnectError(BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectFail();
                        }
                    }
                });
            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
            }

            @Override
            public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected();
                        }
                        if (mCallback2 != null) {
                            mCallback2.onDisConnected();
                        }
                    }
                });
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onServicesDiscovered();
                        }
                    }
                });
            }
        });
    }

    public void scanAndConnectByMac(String mac) {
        scanAndConnectByMac(mac, 5000);
    }

    public void scanAndConnectByMac(String mac, long timeOut) {
        if (mCallback != null) {
            mCallback.onStartScan();
        }

        bleManager.scanMacAndConnect(mac, timeOut, false, new BleGattCallback() {

            @Override
            public void onFoundDevice(ScanResult scanResult) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onScanComplete();
                        }
                    }
                });
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnecting();
                        }
                    }
                });
            }

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectFail();
                        }
                    }
                });
            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectSuccess();
                        }
                    }
                });
            }

            @Override
            public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected();
                        }
                        if (mCallback2 != null) {
                            mCallback2.onDisConnected();
                        }
                    }
                });
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onServicesDiscovered();
                        }
                    }
                });
            }

        });
    }

    public void read(String uuid_service, String uuid_read, BleCharacterCallback callback) {
        bleManager.readDevice(uuid_service, uuid_read, callback);
    }

    public void write(String uuid_service, String uuid_write, String hex, BleCharacterCallback callback) {
        bleManager.writeDevice(uuid_service, uuid_write, HexUtil.hexStringToBytes(hex), callback);
    }

    public void notify(String uuid_service, String uuid_notify, BleCharacterCallback callback) {
        bleManager.notify(uuid_service, uuid_notify, callback);
    }

    public void indicate(String uuid_service, String uuid_indicate, BleCharacterCallback callback) {
        bleManager.indicate(uuid_service, uuid_indicate, callback);
    }

    public void stopNotify(String uuid_service, String uuid_notify) {
        bleManager.stopNotify(uuid_service, uuid_notify);
    }

    public void stopIndicate(String uuid_service, String uuid_indicate) {
        bleManager.stopIndicate(uuid_service, uuid_indicate);
    }

//    public void closeConnect() {
//        bleManager.closeBluetoothGatt();
//    }

    public void closeConnect() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                System.out.println("-------- close connect ------");
                writeBTDevice(Constants.COMMAND_DISCONNECT_BLE, new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        System.out.println("------- write disconncet ble command success");
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        System.out.println("------- write disconncet ble command fail : " + exception);
                    }

                    @Override
                    public void onInitiatedResult(boolean result) {
                    }
                });
            }
        }, 2000);
    }

    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mMainHandler.post(runnable);
        }
    }

    private void post(Runnable runnable) {
        mMainHandler.postDelayed(runnable, 500);
    }

    private void postDelayed(Runnable runnable, long delay) {
        mMainHandler.postDelayed(runnable, delay);
    }
}
