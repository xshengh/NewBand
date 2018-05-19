package com.xshengh.newband.device.activities;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.xshengh.newband.R;
//import com.xshengh.newband.scanner.BleScanManager;
import com.xshengh.newband.scanner.RecycleManager;
import com.xshengh.newband.utils.Utils;

public class OperationActivity extends AppCompatActivity implements View.OnClickListener {

    private ScanResult mDevice;
    private RecycleManager mBleScanManager;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private TextView mRateText;
    private TextView mStepText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operation);
        mDevice = getIntent().getParcelableExtra("device");
        if (mDevice == null) {
            finish();
            return;
        }
        initViews();
        mBleScanManager = RecycleManager.getInstance(this);
//        mBleScanManager.setCollectDataCallback(new BleScanManager.Callback3() {
//            @Override
//            public void onStart() {
//            }
//
//            @Override
//            public void onStartFetchRate() {
//
//            }
//
//            @Override
//            public void onStartFetchStep() {
//
//            }
//
//            @Override
//            public boolean isRateFetched() {
//                return false;
//            }
//
//            @Override
//            public boolean isStepFetched() {
//                return false;
//            }
//
//            @Override
//            public void onRateDataReceived(final byte[] rate) {
//                mMainHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        mRateText.setText("心率：" + Utils.parseByte2Int(rate));
//                    }
//                });
//            }
//
//            @Override
//            public void onStepDataReceived(final byte[] steps, final byte[] cal) {
//                mMainHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        mStepText.setText("步数：" + Utils.parseByte2Int(steps));
//                    }
//                });
//            }
//
//            @Override
//            public void onReceiveFail(BleException exception) {
//                Utils.showToast(OperationActivity.this, "闹钟设置失败");
//            }
//
//            @Override
//            public void onAlarmSetup(int hour, int minute) {
//                Utils.showToast(OperationActivity.this, "闹钟设置成功= " + hour + ":" + minute);
//            }
//
//            @Override
//            public void onAlarmCancel() {
//                Utils.showToast(OperationActivity.this, "取消闹钟成功");
//            }
//        });
    }

//    private void startWork() {
//        mBleScanManager.notifyBandData(2, null);
//    }

    private void initViews() {
        mRateText = (TextView) findViewById(R.id.txt_rate);
        mStepText = (TextView) findViewById(R.id.txt_step);
        ((TextView) findViewById(R.id.txt_name)).setText(mDevice.getDevice().getName());
        ((TextView) findViewById(R.id.txt_mac)).setText(mDevice.getDevice().getAddress());
        ((Button) findViewById(R.id.btn_collect)).setOnClickListener(this);
        final TimePicker timePicker = (TimePicker) findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);
//        findViewById(R.id.btn_alarm).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                System.out.println("------- hour : " + timePicker.getCurrentHour() + ", minute : " + timePicker.getCurrentMinute());
//                mBleScanManager.setAlarm(timePicker.getCurrentHour(), timePicker.getCurrentMinute());
//            }
//        });
//        findViewById(R.id.btn_cancel_alarm).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mBleScanManager.cancelAlarm();
//            }
//        });
    }

    @Override
    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.btn_collect:
//                startWork();
//                break;
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mMainHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mBleScanManager.cancelAlarm();
//            }
//        }, 500);
//        mMainHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                mBleScanManager.closeConnect();
//                Utils.showToast(OperationActivity.this, "连接断开");
//            }
//        });
    }
}
