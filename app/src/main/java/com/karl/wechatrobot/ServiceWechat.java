package com.karl.wechatrobot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.karl.wechatrobot.job.AccessbilityJob;
import com.karl.wechatrobot.job.WechatAccessbilityJob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class ServiceWechat extends AccessibilityService {
    private static final String TAG = "WeRobot";

//    private static ServiceWechat service;
    PendingIntent mPendingIntent = null;

    AlarmManager mAlarmManager = null;

    AccessibilityNodeInfo itemNodeinfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScontent() {
        return scontent;
    }

    public void setScontent(String scontent) {
        this.scontent = scontent;
    }

    private String name;
    private String scontent;

    private static final Class[] ACCESSBILITY_JOBS= {
            WechatAccessbilityJob.class
    };

    private List<AccessbilityJob> mAccessbilityJobs;
    private HashMap<String, AccessbilityJob> mPkgAccessbilityJobMap;
    private ServiceUsbConnection usbConnection;


    @Override
    public void onCreate() {
        super.onCreate();

//        usbConnection = new ServiceUsbConnection();
        mAccessbilityJobs = new ArrayList<>();
        mPkgAccessbilityJobMap = new HashMap<>();

        for(Class clazz : ACCESSBILITY_JOBS) {
            try {
                Object object = clazz.newInstance();
                if(object instanceof AccessbilityJob) {
                    AccessbilityJob job = (AccessbilityJob) object;
                    job.onCreateJob(this,usbConnection);
                    mAccessbilityJobs.add(job);
                    mPkgAccessbilityJobMap.put(job.getTargetPackageName(), job);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        //start the service through alarm repeatly
        Intent intent = new Intent(getApplicationContext(), ServiceWechat.class);
        mAlarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        mPendingIntent = PendingIntent.getService(this, 0, intent, Intent.FILL_IN_ACTION);
        long now = System.currentTimeMillis();
        mAlarmManager.setInexactRepeating(AlarmManager.RTC, now, 60000, mPendingIntent);

    }

    /**
     * 寻找窗体中的“发送”按钮，并且点击。
     */
    public void send() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByText("发送");
            if (list != null && list.size() > 0) {
                for (AccessibilityNodeInfo n : list) {
                    if(n.getClassName().equals("android.widget.Button") && n.isEnabled()){
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }

            } else {
                List<AccessibilityNodeInfo> liste = nodeInfo
                        .findAccessibilityNodeInfosByText("Send");
                if (liste != null && liste.size() > 0) {
                    for (AccessibilityNodeInfo n : liste) {
                        if(n.getClassName().equals("android.widget.Button") && n.isEnabled()){
                            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
            }
            pressBackButton();
        }
    }

    /**
     * 模拟back按键
     */
    private void pressBackButton(){
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("input keyevent " + KeyEvent.KEYCODE_BACK);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    public boolean fill(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            return findEditText(rootNode, content);
        }
        return false;
    }

    public boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
        int count = rootNode.getChildCount();
        android.util.Log.d("maptrix", "root class=" + rootNode.getClassName() + ","+ rootNode.getText()+","+count);
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) {
                android.util.Log.d("maptrix", "nodeinfo = null");
                continue;
            }
            android.util.Log.i("maptrix", "class=" + nodeInfo.getClassName());
            android.util.Log.i("maptrix", "ds=" + nodeInfo.getContentDescription());

            if ("android.widget.EditText".equals(nodeInfo.getClassName())) {
                android.util.Log.i("maptrix", "==================");
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                        true);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                        arguments);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", content);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                return true;
            }

            if (findEditText(nodeInfo, content)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        Log.d(TAG, "【Capture event】" + event );

        String pkn = String.valueOf(event.getPackageName());
        if(mAccessbilityJobs != null && !mAccessbilityJobs.isEmpty()) {
            for (AccessbilityJob job : mAccessbilityJobs) {
                if(pkn.equals(job.getTargetPackageName()) && job.isEnable()) {
                    job.onReceiveJob(event);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service destory");
        if(mPkgAccessbilityJobMap != null) {
            mPkgAccessbilityJobMap.clear();
        }
        if(mAccessbilityJobs != null && !mAccessbilityJobs.isEmpty()) {
            for (AccessbilityJob job : mAccessbilityJobs) {
                job.onStopJob();
            }
            mAccessbilityJobs.clear();
        }

        mAccessbilityJobs = null;
        mPkgAccessbilityJobMap = null;
        //发送广播，已经断开辅助服务
//        Intent intent = new Intent(onfig.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT);
//        sendBroadcast(intent);
//        Intent localIntent = new Intent();
//        localIntent.setClass(this, ServiceWechat.class);
//        startService(localIntent);
        usbConnection.onDestroy();
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "service interrupt");
        Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
//        service = this;
        //发送广播，已经连接上了
//        Intent intent = new Intent(Config.ACTION_QIANGHONGBAO_SERVICE_CONNECT);
//        sendBroadcast(intent);
//        Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
    }

    /**
     * 判断当前服务是否正在运行
     * */
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//    public static boolean isRunning() {
//        if(service == null) {
//            return false;
//        }
//        AccessibilityManager accessibilityManager = (AccessibilityManager) service.getSystemService(Context.ACCESSIBILITY_SERVICE);
//        AccessibilityServiceInfo info = service.getServiceInfo();
//        if(info == null) {
//            return false;
//        }
//        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
//        Iterator<AccessibilityServiceInfo> iterator = list.iterator();
//
//        boolean isConnect = false;
//        while (iterator.hasNext()) {
//            AccessibilityServiceInfo i = iterator.next();
//            if(i.getId().equals(info.getId())) {
//                isConnect = true;
//                break;
//            }
//        }
//        return isConnect;
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("ServiceWechat","startCommand");
        return START_STICKY;
        //return super.onStartCommand(intent, flags, startId);
    }



}
