package com.example.youxian.easyfiletransfer;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

/**
 * Created by Youxian on 12/6/15.
 */
public class MainFragment extends Fragment {
    private ImageButton transferButton;
    private ImageButton receiverButton;

    private Listener mListener;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transferButton = (ImageButton) view.findViewById(R.id.transfer_button_main);
        transferButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.transferClick();
                }
            }
        });

        receiverButton = (ImageButton) view.findViewById(R.id.receiver_button_main);
        receiverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.receiverClick();
                }
            }
        });
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        void transferClick();
        void receiverClick();
    }
}
