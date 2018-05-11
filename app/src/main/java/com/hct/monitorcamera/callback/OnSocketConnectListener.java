package com.hct.monitorcamera.callback;


public interface OnSocketConnectListener {
    void onConnectSuccess();

    void onConnectFailed();

    void channelInactive();

    void onHandlerMessage(String msg);
}
