package com.xshengh.newband.device.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.clj.fastble.utils.HexUtil;
import com.xshengh.newband.R;
import com.xshengh.newband.adapters.RecycleItemAdapter;
import com.xshengh.newband.models.DeviceInfo;
import com.xshengh.newband.scanner.RecycleManager;
import com.xshengh.newband.socket.SocketClientManager;
import com.xshengh.newband.utils.Constants;
import com.xshengh.newband.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class RecycleActivity extends AppCompatActivity {

    private Button mLoopStartBtn;
    private Button mUploadBtn;
    private RecycleItemAdapter mResultAdapter;
    private RecycleManager mRecycleManager;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private LinkedList<DeviceInfo> mDeviceList = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle);
        mRecycleManager = RecycleManager.getInstance(this);
        initView();
    }

    private void initView() {
        mLoopStartBtn = (Button) findViewById(R.id.btn_recycle_start);
        mLoopStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        mUploadBtn = (Button) findViewById(R.id.btn_upload_data);
        mUploadBtn.setEnabled(false);
        mUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUpload();
            }
        });
        ListView result = (ListView) findViewById(R.id.list_recycle_result);
        mResultAdapter = new RecycleItemAdapter(this);
        result.setAdapter(mResultAdapter);
        ArrayList<? extends Parcelable> models = getIntent().getParcelableArrayListExtra("devices");
        if (models != null && !models.isEmpty()) {
            for (Parcelable p : models) {
                mDeviceList.add((DeviceInfo) p);
            }
            startRecycle(mDeviceList);
        }
    }

    private void startUpload() {
        int len = mResultAdapter.getCount();
        byte[] data = new byte[len * Constants.BYTE_SIZE_DATA_LIST + 1];
        int index = 0;
        data[index++] = (byte) 0xFF;
        for (int i = 0; i < len; i++) {
            DeviceInfo deviceInfo = mResultAdapter.getItem(i);
            String mac = deviceInfo.getMac();
            if (!TextUtils.isEmpty(mac)) {
                String rawMac = mac.replace(":", "");
                byte[] macByte = HexUtil.hexStringToBytes(rawMac);
                System.out.println("---- mac : " + rawMac);
                System.out.println("---- byte : " + Arrays.toString(macByte));
                for (byte aMacByte : macByte) {
                    data[index++] = aMacByte;
                }
                byte[] rate = deviceInfo.getRate();
                for (byte aRate : rate) {
                    data[index++] = aRate;
                }
                byte[] step = deviceInfo.getStep();
                for (byte aStep : step) {
                    data[index++] = aStep;
                }
                byte[] cal = deviceInfo.getCal();
                for (byte aCal : cal) {
                    data[index++] = aCal;
                }
                data[index++] = deviceInfo.getCtime();
                data[index++] = deviceInfo.getWtime();
                System.out.println("---- byte : " + HexUtil.encodeHexStr(data, false));
            }
        }
        SocketClientManager.getInstance().sendData(data);
        Utils.showToast(RecycleActivity.this, "上传成功");
        mUploadBtn.setEnabled(false);
        setResult(RESULT_OK);
        finish();
    }

    private void startRecycle(final LinkedList<DeviceInfo> devices) {
        if (devices != null && !devices.isEmpty()) {
            mLoopStartBtn.setEnabled(false);
            mRecycleManager.setCallback(new RecycleManager.RecycleCallback() {
                long connectTime = 0L;
                long workTime = 0L;
                @Override
                public void onStart() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mResultAdapter.clear();
                            System.out.println("------ size : " + devices.size() + ", list : " + devices);
                            mResultAdapter.addResult(devices);
                            mResultAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onStartConnect(DeviceInfo device) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectTime = System.currentTimeMillis();
                        }
                    });
                }

                @Override
                public void onConnectSuccess(final DeviceInfo device) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int ctime = (int) (System.currentTimeMillis() - connectTime) / 1000;
                            System.out.println("------ connect time : " + ctime);
                            device.setCtime((byte) ctime);
                            workTime = System.currentTimeMillis();
                        }
                    });
                }

                @Override
                public void onRecycling(final DeviceInfo device) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            device.setStatus(1);
                            mResultAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onRecycleFailed(final DeviceInfo device) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            device.setStatus(3);
                            mResultAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onSetAlarm(DeviceInfo device) {
                }

                @Override
                public void onFetchRate(final DeviceInfo device, final byte[] rate) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (rate != null) {
                                device.setRate(rate);
                                mResultAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }

                @Override
                public void onFetchStepAndCal(final DeviceInfo device, final byte[] step, final byte[] cal) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (step != null) {
                                device.setStep(step);
                            }
                            if (cal != null) {
                                device.setCal(cal);
                            }
                            mResultAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onRecycleFinish(final DeviceInfo device) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            device.setStatus(2);
                            mResultAdapter.notifyDataSetChanged();
                            int wtime = (int) (System.currentTimeMillis() - workTime) / 1000;
                            System.out.println("------ work time : " + wtime);
                            device.setWtime((byte) wtime);
                        }
                    });
                }

                @Override
                public void onComplete() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLoopStartBtn.setEnabled(true);
                            Utils.showToast(RecycleActivity.this, "轮询结束");
                            mUploadBtn.setEnabled(true);
                        }
                    });
                    //manual to auto
                    startUpload();
                }
            });
            mRecycleManager.startRecycle(devices);
        } else {
            Utils.showToast(RecycleActivity.this, "表单为空");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecycleManager.stopRecycle();
        mMainHandler.removeCallbacksAndMessages(null);
    }
}
