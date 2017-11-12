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
import android.widget.Toast;

import com.clj.fastble.data.ScanResult;
import com.clj.fastble.utils.HexUtil;
import com.xshengh.newband.R;
import com.xshengh.newband.adapters.ResultAdapter;
import com.xshengh.newband.models.DeviceInfo;
import com.xshengh.newband.scanner.BleScanManager;
import com.xshengh.newband.socket.SocketClientManager;
import com.xshengh.newband.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class AnyScanActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mStartBtn, mUploadBtn, mGoRecyleBtn;
    private ResultAdapter mResultAdapter;
    private ProgressDialog progressDialog;
    private BleScanManager mScanManager;
    private ScanResult mCurDevice;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_any_scan);
        mScanManager = BleScanManager.getInstance(this);
        initView();
        // manual to auto
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
        progressDialog = new ProgressDialog(this);

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
        SocketClientManager.getInstance().setCallback2(new SocketClientManager.Callback2() {
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
        byte[] nameArr = new byte[count * Constants.BYTE_SIZE_NAME_LIST + 1];
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
        SocketClientManager.getInstance().sendBytes(nameArr);
        mGoRecyleBtn.setEnabled(true);
    }

    private void scanDevice() {
        mScanManager.setScanCallback(new BleScanManager.Callback() {
            @Override
            public void onStartScan() {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mResultAdapter.clear();
                        mResultAdapter.notifyDataSetChanged();
                        mStartBtn.setEnabled(false);
                        mUploadBtn.setEnabled(false);
                        mGoRecyleBtn.setEnabled(false);
                    }
                });
            }

            @Override
            public void onScanning(ScanResult result) {
                String name = result.getDevice().getName();
                if (name != null && name.startsWith(Constants.BT_PREFIX)) {
                    mResultAdapter.addResult(result);
                    mResultAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScanComplete() {
                System.out.println("-----onScanComplete");
                mStartBtn.setEnabled(true);
                mUploadBtn.setEnabled(true);
                // manual to auto
                uploadData();
            }

            @Override
            public void onConnecting() {
                progressDialog.show();
            }

            @Override
            public void onConnectFail() {
                System.out.println("-----onConnectFail");
                mStartBtn.setEnabled(true);
                progressDialog.dismiss();
                Toast.makeText(AnyScanActivity.this, "连接失败", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess() {

            }

            @Override
            public void onDisConnected() {
                progressDialog.dismiss();
                mResultAdapter.clear();
                mResultAdapter.notifyDataSetChanged();
                mStartBtn.setEnabled(true);
                mUploadBtn.setVisibility(View.INVISIBLE);
                Toast.makeText(AnyScanActivity.this, "连接断开", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onServicesDiscovered() {
                progressDialog.dismiss();
                Intent intent = new Intent(AnyScanActivity.this, OperationActivity.class);
                intent.putExtra("device", mCurDevice);
                startActivity(intent);
            }
        });
        mScanManager.scanDevice();
    }
}
