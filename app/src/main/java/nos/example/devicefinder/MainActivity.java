package nos.example.devicefinder;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TARGET_DEVICE_NAME = "JBL TUNE660NC";
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private BluetoothAdapter bluetoothAdapter;
    private TextView distanceTextView;
    private MyBluetoothService.ConnectedThread connectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        distanceTextView = findViewById(R.id.distanceTextView);

        Button measureDistanceButton = findViewById(R.id.measureDistanceButton);
        measureDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToDevice(TARGET_DEVICE_NAME);
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
        } else {
            checkBluetoothStatus();
        }
    }

    private void checkBluetoothStatus() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_PERMISSION);
        }
    }

    private void connectToDevice(String deviceName) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(deviceName)) {
                // Creating and starting the ConnectedThread
                connectedThread = new MyBluetoothService.ConnectedThread(device, new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == MyBluetoothService.MessageConstants.MESSAGE_READ) {
                            String distanceData = (String) msg.obj;
                            updateDistanceUI(distanceData);
                        }
                    }
                }, this);
                connectedThread.start();
                return;
            }
        }
        Toast.makeText(this, "Device not found in paired devices", Toast.LENGTH_SHORT).show();
    }

    private void updateDistanceUI(String distanceData) {
        distanceTextView.setText(distanceData);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }
}
