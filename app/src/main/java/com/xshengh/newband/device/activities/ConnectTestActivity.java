package com.xshengh.newband.device.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.ScanResult;
import com.xshengh.newband.R;
import com.xshengh.newband.scanner.BleScanManager;

public class ConnectTestActivity extends Activity {

    private EditText mEditMac;
    private Button mGoTest;
    private BleScanManager mBleManager;
    private TextView mTextStatus;
    private TextView mTextCount;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int count = 0;
    private int hit = 0;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mBleManager.closeConnect();
            scanAndConnectByMac();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_test);
        mEditMac = (EditText) findViewById(R.id.et_mac);
        mBleManager = BleScanManager.getInstance(this);
        mGoTest = (Button) findViewById(R.id.btn_test);
        mTextStatus = (TextView) findViewById(R.id.tv_status);
        mTextCount = (TextView) findViewById(R.id.tv_count);

        mBleManager.setScanCallback(new BleScanManager.Callback() {
            @Override
            public void onStartScan() {
                mTextStatus.setText("开始扫描");
                count++;
                mTextCount.setText(getString(R.string.scan_hit_count, count, hit));
            }

            @Override
            public void onScanning(ScanResult scanResult) {
                mTextStatus.setText("正在扫描");
            }

            @Override
            public void onScanComplete() {
                mTextStatus.setText("扫描完成");
            }

            @Override
            public void onConnecting() {
                mTextStatus.setText("正在连接");
            }

            @Override
            public void onConnectFail() {
                mTextStatus.setText("连接失败");
                scanAndConnectByMac();
            }

            @Override
            public void onConnectSuccess() {
                mTextStatus.setText("连接成功");
                hit++;
                mTextCount.setText(getString(R.string.scan_hit_count, count, hit));
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBleManager.closeConnect();

                    }
                }, 500);
            }

            @Override
            public void onDisConnected() {
                mTextStatus.setText("断开连接");
                scanAndConnectByMac();
            }

            @Override
            public void onServicesDiscovered() {
            }
        });
        mGoTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBleManager.closeConnect();
                scanAndConnectByMac();
                mGoTest.setEnabled(false);
            }
        });
    }
    private void scanAndConnectByMac() {
        mHandler.removeCallbacks(mRunnable);
        String mac = mEditMac.getText().toString();
        mBleManager.scanAndConnectByMac(mac ,7000);
        mHandler.postDelayed(mRunnable, 15000);

    }
}
