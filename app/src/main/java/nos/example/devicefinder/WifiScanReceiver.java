package nos.example.devicefinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.core.content.ContextCompat;

import java.util.List;

public class WifiScanReceiver extends BroadcastReceiver {
    private WifiManager wifiManager;
    private ListView listView;

    public WifiScanReceiver(WifiManager wifiManager, ListView listView) {
        this.wifiManager = wifiManager;
        this.listView = listView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check for the ACCESS_FINE_LOCATION permission before proceeding
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, so return and do nothing
            return;
        }

        List<ScanResult> scanResults = wifiManager.getScanResults();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
        for (ScanResult scanResult : scanResults) {
            adapter.add(scanResult.SSID + " (" + scanResult.level + " dBm)");
        }
        listView.setAdapter(adapter);
    }
}