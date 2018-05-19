package com.xshengh.newband;

import android.app.Application;
import android.os.Handler;

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
    }

    public static BandApplication getInstance() {
        return sBandApplication;
    }
    public static Handler getUiHandler() {
        return sUiHandler;
    }
}
