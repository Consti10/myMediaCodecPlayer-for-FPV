package com.example.wilson.mymediacodecfpvplayer;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class TestActivity extends AppCompatActivity {
    Context mContext;
    TestThread mTestThread;
    TextView mTextView;
    TextView mTextView2;
    TextView mTextView3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        mTextView=(TextView)findViewById(R.id.textView);
        mTextView2=(TextView)findViewById(R.id.textView2);
        mTextView3=(TextView)findViewById(R.id.textView3);
        mContext=this;
        mTestThread=new TestThread();
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(mTestThread != null){
            mTestThread.interrupt();
        }
        System.out.println("onPause");
    }

    @Override
    protected void onResume(){
        super.onResume();
        System.out.println("0nResume");
        if(mTestThread == null) {
            mTestThread = new TestThread();
        }
        mTestThread.start();

    }

    private class TestThread extends Thread{
        long number_received_bytes=0;
        long number_received_NALU=0;
        boolean running=false;
        DatagramPacket p=null;
        DatagramSocket s=null;
        byte[] message = new byte[1024];

        public TestThread(){
        }
        public void interrupt(){
            running=false;
            if(s != null){
                s.close();
            }
        }
        public void run(){
            printLocalIpAddresses();
            running=true;
            makeToast("Opening udp port 5000");
            p = new DatagramPacket(message, message.length);
            try {
                s = new DatagramSocket(5000);
                    s.setSoTimeout(3000);
                } catch (SocketException e) {e.printStackTrace();}
                if(s==null){makeToast("Couldn't open port");return;}
                makeToast("Port opened. Trying to receive data");
            while (running==true && s != null) {
                try {
                    s.receive(p);
                    if(p.getLength()>0){
                        number_received_bytes+=p.getLength();
                        makeText("" +number_received_bytes + " bytes received",mTextView);
                        parseDatagram(message,p.getLength());
                    }
                } catch (IOException e) {
                    makeToast("couldn't receive any bytes");
                }
            }

        }
        private void parseDatagram(byte[] p, int plen) {
            int nalu_data_position=0;
            int NALU_MAXLEN = 1024 * 1024;
            int nalu_search_state=0;
            try {
                for (int i = 0; i < plen; ++i) {
                    nalu_data_position++;
                    if (nalu_data_position == NALU_MAXLEN - 1) {
                        Log.w("parseDatagram", "NALU Overflow");
                        makeToast("Nalu overflow !");
                        nalu_data_position = 0;
                    }
                    switch (nalu_search_state) {
                        case 0:
                        case 1:
                        case 2:
                            if (p[i] == 0)
                                nalu_search_state++;
                            else
                                nalu_search_state = 0;
                            break;
                        case 3:
                            if (p[i] == 1) {
                                number_received_NALU++;
                                makeText(""+ number_received_NALU + "Nalu's (~frames)",mTextView2);
                                nalu_data_position = 4;
                            }
                            nalu_search_state = 0;
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void makeToast(final String message){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext,message,Toast.LENGTH_SHORT).show();
                }
            });
        }
        private void makeText(final String message, final TextView textView){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(message);
                }
            });
        }
        public void printLocalIpAddresses(){
            String s="";
            try{
                for(Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();en.hasMoreElements();){
                    NetworkInterface intf=en.nextElement();
                    for(Enumeration<InetAddress> enumIpAddr=intf.getInetAddresses();enumIpAddr.hasMoreElements();){
                        InetAddress inetAddress=enumIpAddr.nextElement();
                        if(!intf.isLoopback()){
                            s+="Interface "+intf.getName()+": "+inetAddress.getHostAddress()+"\n";
                        }
                    }
                }
                makeText(s,mTextView3);
            }catch(Exception e){e.printStackTrace();}
        }
    }

}
