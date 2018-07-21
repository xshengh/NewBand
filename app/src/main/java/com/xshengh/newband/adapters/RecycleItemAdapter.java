package com.xshengh.newband.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.xshengh.newband.R;
import com.xshengh.newband.models.DeviceInfo;
import com.xshengh.newband.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by xshengh on 17/7/4.
 */

public class RecycleItemAdapter extends BaseAdapter {
    private Context context;
    private List<DeviceInfo> scanResultList;

    public RecycleItemAdapter(Context context) {
        this.context = context;
        scanResultList = new ArrayList<>();
    }

    public void addResult(LinkedList<DeviceInfo> deviceList) {
        scanResultList.clear();
        scanResultList.addAll(deviceList);
    }

    public void clear() {
        scanResultList.clear();
    }

    @Override
    public int getCount() {
        return scanResultList.size();
    }

    @Override
    public DeviceInfo getItem(int position) {
        if (position > scanResultList.size())
            return null;
        return scanResultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RecycleItemAdapter.ViewHolder holder;
        if (convertView != null) {
            holder = (RecycleItemAdapter.ViewHolder) convertView.getTag();
        } else {
            convertView = View.inflate(context, R.layout.adapter_recycle_item, null);
            holder = new RecycleItemAdapter.ViewHolder();
            convertView.setTag(holder);
            holder.mac = (TextView) convertView.findViewById(R.id.recycle_mac);
            holder.status = (TextView) convertView.findViewById(R.id.recycle_status);
            holder.rate = (TextView) convertView.findViewById(R.id.recycle_rate);
            holder.step = (TextView) convertView.findViewById(R.id.recycle_step);
        }
        DeviceInfo info = getItem(position);
        holder.mac.setText(info.getMac());
        holder.status.setText(DeviceInfo.getContentByStatus(info.getStatus()));
        String rate = String.valueOf(Utils.parseByte2Int(info.getRate()));
        holder.rate.setText(context.getResources().getString(R.string.rate_recycle_result, rate));
        String step = String.valueOf(Utils.parseByte2Int(info.getStep()));
        String cal = String.valueOf(Utils.parseByte2Int(info.getCal()));
        holder.step.setText(context.getResources().getString(R.string.step_cal_recycle_result, step, cal));
        return convertView;
    }

    class ViewHolder {
        TextView mac;
        TextView status;
        TextView rate;
        TextView step;
    }
}
