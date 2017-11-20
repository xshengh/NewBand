package com.xshengh.newband;

import android.app.Application;

/**
 * Created by xshengh on 17/11/20.
 */

public class BandApplication extends Application {
    private static BandApplication sBandApplication;
    @Override
    public void onCreate() {
        super.onCreate();
        sBandApplication = this;
    }
    public static BandApplication getInstance() {
        return sBandApplication;
    }
}
