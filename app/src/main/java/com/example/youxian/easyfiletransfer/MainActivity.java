package com.example.youxian.easyfiletransfer;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getName();
    private static final String Fragment_TAG = "fragment_tag";
    public static final  String SELECTED_FILES = "selected_files";
    private static final int File_Chooser = 1;

    private MainFragment mMainFragment;
    private TransferFragment mTransferFragment;
    private ReceiverFragment mReceiverFragment;

    private List<File> mSelectedFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        if (requestCode == File_Chooser) {
            if (resultCode == RESULT_OK) {
                if (data.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
                    mSelectedFiles = (List<File>) data.getSerializableExtra
                            (com.example.youxian.filechooser.MainActivity.SELECTED_FILES);
                    Log.d(TAG, mSelectedFiles.size()+ "");
                    if (mSelectedFiles.size() > 0) {
                        replaceFragment(getTransferFragment(), true);
                    }
                }
            }
        }
    }

    private void initView() {
        replaceFragment(getMainFragment(), false);
    }

    private void replaceFragment(Fragment fragment, boolean addBackStack){
        Log.d(TAG, "replaceFragment: " + fragment);
        FragmentTransaction transaction = this.getFragmentManager().beginTransaction();
        transaction.replace(R.id.container_main, fragment, Fragment_TAG);
        if (addBackStack)
            transaction.addToBackStack(null);
        transaction.commit();
    }

    private TransferFragment getTransferFragment() {
        if (mTransferFragment == null) {
            mTransferFragment = new TransferFragment();
            if (mSelectedFiles != null) {
                Bundle mBundle = new Bundle();
                mBundle.putSerializable(SELECTED_FILES, (Serializable) mSelectedFiles);
                mTransferFragment.setArguments(mBundle);
            }
        }
        return mTransferFragment;
    }

    private ReceiverFragment getReceiverFragment() {
        if (mReceiverFragment == null) {
            mReceiverFragment = new ReceiverFragment();
        }
        return mReceiverFragment;
    }

    private MainFragment getMainFragment() {
        if (mMainFragment == null) {
            mMainFragment = new MainFragment();
            mMainFragment.setListener(new MainFragment.Listener() {
                @Override
                public void transferClick() {
                    Intent intent = new Intent(MainActivity.this,
                            com.example.youxian.filechooser.MainActivity.class);
                    startActivityForResult(intent, File_Chooser);
                }

                @Override
                public void receiverClick() {
                    replaceFragment(getReceiverFragment(), true);
                }
            });
        }
        return mMainFragment;
    }
}
