package com.example.youxian.easyfiletransfer;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getName();
    private static final String Fragment_TAG = "fragment_tag";
    private static final int File_Chooser = 1;

    private MainFragment mMainFragment;

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

    private MainFragment getMainFragment() {
        if (mMainFragment == null) {
            mMainFragment = new MainFragment();
            mMainFragment.setListener(new MainFragment.Listener() {
                @Override
                public void transferClick() {

                }

                @Override
                public void receiverClick() {

                }
            });
        }
        return mMainFragment;
    }
}
