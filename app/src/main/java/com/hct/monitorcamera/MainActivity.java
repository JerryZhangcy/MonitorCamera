package com.hct.monitorcamera;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.hct.monitorcamera.File.FileInfo;
import com.hct.monitorcamera.File.FileSave;
import com.hct.monitorcamera.File.FileUtil;
import com.hct.monitorcamera.callback.OnSocketConnectListener;
import com.hct.monitorcamera.connect.ConnectManager;
import com.hct.monitorcamera.connect.SendMessage;
import com.hct.monitorcamera.wifi.MonitorcameraWifiManager;

import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class MainActivity extends Activity implements View.OnClickListener, OnSocketConnectListener {

    private static final int UPDATE_DEVICE_STATE = 0x1000;
    private static final int UPDATE_RECV_FILElIST_MSG = 0x1001;
    private static final int UPDATE_WIFI_STATE = 0X1002;
    private static final int UPDATE_RECV_TIME_MSG = 0x1003;
    private static final int UPDATE_RECV_PICTURER_MSG = 0x1004;

    private Button mConnectWifi, mConnectDevice, mDisConnectDevice, mSend;
    private EditText mSsid, mPws, mIp, mPort, mSendMsg;
    private ListView mListView;
    private FileListAdapter mFileListAdapter;

    private static final int PERMISSIONS_REQUEST = 1;
    private boolean mIsWifiConnected = false;
    private NetworkInfo.DetailedState mLastState = NetworkInfo.DetailedState.IDLE;

    private WifiConnectChangedReceiver mWifiConnectChangedReceiver = new WifiConnectChangedReceiver();

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.CAMERA,//照相权限
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private Handler mHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_DEVICE_STATE:
                    if (ConnectManager.getInstance().getConnectState()
                            == ConnectManager.CURRENT_CONNECTED) {
                        mConnectDevice.setEnabled(false);
                        mDisConnectDevice.setEnabled(true);
                        Util.toast(R.string.device_connect_success);
                    } else if (ConnectManager.getInstance().getConnectState()
                            == ConnectManager.CURRENT_DISCONNECTED) {
                        mConnectDevice.setEnabled(true);
                        mDisConnectDevice.setEnabled(false);
                    }
                    break;
                case UPDATE_RECV_FILElIST_MSG:
                    mFileListAdapter.updateAdapter(FileUtil.getInstance().getPictureNameList());
                    break;
                case UPDATE_WIFI_STATE:
                    NetworkInfo.DetailedState currentdState = (NetworkInfo.DetailedState) msg.obj;
                    if (currentdState != null) {
                        switch (currentdState) {
                            case CONNECTED:
                                if (msg.arg1 == 1) {
                                    if (ConnectManager.getInstance().getConnectState()
                                            == ConnectManager.CURRENT_CONNECTED) {
                                        mConnectWifi.setText(R.string.wifi_connect_success);
                                        mConnectWifi.setEnabled(false);
                                        mConnectDevice.setEnabled(false);
                                    } else {
                                        mConnectWifi.setText(R.string.wifi_connect_success);
                                        mConnectWifi.setEnabled(false);
                                        mConnectDevice.setEnabled(true);
                                    }
                                } else {
                                    mConnectWifi.setText(R.string.connect_wifi);
                                    mConnectWifi.setEnabled(true);
                                    mConnectDevice.setEnabled(false);
                                    mDisConnectDevice.setEnabled(false);
                                }
                                break;
                            case DISCONNECTED:
                                mConnectWifi.setText(R.string.connect_wifi);
                                mConnectWifi.setEnabled(true);
                                mConnectDevice.setEnabled(false);
                                mDisConnectDevice.setEnabled(false);
                                break;
                            case OBTAINING_IPADDR:
                                mConnectWifi.setText(R.string.Wifi_OBTAINING_IPADDR);
                                mConnectWifi.setEnabled(false);
                                mConnectDevice.setEnabled(false);
                                mDisConnectDevice.setEnabled(false);
                                break;
                            case CONNECTING:
                                mConnectWifi.setText(R.string.Wifi_AUTHENTICATING);
                                mConnectWifi.setEnabled(false);
                                mConnectDevice.setEnabled(false);
                                mDisConnectDevice.setEnabled(false);
                                break;
                            case AUTHENTICATING:
                                mConnectWifi.setText(R.string.Wifi_AUTHENTICATING);
                                mConnectWifi.setEnabled(false);
                                mConnectDevice.setEnabled(false);
                                mDisConnectDevice.setEnabled(false);
                                break;
                        }
                    }
                    break;
                case UPDATE_RECV_TIME_MSG:
                    String value = (String) msg.obj;
                    int id = R.string.calibration_time_success;
                    if (TransferProtocol.ERROR.equals(value)) {
                        id = R.string.calibration_time_fail;
                    }
                    Util.toast(id);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkPermissions(
                this, PERMISSIONS_STORAGE)) {
            initFun();
            initView();
        } else {
            requestPermissions(
                    PERMISSIONS_STORAGE, PERMISSIONS_REQUEST);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerWifiConnectChangeReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unRegisterWifiConnectChangeReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConnectManager.getInstance().disconnect();
        FileSave.getInstance().stopThread();
    }

    private boolean checkPermissions(Context context, String[] permissions) {
        for (int i = 0; i < permissions.length; i++) {
            if (!hasPermission(context, permissions[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission(Context context, String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String permissions[], final int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST:
                if (checkPermissions(this, PERMISSIONS_STORAGE)) {
                    initFun();
                    initView();
                } else {
                    Util.toast(R.string.permission_miss);
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                    finish();
                }
                break;
        }
    }

    private void initView() {
        mConnectWifi = (Button) findViewById(R.id.connect_wifi);
        mConnectWifi.setOnClickListener(this);

        mConnectDevice = (Button) findViewById(R.id.connect_device);
        mConnectDevice.setOnClickListener(this);

        mDisConnectDevice = (Button) findViewById(R.id.disconnect_device);
        mDisConnectDevice.setOnClickListener(this);

        mSsid = (EditText) findViewById(R.id.ssid);
        mSsid.setText(Constant.WIFI_SSID);
        mPws = (EditText) findViewById(R.id.pws);
        mPws.setText(Constant.WIFI_PASSWORDS);
        mIp = (EditText) findViewById(R.id.ip);
        mIp.setText(Constant.MONITORCAMERA_IP);
        mPort = (EditText) findViewById(R.id.port);
        mPort.setText(String.valueOf(Constant.MONITORCAMERA_PORT));

        mSendMsg = (EditText) findViewById(R.id.send_msg);
        mSend = (Button) findViewById(R.id.send);
        mSend.setOnClickListener(this);

        mListView = (ListView) findViewById(R.id.filelist);
        if (mFileListAdapter != null)
            mListView.setAdapter(mFileListAdapter);
    }

    private void initFun() {
        ConnectManager connectManager = ConnectManager.getInstance();
        connectManager.setLocationSocketConnectListener(this);

        mFileListAdapter = new FileListAdapter(this);

        FileSave.getInstance().startThread();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_wifi:
                MonitorcameraWifiManager.getInstance().startConnectWifi();
                mConnectWifi.setEnabled(false);
                break;
            case R.id.connect_device:
                InetSocketAddress inetSocketAddress
                        = new InetSocketAddress(Constant.MONITORCAMERA_IP, Constant.MONITORCAMERA_PORT);
                ConnectManager.getInstance().connect(inetSocketAddress);
                mConnectDevice.setEnabled(false);
                break;
            case R.id.disconnect_device:
                ConnectManager.getInstance().disconnect();
                break;
            case R.id.send:

                String value = mSendMsg.getText().toString();
                if (!TextUtils.isEmpty(value)) {
                    if (ConnectManager.getInstance().getConnectState()
                            == ConnectManager.CURRENT_CONNECTED) {
                        ConnectManager.getInstance().sendMessage(new SendMessage(value,
                                TransferProtocol.UNKNOW));
                    }
                } else {
                    Util.toast("发送的数据不能为空");
                }
                break;
        }
    }

    @Override
    public void onConnectSuccess() {
        Util.d("--------onConnectSuccess");
        mHander.obtainMessage(UPDATE_DEVICE_STATE).sendToTarget();
    }

    @Override
    public void onConnectFailed() {
        Util.d("--------onConnectFailed");
        mHander.obtainMessage(UPDATE_DEVICE_STATE).sendToTarget();
    }

    @Override
    public void channelInactive() {
        Util.d("--------channelInactive");
        mHander.obtainMessage(UPDATE_DEVICE_STATE).sendToTarget();
    }

    @Override
    public void onHandlerMessage(final SendMessage msg) {
        Util.d("--------onHandlerMessage  msg = " + msg);
        String tag = msg.getTag();
        if (TransferProtocol.FILE_LIST.equals(tag)) {
            String[] listValue = msg.message.split(TransferProtocol.DELIMITER_DIV);
            for (String item : listValue) {
                FileUtil.getInstance().addPictureNameList(item);
            }
            mHander.obtainMessage(UPDATE_RECV_FILElIST_MSG).sendToTarget();
        } else if (TransferProtocol.TIME.equals(tag)) {
            Message message = new Message();
            message.obj = msg.message;
            message.what = UPDATE_RECV_TIME_MSG;
            mHander.sendMessage(message);
        } else if (TransferProtocol.PICTURE.equals(tag)) {
            if (TransferProtocol.ERROR.equals(msg.message)) {
                return;
            }

            FileUtil.getInstance().writeCaching(msg.message);

            byte[] data = Util.toBytes(msg.message);
            String fileName = "20180525.jpg";//FileUtil.getInstance().
            //getPictureNameAtPostion(FileUtil.mCurrentPictureNum);
            FileSave.getInstance().addFileToQueue(new FileInfo(data, fileName));
            FileUtil.mCurrentPictureNum++;
        }
    }

    private void registerWifiConnectChangeReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mWifiConnectChangedReceiver, filter);
    }

    private void unRegisterWifiConnectChangeReceiver() {
        unregisterReceiver(mWifiConnectChangedReceiver);
    }

    class WifiConnectChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Util.d("-------- action = " + action);
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (parcelableExtra != null) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.DetailedState currentdState = networkInfo.getDetailedState();
                    Util.d("--------state = " + currentdState);
                    if (mLastState != currentdState) {
                        Message message = new Message();
                        message.what = UPDATE_WIFI_STATE;
                        switch (currentdState) {
                            case CONNECTED:
                                message.obj = NetworkInfo.DetailedState.CONNECTED;
                                if (Constant.WIFI_SSID
                                        .equals(MonitorcameraWifiManager.getInstance().getConnectedWifiSsid())) {
                                    message.arg1 = 1;
                                } else {
                                    message.arg1 = 0;
                                }
                                break;
                            case DISCONNECTED:
                                message.obj = NetworkInfo.DetailedState.DISCONNECTED;
                                break;
                            case OBTAINING_IPADDR:
                                message.obj = NetworkInfo.DetailedState.OBTAINING_IPADDR;
                                break;
                            case CONNECTING:
                                message.obj = NetworkInfo.DetailedState.CONNECTING;
                                break;
                            case AUTHENTICATING:
                                message.obj = NetworkInfo.DetailedState.AUTHENTICATING;
                                break;
                        }
                        mHander.sendMessage(message);
                    }
                }
            }
        }
    }
}
