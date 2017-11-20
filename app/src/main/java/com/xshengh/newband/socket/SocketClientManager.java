package com.xshengh.newband.socket;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.clj.fastble.utils.HexUtil;
import com.xshengh.newband.BandApplication;
import com.xshengh.newband.device.activities.RecycleActivity;
import com.xshengh.newband.models.DeviceInfo;
import com.xshengh.newband.scanner.BleScanManager;
import com.xshengh.newband.utils.Constants;
import com.xshengh.newband.utils.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by xshengh on 17/7/2.
 */

public class SocketClientManager {
    private Socket socket = null;
    private InputStream in = null;
    private OutputStream out = null;
    private String host;
    private int port;
    private Handler main = new Handler(Looper.getMainLooper());
    private Handler async = null;
    private static SocketClientManager sSocketClientManager = null;
    private ArrayList<DeviceInfo> devices = new ArrayList<>();
    private Callback1 callback1;
    private Callback2 callback2;
    private boolean isConnected;

    private SocketClientManager(String host, int port) {
        this.host = host;
        this.port = port;

    }

    public static synchronized void init(String host, int port) {
        if (sSocketClientManager == null) {
            sSocketClientManager = new SocketClientManager(host, port);
        }
    }

    public static SocketClientManager getInstance() {
        if (sSocketClientManager == null) {
            throw new IllegalStateException("SocketClientManager not init!");
        }
        return sSocketClientManager;
    }

    public void sendBytes(final byte[] bytes) {
        async.post(new Runnable() {
            @Override
            public void run() {
                System.out.println("------ isConnected : " + socket.isConnected() + ", isOutputShutdown : " + socket.isOutputShutdown());
                if (socket.isConnected()) {
                    if (!socket.isOutputShutdown()) {
                        try {
                            if (bytes != null && bytes.length > 0) {
                                out.write(bytes, 0, bytes.length);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public void setCallback1(Callback1 callback1) {
        this.callback1 = callback1;
    }

    public ArrayList<DeviceInfo> fetchDataList() {
        return devices;
    }
    public void setCallback2(Callback2 listener) {
        this.callback2 = listener;


    }

    public void stop() {
        if (socket != null) {
            sendBytes(new byte[]{(byte) 0xFD});
            async.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    isConnected = false;
                }
            }, 1000);
        }
        async.getLooper().quit();
    }

    public void connect() {
        HandlerThread thread = new HandlerThread("SocketClientManager");
        thread.start();
        async = new Handler(thread.getLooper());
        new Thread() {
            @Override
            public void run() {
                try {
                    socket = new Socket(host, port);
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                    while (true) {
                        if (!socket.isClosed() && socket.isConnected()) {
                            if (callback1 != null && !isConnected) {
                                isConnected = true;
                                callback1.onConnect();
                            }
                            if (!socket.isInputShutdown()) {
                                int count = in.available();
                                if (count > 0) {
                                    byte[] content = new byte[count];
                                    in.read(content, 0, content.length);
                                    final int len = content.length;
                                    if (len == 1 && content[0] == (byte)0xFE) {
                                        if (callback1!= null) {
                                            callback1.onStartConnect();
                                        }
                                    } else if (len != 0 && len % (Constants.BYTE_SIZE_RECEIVE_UNIT) == 0) {
                                        if (BandApplication.getInstance() != null) {
                                            Handler handler = new Handler(Looper.getMainLooper());
                                            handler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Utils.showToast(BandApplication.getInstance(), String.format(Locale.getDefault(), "收到的字节数%d", len));
                                                }
                                            });
                                        }
                                        devices.clear();
                                        int i = 0;
                                        while (i < len) {
                                            byte[] macBytes = Arrays.copyOfRange(content, i, i += Constants.BYTE_LEN_MAC);
                                            String mac = Utils.macWithColon(HexUtil.encodeHexStr(macBytes, false));
                                            System.out.println("-------mac :" + mac);
                                            DeviceInfo device = new DeviceInfo(mac);
                                            byte[] timeBytes = Arrays.copyOfRange(content, i, i += Constants.BYTE_LEN_ALARM);
                                            System.out.println("-------alarm : " + Arrays.toString(timeBytes));
                                            device.setAlarm(timeBytes);
//                                            byte exerciseOnoff = Arrays.copyOfRange(content, i, i+= Constants.BYTE_LEN_EXERCISE;
                                            byte exerciseOnoff = content[i++];
                                            device.setExerciseOnOff((int)exerciseOnoff);
                                            System.out.println("-------exercise : " + device.getExerciseOnOff());
                                            devices.add(device);
                                            System.out.println("------ size : " + devices.size() + ", list : " + devices);
                                        }
                                        if (callback2 != null) {
                                            callback2.onDataReceive(devices);
                                        }
                                        System.out.println("收到表单信息");
                                    } else {
                                        System.out.println("表单格式有误");
                                    }
                                }
                            }
                        } else {
                            if (callback1 != null) {
                                isConnected = false;
                                callback1.onDisConnect();
                                break;
                            }
                        }
                    }
                } catch (final IOException ex) {
                    if (callback1 != null) {
                        callback1.onDisConnect();
                    }
                    ex.printStackTrace();
                }
            }
        }.start();
    }


    public interface Callback1 {
        void onConnect();

        void onStartConnect();

        void onDisConnect();
    }

    public interface Callback2 {
        void onDataReceive(ArrayList<DeviceInfo> devices);
    }
}
