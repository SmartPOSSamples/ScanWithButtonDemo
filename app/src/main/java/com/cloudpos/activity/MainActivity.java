package com.cloudpos.activity;


import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.cloudpos.scanserver.aidl.IScanCallBack;
import com.cloudpos.scanserver.aidl.IScanService;
import com.cloudpos.scanserver.aidl.ScanParameter;
import com.cloudpos.scanserver.aidl.ScanResult;
import com.wizarpos.scan.init.R;
import com.cloudpos.util.Logger;
import com.cloudpos.util.TextViewUtil;


public class MainActivity extends AbstractActivity implements OnClickListener {

    IScanCallBack callBack = new IScanCallBack.Stub() {
        @Override
        public void foundBarcode(ScanResult result) throws RemoteException {
            if(result.getResultCode() == ScanResult.SCAN_SUCCESS){
                writerInSuccessLog("format: " + result.getBarcodeFormat() + ", result: "+ result.getText());
                scanService.stopScan();
            }else{
                writerInFailedLog("scan failed! reason : " + result.getResultCode());
            }
            isScanning = false;
        }
    };
    private boolean isScanning = false;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_run3 = (Button) this.findViewById(R.id.btn_run3);
        Button btn_run4 = (Button) this.findViewById(R.id.btn_run4);
        Button btn_run5 = (Button) this.findViewById(R.id.btn_run5);
        log_text = (TextView) this.findViewById(R.id.text_result);
        log_text.setMovementMethod(ScrollingMovementMethod.getInstance());

        findViewById(R.id.settings).setOnClickListener(this);
        btn_run3.setOnClickListener(this);
        btn_run4.setOnClickListener(this);
        btn_run5.setOnClickListener(this);


        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == R.id.log_default) {
                    log_text.append("\t" + msg.obj + "\n");
                } else if (msg.what == R.id.log_success) {
                    String str = "\t" + msg.obj + "\n";
                    TextViewUtil.infoBlueTextView(log_text, str);
                } else if (msg.what == R.id.log_failed) {
                    String str = "\t" + msg.obj + "\n";
                    TextViewUtil.infoRedTextView(log_text, str);
                } else if (msg.what == R.id.log_clear) {
                    log_text.setText("");
                }
            }
        };
        bindService();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanService != null) {
            try {
                this.scanService.stopScan();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            this.scanService = null;
            this.unbindService(conn);
        }
    }

    IScanService scanService;
    ServiceConnection conn;

    public void bindService() {
        try {
            conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    try {
                        scanService = IScanService.Stub.asInterface(service);
                        ScanParameter parameter = new ScanParameter();
                        parameter.set(ScanParameter.KEY_CAMERA_INDEX, 1);
                        scanService.startScan(parameter, callBack);
                        isScanning = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };
            boolean result = startConnectService(MainActivity.this,
                    "com.cloudpos.scanserver",
                    "com.cloudpos.scanserver.service.ScannerService", conn);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    protected boolean startConnectService(Context mContext, String packageName, String className, ServiceConnection connection) {
        boolean isSuccess = startConnectService(mContext, new ComponentName(packageName, className), connection);
        return isSuccess;
    }

    protected boolean startConnectService(Context context, ComponentName comp, ServiceConnection connection) {
        Intent intent = new Intent();
        intent.setPackage(comp.getPackageName());
        intent.setComponent(comp);
        boolean isSuccess = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Logger.debug("bind service (%s, %s)", isSuccess, comp.getPackageName(), comp.getClassName());
        return isSuccess;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!isScanning){
            this.finish();
        }
    }


    @Override
    public void onClick(View arg0) {
        int index = arg0.getId();
        if (scanService == null) {
            writerInFailedLog("bind service failed!");
            return;
        }
        if (index == R.id.btn_run3) {
        } else if (index == R.id.btn_run4) {
        } else if (index == R.id.btn_run5) {
        } else if (index == R.id.settings) {
            log_text.setText("");
        }
    }


    /**
     *  Use like the other keys, when press the key , the activated and top task will get the
     *  key value and do something. Firstly the application must override the function of
     *  Page 72 Of 84CloudPOS SDK Development Guidance onKeydown, as follows:
     */
    /**
     * Key code constant: left scan key.
     */
    public static final int KEYCODE_SCAN_LEFT = 229;
    /**
     * Key code constant: right scan key.
     */
    public static final int KEYCODE_SCAN_RIGHT = 230;
    /**
     * Key code constant: qr key.
     */
    public static final int KEYCODE_QR = 232;

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KEYCODE_SCAN_LEFT || keyCode == KEYCODE_SCAN_RIGHT || keyCode == KEYCODE_QR) {
            if(!isScanning){
                ScanParameter parameter = new ScanParameter();
                parameter.set(ScanParameter.KEY_CAMERA_INDEX, 1);
                try {
                    scanService.stopScan();
                    scanService.startScan(parameter, callBack);
                    isScanning = true;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}
