package com.xshengh.newband.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.clj.fastble.data.BleDevice;
import com.clj.fastble.utils.HexUtil;
import com.xshengh.newband.utils.Constants;
import com.xshengh.newband.utils.Utils;

import java.util.Arrays;

/**
 * Created by xshengh on 17/7/4.
 */

public class DeviceInfo implements Parcelable {
    private String mac;
    private byte[] rate = new byte[Constants.BYTE_LEN_RATE];
    private byte[] step = new byte[Constants.BYTE_LEN_STEP];
    private byte[] cal = new byte[Constants.BYTE_LEN_CAL];
    private byte[] alarm = new byte[Constants.BYTE_LEN_ALARM];
    private byte ctime;
    private byte wtime;
    private int status;
    private int retryCount;
    private int exerciseOnOff;
    private transient BleDevice device;

    public BleDevice getDevice() {
        return device;
    }

    public void setDevice(BleDevice device) {
        this.device = device;
    }

    public byte getCtime() {
        return ctime;
    }

    public void setCtime(byte ctime) {
        this.ctime = ctime;
    }

    public byte getWtime() {
        return wtime;
    }

    public void setWtime(byte wtime) {
        this.wtime = wtime;
    }

    public DeviceInfo() {
        ctime = (byte) 0xFF;
        wtime = (byte) 0xFF;
        Arrays.fill(rate, (byte) 0xFF);
        Arrays.fill(step, (byte) 0xFF);
        Arrays.fill(cal, (byte) 0xFF);
    }

    protected DeviceInfo(Parcel in) {
        mac = in.readString();
        rate = in.createByteArray();
        step = in.createByteArray();
        cal = in.createByteArray();
        alarm = in.createByteArray();
        status = in.readInt();
        retryCount = in.readInt();
        ctime = in.readByte();
        wtime = in.readByte();
        exerciseOnOff = in.readInt();
    }

    public static final Creator<DeviceInfo> CREATOR = new Creator<DeviceInfo>() {
        @Override
        public DeviceInfo createFromParcel(Parcel in) {
            return new DeviceInfo(in);
        }

        @Override
        public DeviceInfo[] newArray(int size) {
            return new DeviceInfo[size];
        }
    };

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public byte[] getRate() {
        return rate;
    }

    public void setRate(byte[] rate) {
        this.rate = rate;
    }

    public byte[] getStep() {
        return step;
    }

    public void setStep(byte[] step) {
        this.step = step;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public byte[] getCal() {
        return cal;
    }

    public void setCal(byte[] cal) {
        this.cal = cal;
    }

    public byte[] getAlarm() {
        return alarm;
    }

    public void setAlarm(byte[] alarm) {
        this.alarm = alarm;
    }

    public static String getContentByStatus(int status) {
        switch (status) {
            case 0:
                return "待测量";
            case 1:
                return "正在测量";
            case 2:
                return "已测量";
            case 3:
                return "获取数据失败";
        }
        return "";
    }

    public int getExerciseOnOff() {
        return exerciseOnOff;
    }

    public void setExerciseOnOff(int exerciseOnOff) {
        this.exerciseOnOff = exerciseOnOff;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int unpack(int index, byte[] content) {
        int i = index;
        if (content != null) {
            byte[] macBytes = Arrays.copyOfRange(content, i, i += Constants.BYTE_LEN_MAC);
            this.mac = Utils.macWithColon(HexUtil.encodeHexStr(macBytes, false));
            System.out.println("----Received mac :" + mac);
            this.alarm = Arrays.copyOfRange(content, i, i += Constants.BYTE_LEN_ALARM);
            System.out.println("----Received alarm : " + Arrays.toString(alarm));
            this.exerciseOnOff = content[i++];
            System.out.println("----Received exercise : " + this.exerciseOnOff);
        }
        return i;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mac);
        dest.writeByteArray(rate);
        dest.writeByteArray(step);
        dest.writeByteArray(cal);
        dest.writeByteArray(alarm);
        dest.writeInt(status);
        dest.writeInt(retryCount);
        dest.writeByte(ctime);
        dest.writeByte(wtime);
        dest.writeInt(exerciseOnOff);
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "mac='" + mac + '\'' +
                ", rate=" + Arrays.toString(rate) +
                ", step=" + Arrays.toString(step) +
                ", cal=" + Arrays.toString(cal) +
                ", alarm=" + Arrays.toString(alarm) +
                ", ctime=" + ctime +
                ", wtime=" + wtime +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", exerciseOnOff=" + exerciseOnOff +
                ", device=" + device +
                '}';
    }
}
