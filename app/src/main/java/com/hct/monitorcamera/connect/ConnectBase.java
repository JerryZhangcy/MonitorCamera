package com.hct.monitorcamera.connect;

import com.hct.monitorcamera.TransferProtocol;
import com.hct.monitorcamera.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class ConnectBase {
    protected SocketChannel mSocketChannel;
    protected LinkedBlockingQueue<SendMessage> mSendMessageQueue =
            new LinkedBlockingQueue<SendMessage>();

    protected static int CONNECT_TIME_OUT = 10;

    protected boolean mIsStoping = false;
    protected boolean mIsStarting = false;
    private boolean mNeedSleep = false;
    private InetSocketAddress mInetSocketAddress;

    protected boolean mIsConnected = false;

    protected Thread mSendThread;
    protected Thread mReceiveThread;
    protected Thread mConnectThread;
    protected SendRunnable mSendRunnable;
    protected ReceiveRunnable mReceiveRunnable;
    protected ConnectRunnable mConnectRunnable;
    private String mTagMessage = "";
    private byte[] mPictureData;

    public synchronized void connect(InetSocketAddress serverAddress) {
        if (mIsStarting) {
            stop();
        }
        mIsStarting = true;
        mIsStoping = false;
        mNeedSleep = false;
        mInetSocketAddress = serverAddress;

        mConnectRunnable = new ConnectRunnable();
        mConnectThread = new Thread(mConnectRunnable, "CONNECT_THREAD");
        synchronized (mConnectThread) {
            mConnectThread.start();
        }
    }

    private synchronized void start() throws Exception {
        mReceiveRunnable = new ReceiveRunnable();
        mReceiveThread = new Thread(mReceiveRunnable, "RECEIVE_THREAD");
        mReceiveThread.setDaemon(true);
        synchronized (mReceiveThread) {
            mReceiveThread.start();
            mReceiveThread.wait();
        }

        mSendRunnable = new SendRunnable();
        mSendThread = new Thread(mSendRunnable, "SEND_THREAD");
        mSendThread.setDaemon(true);
        synchronized (mSendThread) {
            mSendThread.start();
            mSendThread.wait();
        }
    }

    public synchronized void stop() {
        mIsStoping = true;
        mIsStarting = false;
        mNeedSleep = false;

        if (mSocketChannel != null) {
            try {
                mSocketChannel.socket().close();
            } catch (Exception e) {
            }

            try {
                mSocketChannel.close();
            } catch (Exception e) {
            }

        }
        mSocketChannel = null;

        if (mReceiveThread != null) {
            try {
                mReceiveThread.interrupt();
            } catch (Exception e) {
            }
        }

        if (mSendThread != null) {
            try {
                mSendThread.interrupt();
            } catch (Exception e) {
            }
        }
    }


    private void send(SendMessage msg) throws Exception {
        if (msg == null || !msg.isValid()) {
            return;
        }

        if (mSocketChannel == null
                || mSocketChannel.isOpen() == false
                || mSocketChannel.isConnected() == false) {
            return;
        }
        Util.e("send----->msg = " + msg.toString());
        ByteBuffer bb = ByteBuffer.wrap(msg.toBytes());
        while (bb.hasRemaining()) {
            mSocketChannel.write(bb);
        }

        mSocketChannel.socket().getOutputStream().flush();
    }

    public void sendMessage(SendMessage msg) {
        if (msg != null && msg.isValid()) {
            try {
                mSendMessageQueue.add(msg);
            } catch (Exception e) {
            }
        }
    }

    protected SendMessage takeMessage() {
        SendMessage message = null;
        try {
            message = mSendMessageQueue.take();
        } catch (Exception e) {

        }
        return message;
    }

    class SendRunnable implements Runnable {
        public void run() {
            synchronized (mSendThread) {
                mSendThread.notifyAll();
            }
            while (mIsStoping == false) {
                try {
                    SendMessage message = takeMessage();
                    if (message != null) {
                        send(message);
                    }
                } catch (Exception e) {

                }
            }
        }
    }

    public abstract void onReceiveMessage(SendMessage message);

    public abstract void onConnectSuccess();

    public abstract void onConnectFailed();

    public abstract void channelInactive();

    class ReceiveRunnable implements Runnable {

        @Override
        public void run() {
            synchronized (mReceiveThread) {
                mReceiveThread.notifyAll();
            }
            while (mIsStoping == false) {
                Util.d("ConnectBase--------->read");
                try {
                    receiveData();
                    mNeedSleep = false;
                } catch (SocketTimeoutException e) {
                    mNeedSleep = true;
                    Util.d("ConnectBase--------->SocketTimeoutException");
                } catch (ClosedChannelException e) {
                    Util.d("ConnectBase--------->ClosedChannelException");
                    mNeedSleep = true;
                    channelInactive();
                } catch (JSONException e) {
                    Util.d("ConnectBase--------->JSONException");
                } catch (Exception e) {
                    Util.d("ConnectBase--------->Exception " + e.toString());
                    if (e.toString().contains("Monitor_channelInactive")) {
                        mNeedSleep = true;
                        channelInactive();
                    }
                } catch (Throwable t) {
                    Util.d("ConnectBase---------> throwable Exception");
                } finally {
                    if (mNeedSleep) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                    }
                }
            }

            if (mSocketChannel != null) {
                try {
                    mSocketChannel.socket().close();
                } catch (Exception e) {
                }

                try {
                    mSocketChannel.close();
                } catch (Exception e) {
                }

                mSocketChannel = null;
            }
        }
    }

    private void receiveData() throws Exception {
        String content = "";
        ByteBuffer buf = ByteBuffer.allocate(1024);
        int bytesRead = mSocketChannel.read(buf);
        Util.d("ConnectBase--------->bytesRead = " + bytesRead);
        if (bytesRead == 0) {

        } else if (bytesRead < 0) {
            stop();//服务端关闭连接时会走此case
            throw new Exception("Monitor_channelInactive");
        }

        dispatchMessage(buf, bytesRead);
    }

    class ConnectRunnable implements Runnable {

        @Override
        public void run() {
            try {
                mSocketChannel = SocketChannel.open();
                mSocketChannel.configureBlocking(true);
                mSocketChannel.socket().connect(mInetSocketAddress,
                        1000 * CONNECT_TIME_OUT);
                mSocketChannel.socket().setSoTimeout(1000 * 5);
                mIsConnected = true;
            } catch (SocketTimeoutException e) {
                Util.d("ConnectBase----ConnectRunnable----->SocketTimeoutException");
                e.printStackTrace();
                onConnectFailed();
                mIsConnected = false;
                mIsStarting = false;
            } catch (IOException e) {
                Util.d("ConnectBase-----ConnectRunnable---->connect failed");
                e.printStackTrace();
                onConnectFailed();
                mIsConnected = false;
                mIsStarting = false;
            }

            if (mIsConnected) {
                try {
                    start();
                } catch (Exception e) {
                    Util.d("ConnectBase--------->rev or send thread start failed");
                    e.printStackTrace();
                    mIsStarting = false;
                }
                if (mIsStarting) {
                    onConnectSuccess();
                }
            }
        }
    }

    private void dispatchMessage(ByteBuffer buf, int bytesRead) {
        String tag = TransferProtocol.UNKNOW;
        String content = "";
        content += new String(buf.array());
        buf.clear();
        if (content.equals("")) {
            return;
        }
        content = content.substring(0, bytesRead);
        Util.d("dispatchMessage--------->content = " + content);
        if (content.contains(TransferProtocol.DELIMITER)) {
            String[] items = content.split(TransferProtocol.DELIMITER_DIV);
            String head = items[0];
            String value = items[1];
            Util.d("dispatchMessage--------->head = " + head + " value = " + value);
            if (TransferProtocol.FILE_LIST.equals(head)) {
                tag = TransferProtocol.FILE_LIST;
                if (items.length == 2 || items.length == 3) {//2表示消息不包含结束字符 3表示消息包含结束符
                    mTagMessage += items[1];
                }
            } else if (TransferProtocol.TIME.equals(head)) {
                tag = TransferProtocol.TIME;
                if (items.length == 2 || items.length == 3) {//2表示消息不包含结束字符 3表示消息包含结束符
                    mTagMessage += items[1];
                }
            } else if (TransferProtocol.PICTURE.equals(head)) {
                tag = TransferProtocol.PICTURE;
                if (items.length == 2 || items.length == 3) {//2表示消息不包含结束字符 3表示消息包含结束符
                    mTagMessage += items[1];
                }
            }

            if (items.length == 3) {
                SendMessage msg = new SendMessage(mTagMessage, tag);
                if (!msg.isValid()) {
                    return;
                }
                Util.d("dispatchMessage--------->msg = " + msg.toString() + " tag = " + tag);
                onReceiveMessage(msg);
                mTagMessage = "";
            }
        }
    }
}
