package com.example.youxian.easyfiletransfer;

import android.app.Fragment;
import android.app.ProgressDialog;
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
import java.net.InetSocketAddress;
import java.net.Socket;
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

    private boolean mReadyToTransfer;
    private ProgressDialog mProgressDialog;
    private TransferFilesTask mTransferFilesTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReadyToTransfer = false;
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
        mReadyToTransfer = true;
    }

    public boolean getReadyToTransfer() {
        return mReadyToTransfer;
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

    private class TransferFilesTask extends AsyncTask<String, Integer, Long> {
        private String mAddress;
        private String mPort;
        private List<File> mFiles = new ArrayList<>();
        private static final int SOCKET_TIMEOUT = 5000;

        public TransferFilesTask(List<File> files, String address, String port){
            mFiles = files;
            mAddress = address;
            mPort = port;
        }
        @Override
        protected Long doInBackground(String... params) {
            while (!isCancelled()) {
                Socket socket = new Socket();

                try {
                    Log.d(TAG, "Opening client socket - ");
                    socket.bind(null);
                    Log.d(TAG, mAddress + " port: " + mPort);
                    socket.connect((new InetSocketAddress(mAddress, Integer.parseInt(mPort))), SOCKET_TIMEOUT);

                    Log.d(TAG, "Client socket - " + socket.isConnected());

                    BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                    DataOutputStream dos = new DataOutputStream(bos);

                    dos.writeInt(mFiles.size());
                    Log.d(TAG, mFiles.size()+"");
                    int added = 100 / mFiles.size();
                    int status = 100 % mFiles.size();
                    for (File file : mFiles){
                        //File file = new File(music.getPath());
                        Log.d(TAG, file.getPath());
                        long length = file.length();
                        dos.writeLong(length);
                        String name = file.getName();
                        dos.writeUTF(name);
                        FileInputStream fis = new FileInputStream(file);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        int theByte;
                        for (long i = 0; i < length; i++){
                            theByte = bis.read();
                            bos.write(theByte);
                        }
                        /*
                        while((theByte = bis.read()) != -1){
                            bos.write(theByte);
                        }*/
                        bis.close();
                        status = status + added;
                        mProgressDialog.setProgress(status);
                        Log.d(TAG, "Client: Data written");
                    }
                    dos.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                } finally {
                    if (socket != null) {
                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                Log.d(TAG, "Client: stop service");
                mProgressDialog.dismiss();
                this.cancel(true);
            }
            return null;
        }

    }
}
