package com.cybernetic87.GAssist;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

public class MainFragment extends Fragment {

    private View mainView;

    public MainFragment() {
        // Required empty public constructor
    }

//    public void showBadge(boolean visible){
//        getActivity().runOnUiThread(() -> {
//
//            if(visible){
//                mainView.findViewById(R.id.badgeLayout).setVisibility(View.VISIBLE);
//            }
//            else{
//                mainView.findViewById(R.id.badgeLayout).setVisibility(View.GONE);
//            }
//
//        });
//
//    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_main, container, false);

        String badge = "<a href=\"https://galaxy.store/gassistn\"><img src=\"https://img.samsungapps.com/seller/images/badges/galaxyStore/png_big/GalaxyStore_English.png\" alt=\"Available on Samsung Galaxy Store\" style=\"max-width: 100%; height: auto;\"></a>";
        WebView webviewSamsungBadge = mainView.findViewById(R.id.webviewSamsungBadge);
        webviewSamsungBadge.loadData(badge, "text/html; charset=utf-8", "UTF-8");

        mainView.findViewById(R.id.textView);

        return mainView;
    }

}
