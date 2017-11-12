package com.xshengh.newband.device.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.xshengh.newband.R;

public class MenuActivity extends AppCompatActivity {

    private static final String TAG = "MenuActivity";
    private Button mRecycleBtn;
    private Button mSetAlarmBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);


        mRecycleBtn = (Button) findViewById(R.id.btn_recycle_start);
        mRecycleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, RecycleActivity.class));
            }
        });
        mSetAlarmBtn = (Button) findViewById(R.id.btn_upload_data);
        mSetAlarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, AnyScanActivity.class));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
