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
import java.io.InputStream;
import java.io.OutputStream;
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
    private ConnectThread connectThread; // Declare connectThread variable
    private TextView signalStrengthTextView;
    private Button calculateDistanceButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothStatusTextView = findViewById(R.id.bluetoothStatusTextView);
        bluetoothConnectionStatus = findViewById(R.id.bluetoothConnectionStatus);

        Log.d("MainActivity", "onCreate: Checking connection status");
        if (connectedDevice != null) {
            Log.d("MainActivity", "onCreate: Device is already connected, disconnecting...");
            disconnectDevice(connectThread);
        } else {
            Log.d("MainActivity", "onCreate: No device connected");
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            bluetoothStatusTextView.setText("Bluetooth not supported on this device");
        } else {
            checkBluetoothStatus();
        }

        signalStrengthTextView = findViewById(R.id.distanceTextView);
        calculateDistanceButton = findViewById(R.id.measureDistanceButton);

        calculateDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call functions for distance measuring and display the results in the TextView
                int signalLevel = measureSignalStrength(TARGET_DEVICE_NAME);
                double distance = calculateDistance(signalLevel);
                if (signalLevel == -25){
                    signalStrengthTextView.setText("Bluetooth not supported by device");
                } else if (signalLevel == -50) {
                    signalStrengthTextView.setText("Device not paired");
                } else if (signalLevel == -100){
                    signalStrengthTextView.setText("Error while obtaining RSSI");
                } else {
                    signalStrengthTextView.setText("\nSignal Strength: " + signalLevel + "\nEstimated Distance:\n" + distance + " meters");
                }
            }
        });

        Button connectButton = findViewById(R.id.button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToDevice(TARGET_DEVICE_NAME);
            }
        });

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

        // Check for already connected devices
        Set<BluetoothDevice> connectedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : connectedDevices) {
            if (device.getName().equals(TARGET_DEVICE_NAME)) {
                connectedDevice = device;
                break;
            }
        }


        // Register broadcast receiver for Bluetooth state changes
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);

        // Register broadcast receiver for Bluetooth connection events
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter2.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothConnectionReceiver, filter2);

        // Update Bluetooth message to reflect current connection status
        updateBluetoothMessage();
    }

    // Function to measure signal strength (replace with your implementation)
    private int measureSignalStrength(String deviceName) {
        // Check if Bluetooth is supported on the device
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return -25; // Bluetooth not supported
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Permission hasn't been granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            // After this call, your onRequestPermissionsResult() method will be called
            bluetoothStatusTextView.setText("Permissions not granted!");
        }

        // Get a list of currently paired Bluetooth devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // Iterate through the list to find the specified device
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(deviceName)) {
                // Use a BluetoothSocket to connect to the device and obtain RSSI
                try {
                    // Create a BluetoothSocket for the specified device
                    BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.randomUUID());
                    // Connect to the device
                    socket.connect();
                    // Get the InputStream and OutputStream from the socket
                    InputStream inputStream = socket.getInputStream();
                    OutputStream outputStream = socket.getOutputStream();
                    // Request RSSI from the device
                    outputStream.write(0x42); // Arbitrary value to request RSSI
                    // Read RSSI response from the device
                    int rssi = inputStream.read();
                    // Close the socket
                    socket.close();
                    // Return the RSSI value
                    return rssi;
                } catch (IOException e) {
                    e.printStackTrace();
                    // Error occurred while obtaining RSSI
                    return -100; // Return a default value
                }
            }
        }

        // Device not found among paired devices
        return -50; // Return a default value
    }



    // Function to calculate distance based on signal strength (replace with your implementation)
    private double calculateDistance(int signalLevel) {
        // Dummy implementation for demonstration purposes
        return Math.pow(10, ((-65 - signalLevel) / 20.0)); // Replace this with actual distance calculation
    }

    private void disconnectDevice(ConnectThread connectThread) {
        if (connectedDevice != null) {
            // Perform disconnection operations here
            try {
                // Close the Bluetooth socket
                if (connectThread != null) {
                    connectThread.cancel();
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error occurred while disconnecting", e);
            } finally {
                connectedDevice = null;
            }
        }
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
                disconnectDevice(connectThread); // Pass connectThread to disconnectDevice
                return;
            }
        }
        bluetoothConnectionStatus.setText("Device not found in paired devices");
    }

    public class ConnectThread extends Thread {
        private final BluetoothDevice device;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private BluetoothSocket socket;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            try {
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
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e("ConnectThread", "Socket creation failed", e);
            }
        }

        public void run() {
            if (socket == null) {
                return;
            }

            // Connect to the Bluetooth device
            try {
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
                socket.connect();
            } catch (IOException e) {
                Log.e("ConnectThread", "Socket connection failed", e);
                // Close the socket in case of failure
                try {
                    socket.close();
                } catch (IOException closeException) {
                    Log.e("ConnectThread", "Error closing socket", closeException);
                }
                return; // Exit run() method if connection fails
            }

            // Connection success, manage the connection...
        }

        public void cancel() {
            try {
                if (socket != null) {
                    socket.close(); // Close the socket to disconnect
                }
            } catch (IOException e) {
                Log.e("ConnectThread", "Error when closing socket", e);
            }
        }
    }

    private void updateBluetoothMessage() {
        Log.d("updateBluetoothMessage", "Updating Bluetooth message");
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
                        Log.d("updateBluetoothMessage", "Connected to device: " + connectedDevice.getName());
                    } else {
                        bluetoothStatusTextView.append("\nNot connected to any device");
                        Log.d("updateBluetoothMessage", "Not connected to any device");
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
                // Update UI to reflect reconnection
                updateBluetoothMessage();
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.equals(connectedDevice)) {
                    // Reset connectedDevice variable when the connected device is disconnected
                    connectedDevice = null;
                    // Update the Bluetooth message to reflect the disconnection
                    updateBluetoothMessage();
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
                    Log.d("Bluetooth", "Device disconnected: " + device.getName());
                }
            }
        }
    };





    @Override
    protected void onResume() {
        super.onResume();
        // Register broadcast receiver for Bluetooth connection events
        registerReceiver(bluetoothConnectionReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        registerReceiver(bluetoothConnectionReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        // Update Bluetooth message to reflect current connection status
        updateBluetoothMessage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister broadcast receiver to avoid memory leaks
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
