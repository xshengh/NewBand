package com.xshengh.newband.alarm;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created by xshengh on 17/5/3.
 */

public class BLEAlarmManager {
    private static final String TAG = "BLEAlarmManager";
    private static final long SCAN_TIME = 5000L;
    private static BLEAlarmManager sBLEAlarmManager;
    private Context mContext;
    private HandlerThread mHandlerThread;
    private Handler mHandler = new Handler();
    private int hour;
    private int minute;
    private boolean isSetAlarm = false;
    private String mCurrentDevice;

    public static BLEAlarmManager getInstance(Context context) {
        if (sBLEAlarmManager == null) {
            sBLEAlarmManager = new BLEAlarmManager(context);
        }
        return sBLEAlarmManager;
    }

    private BLEAlarmManager(Context context) {
        mContext = context;
    }
}
