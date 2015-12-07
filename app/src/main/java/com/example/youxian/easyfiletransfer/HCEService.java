package com.example.youxian.easyfiletransfer;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Youxian on 12/7/15.
 */
public class HCEService extends HostApduService {

    private static final String TAG = HostApduService.class.getName();
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (selectAidApdu(commandApdu)) {
            Log.d(TAG, "application selected");
            return "EasyFileTransfer".getBytes();
        } else {


        }
        return new byte[0];
    }

    private boolean selectAidApdu(byte[] apdu) {
        return apdu.length >= 2 && apdu[0] == (byte)0 && apdu[1] == (byte)0xa4;
    }


    @Override
    public void onDeactivated(int reason) {

    }
}
