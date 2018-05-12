package com.hct.monitorcamera.callback;


import com.hct.monitorcamera.connect.SendMessage;

public interface OnSocketConnectListener {
    void onConnectSuccess();

    void onConnectFailed();

    void channelInactive();

    void onHandlerMessage(SendMessage msg);
}
