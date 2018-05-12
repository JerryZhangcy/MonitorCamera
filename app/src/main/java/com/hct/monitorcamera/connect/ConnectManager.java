package com.hct.monitorcamera.connect;


import com.hct.monitorcamera.Util;
import com.hct.monitorcamera.callback.OnSocketConnectListener;

import java.net.InetSocketAddress;

public class ConnectManager {

    private static ConnectManager mInstance;
    private ConnectBase mConnection;
    private OnSocketConnectListener mOnSocketConnectListener;
    public static int CURRENT_CONNECTED = 0;
    public static int CURRENT_DISCONNECTED = 1;
    private int mConnectState = CURRENT_DISCONNECTED;

    public synchronized static ConnectManager getInstance() {
        if (mInstance == null) {
            mInstance = new ConnectManager();
        }
        return mInstance;
    }

    private ConnectManager() {
        init();
    }

    private void init() {
        Util.d("ConnectManager--------->init");
        mConnection = new ConnectBase() {
            @Override
            public void onReceiveMessage(SendMessage message) {
                if (message.isValid()) {
                    Util.d("ConnectManager-----onReceiveMessage------->message = "
                            + message.toString());
                    if (mOnSocketConnectListener != null) {
                        mOnSocketConnectListener.onHandlerMessage(message);
                    }
                }
            }

            @Override
            public void onConnectSuccess() {
                Util.d("ConnectManager-----onConnectSuccess------->");
                mConnectState = CURRENT_CONNECTED;
                if (mOnSocketConnectListener != null) {
                    mOnSocketConnectListener.onConnectSuccess();
                }
            }

            @Override
            public void onConnectFailed() {
                Util.d("ConnectManager-----onConnectFailed------->");
                mConnectState = CURRENT_DISCONNECTED;
                if (mOnSocketConnectListener != null) {
                    mOnSocketConnectListener.onConnectFailed();
                }
            }

            @Override
            public void channelInactive() {
                Util.d("ConnectManager-----channelInactive------->");
                mConnectState = CURRENT_DISCONNECTED;
                if (mOnSocketConnectListener != null) {
                    mOnSocketConnectListener.channelInactive();
                }
            }
        };
    }

    public void connect(InetSocketAddress serverAddress) {
        Util.d("ConnectManager--------->startConnect = " + serverAddress.toString());
        if (mConnection != null)
            mConnection.connect(serverAddress);
    }

    public void disconnect() {
        if (mConnection != null) {
            mConnection.stop();
        }
    }

    public void sendMessage(SendMessage msg) {
        if (mConnection != null) {
            mConnection.sendMessage(msg);
        }
    }

    public void setLocationSocketConnectListener(OnSocketConnectListener listener) {
        mOnSocketConnectListener = listener;
    }

    public int getConnectState() {
        return mConnectState;
    }
}
