package com.xshengh.newband.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.clj.fastble.utils.HexUtil;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xshengh on 17/5/8.
 */

public class Utils {
    public static final ExecutorService FIX_EXECUTOR = Executors.newFixedThreadPool(5);
    public static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

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

    public static <T> T[] concat(T[] first, T[] second, int newLength) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
    public static void post(Runnable runnable) {
        MAIN_HANDLER.postDelayed(runnable, 500);
    }

    public void postDelayed(Runnable runnable, long delay) {
        MAIN_HANDLER.postDelayed(runnable, 500 + delay);
    }
}
