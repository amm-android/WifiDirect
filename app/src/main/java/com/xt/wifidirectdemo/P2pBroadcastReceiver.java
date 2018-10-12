package com.xt.wifidirectdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by xuti on 2018/10/9.
 */
public class P2pBroadcastReceiver extends BroadcastReceiver {
    private AWifiP2pCotroller mAWifiP2pCotroller;

    public P2pBroadcastReceiver(AWifiP2pCotroller aWifiP2pCotroller) {
        super();
        mAWifiP2pCotroller = aWifiP2pCotroller;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mAWifiP2pCotroller.onReceive(intent);
    }
}
