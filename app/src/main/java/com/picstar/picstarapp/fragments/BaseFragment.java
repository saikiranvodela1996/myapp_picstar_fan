package com.picstar.picstarapp.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.picstar.picstarapp.helpers.PSR_PrefsManager;

class BaseFragment   extends Fragment {

    public PSR_PrefsManager psr_prefsManager;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        psr_prefsManager = new PSR_PrefsManager(getActivity());
    }
}
