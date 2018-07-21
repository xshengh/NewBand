package com.xshengh.newband.recycle;

import android.bluetooth.BluetoothDevice;

import com.clj.fastble.data.BleDevice;

/**
 * Created by xshengh on 17/4/2.
 */

public class DeviceInfo extends BleDevice {
    private int heartRate;
    private boolean testing;
    private int steps;

    public DeviceInfo(BluetoothDevice device, int rssi, byte[] scanRecord, long timestampNano) {
        super(device, rssi, scanRecord, timestampNano);
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public boolean isTesting() {
        return testing;
    }

    public void setTesting(boolean testing) {
        this.testing = testing;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    @Override
    public String toString() {
        BluetoothDevice device = getDevice();
        return "DeviceInfo{" +
                "address='" + device.getAddress() + '\'' +
                ", name='" + device.getName() + '\'' +
                ", rssi=" + getRssi() +
                ", heartRate=" + heartRate +
                ", steps=" + steps +
                '}';
    }
}
