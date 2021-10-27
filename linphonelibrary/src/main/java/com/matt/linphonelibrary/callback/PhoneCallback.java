package com.matt.linphonelibrary.callback;

import org.linphone.core.Call;


/**
 * File Name: PhoneCallback.java
 * Crete By: 19-7-31 上午10:40
 * Description: 通话状态回调
 * Author: lik
 * Modify Date:
 * Modifier Author:
 */
/**
 * @ Author : 廖健鹏
 * @ Time : 2021/6/23
 * @ e-mail : 329524627@qq.com
 * @ Description :
 */
public abstract class PhoneCallback {
    /**
     * 来电状态
     *
     * @param call
     */
    public void incomingCall(Call call) {
    }

    /**
     * 呼叫初始化
     */
    public void outgoingInit(Call call) {
    }

    /**
     * 电话接通
     */
    public void callConnected(Call call) {
    }

    /**
     * 电话挂断
     */
    public void callEnd(Call call) {
    }

    /**
     * 释放通话
     */
    public void callReleased(Call call) {
    }

    /**
     * 连接失败
     */
    public void error(String string) {
    }


}
