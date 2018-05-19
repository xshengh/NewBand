package com.xshengh.newband.scanner;

import android.support.annotation.NonNull;

import com.clj.fastble.exception.BleException;
import com.xshengh.newband.utils.Utils;

/**
 * Created by xshengh on 18/1/28.
 */

class MessageSender {
    private Commander mCommander;

    public MessageSender(@NonNull Commander commander) {
        this.mCommander = commander;
    }

    void sendMessage(byte[] msg, final Commander.CommandCallback callback) {
        if (msg != null && msg.length > 0) {
            sendShakeSignal(msg, callback);
        }
    }

    private void sendShakeSignal(final byte[] msg, final Commander.CommandCallback callback) {
        Utils.post(new Runnable() {
            @Override
            public void run() {
                mCommander.sendShakeSignal(new Commander.CommandCallback() {
                    @Override
                    public void onFailure(BleException exception) {
                        if (callback != null) {
                            callback.onFailure(exception);
                        }
                    }

                    @Override
                    public void onSuccess(byte[]... b) {
                        setMessageSender(msg, callback);
                    }
                });
            }
        });
    }

    private void setMessageSender(final byte[] msg, final Commander.CommandCallback callback) {
        Utils.post(new Runnable() {
            @Override
            public void run() {
                mCommander.setMessageSender("xshengh".getBytes(), new Commander.CommandCallback() {
                    @Override
                    public void onFailure(BleException exception) {
                        if (callback != null) {
                            callback.onFailure(exception);
                        }
                    }

                    @Override
                    public void onSuccess(byte[]... b) {
                        setMessageByteNum(msg, callback);
                    }
                });
            }
        });
    }

    private void setMessageByteNum(final byte[] msg, final Commander.CommandCallback callback) {
        Utils.post(new Runnable() {
            @Override
            public void run() {
                mCommander.setMessageByteNum(msg.length-1, new Commander.CommandCallback() {
                    @Override
                    public void onFailure(BleException exception) {
                        if (callback != null) {
                            callback.onFailure(exception);
                        }
                    }

                    @Override
                    public void onSuccess(byte[]... b) {
                        showMessageContent(msg, callback);
                    }
                });
            }
        });
    }

    private void showMessageContent(final byte[] msg, final Commander.CommandCallback callback) {
        Utils.post(new Runnable() {
            @Override
            public void run() {
                mCommander.showMessageContent(msg, msg.length-1, new Commander.CommandCallback() {
                    @Override
                    public void onFailure(BleException exception) {
                        callback.onFailure(exception);
                    }

                    @Override
                    public void onSuccess(byte[]... b) {
                        callback.onSuccess(b);
                    }
                });
            }
        });
    }
}
