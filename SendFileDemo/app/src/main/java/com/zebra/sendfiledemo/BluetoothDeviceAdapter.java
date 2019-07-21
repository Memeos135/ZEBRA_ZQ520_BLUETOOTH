package com.zebra.sendfiledemo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.MyViewHolder> {

    private Activity activity;
    private ArrayList<BluetoothDevice> deviceArrayList;

    public BluetoothDeviceAdapter(Activity activity, ArrayList<BluetoothDevice> deviceArrayList){
        this.activity = activity;
        this.deviceArrayList = deviceArrayList;
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        LayoutInflater mInflater = LayoutInflater.from(activity);
        view = mInflater.inflate(R.layout.printer_info_card, parent, false);

        return new BluetoothDeviceAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.printerName.setText(deviceArrayList.get(position).getName());

        holder.printerName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new PrintUtils(activity, null).fetchMacAddress(deviceArrayList.get(position).getAddress());
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceArrayList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView printerName;
        public MyViewHolder(View itemView) {
            super(itemView);
            printerName = (TextView) itemView.findViewById(R.id.printer_name);
        }
    }
}
