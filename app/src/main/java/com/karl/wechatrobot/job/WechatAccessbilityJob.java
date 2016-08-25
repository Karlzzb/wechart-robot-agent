package com.karl.wechatrobot.job;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.karl.wechatrobot.ServiceWechat;
import com.karl.wechatrobot.utils.AccessibilityHelper;

import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class WechatAccessbilityJob extends BaseAccessbilityJob {

    private static final String TAG = "WechatAccessbilityJob";

    public static final String WECHAT_PACKAGENAME = "com.tencent.mm";


    private static final String BUTTON_CLASS_NAME = "android.widget.Button";


    private static final int USE_ID_MIN_VERSION = 700;// 6.3.8 对应code为680,6.3.9对应code为700

    private static final int WINDOW_NONE = 0;
    private static final int WINDOW_LUCKYMONEY_RECEIVEUI = 1;
    private static final int WINDOW_LUCKYMONEY_DETAIL = 2;
    private static final int WINDOW_LAUNCHER = 3;
    private static final int WINDOW_OTHER = -1;

    private int mCurrentWindow = WINDOW_NONE;

    private boolean isReceivingHongbao;
    private PackageInfo mWechatPackageInfo = null;
    private Handler mHandler = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatePackageInfo();
        }
    };

    @Override
    public void onCreateJob(ServiceWechat service) {
        super.onCreateJob(service);

        updatePackageInfo();

        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");

        getContext().registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onStopJob() {
        try {
            getContext().unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {}
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    @Override
    public String getTargetPackageName() {
        return WECHAT_PACKAGENAME;
    }

    @Override
    public void onReceiveJob(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        if(eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Parcelable data = event.getParcelableData();
            if(data == null || !(data instanceof Notification)) {
                return;
            }
            List<CharSequence> texts = event.getText();
            if(!texts.isEmpty()) {
                String text = String.valueOf(texts.get(0));
            }
        } else if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            windowStateHandle(event);
        } else if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            windowStateHandle(event);
        }
    }

    /** 是否为群聊天*/
    private boolean isMemberChatUi(AccessibilityNodeInfo nodeInfo) {
        if(nodeInfo == null) {
            return false;
        }
        String id = "com.tencent.mm:id/ces";
        int wv = getWechatVersion();
        if(wv <= 680) {
            id = "com.tencent.mm:id/ew";
        } else if(wv <= 700) {
            id = "com.tencent.mm:id/cbo";
        }
        String title = null;
        AccessibilityNodeInfo target = AccessibilityHelper.findNodeInfosById(nodeInfo, id);
        if(target != null) {
            title = String.valueOf(target.getText());
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("返回");

        if(list != null && !list.isEmpty()) {
            AccessibilityNodeInfo parent = null;
            for(AccessibilityNodeInfo node : list) {
                if(!"android.widget.ImageView".equals(node.getClassName())) {
                    continue;
                }
                String desc = String.valueOf(node.getContentDescription());
                if(!"返回".equals(desc)) {
                    continue;
                }
                parent = node.getParent();
                break;
            }
            if(parent != null) {
                parent = parent.getParent();
            }
            if(parent != null) {
                if( parent.getChildCount() >= 2) {
                    AccessibilityNodeInfo node = parent.getChild(1);
                    if("android.widget.TextView".equals(node.getClassName())) {
                        title = String.valueOf(node.getText());
                    }
                }
            }
        }


        return title != null && title.endsWith(")");
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void windowStateHandle(AccessibilityEvent event) {
        if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LUCKYMONEY_RECEIVEUI;
        } else if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName()) || "android.widget.ListView".equals(event.getClassName())) {
            AccessibilityNodeInfo source = event.getSource();
            List<AccessibilityNodeInfo> userlist = AccessibilityHelper.findNodeInfosByIdAll(source,"com.tencent.mm:id/baf");
            List<AccessibilityNodeInfo> moneylist = AccessibilityHelper.findNodeInfosByIdAll(source,"com.tencent.mm:id/baj");
            if (userlist == null || moneylist == null) {
                return;
            }
            String msg = "===============================\n";
            for (int i = 0; i < userlist.size(); i++) {
                userlist.get(i);
                if(userlist.get(i) != null) {
                    Log.d("userlist["+i+"]",userlist.get(i).toString());
                }
                msg += "【"+userlist.get(i).getText()+"】";

                        moneylist.get(i);
                if(moneylist.get(i) != null) {
                    Log.d("moneylist["+i+"]",moneylist.get(i).toString());
                }
                moneylist.get(i).getText();
                msg += "抽到【"+moneylist.get(i).getText()+"】\n";
            }
            Log.d("【luckmoney message】=",msg);
        } else if("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LAUNCHER;
        } else {
            mCurrentWindow = WINDOW_OTHER;
        }
    }

    private AccessibilityNodeInfo getListItemNodeInfo(AccessibilityNodeInfo source) {
        AccessibilityNodeInfo current = source;
        while (true) {
            AccessibilityNodeInfo parent = current.getParent();
            if(parent == null) {
                return null;
            }

            // Note: Recycle the infos
            AccessibilityNodeInfo oldCurrent = current;
            current = parent;
            oldCurrent.recycle();

        }
    }



    private Handler getHandler() {
        if(mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    /** 获取微信的版本*/
    private int getWechatVersion() {
        if(mWechatPackageInfo == null) {
            return 0;
        }
        return mWechatPackageInfo.versionCode;
    }

    /** 更新微信包信息*/
    private void updatePackageInfo() {
        try {
            mWechatPackageInfo = getContext().getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
