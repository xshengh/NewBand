package com.xshengh.newband.adapters;

import com.xshengh.newband.scanner.BleScanManager;

/**
 * Created by xshengh on 17/7/1.
 */

public abstract class OnCancelAlarmCallbackAdapter implements BleScanManager.Callback3 {
    @Override
    public void onAlarmSetup(int hour, int minute) {
    }
}
