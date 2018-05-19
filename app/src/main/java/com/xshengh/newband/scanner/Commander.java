package com.xshengh.newband.scanner;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.utils.HexUtil;
import com.xshengh.newband.utils.Constants;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by xshengh on 18/1/27.
 */

class Commander {

    private BleManager mBleManager;
    private CommandCallback mCurCallback;
    private String mCurCommand;

    Commander(@NonNull BleManager bleManager) {
        this.mBleManager = bleManager;
    }

    void init(final CommandCallback callback) {
        mBleManager.notify(Constants.UUID_SERVICE, Constants.UUID_READ_NOTIFY, new BleCharacterCallback() {
            @Override
            public void onFailure(BleException exception) {
                System.out.println("-------- onFailure :" + exception);
                if (callback != null) {
                    callback.onFailure(exception);
                }
            }

            @Override
            public void onInitiatedResult(boolean result) {
                System.out.println("-------- onInitiatedResult :" + result);
                if (callback != null) {
                    if (result) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(new OtherException("Init failed"));
                    }
                }
            }

            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                if (characteristic != null) {
                    final byte[] res = characteristic.getValue();
                    System.out.println("return value : " + HexUtil.encodeHexStr(res) + ", length :" + res.length);
                    if (res.length >= 1) {
                        String prefix = HexUtil.extractData(res, 0);
                        System.out.println("------- cmd : " + mCurCommand + ", prefix : " + prefix);
                        if (Constants.COMMAND_ACK1.equalsIgnoreCase(prefix) && !Constants.COMMAND_MANUAL_HR.equals(mCurCommand) && !Constants.COMMAND_SEND_STEP.equals(mCurCommand)) {
                            if (mCurCallback != null) {
                                mCurCallback.onSuccess();
                            }
                        } else if (Constants.COMMAND_ACK3.equalsIgnoreCase(prefix)) {
                            if (mCurCallback != null) {
                                mCurCallback.onSuccess();
                            }
                        } else if (Constants.RETURN_RATE_PREFIX.equalsIgnoreCase(prefix) && res.length == 16) {
                            if (mCurCallback != null) {
                                mCurCallback.onSuccess(Arrays.copyOfRange(res, 13, 14));
                            }
                        } else if (Constants.COMMAND_RECEIVE_STEP_PEEFIX.equalsIgnoreCase(prefix) && res.length == 19) {
                            if (mCurCallback != null) {
                                mCurCallback.onSuccess(Arrays.copyOfRange(res, 7, 11), Arrays.copyOfRange(res, 11, 13));
                            }
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

    void fetchHeartRate(CommandCallback commandCallback) {
        writeCommand(Constants.COMMAND_MANUAL_HR, commandCallback);
    }

    void fetchStepRecord(CommandCallback commandCallback) {
        writeCommand(Constants.COMMAND_SEND_STEP, commandCallback);
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
        writeCommand(res, commandCallback);
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
        writeCommand(res, commandCallback);
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
        writeCommand(res, callback);
    }

    void writeCommand(final byte[] command, final CommandCallback callback) {
        mCurCallback = callback;
        mBleManager.writeDevice(Constants.UUID_SERVICE, Constants.UUID_WRITE, command, new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                System.out.println("------- Write command success : " + Arrays.toString(command));
            }

            @Override
            public void onFailure(BleException exception) {
                if (callback != null) {
                    callback.onFailure(exception);
                }
            }

            @Override
            public void onInitiatedResult(boolean result) {
                System.out.println("Write command : " + Arrays.toString(command) + ", init result : " + result);
                if (!result) {
                    if (callback != null) {
                        callback.onFailure(new OtherException("Write command init failed : " + command));
                    }
                }
            }
        });
    }

    void writeCommand(final String command, final CommandCallback callback) {
        mCurCommand = command;
        writeCommand(HexUtil.hexStringToBytes(command), callback);
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

    interface CommandCallback {
        void onFailure(BleException exception);

        void onSuccess(byte[]... b);
    }
}
