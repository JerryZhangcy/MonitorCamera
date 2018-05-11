package com.hct.monitorcamera.callback;

import org.json.JSONObject;

public interface OnReceiverListener {
    void handleReceiveMessage(JSONObject jsonObject);
    void channelInactive();
}
