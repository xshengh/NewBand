package com.xshengh.newband;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Process;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.xshengh.newband.socket.SocketClientManager;

import static com.xshengh.newband.utils.Constants.HOST;
import static com.xshengh.newband.utils.Constants.PORT;

/**
 * Created by xshengh on 17/11/20.
 */

public class BandApplication extends Application {
    private static BandApplication sBandApplication;
    private static Handler sUiHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        sBandApplication = this;
        sUiHandler = new Handler();
        SocketClientManager.init(HOST, PORT);
        BleManager.getInstance().init(this);
        BleManager.getInstance().enableLog(true);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && !adapter.isEnabled()) {
            if (!adapter.enable()) {
                Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_SHORT).show();
                Process.killProcess(Process.myPid());
            }
        }
    }

    public static BandApplication getInstance() {
        return sBandApplication;
    }

    public static Handler getUiHandler() {
        return sUiHandler;
    }
}
