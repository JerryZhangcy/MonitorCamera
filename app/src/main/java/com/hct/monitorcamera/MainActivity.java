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
    private static final int UPDATE_RECV_MSG = 0x1001;
    private static final int UPDATE_WIFI_STATE = 0X1002;

    private Button mConnectWifi, mConnectDevice, mDisConnectDevice, mSend;
    private EditText mSsid, mPws, mIp, mPort, mSendMsg;
    private ListView mListView;
    private FileListAdapter mFileListAdapter;

    private static final int PERMISSIONS_REQUEST = 1;
    private boolean mIsWifiConnected = false;

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
                case UPDATE_RECV_MSG:
                    mFileListAdapter.updateAdapter(String.valueOf(msg.obj), false);
                    break;
                case UPDATE_WIFI_STATE:
                    boolean flag = (boolean) msg.obj;
                    if (flag) {
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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_wifi:
                MonitorcameraWifiManager.getInstance().startConnectWifi();
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
    public void onHandlerMessage(String msg) {
        Util.d("--------onHandlerMessage  msg = " + msg);
        Message message = new Message();
        message.obj = msg;
        message.what = UPDATE_RECV_MSG;
        mHander.sendMessage(message);
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
                    NetworkInfo.State state = networkInfo.getState();
                    Util.d("--------state = " + state);
                    Message message = new Message();
                    message.what = UPDATE_WIFI_STATE;
                    if (state == NetworkInfo.State.CONNECTED) {
                        if (Constant.WIFI_SSID
                                .equals(MonitorcameraWifiManager.getInstance().getConnectedWifiSsid())) {
                            message.obj = true;
                        } else {
                            message.obj = false;
                        }
                    } else {
                        message.obj = false;
                    }
                    mHander.sendMessage(message);
                }
            }
        }
    }
}
