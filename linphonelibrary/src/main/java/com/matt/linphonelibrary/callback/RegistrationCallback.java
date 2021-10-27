package com.matt.linphonelibrary.callback;

/**
  * File Name: RegistrationCallback.java
  * Crete By: 19-7-31 上午10:41
  * Description: 注册状态接口回调
  * Author: lik
  * Modify Date: 
  * Modifier Author:
  */

public abstract class RegistrationCallback {
    public void registrationNone() {}

    public void registrationProgress() {}

    public void registrationOk() {}

    public void registrationCleared() {}

    public void registrationFailed() {}
}
