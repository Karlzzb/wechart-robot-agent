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

import com.karl.wechatrobot.ServiceUsbConnection;
import com.karl.wechatrobot.ServiceWechat;
import com.karl.wechatrobot.utils.AccessibilityHelper;
import com.karl.wechatrobot.utils.PackageDomain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WechatAccessbilityJob extends BaseAccessbilityJob {

    private static final String TAG = "WechatAccessbilityJob";

    public static final String WECHAT_PACKAGENAME = "com.tencent.mm";


    private static final String BUTTON_CLASS_NAME = "android.widget.Button";

    private Map<String, PackageDomain> currentPackageInfo;

    private List<String> currentPackageTextList;

    private Integer currentPackageSize;


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
    public void onCreateJob(ServiceWechat service, ServiceUsbConnection usbConnection) {
        super.onCreateJob(service, usbConnection);
        currentPackageInfo = new LinkedHashMap<>();
        currentPackageTextList = new ArrayList<String>();
        currentPackageSize = 0;

        updatePackageInfo();

        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");

        getContext().registerReceiver(broadcastReceiver, filter);
        this.usbConnection = usbConnection;
    }

    @Override
    public void onStopJob() {
        try {
            getContext().unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
        }
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
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Parcelable data = event.getParcelableData();
            if (data == null || !(data instanceof Notification)) {
                return;
            }
            List<CharSequence> texts = event.getText();
            if (!texts.isEmpty()) {
                String text = String.valueOf(texts.get(0));
            }
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            windowStateHandle(event);
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            windowStateHandle(event);
        }
    }

    /**
     * 是否为群聊天
     */
    private boolean isMemberChatUi(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return false;
        }
        String id = "com.tencent.mm:id/ces";
        int wv = getWechatVersion();
        if (wv <= 680) {
            id = "com.tencent.mm:id/ew";
        } else if (wv <= 700) {
            id = "com.tencent.mm:id/cbo";
        }
        String title = null;
        AccessibilityNodeInfo target = AccessibilityHelper.findNodeInfosById(nodeInfo, id);
        if (target != null) {
            title = String.valueOf(target.getText());
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("返回");

        if (list != null && !list.isEmpty()) {
            AccessibilityNodeInfo parent = null;
            for (AccessibilityNodeInfo node : list) {
                if (!"android.widget.ImageView".equals(node.getClassName())) {
                    continue;
                }
                String desc = String.valueOf(node.getContentDescription());
                if (!"返回".equals(desc)) {
                    continue;
                }
                parent = node.getParent();
                break;
            }
            if (parent != null) {
                parent = parent.getParent();
            }
            if (parent != null) {
                if (parent.getChildCount() >= 2) {
                    AccessibilityNodeInfo node = parent.getChild(1);
                    if ("android.widget.TextView".equals(node.getClassName())) {
                        title = String.valueOf(node.getText());
                    }
                }
            }
        }


        return title != null && title.endsWith(")");
    }

    /**
     * 拆红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void getPacket(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return;
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("看看大家的手气");
        for (AccessibilityNodeInfo n : list) {
            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void windowStateHandle(AccessibilityEvent event) {
        if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LUCKYMONEY_RECEIVEUI;
            getPacket(event.getSource());
        } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName()) || "android.widget.ListView".equals(event.getClassName())) {
            AccessibilityNodeInfo source = event.getSource();
            if (source == null) {
                Log.i(TAG, "noteInfo is　null");
                return;
            } else {
                currentPackageTextList.clear();
                recycle(source);
            }
            reviewPackageRaw();


            if (currentPackageInfo == null || currentPackageSize.compareTo(0) == 0 || currentPackageInfo.size() < currentPackageSize) {
                return;
            }
            String msg = "{\"LuckPeople\" : [";
            int i = 0;
            for (String key : currentPackageInfo.keySet()) {
                if (currentPackageInfo.get(key) != null) {
                    msg += "{\"RemarkName\" : \"" + key + "\",";
                    msg += "\"Time\" : \"" + currentPackageInfo.get(key).getTimeStr() + "\",";
                    msg += "\"Money\": \"" + currentPackageInfo.get(key).getMoneyStr() + "\"}";
                }
                msg += i == currentPackageInfo.keySet().size() - 1 ? "" : ",";
                i++;
            }
            msg += "]}";
            Log.i("【luckmoney message】=", msg);
            //// TODO: 2016/9/24
            usbConnection.sendMsg(msg);
            currentPackageInfo.clear();
        } else if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LAUNCHER;
        } else {
            mCurrentWindow = WINDOW_OTHER;
        }
    }

    private void recycle(AccessibilityNodeInfo info) {
        if (info.getChildCount() == 0) {
        } else {
            AccessibilityNodeInfo subInfo = null;
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    subInfo = info.getChild(i);
                    Log.d(TAG, "child widget----------------------------" + i + "|" + subInfo.getClassName());
                    Log.d(TAG, "showDialog:i" + i + "|" + subInfo.canOpenPopup());
                    Log.d(TAG, "Text：i " + i + "|" + subInfo.getText());
                    Log.d(TAG, "windowId: i " + i + "|" + subInfo.getWindowId());
                    if (subInfo.getText() != null && !subInfo.getText().toString().isEmpty()) {
                        currentPackageTextList.add(subInfo.getText().toString());
                    }
                    recycle(info.getChild(i));
                }
            }
        }
    }

    private void reviewPackageRaw() {
        if (currentPackageTextList == null || currentPackageTextList.size() < 1) {
            return;
        }
        String text = "";
        for (int i = 0; i < currentPackageTextList.size(); i++) {
            text = currentPackageTextList.get(i);
            if (text == null || text.isEmpty()) {
                continue;
            }
            Pattern patternSize = Pattern
                    .compile("^([0-9]+)个红包.*([0-9]+).*被抢光$");
            Matcher matcherSize = patternSize.matcher(text);
            if (matcherSize.matches()) {
                this.currentPackageSize = Integer.valueOf(matcherSize.group(1));
                Log.i(TAG, "The current packageSize is：" + text);
                continue;
            }
            Pattern patternMony = Pattern
                    .compile("^([0-9]+\\.[0-9]+)元$");
            Matcher matcherMoney = patternMony.matcher(text);
            if (matcherMoney.matches()) {
                if (!currentPackageInfo.containsKey(currentPackageTextList.get(i - 2))) {
                    this.currentPackageInfo.put(currentPackageTextList.get(i - 2), new PackageDomain(currentPackageTextList.get(i - 2).toString(), String.valueOf(matcherMoney.group(1)), currentPackageTextList.get(i - 1).toString()));
                    Log.i(TAG, "User Package info ：Name=" + currentPackageTextList.get(i - 2).toString() + ", Money=" + matcherMoney.group(1));
                }
            }

        }
    }

    private AccessibilityNodeInfo getListItemNodeInfo(AccessibilityNodeInfo source) {
        AccessibilityNodeInfo current = source;
        while (true) {
            AccessibilityNodeInfo parent = current.getParent();
            if (parent == null) {
                return null;
            }

            // Note: Recycle the infos
            AccessibilityNodeInfo oldCurrent = current;
            current = parent;
            oldCurrent.recycle();

        }
    }


    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    /**
     * 获取微信的版本
     */
    private int getWechatVersion() {
        if (mWechatPackageInfo == null) {
            return 0;
        }
        return mWechatPackageInfo.versionCode;
    }

    /**
     * 更新微信包信息
     */
    private void updatePackageInfo() {
        try {
            mWechatPackageInfo = getContext().getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

}
