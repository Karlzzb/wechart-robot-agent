package com.karl.wechatrobot.job;

import android.content.Context;
import com.karl.wechatrobot.ServiceWechat;

/**
 * <p>Created 16/1/16 上午12:38.</p>
 * <p><a href="mailto:codeboy2013@gmail.com">Email:codeboy2013@gmail.com</a></p>
 * <p><a href="http://www.happycodeboy.com">LeonLee Blog</a></p>
 *
 * @author LeonLee
 */
public abstract class BaseAccessbilityJob implements AccessbilityJob {

    private ServiceWechat service;

    @Override
    public void onCreateJob(ServiceWechat service) {
        this.service = service;
    }

    public Context getContext() {
        return service.getApplicationContext();
    }

    public ServiceWechat getService() {
        return service;
    }
}
