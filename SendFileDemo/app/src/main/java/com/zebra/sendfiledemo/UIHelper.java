/**
 * *******************************************
 * CONFIDENTIAL AND PROPRIETARY
 * <p/>
 * The source code and other information contained herein is the confidential and the exclusive property of
 * ZIH Corp. and is subject to the terms and conditions in your end user license agreement.
 * This source code, and any other information contained herein, shall not be copied, reproduced, published,
 * displayed or distributed, in whole or in part, in any medium, by any means, for any purpose except as
 * expressly permitted under such license agreement.
 * <p/>
 * Copyright ZIH Corp. 2010
 * <p/>
 * ALL RIGHTS RESERVED
 * *********************************************
 */
package com.zebra.sendfiledemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * @author mlim3 This class handles the display of dialogs.
 */
public class UIHelper {

    private ProgressDialog loadingDialog;
    private Activity activity;
    private RecyclerView recyclerView;
    private BluetoothDeviceAdapter bluetoothDeviceAdapter;
    private ArrayList<BluetoothDevice> deviceList;
    private BluetoothAdapter bluetoothAdapter;

    public UIHelper(Activity activity) {
        this.activity = activity;
    }

    public void showLoadingDialog(final String message) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    loadingDialog = new ProgressDialog(activity, R.style.ButtonAppearance);
                    loadingDialog.setMessage(message);
                    loadingDialog.show();
                    TextView tv1 = (TextView) loadingDialog.findViewById(android.R.id.message);
                    tv1.setTextAppearance(activity, R.style.ButtonAppearance);

                }
            });
        }
    }

    public void updateAdapter(){
        bluetoothDeviceAdapter.notifyDataSetChanged();
    }

    public void setList(ArrayList<BluetoothDevice> devices){
        this.deviceList = devices;
    }

    public void showRecyclerDialog(Activity activity, BluetoothDeviceAdapter bluetoothDeviceAdapter, final BluetoothAdapter bluetoothAdapter){
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.printer_list_layout);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawableResource(R.color.near_black);
        dialog.setCancelable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.create();
        }
        this.bluetoothDeviceAdapter = bluetoothDeviceAdapter;
        recyclerView = (RecyclerView) dialog.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(bluetoothDeviceAdapter);

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                bluetoothAdapter.cancelDiscovery();
            }
        });

        dialog.show();
    }

    public void updateLoadingDialog(final String message) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    loadingDialog.setMessage(message);
                }
            });
        }
    }

    public boolean isDialogActive() {
        if (loadingDialog != null) {
            return loadingDialog.isShowing();
        } else {
            return false;
        }
    }

    public void dismissLoadingDialog() {
        if (activity != null && loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    public void showErrorDialog(String errorMessage) {
        new AlertDialog.Builder(activity).setMessage(errorMessage).setTitle("Error").setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        }).create().show();
    }

    public void showErrorDialogOnGuiThread(final String errorMessage) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    AlertDialog.Builder builder=new  AlertDialog.Builder(activity,R.style.ErrorButtonAppearance);
                    builder.setMessage(errorMessage).setTitle("Error").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            dismissLoadingDialog();
                        }

                    }).create();
                    Dialog d= builder.show();
                    TextView tv1 = (TextView) d.findViewById(android.R.id.message);
                    tv1.setTextAppearance(activity, R.style.ErrorButtonAppearance);
                }
            });
        }
    }

}
