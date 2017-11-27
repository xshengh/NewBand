package com.xshengh.newband;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.xshengh.newband.device.activities.AnyScanActivity;
import com.xshengh.newband.device.activities.ConnectTestActivity;
import com.xshengh.newband.device.activities.MenuActivity;
import com.xshengh.newband.device.activities.OperationActivity;
import com.xshengh.newband.socket.SocketClientManager;
import com.xshengh.newband.utils.Utils;

public class MainActivity extends AppCompatActivity {

    private EditText mEditIP;
    private EditText mEditPort;
    private Button mBtnConnect;
    private Button mBtnDisconnect;
    private ProgressDialog mProgressDialog;
    private Handler mHandler = new Handler();
    private SocketClientManager mSocketManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ConnectTestActivity.class));
            }
        });

        mProgressDialog = new ProgressDialog(this);
        mEditIP = (EditText) findViewById(R.id.et_ip);
        mEditPort = (EditText) findViewById(R.id.et_port);
        if (BuildConfig.APP_TEST) {
            mEditIP.setText("192.168.0.101");
            mEditPort.setText("9999");
        } else {
            mEditIP.setText("192.168.12.151");
            mEditPort.setText("18087");
        }
        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mBtnDisconnect = (Button) findViewById(R.id.btn_disconnect);
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectSocket();
            }
        });
        mBtnDisconnect.setEnabled(false);
        mBtnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SocketClientManager.getInstance().stop();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBtnConnect.setEnabled(true);
                        Utils.showToast(MainActivity.this, "断开socket");
                    }
                }, 2000);
            }
        });
        connectSocket();
    }

    private void connectSocket() {
        Editable ipEdit = mEditIP.getText();
        Editable portEdit = mEditPort.getText();
        if (ipEdit != null && portEdit != null) {
            try {
                String ip = ipEdit.toString();
                int port = Integer.parseInt(portEdit.toString());
                System.out.println("------ ip : " + ip + ", port : " + port);
                SocketClientManager.init(ip, port);
                mSocketManager = SocketClientManager.getInstance();
                mProgressDialog.show();
                mSocketManager.setCallback1(new SocketClientManager.Callback1() {
                    @Override
                    public void onConnect() {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mProgressDialog.dismiss();
                                mBtnConnect.setEnabled(false);
                                mBtnDisconnect.setEnabled(true);
                                Utils.showToast(MainActivity.this, "连接socket成功");
                            }
                        });
                    }

                    @Override
                    public void onStartConnect() {
                        startActivity(new Intent(MainActivity.this, AnyScanActivity.class));
                    }

                    @Override
                    public void onDisConnect() {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mProgressDialog.dismiss();
                                Utils.showToast(MainActivity.this, "连接socket失败");
                            }
                        });
                        mSocketManager.stop();
                    }
                });
                mSocketManager.connect();
            } catch (Exception e) {
                e.printStackTrace();
                Utils.showToast(MainActivity.this, "格式输入有误");
            }
        } else {
            Utils.showToast(MainActivity.this, "格式输入有误");
        }
    }
}
