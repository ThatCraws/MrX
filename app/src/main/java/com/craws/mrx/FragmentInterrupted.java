package com.craws.mrx;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class FragmentInterrupted extends Fragment {
    private TextView txtMessage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        super.onCreateView(inflater, parent, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_interrupted, parent, false);

        txtMessage = (TextView)view.findViewById(R.id.txt_interrupted_message);

        return view;
    }

    public void setText(final String newText) {
        if(txtMessage != null) {
            txtMessage.setText(newText);
        }
    }
}
