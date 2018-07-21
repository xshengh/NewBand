package com.xshengh.newband.device.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.xshengh.newband.R;

public class ConnectTestActivity extends Activity {

    private EditText mEditMac;
    private Button mGoTest;
    private TextView mTextStatus;
    private TextView mTextCount;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int count = 0;
    private int hit = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_test);
        mEditMac = (EditText) findViewById(R.id.et_mac);
        mGoTest = (Button) findViewById(R.id.btn_test);
        mTextStatus = (TextView) findViewById(R.id.tv_status);
        mTextCount = (TextView) findViewById(R.id.tv_count);
        mGoTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                scanAndConnectByMac();
                mGoTest.setEnabled(false);
            }
        });
    }

//    private void scanAndConnectByMac() {
//        String mac = mEditMac.getText().toString();
//        final BleScanManager manager = new BleScanManager(this);
//
//        manager.setScanCallback(new BleScanManager.Callback() {
//            @Override
//            public void onStartScan() {
//                mTextStatus.setText("开始扫描");
//                count++;
//                mTextCount.setText(getString(R.string.scan_hit_count, count, hit));
//            }
//
//            @Override
//            public void onScanning(ScanResult scanResult) {
//                mTextStatus.setText("正在扫描");
//            }
//
//            @Override
//            public void onScanComplete() {
//                mTextStatus.setText("扫描完成");
//            }
//
//            @Override
//            public void onConnecting() {
//                mTextStatus.setText("正在连接");
//            }
//
//            @Override
//            public void onConnectFail() {
//                mTextStatus.setText("连接失败");
//                scanAndConnectByMac();
//            }
//
//            @Override
//            public void onConnectSuccess() {
//                mTextStatus.setText("连接成功");
//                hit++;
//                mTextCount.setText(getString(R.string.scan_hit_count, count, hit));
////                mHandler.postDelayed(new Runnable() {
////                    @Override
////                    public void run() {
////                        manager.closeConnect();
////                    }
////                }, 500);
//            }
//
//            @Override
//            public void onDisConnected() {
//                mTextStatus.setText("断开连接");
//                scanAndConnectByMac();
//            }
//
//            @Override
//            public void onServicesDiscovered() {
////                manager.closeConnect();
//                manager.notify(Constants.UUID_SERVICE, Constants.UUID_READ_NOTIFY, new BleCharacterCallback() {
//                    @Override
//                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
//                        if (characteristic != null) {
//                            byte[] res = characteristic.getValue();
//                            System.out.println("return value : " + HexUtil.encodeHexStr(res) + ", length :" + res.length);
//                        }
//                    }
//
//                    @Override
//                    public void onFailure(BleException exception) {
//                        System.out.println("Notify exception : " + exception);
//                    }
//
//                    @Override
//                    public void onInitiatedResult(boolean result) {
//                        System.out.println("Notify onInitiatedResult : " + result);
//                        if (result) {
//                            mHandler.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    manager.write(Constants.UUID_SERVICE, Constants.UUID_WRITE, Constants.COMMAND_DISCONNECT_BLE, new BleCharacterCallback() {
//                                        @Override
//                                        public void onSuccess(BluetoothGattCharacteristic characteristic) {
//                                            System.out.println("Write disconnect command success : " + characteristic);
//                                        }
//
//                                        @Override
//                                        public void onFailure(BleException exception) {
//                                            System.out.println("Write exception : " + exception);
//                                        }
//
//                                        @Override
//                                        public void onInitiatedResult(boolean result) {
//                                            System.out.println("Write onInitiatedResult : " + result);
//                                        }
//                                    });
//                                }
//                            }, 2000);
//                        }
//                    }
//                });
//            }
//        });
//        manager.scanAndConnectByMac(mac, 7000);
//    }
}
