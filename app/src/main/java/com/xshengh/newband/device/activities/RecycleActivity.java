package com.xshengh.newband.device.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class RecycleActivity extends AppCompatActivity {

    @BindView(R.id.btn_recycle_start)
    Button mLoopStartBtn;
    @BindView(R.id.btn_upload_data)
    Button mUploadBtn;
    private RecycleItemAdapter mResultAdapter;
    private RecycleManager mRecycleManager;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private LinkedList<DeviceInfo> mDeviceList = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle);
        ButterKnife.bind(this);
        mRecycleManager = RecycleManager.getInstance();
        initView();
    }

    private void initView() {
        mUploadBtn.setEnabled(false);
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

    @OnClick(R.id.btn_upload_data)
    void startUpload() {
        int len = mResultAdapter.getCount();
        final byte[] data = new byte[len * Constants.BYTE_SIZE_DATA_LIST + 1];
        int index = 0;
        data[index++] = (byte) 0xFF;
        for (int i = 0; i < len; i++) {
            DeviceInfo deviceInfo = mResultAdapter.getItem(i);
            String mac = deviceInfo.getMac();
            if (!TextUtils.isEmpty(mac)) {
                String rawMac = mac.replace(":", "");
                byte[] macByte = HexUtil.hexStringToBytes(rawMac);
                System.out.println("----Upload2 mac: " + rawMac);
                System.out.println("----Upload2 byte : " + Arrays.toString(macByte));
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
                System.out.println("----Upload2 hex : " + HexUtil.encodeHexStr(data, false));
            }
        }
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> observableEmitter) throws Exception {
                observableEmitter.onNext(SocketClientManager.getInstance().sendData(data));
                observableEmitter.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean success) throws Exception {
                if (success) {
                    Utils.showToast(RecycleActivity.this, "上传成功");
                }
                mUploadBtn.setEnabled(false);
                setResult(RESULT_OK);
                finish();
            }
        });
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
                            System.out.println("----Connect time : " + ctime);
                            device.setCtime((byte) ctime);
                            workTime = System.currentTimeMillis();
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
                public void onFetchStatistic(final DeviceInfo device, final byte[] rate, final byte[] step, final byte[] cal) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (rate != null) {
                                device.setRate(rate);
                            }
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
                            System.out.println("----Work time : " + wtime);
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
        if (!mRecycleManager.isStopped()) {
            mRecycleManager.stopRecycle();
        }
        mMainHandler.removeCallbacksAndMessages(null);
    }
}
