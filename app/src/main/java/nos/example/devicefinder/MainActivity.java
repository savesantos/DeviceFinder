// MainActivity.java
package nos.example.devicefinder;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private TextView bluetoothStatusTextView;
    private TextView bluetoothConnectionStatus;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final String TARGET_DEVICE_NAME = "JBL TUNE660NC";
    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_BLUETOOTH_SCAN_PERMISSION = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothStatusTextView = findViewById(R.id.bluetoothStatusTextView);
        bluetoothConnectionStatus = findViewById(R.id.bluetoothConnectionStatus);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            bluetoothStatusTextView.setText("Bluetooth not supported on this device");
        } else {
            checkBluetoothStatus();
        }

        Button connectButton = findViewById(R.id.button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToDevice(TARGET_DEVICE_NAME);
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);
    }

    private void checkBluetoothStatus() {
        if (bluetoothAdapter.isEnabled()) {
            // Perform Bluetooth initialization
            initializeBluetooth();
            // Check Bluetooth connect permission
            checkBluetoothConnectPermission();
            // Update Bluetooth message immediately
            updateBluetoothMessage();
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    // Override onRequestPermissionsResult() to handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // If Bluetooth permissions are granted, check Bluetooth status again
                checkBluetoothStatus();
            } else {
                // If Bluetooth permissions are denied, inform the user
                Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void checkBluetoothConnectPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Permission hasn't been granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            // After this call, your onRequestPermissionsResult() method will be called
            bluetoothStatusTextView.setText("Permissions not granted!");
        } else {
            // Permission has been granted, proceed with your operation
            // For example:
            // Do something with Bluetooth
            // bluetoothStatusTextView.setText("Bluetooth permissions granted on creation!");
        }
    }


    private void initializeBluetooth() {
        // Initialize Bluetooth components and perform Bluetooth operations here
        // For example, you can start Bluetooth scanning, connect to devices, etc.
    }


    private void connectToDevice(String deviceName) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Permission hasn't been granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            // After this call, your onRequestPermissionsResult() method will be called
            bluetoothStatusTextView.setText("Permissions not granted!");
        } else {
            // Permission has been granted, proceed with your operation
            // For example:
            // Do something with Bluetooth
            // bluetoothStatusTextView.setText("Bluetooth permissions granted on creation!");
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(deviceName)) {
                ConnectThread connectThread = new ConnectThread(device);
                connectThread.start();
                return;
            }
        }
        bluetoothConnectionStatus.setText("Device not found in paired devices");
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice device;
        private final BluetoothAdapter adapter;
        private BluetoothAdapter bluetoothAdapter;
        private BluetoothAdapter tmpAdapter;
        private BluetoothDevice tmpDevice;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothAdapter tmpAdapter = null;
            try {
                tmpAdapter = BluetoothAdapter.getDefaultAdapter();
            } catch (Exception e) {
                Log.e("ConnectThread", "BluetoothAdapter getDefaultAdapter() failed", e);
            }
            adapter = tmpAdapter;
            BluetoothDevice tmpDevice = null;
            try {
                tmpDevice = device;
            } catch (Exception e) {
                Log.e("ConnectThread", "BluetoothDevice creation failed", e);
            }
            this.tmpAdapter = adapter;
            this.tmpDevice = tmpDevice;
        }

        public void run() {
            if (adapter == null || !adapter.isEnabled() || tmpDevice == null) {
                return;
            }

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Permission hasn't been granted, request it
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                // After this call, your onRequestPermissionsResult() method will be called
                bluetoothStatusTextView.setText("Permissions not granted!");
            } else {
                // Permission has been granted, proceed with your operation
                // For example:
                // Do something with Bluetooth
                // bluetoothStatusTextView.setText("Bluetooth permissions granted on creation!");
            }

            adapter.cancelDiscovery();

            try {
                BluetoothSocket socket = tmpDevice.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
            } catch (IOException e) {
                Log.e("ConnectThread", "Socket connection failed", e);
            }
        }
    }

    private void updateBluetoothMessage() {
        if (bluetoothAdapter == null) {
            bluetoothStatusTextView.setText("Bluetooth not supported on this device");
        } else {
            if (bluetoothAdapter.isEnabled()) {
                try {
                    // Check for BLUETOOTH_CONNECT permission
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // Permission hasn't been granted, request it
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                        // After this call, your onRequestPermissionsResult() method will be called
                        return; // Return without setting text to avoid overriding the text
                    }

                    // Permission is granted, proceed with Bluetooth operations
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    StringBuilder devicesText = new StringBuilder();
                    int count = 0;
                    for (BluetoothDevice device : pairedDevices) {
                        if (count < 4) {
                            String deviceName = device.getName();
                            devicesText.append(deviceName).append("\n");
                            count++;
                        } else {
                            break;
                        }
                    }
                    if (count > 0) {
                        bluetoothStatusTextView.setText("Bluetooth enabled\n\nPaired Devices:\n" + devicesText.toString());
                    } else {
                        bluetoothStatusTextView.setText("Bluetooth enabled, but no paired devices");
                    }

                    if (connectedDevice != null) {
                        String connectedDeviceInfo = "Connected to: " + connectedDevice.getName();
                        bluetoothStatusTextView.append("\n" + connectedDeviceInfo);
                    }
                } catch (SecurityException e) {
                    // Handle the SecurityException, e.g., log or notify the user
                    Log.e("updateBluetoothMessage", "Permission check failed", e);
                    bluetoothStatusTextView.setText("Permission check failed!");
                }
            } else {
                bluetoothStatusTextView.setText("Bluetooth is disabled");
            }
        }
    }




    private final BroadcastReceiver bluetoothConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                connectedDevice = device;
                updateBluetoothMessage();
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                connectedDevice = null;
                updateBluetoothMessage();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothConnectionReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bluetoothConnectionReceiver);
    }

    private BluetoothDevice connectedDevice = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothStateReceiver);
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                updateBluetoothMessage();
            }
        }
    };
}
