/**
 * ********************************************
 * CONFIDENTIAL AND PROPRIETARY
 * <p/>
 * The source code and other information contained herein is the confidential and the exclusive property of
 * ZIH Corp. and is subject to the terms and conditions in your end user license agreement.
 * This source code, and any other information contained herein, shall not be copied, reproduced, published,
 * displayed or distributed, in whole or in part, in any medium, by any means, for any purpose except as
 * expressly permitted under such license agreement.
 * <p/>
 * Copyright ZIH Corp. 2015
 * <p/>
 * ALL RIGHTS RESERVED
 * *********************************************
 */

package com.zebra.sendfiledemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ConnectionScreen extends AppCompatActivity {

    UIHelper uIHelper = new UIHelper(this);
    private Connection connection = null;
    private BluetoothAdapter mBluetoothAdapter;
    private String mHardwareAddress;
    private SharedPreferences storage;

    public static final String bluetoothAddressKey = "ZEBRA_DEMO_BLUETOOTH_ADDRESS";
    public static final String PREFS_NAME = "OurSavedAddress";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        storage = getSharedPreferences(PREFS_NAME, 0);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

    }

    public void clickMethod(View view){
        testRetrieveMacAddress();
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mHardwareAddress = device.getAddress(); // MAC address

                if(!storage.getString(bluetoothAddressKey, "").isEmpty()) {
                    fetchMacAddress(storage.getString(bluetoothAddressKey, ""));
                }else{
                    fetchMacAddress(getString(R.string.macAdd));
                }
            }
        }
    };

    public void fetchMacAddress(String input){
        if (mHardwareAddress.equalsIgnoreCase(input)) {
            uIHelper.dismissLoadingDialog();
            connectAndGetData();
        }
    }

    /**
     * Check for the printer status and language and send the test file to the printer,implements best practices to show status of the printer.
     */

    // NEEDED
    private void connectAndGetData() {

        connection = new BluetoothConnection(mHardwareAddress);
        try {
            Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();
            connection.open();
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
            ZebraPrinterLinkOs linkOsPrinter = ZebraPrinterFactory.createLinkOsPrinter(printer);
            PrinterStatus printerStatus = (linkOsPrinter != null) ? linkOsPrinter.getCurrentStatus() : printer.getCurrentStatus();
            getPrinterStatus();

            if (printerStatus.isReadyToPrint) {
                testSendFile(printer);
            } else if (printerStatus.isHeadOpen) {
                Log.i("log", "Head open, please close printet head to print");
            } else if (printerStatus.isPaused) {
                Log.i("log", "Printer paused");
            } else if (printerStatus.isPaperOut) {
                Log.i("log", "Media out. Please load media to print");
            }
            connection.close();
            saveSettings();
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
        }
    }


    /**
     * This method implements best practices to check the language of the printer and set the language of the printer to ZPL.
     * @return printer
     * @throws ConnectionException
     */


    public void testRetrieveMacAddress(){
        if (!mBluetoothAdapter.isEnabled()) {
            uIHelper.showErrorDialog("Bluetooth is disabled.");
        } else {
            mBluetoothAdapter.startDiscovery();
            uIHelper.showLoadingDialog("Scanning...");
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    // NEEDED
    private void getPrinterStatus() throws ConnectionException {

        SGD.SET("device.languages", "hybrid_xml_zpl", connection);
    }

    // NEEDED
    private void testSendFile(ZebraPrinter printer) {
        try {
            File filepath = getFileStreamPath("TEST.ZPL");
            createDemoFile(printer, "TEST.ZPL");
            printer.sendFileContents(filepath.getAbsolutePath());

        } catch (ConnectionException e1) {
            Log.i("log", "Error sending file to printer");
        } catch (IOException e) {
            Log.i("log", "Error creating file");
        }
    }

    /**
     * This method includes the creation of test file in ZPL and CPCL formats.
     * @param printer
     * @param fileName
     * @throws IOException
     */

    // NEEDED
    private void createDemoFile(ZebraPrinter printer, String fileName) throws IOException {

        FileOutputStream os = this.openFileOutput(fileName, Context.MODE_PRIVATE);

        byte[] configLabel = null;

        PrinterLanguage pl = printer.getPrinterControlLanguage();
        if (pl == PrinterLanguage.ZPL) {
            configLabel = "^XA^LH100,20^FO180,100^AQN,23,20^FDBaggage Care App^FS^FX^BY3,2,150^FO100,200^BC^FD12345678^FS^LH100,20^FO180,500^AQN,23,20^FDBaggage Care App^FS^FX ^BY3,2,150^FO100,550^BC^FD12345678^FS^LH100,20^FO180,900^AQN,23,20^FDBaggage Care App^FS^FX Third section with barcode.^BY3,2,150^FO100,950^BC^FD12345678^FS^XZ".getBytes();
        } else if (pl == PrinterLanguage.CPCL) {
            String cpclConfigLabel = "! 0 200 200 406 1\r\n" + "ON-FEED IGNORE\r\n" + "BOX 20 20 380 380 8\r\n" + "T 0 6 137 177 TEST\r\n" + "PRINT\r\n";
            configLabel = cpclConfigLabel.getBytes();
        }
        os.write(configLabel);
        os.flush();
        os.close();
    }

    private void saveSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(bluetoothAddressKey, mHardwareAddress);
        editor.commit();
    }
}
