package com.karl.wechatrobot.job;

import android.content.Context;

import com.karl.wechatrobot.ServiceUsbConnection;
import com.karl.wechatrobot.ServiceWechat;

/**
 * <p>Created 16/1/16 上午12:38.</p>
 * <p><a href="mailto:codeboy2013@gmail.com">Email:codeboy2013@gmail.com</a></p>
 * <p><a href="http://www.happycodeboy.com">LeonLee Blog</a></p>
 *
 * @author LeonLee
 */
public abstract class BaseAccessbilityJob implements AccessbilityJob {

    protected ServiceWechat service;
//    protected ServiceUsbConnection usbConnection;

    @Override
    public void onCreateJob(ServiceWechat service, ServiceUsbConnection usbConnection) {
        this.service = service;
//        this.usbConnection = usbConnection;
    }

    public Context getContext() {
        return service.getApplicationContext();
    }

    public ServiceWechat getService() {
        return service;
    }
}
