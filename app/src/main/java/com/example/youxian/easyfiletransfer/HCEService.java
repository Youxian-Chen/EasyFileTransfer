package com.example.youxian.easyfiletransfer;

import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Youxian on 12/7/15.
 */
public class HCEService extends HostApduService {

    private static final String TAG = HostApduService.class.getName();
    public static final String WIFI_CONFIG = "wifi_config";
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (selectAidApdu(commandApdu)) {
            Log.d(TAG, "application selected");
            return "EasyFileTransfer".getBytes();
        } else {
            String stringApdu = new String(commandApdu);
            if (stringApdu.contains("EasyFileTransfer@love0925")) {
                Log.d(TAG, "send config broadcast");
                Intent configIntent = new Intent();
                configIntent.setAction(WIFI_CONFIG);
                configIntent.putExtra(WIFI_CONFIG, stringApdu);
                sendBroadcast(configIntent);
                return "Got it".getBytes();
            }
        }
        return new byte[0];
    }

    private boolean selectAidApdu(byte[] apdu) {
        return apdu.length >= 2 && apdu[0] == (byte)0 && apdu[1] == (byte)0xa4;
    }


    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "onDeactivated");
    }
}
