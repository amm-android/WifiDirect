package com.xt.wifidirectdemo;

import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import com.xt.m_common_utils.MConvertUtils;
import com.xt.m_common_utils.MLogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;

/**
 * Created by xuti on 2018/10/9.
 */
public abstract class AWifiP2pCotroller {
    private static final int PORT = 10000;
    private static final String TAG = AWifiP2pCotroller.class.getSimpleName();
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;

    public AWifiP2pCotroller(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel) {
        mWifiP2pManager = wifiP2pManager;
        mChannel = channel;
    }

    public void onReceive(Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            //确定wifi direct模式是否已经启用，并提醒activity。
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

            } else {

            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            mWifiP2pManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    onPeers(peers.getDeviceList());
                }
            });
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            onConnectionChanged();
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String groupOwnerAddressHostAddress = info.groupOwnerAddress.getHostAddress();
                                if (info.isGroupOwner) {
                                    initServerSocket();

                                } else {
                                    //其他设备都作为客户端。在这种情况下，你会希望创建一个客户端线程来连接群主
                                    initSocket(groupOwnerAddressHostAddress);
                                }
                            }
                        }).start();
                    }
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

        }
    }

    public void discoverPeers() {
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reasonCode) {
            }
        });
    }

    public void connect(WifiP2pDevice wifiP2pDevice, WifiP2pManager.ActionListener actionListener) {
        WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
        wifiP2pConfig.deviceAddress = wifiP2pDevice.deviceAddress;
//        wifiP2pConfig.wps.setup = WpsInfo.PBC;
        mWifiP2pManager.connect(mChannel, wifiP2pConfig, actionListener);
    }

    public void removeGroup() {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    public void cancelConnect() {
        mWifiP2pManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }


    private void initServerSocket() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(PORT);
            try {
                mSocket = serverSocket.accept();
                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();
                receive();
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private void initSocket(String groupOwnerAddressHostAddress) {
        mSocket = new Socket();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(groupOwnerAddressHostAddress, PORT);
        try {
            mSocket.connect(inetSocketAddress);
            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();
            receive();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receive() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buf = new byte[1024];
                int len = -1;
                try {
                    while ((len = mInputStream.read(buf)) != -1) {
                        if (mSocket.isClosed()) {
                            break;
                        }
                        byte[] data = new byte[len];
                        System.arraycopy(buf, 0, data, 0, len);
                        MLogUtil.d("接收的数据--->" + MConvertUtils.bytes2HexString(data));
                        onReceiveData(data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // TODO: 2018/10/11 释放资源
                    try {
                        if (mOutputStream != null)
                            mOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (mInputStream != null)
                            mInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (mSocket != null) {
                            mSocket.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    boolean write(byte[] datas) {
        if (datas == null) {
            MLogUtil.d(TAG, "send data null");
            return false;
        }
        if (mSocket == null) {
            MLogUtil.d(TAG, "mSocket null");
            return false;
        }
        if (mSocket.isClosed()) {
            MLogUtil.d(TAG, "mSocket isClosed");
            return false;
        }
        if (mOutputStream == null) {
            MLogUtil.d(TAG, "mOutputStream null");
            return false;
        }
        try {
            mOutputStream.write(datas);
            mOutputStream.flush();
            return true;
        } catch (IOException e) {
            MLogUtil.d(TAG, e.getMessage());
        }
        return false;
    }

    public static String getStatusText(int status) {
        switch (status) {
            case WifiP2pDevice.CONNECTED:
                return "已连接";
            case WifiP2pDevice.INVITED:
                return "正在连接";
            case WifiP2pDevice.FAILED:
                return "失败";
            case WifiP2pDevice.AVAILABLE:
                return "可用";
            case WifiP2pDevice.UNAVAILABLE:
                return "不可用";
        }
        return "未知";
    }


    protected abstract void onPeers(Collection<WifiP2pDevice> deviceList);

    protected abstract void onConnectionChanged();

    protected abstract void onReceiveData(byte[] data);
}
