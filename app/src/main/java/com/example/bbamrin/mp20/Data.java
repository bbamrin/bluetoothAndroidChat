package com.example.bbamrin.mp20;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bbamrin on 30.07.17.
 */

public class Data {

    public HashMap<BluetoothDevice,String> getDeviceMap() {
        return deviceMap;
    }

    public void addDevice(BluetoothDevice b, Context ctx) {
        this.deviceMap.put(b,b.getAddress());
        this.deviceNames.put(b.getAddress(),b.getName());
        Intent i = new Intent();
        i.putExtra("hm",deviceMap);
        i.setAction("addDevices");
        ctx.sendBroadcast(i);
    }
    public void clearAll(){
        deviceNames.clear();
        deviceMap.clear();
    }

    public static boolean CHOSEN_DEVICE = false;
    public static boolean CONNECTED = false;
    public HashMap<String, String> getDeviceNames() {
        return deviceNames;
    }


    private HashMap<BluetoothDevice,String> deviceMap = new HashMap<>();
    private HashMap<String,String> deviceNames = new HashMap<>();


}
