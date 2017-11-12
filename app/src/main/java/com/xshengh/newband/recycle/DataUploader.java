package com.xshengh.newband.recycle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.xshengh.newband.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xshengh on 17/4/23.
 */

class DataUploader {
    private static DataUploader sDataUploader = null;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    public interface UploaderListener {
        void onUploadSuccess();

        void onUploadFail(Exception e);
    }

    private static final File FILE_DIR = new File("/sdcard/BandLog/");
    private int mFileCount = 0;
    private static final ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor();
    private Context mContext;

    static synchronized DataUploader getInstance(Context context) {
        if (sDataUploader == null) {
            sDataUploader = new DataUploader(context);
        }
        return sDataUploader;
    }

    private DataUploader(Context context) {
        mContext = context;
    }

    public synchronized void writeToFile(final Hashtable<String, ArrayList<DeviceInfo>> result, final UploaderListener listener) {
        if (!result.isEmpty()) {
            THREAD_POOL.execute(new Runnable() {
                @Override
                public void run() {
                    FileWriter fw = null;
                    try {
                        if (!FILE_DIR.exists()) {
                            FILE_DIR.mkdirs();
                        }
                        File file = new File(FILE_DIR, "bandlog_" + System.currentTimeMillis());
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        fw = new FileWriter(file);
                        int i = 0;
                        System.out.println("--------------------Collect data start-------------------");
                        for (String key : result.keySet()) {
                            ArrayList<DeviceInfo> deviceList = result.get(key);
                            System.out.println("----------- device name : " + key + ", rateValue : ");
                            for (DeviceInfo device : deviceList) {
                                StringBuffer sb = new StringBuffer();
                                sb.append(i).append("\t").append(key).append("\t").append(device.getHeartRate()).append("\t").append(device.getSteps()).append("\n");
                                fw.write(sb.toString());
                                System.out.print(i + "-" + device.getHeartRate() + ", ");
                                i++;
                            }
                            System.out.println("");
                        }
                        listener.onUploadSuccess();
                        mFileCount++;
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Utils.showToast(mContext, "第" + mFileCount + "次数据收集成功");
                            }
                        });
                        System.out.println("--------------------Collect data end-------------------");
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onUploadFail(e);
                    } finally {
                        if (fw != null) {
                            try {
                                fw.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                listener.onUploadFail(e);
                            }
                        }
                    }
                }
            });
        } else {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Utils.showToast(mContext, "本次采集无数据，跳过");
                }
            });
            listener.onUploadSuccess();
        }
    }
}
