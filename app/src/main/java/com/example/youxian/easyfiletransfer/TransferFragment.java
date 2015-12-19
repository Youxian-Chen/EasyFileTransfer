package com.example.youxian.easyfiletransfer;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Youxian on 12/6/15.
 */
public class TransferFragment extends Fragment {
    private static final String TAG = TransferFragment.class.getName();

    private ListView mListView;
    private List<File> mSelectedFiles;
    private FilesAdapter mAdapter;

    private ProgressDialog mProgressDialog;
    private TransferFilesTask mTransferFilesTask;

    private IntentFilter mNetWorkIntent;
    private NetWorkReceiver mNetWorkReceiver;

    private String[] mConfigStrings;
    private WifiConfiguration mWifiConfig;
    private WifiManager mWifiManager;

    private boolean hasTransfer = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        mNetWorkIntent = new IntentFilter();
        mNetWorkIntent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mNetWorkIntent.addAction(HCEService.WIFI_CONFIG);
        mNetWorkReceiver = new NetWorkReceiver();
        getActivity().registerReceiver(mNetWorkReceiver, mNetWorkIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mNetWorkReceiver);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mSelectedFiles = (List<File>) getArguments().getSerializable(MainActivity.SELECTED_FILES);
        return inflater.inflate(R.layout.fragment_transfer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListView = (ListView) view.findViewById(R.id.list_transfer);
        mAdapter = new FilesAdapter(mSelectedFiles);
        mListView.setAdapter(mAdapter);
    }

    private void connectToWiFi() {
        WifiManager wifiManager = (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
        wifiManager.disconnect();
        //disable others
        for (WifiConfiguration wifiConfiguration: wifiManager.getConfiguredNetworks()) {
            wifiManager.disableNetwork(wifiConfiguration.networkId);
        }

        mWifiConfig = new WifiConfiguration();
        mWifiConfig.SSID = "\"" + mConfigStrings[0] + "\"";
        mWifiConfig.preSharedKey = "\"" + mConfigStrings[1] + "\"";
        mWifiConfig.priority = 100000;
        int res = wifiManager.addNetwork(mWifiConfig);
        Log.d("WifiPreference", "add Network returned " + res);
        wifiManager.disconnect();
        boolean isEnable = wifiManager.enableNetwork(res, true);
        Log.d("WifiPreference", "enable Network returned " + isEnable);
        wifiManager.reconnect();
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
        byte[] gateAddress = convert2Bytes(dhcpInfo.gateway);
        try {
            String apIpAddr = InetAddress.getByAddress(ipAddress).getHostAddress();
            Log.d(TAG, "ip: " + apIpAddr);
            String gateAddr = InetAddress.getByAddress(gateAddress).getHostAddress();
            Log.d(TAG, "gate: " + gateAddr);
            return apIpAddr;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void resetState() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        if (mTransferFilesTask != null) {
            mTransferFilesTask.cancel(true);
            mTransferFilesTask = null;
        }
    }

    private void startTransfer() {
        resetState();
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setProgress(0);
        mTransferFilesTask = new TransferFilesTask(mSelectedFiles);
        mTransferFilesTask.execute("");
        mProgressDialog.show();
    }

    private void transferFinish() {
        for (WifiConfiguration config: mWifiManager.getConfiguredNetworks()) {
            if (config.SSID.contains(mWifiConfig.SSID)) {
                mWifiManager.removeNetwork(config.networkId);
                Log.d(TAG, "remove: " + config.SSID);
                continue;
            }
            mWifiManager.enableNetwork(config.networkId, true);
        }
        resetState();
        getActivity().getFragmentManager().popBackStackImmediate();
    }

    private class FilesAdapter extends BaseAdapter {
        private List<File> mFiles;

        public FilesAdapter(List<File> files) {
            mFiles = files;
        }
        @Override
        public int getCount() {
            return mFiles.size();
        }

        @Override
        public Object getItem(int position) {
            return mFiles.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            File file = mFiles.get(position);
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.listrow_item, null);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.title = (TextView) convertView.findViewById(R.id.title_text_item);
                convertView.setTag(viewHolder);
            }

            if (file != null) {
                ViewHolder viewHolder = (ViewHolder) convertView.getTag();
                viewHolder.title.setText(file.getName());
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView title;
    }

    private class NetWorkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action);
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(info != null && info.isConnected() && info.getExtraInfo().contains("EasyFileTransfer")) {
                    //connection done
                    if (hasTransfer) {

                    } else {
                        startTransfer();
                        hasTransfer = true;
                    }
                } else if (info != null && info.isConnected() && !info.getExtraInfo().contains("EasyFileTransfer")) {
                    if (hasTransfer) {
                        transferFinish();
                        hasTransfer = false;
                    }
                }
            } else if (HCEService.WIFI_CONFIG.equals(action)) {
                //get wifi config
                mProgressDialog = new ProgressDialog(getActivity());
                mProgressDialog.setMessage("connecting...");
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                mConfigStrings = intent.getStringExtra(HCEService.WIFI_CONFIG).split("@");
                connectToWiFi();
            }
        }
    }

    private class TransferFilesTask extends AsyncTask<String, Integer, Long> {
        private List<File> mFiles = new ArrayList<>();

        public TransferFilesTask(List<File> files){
            mFiles = files;
        }
        @Override
        protected Long doInBackground(String... params) {
            while (!isCancelled()) {
                try {

                    Log.d(TAG, "server create");
                    ServerSocket serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(8899));
                    Socket client = serverSocket.accept();
                    Log.d(TAG, "client accept");



                    BufferedOutputStream bos = new BufferedOutputStream(client.getOutputStream());
                    DataOutputStream dos = new DataOutputStream(bos);

                    dos.writeInt(mFiles.size());
                    Log.d(TAG, mFiles.size() + "");
                    int added = 100 / mFiles.size();
                    int status = 100 % mFiles.size();
                    for (File file : mFiles) {
                        //File file = new File(music.getPath());
                        Log.d(TAG, file.getPath());
                        long length = file.length();
                        dos.writeLong(length);
                        Log.d(TAG, "length: " + length);
                        String name = file.getName();
                        dos.writeUTF(name);
                        Log.d(TAG, "name: " + name);
                        FileInputStream fis = new FileInputStream(file);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        int theByte;
                        /*
                        for (long i = 0; i < length; i++) {
                            theByte = bis.read();
                            bos.write(theByte);
                        }
                        */
                        byte[] buffer = new byte[1024];
                        while((theByte = bis.read(buffer)) > 0 ){
                            bos.write(buffer, 0, theByte);
                            bos.flush();
                        }
                        bis.close();
                        status = status + added;
                        mProgressDialog.setProgress(status);
                        Log.d(TAG, "Client: Data written");
                    }
                    dos.close();
                    serverSocket.close();
                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                Log.d(TAG, "Client: stop service");
                mProgressDialog.dismiss();
                this.cancel(true);
            }
            return null;
        }

    }
}
