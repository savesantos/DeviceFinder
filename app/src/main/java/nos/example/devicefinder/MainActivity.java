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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.media.MediaPlayer;

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
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signalStrengthTextView = findViewById(R.id.distanceTextView);
        measureDistanceButton = findViewById(R.id.measureDistanceButton);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.mario_song);

        measureDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPermissions()) {
                    int wifiStateExtra = wifiManager.getConnectionInfo().getRssi();

                    // Display signal strength level
                    signalStrengthTextView.setText("RSSI: " + wifiStateExtra + "\n\n");

                    int signalLevel = WifiManager.calculateSignalLevel(wifiStateExtra, 5);

                    // Display signal strength level
                    signalStrengthTextView.append("Signal Strength: " + signalLevel);

                    if (Math.abs(wifiStateExtra) > 23) {

                        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                        int vibrationAmplitude = calculateVibrationAmplitude(Math.abs(wifiStateExtra));
                        long vibrationDuration = calculateVibrationDuration(Math.abs(wifiStateExtra));
                        int audioVolume = calculateAudioVolume(Math.abs(wifiStateExtra));

                        signalStrengthTextView.append("\n\n Vibration Strength: " + vibrationAmplitude);

                        if (vibrator.hasVibrator()) {
                            VibrationEffect effect = VibrationEffect.createOneShot(vibrationDuration, vibrationAmplitude);
                            vibrator.vibrate(effect);
                            playSoundAndVibrate(Math.abs(wifiStateExtra), audioVolume);
                        }
                    } else {
                        playMarioSong();
                        signalStrengthTextView.append("\n\n  Congratulations, you found the router!");
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
        // Unregister the broadcast receiver when the activity is destroyed to avoid memory leaks
        if (wifiReceiver != null) {
            unregisterReceiver(wifiReceiver);
        }
    }

    private void playSoundAndVibrate(int value, int volume) {
        // Play sound
        playSound(value, volume);
    }

    private void playMarioSong() {
        if (mediaPlayer != null) {
            mediaPlayer.start(); // Start playing the Mario song
        }
    }

    private void playSound(int value, int volume) {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, volume);
        // Map value to frequency
        int frequency = mapValueToFrequency(value);
        // Start tone with specified frequency
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, frequency); // Adjust tone type as needed
    }

    private int mapValueToFrequency(int value) {
        // Map the value to a frequency between 20 and 60, with 20 having the lowest frequency and 60 having the highest
        // Use a higher power of the normalized value to increase sensitivity
        double normalizedValue = Math.max(0, 1.0 - (double) (value - 20) / (50 - 20)); // Invert the normalization and normalize the value between 0 and 1
        // Adjust the scaling factor to make changes in value more noticeable in the frequency
        int frequencyRange = 1000 - 20; // Define the frequency range
        return (int) (20 + Math.pow(normalizedValue, 4) * frequencyRange); // Map the normalized value to the frequency range
    }

    private int calculateAudioVolume(int value) {
        // Map value to vibration duration
        double normalizedValue = (double) Math.max(0, (value - 20) / (50 - 20)); // Normalize the value between 0 and 1
        // Volume between 100 and 0
        int frequencyRange = 100 - 0;
        return (int) (100 - Math.pow(normalizedValue, 4) * frequencyRange);
    }

    private long calculateVibrationDuration(int value) {
        // Map value to vibration duration
        long normalizedValue = (long) Math.max(0, (value - 20) / (50 - 20)); // Normalize the value between 0 and 1
        // Vibrate for 1 second to 0.1 second based on value
        int frequencyRange = 1000 - 10;
        return (long) 1000 + normalizedValue * frequencyRange;
    }

    private int calculateVibrationAmplitude(int value) {
        // You can adjust this formula as per your requirement
        // For example, you can use a linear scale to map the integer value to vibration amplitude
        // The lower the value, the higher the vibration amplitude
        // You can experiment with different formulas to get the desired effect
        return Math.max(0, Math.min(255, 255 - (value - 20) * 51 / 8)); // Example formula
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
