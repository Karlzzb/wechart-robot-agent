package com.karl.wechatrobot;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServiceUsbConnection{
    public static final String TAG = "USBConnection";
    public static Boolean mainThreadFlag = true;
    public static int retryTimes = R.integer.socket_connection_retry;
    private ServerSocket serverSocket;
    private static final int SERVER_PORT = 10086;
    private Socket client;

    public ServiceUsbConnection() {
        startConection();
    }

    private void startConection() {
        serverSocket = null;
        try {
            Thread socketServerThread = new Thread(new SocketServerThread());
            socketServerThread.start();
        } catch (Exception e) {
            Log.e(TAG, "Socket connection failed!", e);
        }
    }

    public void sendMsg(String msg) {
        Thread socketServerThread = new Thread(new SocketServerOutputThread(client, msg));
        socketServerThread.start();
    }

    private class SocketServerOutputThread extends Thread {

        private Socket hostThreadSocket;
        private int cnt;
        private String message;

        public SocketServerOutputThread(Socket socket, String msg) {
            hostThreadSocket = socket;
            message = msg;
        }

        @Override
        public void run() {
            OutputStream outputStream;

            try {
                if(serverSocket == null || serverSocket.isClosed()) {
                    startConection();
                }

                if (hostThreadSocket == null || hostThreadSocket.isClosed()) {
                    hostThreadSocket = serverSocket.accept();
                }
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(message);

            } catch (Exception e) {
                Log.e(TAG, "Message【"+message+"】sent failed!", e);
            }
        }
    }


    private class SocketServerThread extends Thread {
        int count = 0;
        @Override
        public void run() {
            try {
                // create ServerSocket using specified port
                serverSocket = new ServerSocket(SERVER_PORT);

                while (true) {
                    client = serverSocket.accept();
                    sendMsg("Your connection is established!");
                }
            } catch (IOException e) {
                Log.e(TAG, "Server initial failed!", e);
            }
        }
    }

    public void onDestroy() {

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if( client != null && !client.isClosed()) {
                client.isClosed();
            }
        } catch (Exception e) {
        }
        ;
    }


}
