package com.xt.wifidirectdemo;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.xt.m_common_utils.MConvertUtils;
import com.xt.m_common_utils.MToastUtils;
import com.zhy.adapter.abslistview.CommonAdapter;
import com.zhy.adapter.abslistview.ViewHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private Button btnSearch;
    private EditText edtContent;
    private Button btnSend;
    private ListView listV;
    private IntentFilter mIntentFilter;
    private P2pBroadcastReceiver mP2pBroadcastReceiver;
    private AWifiP2pCotroller mAWifiP2pCotroller;
    private List<WifiP2pDevice> datas = new ArrayList<>();
    private CommonAdapter<WifiP2pDevice> mCommonAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        mP2pBroadcastReceiver = new P2pBroadcastReceiver(mAWifiP2pCotroller);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void initView() {
        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);
        edtContent = (EditText)findViewById(R.id.edtContent);
        btnSend = (Button)findViewById(R.id.btnSend);
        btnSend.setOnClickListener(this);

        listV = (ListView)findViewById(R.id.listV);
        mCommonAdapter = new CommonAdapter<WifiP2pDevice>(this, R.layout.item, datas) {
            @Override
            protected void convert(ViewHolder viewHolder, WifiP2pDevice item, int position) {
                viewHolder.setText(R.id.textVName, item.deviceName);
                viewHolder.setText(R.id.textVState, AWifiP2pCotroller.getStatusText(item.status));
            }
        };
        listV.setAdapter(mCommonAdapter);
        listV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiP2pDevice p2pDevice = datas.get(position);
                if (p2pDevice != null) {
                    if (p2pDevice.status == WifiP2pDevice.CONNECTED) {
                        mAWifiP2pCotroller.removeGroup();
                    } else if (p2pDevice.status == WifiP2pDevice.AVAILABLE) {
                        mAWifiP2pCotroller.connect(p2pDevice, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                            }

                            @Override
                            public void onFailure(int reason) {
                            }
                        });
                    } else if (p2pDevice.status == WifiP2pDevice.INVITED) {
                        mAWifiP2pCotroller.cancelConnect();
                    }
                }
            }
        });
    }

    private void initData() {
        WifiP2pManager wifiP2pManager = (WifiP2pManager) this.getSystemService(Context.WIFI_P2P_SERVICE);
        WifiP2pManager.Channel channel = wifiP2pManager.initialize(this, getMainLooper(), new WifiP2pManager.ChannelListener() {
            @Override
            public void onChannelDisconnected() {

            }
        });

        mAWifiP2pCotroller = new AWifiP2pCotroller(wifiP2pManager, channel) {
            @Override
            protected void onPeers(final Collection<WifiP2pDevice> deviceList) {
                datas.clear();
                if (deviceList.isEmpty()) {
                    MToastUtils.showShort(MainActivity.this, "未扫描到附近的P2P设备");
                } else {
                    datas.addAll(deviceList);
                }
                mCommonAdapter.notifyDataSetChanged();
            }

            @Override
            protected void onConnectionChanged() {
                mCommonAdapter.notifyDataSetChanged();
            }

            @Override
            protected void onReceiveData(byte[] data) {
//                MToastUtils.showShort(MainActivity.this, MConvertUtils.bytes2HexString(data));
                MToastUtils.showShort(MainActivity.this,new String(data));
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mP2pBroadcastReceiver, mIntentFilter);
        mAWifiP2pCotroller.discoverPeers();
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == btnSearch.getId()){
            mAWifiP2pCotroller.discoverPeers();
        }else if (id == btnSend.getId()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String text = edtContent.getText().toString();
//                        String text = "FF";
//                        mAWifiP2pCotroller.write(MConvertUtils.hexString2Bytes(text));
                    mAWifiP2pCotroller.write(text.getBytes());
                }
            }).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mP2pBroadcastReceiver);
    }
}
