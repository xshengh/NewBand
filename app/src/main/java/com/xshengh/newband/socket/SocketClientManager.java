package com.xshengh.newband.socket;

import android.os.Handler;
import android.os.HandlerThread;

import com.xshengh.newband.models.DeviceInfo;
import com.xshengh.newband.utils.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by xshengh on 17/7/2.
 */

public class SocketClientManager {
    private Socket socket = null;
    private String host;
    private int port;
    private Handler async = null;
    private static SocketClientManager sSocketClientManager = null;
    private SocketConnectCallback connectCallback;
    private DataReceiveCallback dataReceiveCallback;
    private boolean isConnected;

    private SocketClientManager(String host, int port) {
        this.host = host;
        this.port = port;
        HandlerThread thread = new HandlerThread("SocketClientManager");
        thread.start();
        async = new Handler(thread.getLooper());
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

    private void receiveServerData() {
        async.post(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (!socket.isClosed() && socket.isConnected() && !socket.isInputShutdown()) {
                            InputStream in = socket.getInputStream();
                            int count = in.available();
                            if (count > 0) {
                                byte[] content = new byte[count];
                                in.read(content, 0, content.length);
                                final int len = content.length;
                                if (len != 0 && len % (Constants.BYTE_SIZE_RECEIVE_UNIT) == 0) {
                                    ArrayList<DeviceInfo> devices = new ArrayList<>();
                                    int i = 0;
                                    while (i < len) {
                                        DeviceInfo device = new DeviceInfo();
                                        i += device.unpack(i, content);
                                        devices.add(device);
                                        System.out.println("------ size : " + devices.size() + ", list : " + devices);
                                    }
                                    if (dataReceiveCallback != null) {
                                        dataReceiveCallback.onDataReceive(devices);
                                    }
                                    System.out.println("收到表单信息");
                                } else {
                                    System.out.println("表单格式有误");
                                }
                            }
                        } else {
                            if (connectCallback != null) {
                                isConnected = false;
                                connectCallback.onDisConnect();
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
   }

    public synchronized void sendData(byte[] bytes) {
        if (isConnected) {
            System.out.println("------ isConnected : " + socket.isConnected() + ", " +
                    "isOutputShutdown : " + socket.isOutputShutdown());
            if (socket.isConnected() && !socket.isOutputShutdown()) {
                try {
                    if (bytes != null && bytes.length > 0) {
                        OutputStream out = socket.getOutputStream();
                        out.write(bytes, 0, bytes.length);
                        out.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setConnectCallback(SocketConnectCallback connectCallback) {
        this.connectCallback = connectCallback;
    }

    public void setDataReceiveCallback(DataReceiveCallback listener) {
        this.dataReceiveCallback = listener;
    }

    public synchronized void stop() {
        if (isConnected && socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            isConnected = false;
        }
    }

    public synchronized void connect() {
        if (!isConnected) {
            try {
                socket = new Socket(host, port);
                if (connectCallback != null) {
                    connectCallback.onConnect(true);
                }
                isConnected = true;
                receiveServerData();
            } catch (final IOException ex) {
                if (connectCallback != null) {
                    connectCallback.onConnect(false);
                }
                ex.printStackTrace();
            }
        }
    }

    public interface SocketConnectCallback {
        void onConnect(boolean success);

        void onDisConnect();
    }

    public interface DataReceiveCallback {
        void onDataReceive(ArrayList<DeviceInfo> devices);
    }
}
