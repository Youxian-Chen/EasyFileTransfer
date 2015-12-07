package com.example.youxian.easyfiletransfer;

import android.app.Fragment;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Youxian on 12/7/15.
 */
public class ReceiverFragment extends Fragment implements NfcAdapter.ReaderCallback{
    private static final String TAG = ReceiverFragment.class.getName();
    private static final byte[] CLA_INS_P1_P2 = { 0x00, (byte)0xA4, 0x04, 0x00 };
    private static final byte[] AID_ANDROID = { (byte)0xF0, 0x3, 0x04, 0x05, 0x06, 0x07, 0x08 };
    private NfcAdapter mNfcAdapter;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig;
    private ListView mListView;
    private TextView mStatus;
    private TextView mPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        if (mNfcAdapter != null) {
            mNfcAdapter.enableReaderMode(getActivity(), this, NfcAdapter.FLAG_READER_NFC_A |
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
        }
        openWifiAp();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mNfcAdapter != null) {
            if (mNfcAdapter.isEnabled()) {
                mNfcAdapter.disableReaderMode(getActivity());
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_receiver, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListView = (ListView) view.findViewById(R.id.list_receiver);
        mStatus = (TextView) view.findViewById(R.id.status_text_receive);
        mPath = (TextView) view.findViewById(R.id.path_text_receive);

    }

    private void openWifiAp() {
        setWifiConfig();
        setWifiApEnabled(mWifiConfig, true);
        String ipAddress = getApIpAddr();
        Log.d(TAG, "ip: " + ipAddress);

    }

    private static byte[] convert2Bytes(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };
        return addressBytes;
    }

    private String getApIpAddr() {
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        byte[] ipAddress = convert2Bytes(dhcpInfo.serverAddress);
        try {
            String apIpAddr = InetAddress.getByAddress(ipAddress).getHostAddress();
            return apIpAddr;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setWifiConfig() {
        mWifiConfig = new WifiConfiguration();
        mWifiConfig.SSID = "EasyFileTransfer";
        mWifiConfig.preSharedKey = "love0925";
        mWifiConfig.hiddenSSID = true;
        mWifiConfig.status = WifiConfiguration.Status.ENABLED;
        mWifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        mWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        mWifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        mWifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        mWifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        mWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        mWifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        Log.d(TAG, "ID: " + mWifiConfig.SSID);
        Log.d(TAG, "password: " + mWifiConfig.preSharedKey);
    }

    private boolean setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
        try {
            if (enabled) { // disable WiFi in any case
                mWifiManager.setWifiEnabled(false);
            }

            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return (Boolean) method.invoke(mWifiManager, wifiConfig, enabled);
        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
            return false;
        }
    }

    private byte[] createSelectAidApdu(byte[] aid) {
        byte[] result = new byte[6 + aid.length];
        System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
        result[4] = (byte)aid.length;
        System.arraycopy(aid, 0, result, 5, aid.length);
        result[result.length - 1] = 0;
        return result;
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        IsoDep isoDep = IsoDep.get(tag);
        try {
            isoDep.connect();
            byte[] response = isoDep.transceive(createSelectAidApdu(AID_ANDROID));
            String resString = new String(response);
            Log.d(TAG, "select application response: " + resString);
            if (resString.equals("EasyFileTransfer")) {

                mNfcAdapter.disableReaderMode(getActivity());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
