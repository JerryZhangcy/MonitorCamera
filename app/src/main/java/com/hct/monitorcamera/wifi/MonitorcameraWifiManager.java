package com.hct.monitorcamera.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.hct.monitorcamera.Constant;
import com.hct.monitorcamera.MonitorcameraApplication;
import com.hct.monitorcamera.R;
import com.hct.monitorcamera.Util;

import java.util.List;

import static com.hct.monitorcamera.MonitorcameraApplication.mAppContext;

public class MonitorcameraWifiManager {

    private static MonitorcameraWifiManager mInstance;
    private WifiManager mWifiManager;

    public static synchronized MonitorcameraWifiManager getInstance() {
        if (mInstance == null) {
            synchronized (MonitorcameraWifiManager.class) {
                if (mInstance == null) {
                    mInstance = new MonitorcameraWifiManager();
                }
            }
        }
        return mInstance;
    }

    private MonitorcameraWifiManager() {
        mWifiManager = (WifiManager) mAppContext.getSystemService(Context.WIFI_SERVICE);
    }

    public boolean isWifiEnable() {
        if (mWifiManager != null)
            return mWifiManager.isWifiEnabled();
        return false;
    }

    public String getConnectedWifiSsid() {
        if (mWifiManager != null) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            Util.e("-------->info = " + info.toString());
            if (info.getNetworkId() != -1) {
                String ssid = info.getSSID();
                ssid = ssid.substring(1, ssid.length() - 1);//剔除掉前后的引号
                return ssid;
            }
        }
        return null;
    }

    public boolean startConnectWifi() {
        Util.e("-------->isWifiEnable() = " + isWifiEnable());
        if (!isWifiEnable()) {
            Toast.makeText(MonitorcameraApplication.mAppContext,
                    R.string.open_wifi_toast, Toast.LENGTH_SHORT).show();
            return false;
        }

        String currSsid = getConnectedWifiSsid();
        Util.e("-------->currSsid = " + currSsid);
        if (currSsid != null && currSsid.equals(Constant.WIFI_SSID)) {
            return true;
        }

        WifiConfiguration wifiInfo = createWifiInfo(Constant.WIFI_SSID,
                Constant.WIFI_PASSWORDS);
        int wcgID = mWifiManager.addNetwork(wifiInfo);
        boolean b = mWifiManager.enableNetwork(wcgID, true);
        Util.e("-------->b = " + b);
        return b;
    }

    private void removeWifiBySsid() {
        List<WifiConfiguration> wifiConfigs = mWifiManager.getConfiguredNetworks();
        if (wifiConfigs != null) {
            for (WifiConfiguration wifiConfig : wifiConfigs) {
                boolean status = mWifiManager.removeNetwork(wifiConfig.networkId);
                mWifiManager.saveConfiguration();
            }
        }
    }

    private WifiConfiguration createWifiInfo(String ssid, String key) {
        WifiConfiguration wc = new WifiConfiguration();

        wc.allowedAuthAlgorithms.clear();
        wc.allowedGroupCiphers.clear();
        wc.allowedKeyManagement.clear();
        wc.allowedPairwiseCiphers.clear();
        wc.allowedProtocols.clear();

        wc.SSID = "\"" + ssid + "\"";

        WifiConfiguration tempConfig = isExist(ssid);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        wc.preSharedKey = "\"" + key + "\"";
        wc.hiddenSSID = true;
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.priority = 999999;
        wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return wc;
    }

    private WifiConfiguration isExist(String ssid) {
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            if (config.SSID.equals("\"" + ssid + "\"")) {
                return config;
            }
        }
        return null;
    }
}
