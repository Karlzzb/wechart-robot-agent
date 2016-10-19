package com.karl.wechatrobot;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceBroadcastReceiver extends BroadcastReceiver {

    private static String START_ACTION = "NotifyServiceStart";
    private static String STOP_ACTION = "NotifyServiceStop";

    @Override
    public void onReceive(Context context, Intent intent) {
//		Log.d(ServiceWechat.TAG, Thread.currentThread().getName() + "---->"
//                + "ServiceBroadcastReceiver onReceive");
//
//        String action = intent.getAction();
//        if (START_ACTION.equalsIgnoreCase(action)) {
//            context.startService(new Intent(context, ServiceWechat.class));
//            Log.d(ServiceWechat.TAG, Thread.currentThread().getName() + "---->"
//                    + "ServiceBroadcastReceiver onReceive start end");
//        } else if (STOP_ACTION.equalsIgnoreCase(action)) {
//            context.stopService(new Intent(context, ServiceUsbConnection.class));
//            Log.d(ServiceWechat.TAG, Thread.currentThread().getName() + "---->"
//                    + "ServiceBroadcastReceiver onReceive stop end");
//        }

        boolean isServiceRunning = false;

        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
            //检查Service状态
            ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);

            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {

                if ("com.karl.wechatrobot.ServiceWechat".equals(service.service.getClassName()))
                {
                    isServiceRunning = true;
                }
            }
            if (!isServiceRunning) {
                Intent i = new Intent(context, ServiceWechat.class);
                context.startService(i);
            }
        }
    }

}
