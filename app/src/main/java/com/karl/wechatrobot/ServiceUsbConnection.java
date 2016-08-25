package com.karl.wechatrobot;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

public class ServiceUsbConnection {

        public static final String TAG = "USBConnection";
        public static Boolean mainThreadFlag = true;
        public static Boolean ioThreadFlag = true;
        ServerSocket serverSocket = null;
        final int SERVER_PORT = 10086;


    private void initConection()
    {
        serverSocket = null;
        try
        {
            Log.d("chl", "doListen()");
            serverSocket = new ServerSocket(SERVER_PORT);
            Log.d("chl", "doListen() 2");
            while (mainThreadFlag)
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                Socket socket = serverSocket.accept();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void onDestroy()
    {
        // 关闭线程
        mainThreadFlag = false;
        ioThreadFlag = false;
        // 关闭服务器
        try
        {
            Log.v(TAG, Thread.currentThread().getName() + "---->" + "serverSocket.close()");
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        Log.v(TAG, Thread.currentThread().getName() + "---->" + "**************** onDestroy****************");
    }

}
