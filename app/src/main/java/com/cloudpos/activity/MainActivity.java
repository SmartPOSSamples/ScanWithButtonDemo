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

import com.cloudpos.DeviceException;
import com.cloudpos.advance.ext.POSTerminalAdvance;
import com.cloudpos.advance.ext.scanner.IScanCallBack;
import com.cloudpos.advance.ext.scanner.IScannerDevice;
import com.cloudpos.advance.ext.scanner.ScanParameter;
import com.cloudpos.advance.ext.scanner.ScanResult;
import com.wizarpos.scan.init.R;
import com.cloudpos.util.Logger;
import com.cloudpos.util.TextViewUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AbstractActivity implements OnClickListener {
    private IScannerDevice scannerDevice;
    private boolean isScanning = false;
    private boolean isOpen = false;
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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


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
        initScanner();
    }

    private void initScanner() {
        executor.execute(() -> {
            scannerDevice = POSTerminalAdvance.getInstance().getScannerDevice();
            try {
                isOpen = scannerDevice.open(this);
                startScan();
            } catch (DeviceException e) {
                writerInFailedLog("Failed to open scanner: " + e.getMessage());
            }
        });
    }



    private void startScan() {
        if (scannerDevice == null || isScanning || !isOpen) return;

        executor.execute(() -> {
            ScanParameter parameter = new ScanParameter();
            parameter.set(ScanParameter.KEY_CAMERA_INDEX, 0);
            try {
                scannerDevice.startScan(parameter, new IScanCallBack() {
                    @Override
                    public void foundBarcode(ScanResult result) {
                        isScanning = false;
                        if (result.getResultCode() == ScanResult.SCAN_SUCCESS) {
                            writerInSuccessLog("Result: " + result.getText());
                        } else {
                            writerInFailedLog("Scan failed. Code: " + result.getResultCode());
                        }
                        try {
                            scannerDevice.stopScan();
                        } catch (DeviceException e) {
                            writerInFailedLog("Stop scan failed: " + e.getMessage());
                        }
                    }
                });
                isScanning = true;
            } catch (DeviceException e) {
                writerInFailedLog("Start scan error: " + e.getMessage());
            }
        });
    }

    private void stopScan() {
        executor.execute(() -> {
            if (scannerDevice != null) {
                try {
                    scannerDevice.stopScan();
                    isScanning = false;
                } catch (DeviceException e) {
                    writerInFailedLog("Stop scan error: " + e.getMessage());
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(() -> {
            if (scannerDevice != null) {
                try {
                    scannerDevice.close();
                } catch (DeviceException e) {
                    writerInFailedLog("Close device error: " + e.getMessage());
                }
            }
        });
        executor.shutdown();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isScanning) {
            finish();
        }
    }


    @Override
    public void onClick(View arg0) {
        int index = arg0.getId();
        if (!isOpen) {
            writerInFailedLog("camera not open");
            return;
        }
        if (index == R.id.btn_run3) {
        } else if (index == R.id.btn_run4) {
        } else if (index == R.id.btn_run5) {
        } else if (index == R.id.settings) {
            log_text.setText("");
        }
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KEYCODE_SCAN_LEFT || keyCode == KEYCODE_SCAN_RIGHT || keyCode == KEYCODE_QR) {
            if (!isScanning) {
                stopScan(); // Stop any existing scan before starting
                startScan();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}
