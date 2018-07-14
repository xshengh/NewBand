package com.xshengh.newband.device.activities;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.clj.fastble.data.ScanResult;
import com.clj.fastble.utils.HexUtil;
import com.xshengh.newband.R;
import com.xshengh.newband.adapters.ResultAdapter;
import com.xshengh.newband.models.DeviceInfo;
import com.xshengh.newband.scanner.ScanManager;
import com.xshengh.newband.socket.SocketClientManager;
import com.xshengh.newband.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;

import static com.xshengh.newband.utils.Utils.FIX_EXECUTOR;

public class AnyScanActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mStartBtn, mUploadBtn, mGoRecyleBtn;
    private ResultAdapter mResultAdapter;
    private ProgressDialog mProgressDialog;
    private ScanManager mScanManager;
    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_any_scan);
        mScanManager = ScanManager.getInstance(this);
        initView();
        scanDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScanManager.stop();
    }

    private void initView() {
        mStartBtn = (Button) findViewById(R.id.btn_start);
        mStartBtn.setOnClickListener(this);
        mUploadBtn = (Button) findViewById(R.id.btn_upload);
        mUploadBtn.setOnClickListener(this);
        mUploadBtn.setEnabled(false);
        mGoRecyleBtn = (Button) findViewById(R.id.btn_go_recycle);
        mGoRecyleBtn.setOnClickListener(this);
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
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            scanDevice();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                scanDevice();
                break;
            case R.id.btn_upload:
                uploadData();
                break;
            case R.id.btn_go_recycle:
                goRecycleActivity();
                break;
        }
    }

    private void goRecycleActivity() {
        startActivity(new Intent(this, RecycleActivity.class));
    }

    private void uploadData() {
        int count = mResultAdapter.getCount();
        final byte[] nameArr = new byte[count * Constants.BYTE_SIZE_NAME_LIST + 1];
        int index = 0;
        nameArr[index++] = (byte) 0xFE;
        for (int i = 0; i < count; i++) {
            ScanResult item = mResultAdapter.getItem(i);
            String mac = item.getDevice().getAddress();
            if (!TextUtils.isEmpty(mac)) {
                String rawMac = mac.replace(":", "");
                byte[] macByte = HexUtil.hexStringToBytes(rawMac);
                System.out.println("---- mac : " + rawMac);
                System.out.println("---- byte : " + Arrays.toString(macByte));
                for (byte aMacByte : macByte) {
                    nameArr[index++] = aMacByte;
                }
                nameArr[index++] = (byte) (-item.getRssi());
                System.out.println("---- byte : " + HexUtil.encodeHexStr(nameArr, false));
            }
        }
        FIX_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                SocketClientManager.getInstance().sendData(nameArr);
            }
        });
        mGoRecyleBtn.setEnabled(true);
    }

    private void scanDevice() {
        mScanManager.setScanCallback(new ScanManager.ScanCallback() {
            @Override
            public void onStartScan() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.show();
                        mResultAdapter.clear();
                        mResultAdapter.notifyDataSetChanged();
                        mStartBtn.setEnabled(false);
                        mUploadBtn.setEnabled(false);
                        mGoRecyleBtn.setEnabled(false);
                    }
                });
            }

            @Override
            public void onScanning(final ScanResult scanResult) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String name = scanResult.getDevice().getName();
//                        if (name != null && name.startsWith(Constants.BT_PREFIX)) {
                        if (name != null) {
                            mResultAdapter.addResult(scanResult);
                            mResultAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }

            @Override
            public void onScanComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("-----onScanComplete");
                        mStartBtn.setEnabled(true);
                        mUploadBtn.setEnabled(true);
                        // manual to auto
//                        uploadData();
                        mProgressDialog.dismiss();
                    }
                });
            }
        });
        mScanManager.scanDevice();
    }
}
