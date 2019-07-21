package com.zebra.sendfiledemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;
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
import java.util.ArrayList;

public class PrintUtils {
    private  UIHelper uiHelper;
    private Connection connection = null;
    private BluetoothAdapter mBluetoothAdapter;
    private String mHardwareAddress;
    private SharedPreferences storage;
    private Activity activity;
    private ArrayList<BluetoothDevice> deviceList;

    public static final String bluetoothAddressKey = "ZEBRA_DEMO_BLUETOOTH_ADDRESS";
    public static final String PREFS_NAME = "OurSavedAddress";

    public PrintUtils(Activity activity, SharedPreferences storage){

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        this.storage = storage;

        this.activity = activity;
        uiHelper = new UIHelper(activity);

        deviceList = new ArrayList<>();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(mReceiver, filter);
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(!deviceList.contains(device)){
                    deviceList.add(device);
                }

                // update adapter on scan
                updateRecyclerView();
            }
        }
    };

    public void updateRecyclerView(){
        uiHelper.setList(deviceList);
        uiHelper.updateAdapter();
    }

    public void fetchMacAddress(String input){
        connectAndGetData(input);
    }

    /**
     * Check for the printer status and language and send the test file to the printer,implements best practices to show status of the printer.
     */

    // NEEDED
    private void connectAndGetData(String mHardwareAddress) {

        connection = new BluetoothConnection(mHardwareAddress);
        try {
            Toast.makeText(activity, "Connecting...", Toast.LENGTH_SHORT).show();
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
            uiHelper.showErrorDialog("Bluetooth is disabled.");
        } else {
            mBluetoothAdapter.startDiscovery();
            uiHelper.showRecyclerDialog(activity, new BluetoothDeviceAdapter(activity, deviceList), mBluetoothAdapter);
        }
    }

    // NEEDED
    private void getPrinterStatus() throws ConnectionException {

        SGD.SET("device.languages", "hybrid_xml_zpl", connection);
    }

    // NEEDED
    private void testSendFile(ZebraPrinter printer) {
        try {
            File filepath = activity.getFileStreamPath("TEST.ZPL");
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

        FileOutputStream os = activity.openFileOutput(fileName, Context.MODE_PRIVATE);

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
}
