package com.karl.wechatrobot.job;

import android.view.accessibility.AccessibilityEvent;

import com.karl.wechatrobot.ServiceUsbConnection;
import com.karl.wechatrobot.ServiceWechat;

/**
 * <p>Created 16/1/16 上午12:32.</p>
 * <p><a href="mailto:codeboy2013@gmail.com">Email:codeboy2013@gmail.com</a></p>
 * <p><a href="http://www.happycodeboy.com">LeonLee Blog</a></p>
 *
 * @author LeonLee
 */
public interface AccessbilityJob {
    String getTargetPackageName();
    void onCreateJob(ServiceWechat service, ServiceUsbConnection usbConnection);
    void onReceiveJob(AccessibilityEvent event);
    void onStopJob();
    boolean isEnable();
}
