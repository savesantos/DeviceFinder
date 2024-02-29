package nos.example.devicefinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private TextView bluetoothStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothStatusTextView = findViewById(R.id.bluetoothStatusTextView);

        // Check if Bluetooth permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_BLUETOOTH_PERMISSION);
        } else {
            // Permission is already granted, proceed with your Bluetooth functionality
            checkBluetoothConnection();
        }
    }

    // Check Bluetooth connection status and display the message
    private void checkBluetoothConnection() {
        // Get the name of the connected Bluetooth device
        String connectedDeviceName = getConnectedDeviceName();

        if (connectedDeviceName != null) {
            bluetoothStatusTextView.setText("Bluetooth: Connected to " + connectedDeviceName);
        } else {
            bluetoothStatusTextView.setText("Bluetooth: Not connected");
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with checking Bluetooth connection
                checkBluetoothConnection();
            } else {
                // Permission denied, show a message or disable Bluetooth functionality
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getConnectedDeviceName() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return null;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it from the user
            int requestCode = 1; // You can use any integer value here
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, requestCode);
        } else {
            // Permission is granted, proceed with getting bonded devices
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            // Continue with your code...
        }

        // Get a set of bonded (paired) Bluetooth devices
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> bondedDeviceList = new ArrayList<>(bondedDevices);

        // Check each bonded device
        for (BluetoothDevice device : bondedDeviceList) {
            // Check if we have permission to access device name
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
                String deviceName = device.getName();
                if (deviceName != null) {
                    // Do something with the connected device name
                    return deviceName;
                }
            } else {
                // We don't have permission to access device name
                // Handle this case as needed, e.g., request permission
                // or return a default value
                return null;
            }
        }
        return null;
    }


}
