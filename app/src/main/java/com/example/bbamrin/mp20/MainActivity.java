package com.example.bbamrin.mp20;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {
    private BluetoothAdapter mBtAdapter;
    private Context ctx = this;
    private int RQS_ENABLE = 3;
    private boolean IS_ENABLED;
    private Handler h;
    private Data myData;
    private Executor ex;
    private BluetoothDevice d;
    private PendingIntent pi;
    private ConnectedThread ct;
    private Handler mHandler;
    private String NAME = "mpBT";
    private UUID MY_UUID = UUID.fromString("9e8e893a-75f1-11e7-b5a5-be2e44b06b34");
    private BlockButtonClass blockButton;
    private Button discoveryButton;
    private final BroadcastReceiver deleteR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            myData.clearAll();
            ex.execute(blockButton);
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                 d = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                myData.addDevice(d, getApplicationContext());

            } else if (intent.getAction().equals("device")){
                BluetoothDevice dev = (BluetoothDevice) intent.getParcelableExtra("d");
                ConnectThread connectThread = new ConnectThread(dev);
                connectThread.start();
                System.out.println(dev.getName());
                System.out.println("yes");
            } else if (intent.getAction().equals("sendTextMessage")){
                byte[] msg = intent.getStringExtra("msg").getBytes();
                ct.write(msg);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        blockButton = new BlockButtonClass();
        discoveryButton = (Button)findViewById(R.id.startDiscovery);
        ex = Executors.newFixedThreadPool(1);


        h = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                discoveryButton.setEnabled(true);
            };
        };


        mHandler = new Handler(){
            @Override
            public void handleMessage(android.os.Message msg) {

                switch (msg.what){

                    case MessageConstants.MESSAGE_READ:
                        Toast.makeText(getApplicationContext(),""+msg.getData(),Toast.LENGTH_SHORT).show();
                        System.out.println("message: " + new String((byte[]) msg.obj) + " was handled" );
                        Intent intent = new Intent();
                        intent.setAction("postMessage");
                        intent.putExtra("message",new String((byte[]) msg.obj) );
                        sendBroadcast(intent);
                        break;

                    case MessageConstants.MESSAGE_WRITE:
                        System.out.println(msg.getData() + " was send");
                        break;
                    case MessageConstants.MESSAGE_TOAST:

                        break;

                }
            };
        };

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        myData = new Data();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction("device");
        filter.addAction("sendTextMessage");
        IntentFilter deleteI = new IntentFilter();
        deleteI.addAction("delete");
        registerReceiver(deleteR,deleteI);
        registerReceiver(mReceiver, filter);


    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unregisterReceiver(deleteR);
    }


    @Override
    protected void onActivityResult(int rqsCode, int resultCode, Intent data){
        IS_ENABLED = resultCode == Activity.RESULT_OK;
    }

    public void onClick(View view) {
        switch (view.getId()){
            case R.id.onBT:
                if(mBtAdapter != null){
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent,RQS_ENABLE);
                } else Toast.makeText(this,"an error has occurred, try to turn on your bluetooth",Toast.LENGTH_SHORT).show();
                break;
            case R.id.startServer:
                if(mBtAdapter.isEnabled()){
                    AcceptThread acceptThread = new AcceptThread();
                    acceptThread.start();
                } else Toast.makeText(this,"an error has occurred, try to turn on your bluetooth",Toast.LENGTH_SHORT).show();
                break;
            case R.id.connect:
                if(mBtAdapter != null && mBtAdapter.isEnabled() && Data.CHOSEN_DEVICE) {
                    Intent i = new Intent();
                    i.setAction("connect");
                    sendBroadcast(i);
                } else Toast.makeText(this,"an error has occurred, try to turn on your bluetooth/choose the device you want to connect",Toast.LENGTH_SHORT).show();
                break;
            case R.id.startDiscovery:

                if(mBtAdapter.isEnabled()) {
                    Intent j = new Intent();
                    j.setAction("newScan");
                    sendBroadcast(j);
                    mBtAdapter.startDiscovery();
                    discoveryButton.setEnabled(false);
                } else Toast.makeText(this,"an error has occurred, try to turn on your bluetooth",Toast.LENGTH_SHORT).show();

                break;
            case R.id.chatRoom:
                if(mBtAdapter != null && mBtAdapter.isEnabled() && Data.CONNECTED) {
                    Intent intentAct = new Intent(this, ChatActivity.class);
                    startActivity(intentAct);
                    Data.CONNECTED = false;
                } else Toast.makeText(this,"an error has occurred, try to turn on your bluetooth/connect to device",Toast.LENGTH_SHORT).show();
                break;
            default: System.out.println("dunno");
                break;


        }

    }


    class BlockButtonClass implements Runnable{
        @Override
        public void run(){
            try{
                TimeUnit.SECONDS.sleep(12);
                h.sendEmptyMessage(1);
            } catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }


    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBtAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;System.out.println("ssss");

            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    System.out.println(socket.getRemoteDevice());
                    System.out.println("ssss");

                    if (socket != null) {
                        // A connection was accepted. Perform work associated with
                        // the connection in a separate thread.
                        //manageMyConnectedSocket(socket);
                        Data.CONNECTED = true;
                        ct = new ConnectedThread(socket);
                        ct.run();
                        mmServerSocket.close();
                        System.out.println("connection was accepted");
                        break;
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }


            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }









    private  class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                System.out.println(device.getName());
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBtAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                System.out.println("connecting");

            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                System.out.println("no");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            Data.CONNECTED = true;
            ct = new ConnectedThread(mmSocket);
            System.out.println("ConnectThread");
            ct.run();
            //manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }






    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }



        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                //here,......................................................................................................................................................................

                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }



    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

}
