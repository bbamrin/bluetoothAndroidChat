package com.example.bbamrin.mp20;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by bbamrin on 01.08.17.
 */

public class ChatActivity extends AppCompatActivity {


    private ListView chatList;
    private EditText messageText;
    private ArrayList<String> msgArray;
    private ArrayAdapter<String> msgListAdapter;


    private BroadcastReceiver msgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("intent");
            switch (intent.getAction()){

                case "postMessage":
                    String text = intent.getStringExtra("message");
                    System.out.println(text);
                    msgArray.add(text);
                    msgListAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_layout);


        chatList = (ListView)findViewById(R.id.msgList);
        messageText = (EditText)findViewById(R.id.message);

        IntentFilter intentFilter = new IntentFilter("postMessage");
        registerReceiver(msgReceiver,intentFilter);

        msgArray = new ArrayList<>();
        msgListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,msgArray);

        chatList.setAdapter(msgListAdapter);


    }

    public void chatOnClick(View view) {
        switch (view.getId()){
            case R.id.chatMsg:
                String text = messageText.getText().toString();
                sendMessage(text);

               break;
            case R.id.clearChat:
                clearAll();
                break;
        }
    }


    public void sendMessage(String text){
        msgArray.add(text);
        Intent intent = new Intent();
        intent.setAction("sendTextMessage");
        intent.putExtra("msg",text);
        sendBroadcast(intent);
        msgListAdapter.notifyDataSetChanged();
    }


    public void clearAll(){
        msgArray.clear();
        msgListAdapter.notifyDataSetChanged();
    }


}
