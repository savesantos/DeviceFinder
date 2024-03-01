package nos.example.devicefinder;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Declare TextView variable for displaying the message
    private TextView bluetoothStatusTextView;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1; // You can use any integer value here


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize bluetoothStatusTextView
        bluetoothStatusTextView = findViewById(R.id.bluetoothStatusTextView);

        // Check Bluetooth connection status and update message
        updateBluetoothMessage();

        // Use this check to determine whether Bluetooth classic is supported on the device.
        // Then you can selectively disable BLE-related features.
        boolean bluetoothAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            bluetoothStatusTextView.setText("Bluetooth not supported on this device");
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


        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }

        // Register BroadcastReceiver to listen for Bluetooth state changes
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, update the message
                updateBluetoothMessage();
            } else {
                // Permission denied, handle this as appropriate
                bluetoothStatusTextView.setText("Bluetooth permissions denied");
            }
        }
    }


    // Maintain a list of connected devices
    private List<BluetoothDevice> connectedDevices = new ArrayList<>();

    // Method to update the message based on Bluetooth connection status
    // Method to update the message based on Bluetooth connection status
    private void updateBluetoothMessage() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            bluetoothStatusTextView.setText("Bluetooth not supported on this device");
        } else {
            if (bluetoothAdapter.isEnabled()) {
                // Bluetooth is enabled
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // Permission hasn't been granted, request it
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                    // After this call, your onRequestPermissionsResult() method will be called
                    bluetoothStatusTextView.setText("Permissions not granted!");
                } else {
                    // Permission has been granted, proceed with your operation
                    // For example:
                    // Do something with Bluetooth
                    bluetoothStatusTextView.setText("Bluetooth permissions granted");
                }
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                StringBuilder devicesText = new StringBuilder();
                int count = 0;
                for (BluetoothDevice device : pairedDevices) {
                    if (count < 4) {
                        String deviceName = device.getName();
                        devicesText.append(deviceName).append("\n");
                        count++;
                    } else {
                        break; // Stop iterating after reaching the limit
                    }
                }
                if (count > 0) {
                    bluetoothStatusTextView.setText("Bluetooth enabled\n\nPaired Devices:\n" + devicesText.toString());
                } else {
                    bluetoothStatusTextView.setText("Bluetooth enabled, but no paired devices");
                }
            } else {
                // Bluetooth is not enabled
                bluetoothStatusTextView.setText("Bluetooth is disabled");
            }
        }
    }

    // Method to check if the device is connected to a specific Bluetooth device
    private boolean isConnected(BluetoothDevice device) {
        for (BluetoothDevice connectedDevice : connectedDevices) {
            if (connectedDevice.getAddress().equals(device.getAddress())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // more code

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(bluetoothStateReceiver);
    }

    // BroadcastReceiver to listen for changes in Bluetooth state
    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                updateBluetoothMessage();
            }
        }
    };

    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket = null;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        private static final String TAG = "AcceptThread";
        private static final String NAME = "DeviceFinderBluetoothServiceName";

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // Permission hasn't been granted, request it
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                    // After this call, your onRequestPermissionsResult() method will be called
                    bluetoothStatusTextView.setText("Permissions not granted!");
                } else {
                    // Permission has been granted, proceed with your operation
                    // For example:
                    // Do something with Bluetooth
                    bluetoothStatusTextView.setText("Bluetooth permissions granted");
                }
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }

        private void manageMyConnectedSocket(BluetoothSocket socket) {
            // Create and start the thread for data transfer
            MyBluetoothService.ConnectedThread connectedThread = new MyBluetoothService.ConnectedThread(socket);
            connectedThread.start();
        }
    }

    public static class MyBluetoothService {
        private static final String TAG = "MY_APP_DEBUG_TAG";
        private Handler handler; // handler that gets info from Bluetooth service

        // Defines several constants used when transmitting messages between the
        // service and the UI.
        private interface MessageConstants {
            public static final int MESSAGE_READ = 0;
            public static final int MESSAGE_WRITE = 1;
            public static final int MESSAGE_TOAST = 2;

            // ... (Add other message types here as needed.)
        }

        private static class ConnectedThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;
            private byte[] mmBuffer; // mmBuffer store for the stream
            private Handler handler;

            public ConnectedThread(BluetoothSocket socket) {
                mmSocket = socket;
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams; using temp objects because
                // member streams are final.
                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                }
                try {
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating output stream", e);
                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            }

            public void run() {
                mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    try {
                        // Read from the InputStream.
                        numBytes = mmInStream.read(mmBuffer);
                        // Send the obtained bytes to the UI activity.
                        Message readMsg = handler.obtainMessage(
                                MessageConstants.MESSAGE_READ, numBytes, -1,
                                mmBuffer);
                        readMsg.sendToTarget();
                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
            }

            // Call this from the main activity to send data to the remote device.
            public void write(byte[] bytes) {
                try {
                    mmOutStream.write(bytes);

                    // Share the sent message with the UI activity.
                    Message writtenMsg = handler.obtainMessage(
                            MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                    writtenMsg.sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when sending data", e);

                    // Send a failure message back to the activity.
                    Message writeErrorMsg =
                            handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                    Bundle bundle = new Bundle();
                    bundle.putString("toast",
                            "Couldn't send data to the other device");
                    writeErrorMsg.setData(bundle);
                    handler.sendMessage(writeErrorMsg);
                }
            }

            // Call this method from the main activity to shut down the connection.
            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the connect socket", e);
                }
            }
        }
    }
}
