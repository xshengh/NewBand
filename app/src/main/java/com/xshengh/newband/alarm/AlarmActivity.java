package com.xshengh.newband.alarm;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.xshengh.newband.R;

import java.util.ArrayList;

public class AlarmActivity extends Activity {

    private ListView mDeviceListView;
    private Button mStartScanBtn;
    private Button mSetAlarmBtn;
    private Button mCancelAlarmBtn;

    private BLEAlarmManager mBLEAlarmManager;
    private ArrayList<String> mDeviceList = new ArrayList<>();

    private EditText mAlarmHour;
    private EditText mAlarmMinute;
    private EditText mDeviceEdit;
    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        mBLEAlarmManager = BLEAlarmManager.getInstance(this);

        mDeviceListView = (ListView) findViewById(R.id.device_list);
        mAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item, mDeviceList);
        mDeviceListView.setAdapter(mAdapter);

        mDeviceEdit = (EditText) findViewById(R.id.device_et);
        mAlarmHour = (EditText) findViewById(R.id.alarm_hour);
        mAlarmMinute = (EditText) findViewById(R.id.alarm_minute);

        mStartScanBtn = (Button) findViewById(R.id.scan_btn_start);
        mStartScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter.clear();
            }
        });
        mSetAlarmBtn = (Button) findViewById(R.id.set_alarm);
        mSetAlarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        mCancelAlarmBtn = (Button) findViewById(R.id.cancel_alarm);
        mCancelAlarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }
}
