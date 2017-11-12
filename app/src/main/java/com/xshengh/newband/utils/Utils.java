package com.xshengh.newband.utils;

import android.content.Context;
import android.widget.Toast;

import com.clj.fastble.utils.HexUtil;

import java.util.concurrent.Executors;

/**
 * Created by xshengh on 17/5/8.
 */

public class Utils {
    public static void showToast(Context context, String toast) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }

    public static String macWithColon(String mac) {
        int len = mac.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(mac.charAt(i));
            if ((i + 1) % 2 == 0 && i != len - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    public static boolean isValid(byte[] res) {
        if (res != null) {
            for (byte b : res) {
                if ((b & 0xff) != 0xff) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int parseByte2Int(byte[] res) {
        if (res != null && res.length > 0 && isValid(res)) {
            StringBuilder sb = new StringBuilder();
            for (int i = res.length - 1; i >= 0; i--) {
                sb.append(HexUtil.extractData(res, i));
            }
            return Integer.parseInt(sb.toString(), 16);
        }
        return -1;
    }
}
