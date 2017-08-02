package com.example.bbamrin.mp20;

import android.app.ListFragment;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by bbamrin on 30.07.17.
 */

public class ListOfDevices extends ListFragment {

    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> listName;
    private Executor ex;
    private ArrayList<String> macAdresses = new ArrayList<>();
    private MyRun myRun;
    private HashMap<BluetoothDevice,String> dMap = new HashMap<>();
    private BluetoothDevice bluetoothD;


    private BroadcastReceiver addDevices = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {




            if (intent.getAction() == "addDevices"){
                dMap.putAll( (HashMap<BluetoothDevice,String>) intent.getSerializableExtra("hm"));

                listName.clear();
                macAdresses = new ArrayList<>();
                for (BluetoothDevice d : dMap.keySet()){
                    listName.add(d.getName());
                    macAdresses.add(d.getAddress());
                }
                listAdapter.notifyDataSetChanged();

                //not thread safe, i must write something like handler or must block scanning for 12 secs, but later)
                ex.execute(myRun);
            } else if (intent.getAction() == "connect"){
                ListView ls = getListView();
                int pos = ls.getCheckedItemPosition();
                String address = macAdresses.get(pos);
                    for (BluetoothDevice d : dMap.keySet()){
                        if (d.getAddress() == address){
                            bluetoothD = d;

                        }
                    }
                    Intent addr = new Intent();
                    addr.putExtra("d",bluetoothD);
                    addr.setAction("device");
                    getActivity().sendBroadcast(addr);



            } else if (intent.getAction() == "newScan"){
                dMap.clear();
                listName.clear();
                listAdapter.notifyDataSetChanged();
            }

        }
    };


    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        listName =  new ArrayList<>();
        ex = Executors.newFixedThreadPool(1);
        myRun = new MyRun();

        ListView ls =  getListView();
        ls.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        IntentFilter filter = new IntentFilter("addDevices");
        filter.addAction("connect");
        filter.addAction("newScan");
        getActivity().registerReceiver(addDevices,filter);


        listAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_single_choice, listName);
        setListAdapter(listAdapter);

    }



    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);
        //macAdresses.remove(id);
        //"item with id: " + id + " and pos: " + pos
        Data.CHOSEN_DEVICE = true;
        Toast.makeText(getActivity(),"" + l.getCheckedItemPosition(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        getActivity().unregisterReceiver(addDevices);
    }


    public void addElement(){
        listName.add("sss");
        listAdapter.notifyDataSetChanged();
    }

    public void deleteFirst(){
        listName.remove(0);
        listAdapter.notifyDataSetChanged();
    }


    class MyRun implements Runnable{

        @Override
        public void run(){
            try {
                //12 secs because discovery lasts 12 secs
                TimeUnit.SECONDS.sleep(12);
                Intent i = new Intent("delete");
                getActivity().sendBroadcast(i);
            } catch (InterruptedException e){
                e.printStackTrace();
            }

        }
    }






}

