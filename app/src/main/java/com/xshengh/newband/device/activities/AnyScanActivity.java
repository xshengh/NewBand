package com.xshengh.newband.device.activities;


import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.utils.HexUtil;
import com.xshengh.newband.R;
import com.xshengh.newband.adapters.ResultAdapter;
import com.xshengh.newband.models.DeviceInfo;
import com.xshengh.newband.socket.SocketClientManager;
import com.xshengh.newband.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class AnyScanActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    private static final int REQUEST_CODE = 3;

    @BindView(R.id.btn_start)
    Button mStartBtn;
    @BindView(R.id.btn_upload)
    Button mUploadBtn;
    @BindView(R.id.btn_go_recycle)
    Button mGoRecyleBtn;
    private ResultAdapter mResultAdapter;
    private ProgressDialog mProgressDialog;
    private BleManager mBleManager = BleManager.getInstance();
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private void checkPermissions() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!adapter.isEnabled()) {
                    Toast.makeText(AnyScanActivity.this, getString(R.string.please_open_blue), Toast.LENGTH_SHORT).show();
                    mMainHandler.postDelayed(this, 2000);
                } else {
                    String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
                    List<String> permissionDeniedList = new ArrayList<>();
                    for (String permission : permissions) {
                        int permissionCheck = ContextCompat.checkSelfPermission(AnyScanActivity.this, permission);
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permission);
                        } else {
                            permissionDeniedList.add(permission);
                        }
                    }
                    if (!permissionDeniedList.isEmpty()) {
                        String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
                        ActivityCompat.requestPermissions(AnyScanActivity.this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
                    }
                }
            }
        });
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    scanDevice();
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_any_scan);
        ButterKnife.bind(this);
        initView();
        checkPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBleManager.getScanSate() == BleScanState.STATE_SCANNING) {
            mBleManager.cancelScan();
        }
    }

    private void initView() {
        mUploadBtn.setEnabled(false);
        mGoRecyleBtn.setEnabled(false);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);

        mResultAdapter = new ResultAdapter(this);
        final ListView deviceListView = (ListView) findViewById(R.id.list_device);
        deviceListView.setAdapter(mResultAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                mScanManager.cancelScan();
//                mCurDevice = mResultAdapter.getItem(position);
//                mScanManager.connectDevice(mCurDevice);
            }
        });
        SocketClientManager.getInstance().setDataReceiveCallback(new SocketClientManager.DataReceiveCallback() {
            @Override
            public void onDataReceive(ArrayList<DeviceInfo> devices) {
                // manual to auto
                startActivityForResult(new Intent(AnyScanActivity.this, RecycleActivity.class).putExtra("devices", devices), REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("---onActivityResult : " + requestCode + ", " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                scanDevice();
            } else {
                checkPermissions();
            }
        } else if (requestCode == REQUEST_CODE) {
            scanDevice();
        }
    }

    @OnClick(R.id.btn_go_recycle)
    void goRecycleActivity() {
        startActivity(new Intent(this, RecycleActivity.class));
    }

    @OnClick(R.id.btn_upload)
    void uploadData() {
        int count = mResultAdapter.getCount();
        final byte[] nameArr = new byte[count * Constants.BYTE_SIZE_NAME_LIST + 1];
        int index = 0;
        nameArr[index++] = (byte) 0xFE;
        for (int i = 0; i < count; i++) {
            BleDevice item = mResultAdapter.getItem(i);
            String mac = item.getDevice().getAddress();
            if (!TextUtils.isEmpty(mac)) {
                String rawMac = mac.replace(":", "");
                byte[] macByte = HexUtil.hexStringToBytes(rawMac);
                System.out.println("----Upload1 mac : " + rawMac);
                System.out.println("----Upload1 byte : " + Arrays.toString(macByte));
                for (byte aMacByte : macByte) {
                    nameArr[index++] = aMacByte;
                }
                nameArr[index++] = (byte) (-item.getRssi());
                System.out.println("----Upload1 hex : " + HexUtil.encodeHexStr(nameArr, false));
            }
        }
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> observableEmitter) throws Exception {
                observableEmitter.onNext(SocketClientManager.getInstance().sendData(nameArr));
                observableEmitter.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean success) throws Exception {
                mGoRecyleBtn.setEnabled(true);
            }
        });
    }

    @OnClick(R.id.btn_start)
    void scanDevice() {
        mBleManager.initScanRule(new BleScanRuleConfig.Builder().setScanTimeOut(7000).setDeviceMac(null).build());
        mBleManager.scan(new BleScanCallback() {
            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                System.out.println("----Scan Finished");
                for (BleDevice device : scanResultList) {
                    String name = device.getName();
                    if (name != null && name.startsWith(Constants.BT_PREFIX)) {
                    /* if (name != null) { **/
                        mResultAdapter.addResult(device);
                        mResultAdapter.notifyDataSetChanged();
                    }
                }
                mStartBtn.setEnabled(true);
                mUploadBtn.setEnabled(true);
                /* manual to auto **/
                uploadData();
                mProgressDialog.dismiss();
            }

            @Override
            public void onScanStarted(boolean success) {
                if (success) {
                    mProgressDialog.show();
                    mResultAdapter.clear();
                    mResultAdapter.notifyDataSetChanged();
                    mStartBtn.setEnabled(false);
                    mUploadBtn.setEnabled(false);
                    mGoRecyleBtn.setEnabled(false);
                } else {
                    Toast.makeText(AnyScanActivity.this, "Start scan error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
            }
        });
    }
}
