package com.xshengh.newband.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.clj.fastble.data.ScanResult;
import com.clj.fastble.utils.HexUtil;
import com.xshengh.newband.R;
import com.xshengh.newband.device.activities.AnyScanActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xshengh on 17/7/1.
 */

public class ResultAdapter extends BaseAdapter {

    private Context context;
    private List<ScanResult> scanResultList;

    public ResultAdapter(Context context) {
        this.context = context;
        scanResultList = new ArrayList<>();
    }

    public void addResult(ScanResult result) {
        scanResultList.add(result);
    }

    public void clear() {
        scanResultList.clear();
    }

    @Override
    public int getCount() {
        return scanResultList.size();
    }

    @Override
    public ScanResult getItem(int position) {
        if (position > scanResultList.size())
            return null;
        return scanResultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            convertView = View.inflate(context, R.layout.adapter_scan_result, null);
            holder = new ViewHolder();
            convertView.setTag(holder);
            holder.txt_name = (TextView) convertView.findViewById(R.id.txt_name);
            holder.txt_mac = (TextView) convertView.findViewById(R.id.txt_mac);
            holder.txt_rssi = (TextView) convertView.findViewById(R.id.txt_rssi);
            holder.txt_broadcast = (TextView) convertView.findViewById(R.id.broadcast_record);
        }

        ScanResult result = scanResultList.get(position);
        BluetoothDevice device = result.getDevice();
        String name = device.getName();
        String mac = device.getAddress();
        int rssi = result.getRssi();
        holder.txt_name.setText(name);
        holder.txt_mac.setText(mac);
        holder.txt_rssi.setText(String.valueOf(rssi));
        holder.txt_broadcast.setText(HexUtil.encodeHexStr(result.getScanRecord()));
        return convertView;
    }

    class ViewHolder {
        TextView txt_name;
        TextView txt_mac;
        TextView txt_rssi;
        TextView txt_broadcast;
    }
}