package nos.example.devicefinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private TextView signalStrengthTextView;
    private Button measureDistanceButton;
    private BroadcastReceiver wifiReceiver;

    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};

    private DecimalFormat decimalFormat = new DecimalFormat("#.####");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signalStrengthTextView = findViewById(R.id.distanceTextView);
        measureDistanceButton = findViewById(R.id.measureDistanceButton);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        measureDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPermissions()) {
                    int wifiStateExtra = wifiManager.getConnectionInfo().getRssi();
                    int signalLevel = WifiManager.calculateSignalLevel(wifiStateExtra, 5);

                    // Display signal strength level
                    signalStrengthTextView.setText("Signal Strength: " + signalLevel);

                    // Estimate distance based on signal strength (this is a simple example, actual distance estimation may require calibration)
                    double distance = calculateDistance(wifiStateExtra);

                    // Format the distance to the fourth decimal case
                    String roundedDistance = decimalFormat.format(distance);

                    // Display estimated distance
                    signalStrengthTextView.append("\n\nEstimated Distance: " + roundedDistance + " meters");
                } else {
                    requestPermissions();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the broadcast receiver when the activity is destroyed to avoid memory leaks
        if (wifiReceiver != null) {
            unregisterReceiver(wifiReceiver);
        }
    }

    private boolean hasPermissions() {
        int result;
        for (String permission : REQUIRED_PERMISSIONS) {
            result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Call the superclass implementation to ensure the framework gets the expected behavior
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private double calculateDistance(int signalLevel) {
        // Simple distance estimation formula based on signal strength
        // This formula needs to be calibrated for your specific environment
        return Math.pow(10, ((-65 - signalLevel) / 20.0));
    }
}
