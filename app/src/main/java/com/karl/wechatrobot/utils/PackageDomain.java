package com.karl.wechatrobot.utils;

/**
 * Created by kevin on 2016/9/27.
 */
public class PackageDomain {
    private String remarkName;
    private String moneyStr;
    private String timeStr;

    public PackageDomain(String remarkName, String moneyStr, String timeStr) {
        this.remarkName = remarkName;
        this.moneyStr = moneyStr;
        this.timeStr = timeStr;
    }

    public String getTimeStr() {
        return timeStr;
    }

    public void setTimeStr(String timeStr) {
        this.timeStr = timeStr;
    }


    public String getRemarkName() {
        return remarkName;
    }

    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }

    public String getMoneyStr() {
        return moneyStr;
    }

    public void setMoneyStr(String moneyStr) {
        this.moneyStr = moneyStr;
    }

}
