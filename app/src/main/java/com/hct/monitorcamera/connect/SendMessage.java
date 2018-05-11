package com.hct.monitorcamera.connect;

import com.hct.monitorcamera.TransferProtocol;

public class SendMessage {
    public String message;
    private byte[] mSendBytes;
    private String mTag = TransferProtocol.UNKNOW;

    public SendMessage(String msg, String tag) {
        if (msg != null) {
            message = msg;
            mSendBytes = message.toString().getBytes();
        }
        mTag = tag;
    }

    public byte[] toBytes() {
        return mSendBytes;
    }

    public boolean isValid() {
        return (mSendBytes != null && mSendBytes.length != 0);
    }

    public String getTag() {
        return mTag;
    }

    @Override
    public String toString() {
        return message;
    }
}
