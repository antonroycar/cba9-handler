package com.iotera.cba9handler;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.iotera.cba9handler.util.GFG;
import com.iotera.cba9handler.util.HexUtil;
import com.iotera.cba9handler.util.NumberUtil;
import com.iotera.cba9handler.util.SerialUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static int sequenceFlag = 1;
    private static int Address = 0;
    private UsbSerialDevice usbSerialDevice = null;
    private UsbManager usbManager;
    private TextView tvReceivedData;
    private TextView tvBalance;
    private SharedPreferences sh;
    private StringBuilder dataStringBuilder = new StringBuilder();
    private List<UsbDevice> usbDevices = new ArrayList<>();
    private static final String ACTION_USB_PERMISSION = "com.iotera.cba9handler.USB_PERMISSION";
    private static final long DOUBLE_BACK_PRESS_DELAY = 2000;
    private long backButtonPressedTime;
    private static final String balance_key = "com.iotera.cba9handler.CASH_BALANCE";

    Random rand = new Random();

    @Override
    public void onBackPressed() {
        if (backButtonPressedTime + DOUBLE_BACK_PRESS_DELAY > System.currentTimeMillis()) {
            super.onBackPressed();
            disable();
            closeSerialPort();
            finish();
            return;
        } else {
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        backButtonPressedTime = System.currentTimeMillis();
    }


    private final UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {

        @Override
        public void onReceivedData(byte[] arg0) {
            for (byte b : arg0) {
                dataStringBuilder.append(String.format("%02X", b));
                String data = dataStringBuilder.toString();
                flushReceivedData(data);
            }
        }

        private void flushReceivedData(String data) {

            if (data.length() >= 4){
                String msgLengthStr = data.substring(0,4);
                final String messageToDisplay = handleData(msgLengthStr);
                dataStringBuilder.setLength(0);
                if (messageToDisplay == null || messageToDisplay.isEmpty()) {
                return;
            }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvReceivedData.append(messageToDisplay + "\n");
                        ((ScrollView) tvReceivedData.getParent()).fullScroll(View.FOCUS_DOWN); // Scroll to the bottom
                    }
                });
            }
        }
    };

    private void addBalance(int cash) {
        if (sh != null) {
            int balance = sh.getInt("balance", 0);
            int newBalance = balance + cash;
            SharedPreferences.Editor editor = sh.edit();
            editor.putInt("balance", newBalance);
            editor.apply();
            final String strBalance = "Saldo Rp. " + newBalance;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvBalance.setText(strBalance);
                }
            });
        }
    }

    private void clrBalance() {
        if (sh != null) {
            int newBalance = 0;
            SharedPreferences.Editor editor = sh.edit();
            editor.putInt("balance", newBalance);
            editor.apply();
            final String strBalance = "Saldo Rp. " + newBalance;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvBalance.setText(strBalance);
                }
            });
        }
    }

    private String handleData(String data) {
        String response = null;
        String msg = data.substring(0, 2);
        switch (msg) {
            case "80":
                String power_on = data.substring(2,4);
                switch (power_on) {
                    case "8F":
                        response = "POWER ON";
                        break;
                }
                break;
            case "10":
                response = "Stacking";
                break;
            case "81":
                String bill_stacked = data.substring(2, 4);
                Integer cash = null;
                switch (bill_stacked) {
                    case "40":
                        response = "IDR 1000 Accepted";
                        cash = 1000;
                        break;
                    case "41":
                        response = "IDR 2000 Accepted";
                        cash = 2000;
                        break;
                    case "42":
                        response = "IDR 5000 Accepted";
                        cash = 5000;
                        break;
                    case "43":
                        response = "IDR 10000 Accepted";
                        cash = 10000;
                        break;
                    case "44":
                        response = "IDR 20000 Accepted";
                        cash = 20000;
                        break;
                    case "45":
                        response = "IDR 50000 Accepted";
                        cash = 50000;
                        break;
                    case "46":
                        response = "IDR 100000 Accepted";
                        cash = 100000;
                        break;
                }
                addBalance(cash);
                break;
            case "11":
                response = "Rejected";
                break;
            case "29":
                String failed = data.substring(2, 4);
                switch (failed){
                    case "2F":
                        response = "Failed";
                        break;
                }
                break;
        }
        return response;
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                // Device attached
                Toast.makeText(MainActivity.this, "USB Plugged", Toast.LENGTH_SHORT).show();
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(device, pi);
                Log.i("[USB Device]", device.toString());
                if (device != null) {
                    usbDevices.add(device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                // Device detached
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Toast.makeText(MainActivity.this, "USB Removed", Toast.LENGTH_SHORT).show();
                Log.i("[USB Device]", "USB Removed");
                if (device != null) {
                    usbDevices.remove(device);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        tvReceivedData = findViewById(R.id.tvReceivedData);
        tvBalance = findViewById(R.id.tvBalance);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        sh = getSharedPreferences(balance_key, MODE_PRIVATE);
        if (sh != null) {
            int balance = sh.getInt("balance", 0);
            String strBalance = "Saldo Rp. " + balance;
            tvBalance.setText(strBalance);
        }
        Button btnClrBalance = findViewById(R.id.btnClearBal);
        Button btnOpenSerialPort = findViewById(R.id.btnOpenSerialPort);
        Button btnEnable = findViewById(R.id.btnEnable);
        Button btnDisable = findViewById(R.id.btnDisable);
        Button btnCloseSerialPort = findViewById(R.id.btnClosePort);
        btnOpenSerialPort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSerialPort();
            }
        });
        btnClrBalance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clrBalance();
            }
        });

        btnCloseSerialPort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSerialPort();
            }
        });

        btnDisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usbSerialDevice != null) {
                    disable();
                }
            }
        });

        btnEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fire();
            }
        });
    }

    public static String BuildMessage(String data) {
        String STX = "7F";
        int seqId = (sequenceFlag << 7) | Address;
        sequenceFlag = 1 - sequenceFlag;
        String sequenceId = String.format("%02X", seqId);
        byte[] byteData = HexUtil.hexStringToByteArray(data);
        int length = byteData.length;
        String strLength = String.format("%02X", length);
        String msg = sequenceId + strLength + data;
        byte[] bytes = HexUtil.hexStringToByteArray(msg);
        String crcHexString = SerialUtil.calculateCRC16(bytes);
        return STX + msg + crcHexString;
    }
    private void fire() {
        if (usbSerialDevice != null) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    enable();
                }
            }, 2000);
            handler.post(sendDataRunnable);
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable sendDataRunnable = new Runnable() {
        @Override
        public void run() {
            poll();
            handler.postDelayed(this, 500);
        }
    };


    private void enable() {
        String msg = BuildMessage("0A");
        byte[] msg_byte = HexUtil.hexStringToByteArray(msg);
        usbSerialDevice.write(msg_byte);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvReceivedData.append("Enabling BV" + "\n");
                ((ScrollView) tvReceivedData.getParent()).fullScroll(View.FOCUS_DOWN);
            }
        });
        Log.i("[BV]", "BV Enabled");
    }


    private void poll() {
        String msg = BuildMessage("07");
        byte[] msg_byte = HexUtil.hexStringToByteArray(msg);
        usbSerialDevice.write(msg_byte);
    }

    private void disable() {
        String msg = BuildMessage("09");
        byte[] msg_byte = HexUtil.hexStringToByteArray(msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvReceivedData.append("Disabling BV" + "\n");  // Append the complete message with a new line
                ((ScrollView) tvReceivedData.getParent()).fullScroll(View.FOCUS_DOWN); // Scroll to the bottom
            }
        });
        usbSerialDevice.write(msg_byte);
        Log.i("[BV]", "BV Disabled");
    }

    private void closeSerialPort() {
        if (usbSerialDevice != null) {
            usbSerialDevice.close();
            Log.i("[USB Device]", "Serial Port Closed");
            tvReceivedData.setText(null);
        }
    }


    private void openSerialPort() {
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (UsbDevice device : usbDevices) {
                UsbDeviceConnection connection = usbManager.openDevice(device);
                usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
                if (usbSerialDevice != null) {
                    if (usbSerialDevice.open()) {
                        usbSerialDevice.setBaudRate(9600);
                        usbSerialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                        usbSerialDevice.setStopBits(UsbSerialInterface.STOP_BITS_2);
                        usbSerialDevice.setParity(UsbSerialInterface.PARITY_NONE);
                        usbSerialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                        Toast.makeText(this, "USB Device Open", Toast.LENGTH_SHORT).show();
                        usbSerialDevice.read(mCallback); // start reading data from USB device

                        keep = false;
                    }
                }

                if (!keep) break;
            }

            if (keep) {
                Toast.makeText(this, "No suitable USB device found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No USB device connected", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
    }


}
