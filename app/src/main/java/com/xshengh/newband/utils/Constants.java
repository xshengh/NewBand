package com.xshengh.newband.utils;

/**
 * Created by xshengh on 17/6/22.
 */

public class Constants {
    public static final String UUID_SERVICE = "00005301-0000-0041-4C50-574953450000";
    public static final String UUID_READ_NOTIFY = "00005303-0000-0041-4C50-574953450000";
    public static final String UUID_WRITE = "00005302-0000-0041-4C50-574953450000";

    public static final String RETURN_RATE_PREFIX = "8F";
    public static final String BT_PREFIX = "BT";
//    public static final String COMMAND_EXERCISE_MODE_ON = "3801";
    public static final String COMMAND_EXERCISE_MODE_ON = "7F02";
    public static final String COMMAND_MANUAL_HR = "7E";
    public static final String COMMAND_PREFIX_ALARM = "35";
    public static final String COMMAND_PREFIX_ENABLE_ALARM = "3501";
    public static final String COMMAND_POSTFIX_ALARM = "000000000000000000000000";
    public static final String COMMAND_DISABLE_ALARM = "35000000" + COMMAND_POSTFIX_ALARM;
    public static final String COMMAND_SEND_STEP = "410100000";
    public static final String COMMAND_RECEIVE_STEP_PEEFIX = "82";
    public static final String COMMAND_PREFIX_SET_TIME = "31";
    public static final String COMMAND_DISCONNECT_BLE = "F3";
    public static final String COMMAND_EXERCISE_MODE_OFF = "7F03";

    public static final String COMMAND_ACK1 = "91";
    public static final String COMMAND_ACK2 = "94";

    //Byte length
    public static final int BYTE_LEN_MAC = 6;
    public static final int BYTE_LEN_RSSI = 1;
    public static final int BYTE_LEN_RATE = 1;
    public static final int BYTE_LEN_STEP = 4;
    public static final int BYTE_LEN_CAL = 2;
    public static final int BYTE_LEN_ALARM = 3;
    public static final int BYTE_LEN_TIME = 2;
    public static final int BYTE_SIZE_RECEIVE_UNIT = BYTE_LEN_MAC + BYTE_LEN_ALARM;
    public static final int BYTE_SIZE_NAME_LIST = BYTE_LEN_MAC + BYTE_LEN_RSSI;
    public static final int BYTE_SIZE_DATA_LIST = BYTE_LEN_MAC + BYTE_LEN_RATE + BYTE_LEN_CAL + BYTE_LEN_STEP + BYTE_LEN_TIME;
}
