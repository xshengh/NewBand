package com.xshengh.newband.scanner;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;
import com.xshengh.newband.utils.Constants;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by xshengh on 18/1/27.
 */

class Commander {

    private static final int WRITE_CODE = 1;
    private static final int NOTIFY_CODE = 2;
    private BleManager mBleManager;
    private CommandCallback mCurCallback;
    private String mCurCommand;
    private BleException mCurException;
    private BleDevice mDevice;
    private volatile boolean mFetchStatistic;
    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case NOTIFY_CODE:
                case WRITE_CODE:
                    if (msg.obj instanceof CommandCallback) {
                        ((CommandCallback) msg.obj).onFailure(mCurException);
                        mCurException = null;
                    }
                    break;
            }
        }
    };

    Commander() {
        this.mBleManager = BleManager.getInstance();
    }

    void init(BleDevice bleDevice, final CommandCallback callback) {
        mFetchStatistic = false;
        mDevice = bleDevice;
        mBleManager.notify(bleDevice, Constants.UUID_SERVICE, Constants.UUID_READ_NOTIFY, new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {
                System.out.println("----Init Success : onNotifySuccess");
                if (mMainHandler.hasMessages(NOTIFY_CODE)) {
                    System.out.println("!!!!!!!!!! FastBle lib returned wrong notify callback");
                    mMainHandler.removeMessages(NOTIFY_CODE);
                }
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onNotifyFailure(BleException exception) {
                System.out.println("----Init failed : onNotifyFailure :" + exception);
                Message msg = mMainHandler.obtainMessage(NOTIFY_CODE, callback);
                mMainHandler.sendMessageDelayed(msg, 1000);
                mCurException = exception;
                if (callback != null) {
                    callback.onFailure(exception);
                }
            }

            @Override
            public void onCharacteristicChanged(byte[] res) {
                if (mMainHandler.hasMessages(WRITE_CODE)) {
                    System.out.println("!!!!!!!!!! FastBle lib returned wrong write callback");
                    mMainHandler.removeMessages(WRITE_CODE);
                }
                System.out.println("----OnCharacteristicChanged : " + HexUtil.encodeHexStr(res) + ", length :" + res.length);
                if (res.length >= 1) {
                    String prefix = HexUtil.extractData(res, 0);
                    
                    if (Constants.COMMAND_ACK1.equalsIgnoreCase(prefix) && !Constants.COMMAND_MANUAL_HR.equals(mCurCommand) && !Constants.COMMAND_SEND_STEP.equals(mCurCommand)) {
                        System.out.println("----Command : " + mCurCommand + ", ack : " + prefix);
                        if (mCurCallback != null) {
                            mCurCallback.onSuccess();
                        }
                    } else if (Constants.COMMAND_ACK3.equalsIgnoreCase(prefix) || Constants.COMMAND_ACK2.equalsIgnoreCase(prefix)) {
                        System.out.println("----Command : " + mCurCommand + ", ack : " + prefix);
                        if (mCurCallback != null) {
                            mCurCallback.onSuccess();
                        }
                    } else if (mFetchStatistic && Constants.RETURN_RATE_PREFIX.equalsIgnoreCase(prefix) && res.length == 16) {
                        System.out.println("----Command : " + mCurCommand + ", ack : " + prefix);
                        if (mCurCallback != null) {
                            mCurCallback.onSuccess(Arrays.copyOfRange(res, 15, 16), Arrays.copyOfRange(res, 3, 7), Arrays.copyOfRange(res, 7, 9));
                        }
                    }
                }
            }
        });
    }

    void correctTime(CommandCallback commandCallback) {
        writeCommand(Constants.COMMAND_PREFIX_SET_TIME + HexUtil.encodeHexStr(getCurrentTimeByteArr()), commandCallback);
    }

    void openExerciseMode(CommandCallback commandCallback) {
        writeCommand(Constants.COMMAND_EXERCISE_MODE_ON, commandCallback);
    }

    void exitExerciseMode(CommandCallback commandCallback) {
        writeCommand(Constants.COMMAND_EXERCISE_MODE_OFF, commandCallback);
    }

    void fetchStatistic(CommandCallback commandCallback) {
        writeCommand((String) null, commandCallback, false);
        mFetchStatistic = true;
    }

    void closeConnect() {
        writeCommand(Constants.COMMAND_DISCONNECT_BLE, null);
    }

    void setupAlarm(byte[] b, CommandCallback commandCallback) {
        writeCommand(Constants.COMMAND_PREFIX_ALARM + HexUtil.encodeHexStr(b) + Constants.COMMAND_POSTFIX_ALARM, commandCallback);
    }

    void setMessageSender(byte[] sender, CommandCallback commandCallback) {
        mCurCommand = Constants.COMMAND_SENDER_PREFIX;
        byte[] prefix = HexUtil.hexStringToBytes(Constants.COMMAND_SENDER_PREFIX);
        int newLength = prefix.length + 1;
        int senderLength = 0;
        if (sender != null && sender.length > 0) {
            senderLength = Math.min(sender.length, 18);
            newLength += sender.length;
        }
        byte[] res = new byte[newLength];
        System.arraycopy(prefix, 0, res, 0, prefix.length);
        if (senderLength > 0) {
            System.arraycopy(sender, 0, res, prefix.length, senderLength);
        }
        res[res.length - 1] = '\0';
        writeCommand(res, commandCallback, true);
    }

    void showMessageContent(byte[] msg, int index, CommandCallback commandCallback) {
        mCurCommand = Constants.COMMAND_MESSAGE_CONTENT_PREFIX;
        byte[] prefix = HexUtil.hexStringToBytes(Constants.COMMAND_MESSAGE_CONTENT_PREFIX);
        int newLength = prefix.length + 1;
        int msgLength = 0;
        if (msg != null && msg.length > 0) {
            msgLength = Math.min(msg.length, 18);
            newLength += msgLength;
        }
        byte[] res = new byte[newLength];
        System.arraycopy(prefix, 0, res, 0, prefix.length);
        res[prefix.length] = (byte) index;
        if (msgLength > 0) {
            System.arraycopy(msg, 0, res, prefix.length + 1, msgLength);
        }
        writeCommand(res, commandCallback, true);
    }

    void sendShakeSignal(CommandCallback callback) {
        writeCommand(Constants.COMMAND_MESSAGE_SIGNAL, callback);
    }

    void setMessageByteNum(int num, CommandCallback callback) {
        mCurCommand = Constants.COMMAND_MESSAGE_NUM_PREFIX;
        byte[] prefix = HexUtil.hexStringToBytes(Constants.COMMAND_MESSAGE_NUM_PREFIX);
        byte[] res = new byte[prefix.length + 1];
        System.arraycopy(prefix, 0, res, 0, prefix.length);
        res[res.length - 1] = (byte) num;
        writeCommand(res, callback, true);
    }

    void writeCommand(final byte[] command, final CommandCallback callback, final boolean timeoutCheck) {
        mCurCallback = callback;
        if (command != null && command.length > 0) {
            mBleManager.write(mDevice, Constants.UUID_SERVICE, Constants.UUID_WRITE, command, new BleWriteCallback() {
                @Override
                public void onWriteSuccess(int current, int total, byte[] justWrite) {
                    System.out.println("----OnWriteSuccesss current : " + current + ", total : " + total + ", justWrite : " + Arrays.toString(justWrite) + ", command : " + Arrays.toString(command));
                    if (mMainHandler.hasMessages(WRITE_CODE)) {
                        System.out.println("!!!!!!!!!! FastBle lib returned wrong write callback");
                        mMainHandler.removeMessages(WRITE_CODE);
                    }
                }

                @Override
                public void onWriteFailure(BleException exception) {
                    if (timeoutCheck) {
                        Message msg = mMainHandler.obtainMessage(WRITE_CODE, callback);
                        mMainHandler.sendMessageDelayed(msg, 1000);
                        mCurException = exception;
                    }
                    System.out.println("------- onWriteFailure : " + exception);
                    if (callback != null) {
                        callback.onFailure(exception);
                    }
                }
            });
        }
    }

    void writeCommand(final String command, final CommandCallback callback) {
        writeCommand(command, callback, true);
    }

    void writeCommand(final String command, final CommandCallback callback, boolean timeoutCheck) {
        System.out.println("----Write command : " + command);
        mCurCommand = command;
        writeCommand(HexUtil.hexStringToBytes(command), callback, timeoutCheck);
    }

    private byte[] getCurrentTimeByteArr() {
        Calendar cal = Calendar.getInstance();
        int sec = cal.get(Calendar.SECOND);
        int min = cal.get(Calendar.MINUTE);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int date = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        System.out.println("----Get current time byte array : sec : " + sec + ", min : " + min + ", hour : " + hour + ", date : " + date + ", month : " + month + ", year : " + year);
        byte[] time = new byte[6];
        time[0] = (byte) sec;
        time[1] = (byte) min;
        time[2] = (byte) hour;
        time[3] = (byte) date;
        time[4] = (byte) month;
        time[5] = (byte) year;
        return time;
    }

    interface CommandCallback {
        void onFailure(BleException exception);

        void onSuccess(byte[]... b);
    }
}
