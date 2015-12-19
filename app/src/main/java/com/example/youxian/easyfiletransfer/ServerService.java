package com.example.youxian.easyfiletransfer;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Youxian on 12/8/15.
 */
public class ServerService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    private static final String TAG = ServerService.class.getName();
    public static final  String ACTION_SERVER_START = "action_server_start";
    public static final String FILES_NAME = "files_name";
    public static final String SERVER_CONNECTED = "server_connected";
    private static final int SOCKET_TIMEOUT = 8000;
    private static final int PORT = 8899;
    private String mAddress;
    private List<String> mFileName = new ArrayList<>();

    public ServerService(String name) {
        super(name);
    }

    public ServerService() {
        super("ServerServie");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, action);
        if (ACTION_SERVER_START.equals(action)) {
            while (mAddress == null) {
                BufferedReader br = null;


                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] splitted = line.split(" +");

                        if ((splitted != null) && (splitted.length >= 4)) {
                            // Basic sanity check
                            String mac = splitted[3];

                            if (mac.matches("..:..:..:..:..:..") && !mac.equals("00:00:00:00:00:00")) {
                                Log.d(TAG, "find device: " + mac);
                                mAddress = splitted[0];
                                Log.d(TAG, "server: " + mAddress);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(this.getClass().toString(), e.toString());
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        Log.e(this.getClass().toString(), e.getMessage());
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (mAddress != null) {
                connectToTransfer();
            }
        }
    }

    private void connectToTransfer() {
        String dirPath = android.os.Environment.getExternalStorageDirectory() + "/EasyFileTransfer";
        File dir = new File(dirPath);
        dir.mkdirs();
        try {

            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            Socket socket = new Socket();
            Log.d(TAG, "Opening client socket - ");
            socket.bind(null);
            Log.d(TAG, mAddress + " port: " + PORT);
            socket.connect((new InetSocketAddress(mAddress, PORT)), SOCKET_TIMEOUT);

            Log.d(TAG, "Client socket - " + socket.isConnected());

            Intent connectedIntent = new Intent();
            connectedIntent.setAction(SERVER_CONNECTED);
            sendBroadcast(connectedIntent);

            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
            DataInputStream dis = new DataInputStream(bis);

            int filesCount = dis.readInt();
            File[] files = new File[filesCount];
            for (int i = 0; i < filesCount; i++){
                long fileLength = dis.readLong();
                Log.d(TAG, "length: " + fileLength);
                String fileName = dis.readUTF();
                Log.d(TAG, "name: " + fileName);
                files[i] = new File(dirPath +"/"+ fileName);

                FileOutputStream fos = new FileOutputStream(files[i]);
                //BufferedOutputStream bos = new BufferedOutputStream(fos);
                /*
                for(int j = 0; j < fileLength; j++){
                    bos.write(bis.read());
                }
                */
                int theByte;
                byte[] buffer = new byte[1024];
                while (fileLength > 0 && (theByte = dis.read(buffer, 0, (int) Math.min(buffer.length, fileLength))) != -1) {
                    fos.write(buffer,0,theByte);
                    fileLength -= theByte;
                }
                fos.close();
                Log.d(TAG, "get file: " + fileName);
                mFileName.add(fileName);
                //bos.close();

                new MediaScannerWrapper(getApplicationContext(), dirPath + "/" + fileName, "music/*").scan();
            }
            dis.close();
            socket.close();
            Log.d(TAG, "saved file and close server");
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());

        }
        Intent transferDoneIntent = new Intent();
        transferDoneIntent.setAction(ReceiverFragment.Received_Files);
        transferDoneIntent.putStringArrayListExtra(FILES_NAME, (ArrayList<String>) mFileName);
        sendBroadcast(transferDoneIntent);
        stopSelf();

    }

    private class MediaScannerWrapper implements MediaScannerConnection.MediaScannerConnectionClient{
        private MediaScannerConnection mConnection;
        private String mPath;
        private String mMimeType;
        public MediaScannerWrapper(Context context, String filePath, String mime){
            mPath = filePath;
            mMimeType = mime;
            mConnection = new MediaScannerConnection(context, this);
        }

        public void scan(){
            mConnection.connect();
        }
        @Override
        public void onMediaScannerConnected() {
            mConnection.scanFile(mPath, mMimeType);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {

        }

    }

}
