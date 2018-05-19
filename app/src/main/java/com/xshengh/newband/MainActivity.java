package com.xshengh.newband;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.xshengh.newband.device.activities.AnyScanActivity;
import com.xshengh.newband.device.activities.ConnectTestActivity;
import com.xshengh.newband.socket.SocketClientManager;
import com.xshengh.newband.utils.Constants;
import com.xshengh.newband.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.xshengh.newband.utils.Utils.FIX_EXECUTOR;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_ip)
    TextView mIp;
    @BindView(R.id.tv_port)
    TextView mPort;
    protected ProgressDialog mProgressDialog;
    private Handler mHandler = BandApplication.getUiHandler();
    private SocketClientManager mSocketManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mIp.setText(Constants.HOST);
        mPort.setText(String.valueOf(Constants.PORT));
        mProgressDialog = new ProgressDialog(this);
        connectSocket();
    }

    @OnClick(R.id.btn_test)
    void startConnectTestActivity() {
        startActivity(new Intent(MainActivity.this, ConnectTestActivity.class));
    }

    private void connectSocket() {
        try {
            mSocketManager = SocketClientManager.getInstance();
            mProgressDialog.show();
            mSocketManager.setConnectCallback(new SocketClientManager.SocketConnectCallback() {
                @Override
                public void onConnect(final boolean success) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                mProgressDialog.dismiss();
                                Utils.showToast(MainActivity.this, "连接socket成功");
                                startActivity(new Intent(MainActivity.this, AnyScanActivity.class));
                            } else {
                                Utils.showToast(MainActivity.this, "连接socket失败");
                            }
                        }
                    });
                }

                @Override
                public void onDisConnect() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.dismiss();
                            Utils.showToast(MainActivity.this, "断开连接");
                        }
                    });
                }
            });
            FIX_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    mSocketManager.connect();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Utils.showToast(MainActivity.this, "网络参数有误");
        }
    }
}
