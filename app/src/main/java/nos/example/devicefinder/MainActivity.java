package nos.example.devicefinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.Manifest;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.media.MediaPlayer;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private TextView signalStrengthTextView;
    private Button measureDistanceButton;

    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};

    private DecimalFormat decimalFormat = new DecimalFormat("#.####");
    private MediaPlayer mediaPlayer;
    private boolean isContinuousFeedbackRunning = false;
    private ArrayList<ScanResult> wifiArrayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Spinner spinner; // Change from ListView to Spinner
    private ScanResult selectedNetwork;
    private Handler mHandler = new Handler();
    private int wifiStateExtra = -200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinner = findViewById(R.id.spinner); // Initialize Spinner
        signalStrengthTextView = findViewById(R.id.distanceTextView);
        measureDistanceButton = findViewById(R.id.measureDistanceButton);

        // Update ArrayAdapter initialization
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getSSIDList());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            // If Wi-Fi is disabled, enable it
            wifiManager.setWifiEnabled(true);
        }

        // Register BroadcastReceiver for scan results
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // Schedule the first scan immediately
        mHandler.post(scanRunnable);

        // Listen for item selection events on the Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                // Retrieve the selected network
                selectedNetwork = wifiArrayList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Handle case when nothing is selected
            }
        });

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.mario_song);

        measureDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPermissions()) {
                    if (!isContinuousFeedbackRunning) {
                        // Start continuous feedback
                        isContinuousFeedbackRunning = true;
                        // Change button text to indicate continuous feedback is running
                        measureDistanceButton.setText("STOP SEARCH");
                        // Disable Spinner
                        spinner.setEnabled(false);
                        startContinuousFeedback();
                    } else {
                        spinner.setEnabled(true);
                        stopContinuousFeedback();
                    }
                } else {
                    requestPermissions();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks to avoid memory leaks
        mHandler.removeCallbacks(scanRunnable);
        // Unregister the broadcast receiver when the activity is destroyed to avoid memory leaks
        if (wifiReceiver != null) {
            unregisterReceiver(wifiReceiver);
        }
    }

    // Define a Runnable to initiate the scan
    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            // Start scanning for Wi-Fi networks
            wifiManager.startScan();

            Log.d("startScan", "SCANNING");

            // Schedule the next scan after 30 seconds
            mHandler.postDelayed(this, 30 * 1000); // 30 seconds in milliseconds
        }
    };

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Clear the existing list before adding new scan results
            wifiArrayList.clear();

            // Add the new scan results to the list
            wifiArrayList.addAll(wifiManager.getScanResults());

            // Update the adapter with the new data
            adapter.clear();
            adapter.addAll(getSSIDList());
            adapter.notifyDataSetChanged();
        }
    };

    // Helper method to extract SSID names from the scan results
    private ArrayList<String> getSSIDList() {
        ArrayList<String> ssidList = new ArrayList<>();
        if (wifiManager != null) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for (ScanResult scanResult : scanResults) {
                ssidList.add(scanResult.SSID);
            }
        }
        return ssidList;
    }

    private void startContinuousFeedback() {
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Sample rate for audio track
        int sampleRate = 44100;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        // Initialize AudioTrack
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_ALARM, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);

        // Check if initialization was successful
        if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            // Start playback
            audioTrack.play();
        } else {
            // Handle initialization error
            Log.e("MyAudioPlayer", "AudioTrack initialization failed");
        }

        // Start a new thread for continuous feedback
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (isContinuousFeedbackRunning) { // Check the flag before each iteration

                    // Retrieve and display the RSSI of the selected network
                    if (selectedNetwork != null) { // Add null check here

                        // Log details of the selected network
                        Log.d("ContinuousFeedback", "Selected network: " + selectedNetwork.SSID + ", RSSI: " + selectedNetwork.level);

                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        for (ScanResult result : scanResults) {
                            if (selectedNetwork != null && result.SSID.equals(selectedNetwork.SSID)) {
                                // Access RSSI value
                                wifiStateExtra = result.level;
                                // Log details of the selected network
                                Log.d("ContinuousFeedback", "Selected network: " + selectedNetwork.SSID + ", RSSI: " + wifiStateExtra);
                                break;
                            }
                        }

                        if (wifiStateExtra < -23) {
                            runOnUiThread(new Runnable() { // Updating UI inside runOnUiThread
                                @Override
                                public void run() {
                                    // Display signal strength level
                                    signalStrengthTextView.setText("Wifi Network: " + selectedNetwork.SSID +  "\n\nRSSI: " + wifiStateExtra + "\n\n");

                                    int signalLevel = WifiManager.calculateSignalLevel(wifiStateExtra, 5);

                                    // Display signal strength level
                                    signalStrengthTextView.append("Signal Strength: " + signalLevel);
                                }
                            });

                            int vibrationAmplitude = calculateVibrationAmplitude(Math.abs(wifiStateExtra));
                            int vibrationDuration = 100;

                            if (vibrator.hasVibrator()) {
                                // Create a waveform vibration effect
                                VibrationEffect effect = VibrationEffect.createOneShot(vibrationDuration, vibrationAmplitude);
                                vibrator.vibrate(effect);
                            }

                            // Generate and play tone
                            double frequency = mapValueToFrequency(Math.abs(wifiStateExtra));
                            double volume = 0.05;

                            short[] buffer = new short[bufferSize];
                            for (int i = 0; i < bufferSize; i++) {
                                double sample = Math.sin(2 * Math.PI * i * frequency / sampleRate);
                                buffer[i] = (short) (sample * Short.MAX_VALUE * volume);
                            }

                            audioTrack.write(buffer, 0, bufferSize);
                        }

                        if (wifiStateExtra > -24) {
                            playMarioSong();
                            isContinuousFeedbackRunning = false;

                            // UI update when the thread is done
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Update UI after thread completion
                                    signalStrengthTextView.setText("\n\n  Congratulations, you found the router!\n");
                                    signalStrengthTextView.append("RSSI: " + wifiStateExtra);
                                    measureDistanceButton.setText("START SEARCH");
                                }
                            });
                        }

                        try {
                            int intervalSound = calculateSoundInterval(Math.abs(wifiStateExtra));
                            // Wait for a short duration before the next feedback
                            Thread.sleep(intervalSound); // Adjust this value as needed
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // UI update when the thread is done
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Update UI after thread completion
                                signalStrengthTextView.setText("Wifi network not selected\nPlease choose one from the list");
                                measureDistanceButton.setText("START SEARCH");
                            }
                        });
                        isContinuousFeedbackRunning = false;
                    }
                }

                // Release resources or perform any necessary cleanup here
                vibrator.cancel(); // Cancel vibration when the thread exits
                audioTrack.stop(); // Stop audio track when the thread exits
                audioTrack.release(); // Release audio track resources
            }
        }).start();
    }


    private void stopContinuousFeedback() {
        // Implement this method to stop the continuous feedback loop
        isContinuousFeedbackRunning = false; // Update the flag
        measureDistanceButton.setText("START SEARCH");
        spinner.setEnabled(true); // Re-enable the Spinner
    }

    private void playMarioSong() {
        if (mediaPlayer != null) {
            mediaPlayer.start(); // Start playing the Mario song
        }
    }

    private int mapValueToFrequency(int value) {
        // Map the value to a frequency between 20 and 60, with 20 having the lowest frequency and 60 having the highest
        // Use a higher power of the normalized value to increase sensitivity
        double normalizedValue = Math.min(1, Math.max(0, 1.0 - (double) (value - 20) / (80 - 20))); // Invert the normalization and normalize the value between 0 and 1
        // Adjust the scaling factor to make changes in value more noticeable in the frequency
        int frequencyRange = 5000 - 1000; // Define the frequency range
        return (int) (1000 + Math.pow(normalizedValue, 4) * frequencyRange); // Map the normalized value to the frequency range
    }

    private int calculateVibrationAmplitude(int value) {
        double normalizedValue = (double) Math.min(1, Math.max(0, (value - 20) / (80 - 20))); // Normalize the value between 0 and 1
        int frequencyRange = 255 - 1;
        int vibrationAmplitude = (int) (255 - Math.pow(normalizedValue, 4) * frequencyRange); // Calculate vibration amplitude
        Log.d("VibrationAmplitude", "Amplitude calculated: " + vibrationAmplitude); // Log the result
        return vibrationAmplitude;
    }

    private int calculateSoundInterval(int value) {
        if (value < 30){
            return 250;
        } else if (value < 35 && value >= 30) {
            return 400;
        } else if (value < 40 && value >= 35) {
            return 650;
        } else if (value < 45 && value >= 40) {
            return 800;
        } else if (value < 55 && value >= 45) {
            return 950;
        } else if (value < 60 && value >= 55) {
            return 1100;
        } else if (value >= 60 && value < 70){
            return 1350;
        } else if (value >= 70 && value < 80) {
            return 1500;
        } else if (value >= 90 && value < 80) {
            return 1750;
        } else {
            return 2000;
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
}
