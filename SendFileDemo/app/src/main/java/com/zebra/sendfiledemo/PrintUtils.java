package com.zebra.sendfiledemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
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
    private Activity activity;
    private ArrayList<BluetoothDevice> deviceList;

    public PrintUtils(Activity activity){

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        this.activity = activity;
        uiHelper = new UIHelper(activity);

        deviceList = new ArrayList<>();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(mReceiver, filter);
    }

    /**
     * Receiver for bluetooth scanning events
     */

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

    /**
     * This method sets up the scanned devices recycler view
     */

    public void updateRecyclerView(){
        uiHelper.setList(deviceList);
        uiHelper.updateAdapter();
    }

    /**
     * This method connects to a ZEBRA printer and send it a file to print
     */
    public void fetchMacAddress(String input){
        connectAndGetData(input);
    }

    /**
     * This method checks for the printer status and language and send the test file to the printer, implements best practices to show status of the printer.
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
                Log.i("log", "Head open, please close printer head to print");
            } else if (printerStatus.isPaused) {
                Log.i("log", "Printer paused");
            } else if (printerStatus.isPaperOut) {
                Log.i("log", "Media out. Please load media to print");
            }
            connection.close();
            saveMacAddress(mHardwareAddress);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method saves the mac address of a printer
     */

    public void saveMacAddress(String macAddress){
        PreferenceManager.getDefaultSharedPreferences(activity).edit().putString("mac_address", macAddress).apply();
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
            if(!PreferenceManager.getDefaultSharedPreferences(activity).getString("mac_address", "").isEmpty()){
                connectAndGetData(PreferenceManager.getDefaultSharedPreferences(activity).getString("mac_address", ""));
            }else {
                mBluetoothAdapter.startDiscovery();
                uiHelper.showRecyclerDialog(activity, new BluetoothDeviceAdapter(activity, deviceList), mBluetoothAdapter);
            }
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
            configLabel = "^XA^FX Top section with company logo, name and address.^CF0,60^FO50,50^GB100,100,100^FS^FO75,75^FR^GB100,100,100^FS^FO88,88^GB50,50,50^FS^FO220,50^FDInternational Shipping, Inc.^FS^CF0,40^FO220,100^FD1000 Shipping Lane^FS^FO220,135^FDShelbyville TN 38102^FS^FO220,170^FDUnited States (USA)^FS^FO50,250^GB700,1,3^FS^FX Second section with recipient address and permit information.^CFA,30^FO50,300^FDJohn Doe^FS^FO50,340^FD100 Main Street^FS^FO50,380^FDSpringfield TN 39021^FS^FO50,420^FDUnited States (USA)^FS^CFA,15^FO600,300^GB150,150,3^FS^FO638,340^FDPermit^FS^FO638,390^FD123456^FS^FO50,500^GB700,1,3^FS^FX Third section with barcode.^BY5,2,270^FO100,550^BC^FD12345678^FS^FX Fourth section (the two boxes on the bottom).^FO50,900^GB700,250,3^FS^FO400,900^GB1,250,3^FS^CF0,40^FO100,960^FDShipping Ctr. X34B-1^FS^FO100,1010^FDREF1 F00B47^FS^FO100,1060^FDREF2 BL4H8^FS^CF0,190^FO485,965^FDCA^FS^XZ".getBytes();
        } else if (pl == PrinterLanguage.CPCL) {
            String cpclConfigLabel = "! 0 200 200 406 1\r\n" + "ON-FEED IGNORE\r\n" + "BOX 20 20 380 380 8\r\n" + "T 0 6 137 177 TEST\r\n" + "PRINT\r\n";
            configLabel = cpclConfigLabel.getBytes();
        }
        os.write(configLabel);
        os.flush();
        os.close();
    }
}
