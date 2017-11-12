package com.xshengh.newband.adapters;

import com.clj.fastble.exception.BleException;
import com.xshengh.newband.scanner.BleScanManager;

/**
 * Created by xshengh on 17/7/1.
 */

public abstract class OnSetupAlarmCallbackAdapter implements BleScanManager.Callback3 {

    @Override
    public void onAlarmCancel() {

    }
}
