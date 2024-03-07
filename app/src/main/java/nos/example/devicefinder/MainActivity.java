package nos.example.devicefinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
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
    private boolean isContinuousFeedbackRunning = false;


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
                    if (!isContinuousFeedbackRunning) {
                        // Start continuous feedback
                        isContinuousFeedbackRunning = true;
                        // Change button text to indicate continuous feedback is running
                        measureDistanceButton.setText("STOP SEARCH");
                        startContinuousFeedback();
                    } else {
                        stopContinuousFeedback(); // Stop continuous feedback if signal strength is low
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

        int wifiStateExtra = 120;

        // Start a new thread for continuous feedback
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isContinuousFeedbackRunning && wifiStateExtra > 25) { // Check the flag before each iteration
                    int wifiStateExtra = wifiManager.getConnectionInfo().getRssi();

                    runOnUiThread(new Runnable() { // Updating UI inside runOnUiThread
                        @Override
                        public void run() {
                            // Display signal strength level
                            signalStrengthTextView.setText("RSSI: " + wifiStateExtra + "\n\n");

                            int signalLevel = WifiManager.calculateSignalLevel(wifiStateExtra, 5);

                            // Display signal strength level
                            signalStrengthTextView.append("Signal Strength: " + signalLevel);
                        }
                    });

                    int vibrationAmplitude = calculateVibrationAmplitude(Math.abs(wifiStateExtra));
                    long vibrationDuration = calculateVibrationDuration(Math.abs(wifiStateExtra));

                    if (vibrator.hasVibrator()) {
                        // Define the pattern for continuous vibration (e.g., on-off-on-off pattern)
                        long[] pattern = {0, vibrationDuration};

                        // Create a waveform vibration effect
                        VibrationEffect effect = VibrationEffect.createWaveform(pattern, vibrationAmplitude);
                        vibrator.vibrate(effect);
                    }

                    // Generate and play tone
                    double frequency = mapValueToFrequency(Math.abs(wifiStateExtra));
                    double volume = mapValueToVolume(calculateAudioVolume(Math.abs(wifiStateExtra)));

                    short[] buffer = new short[bufferSize];
                    for (int i = 0; i < bufferSize; i++) {
                        double sample = Math.sin(2 * Math.PI * i * frequency / sampleRate);
                        buffer[i] = (short) (sample * Short.MAX_VALUE * volume);
                    }

                    audioTrack.write(buffer, 0, bufferSize);

                    try {
                        // Wait for a short duration before the next feedback
                        Thread.sleep(1000); // Adjust this value as needed
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // Release resources or perform any necessary cleanup here
                vibrator.cancel(); // Cancel vibration when the thread exits
                audioTrack.stop(); // Stop audio track when the thread exits
                audioTrack.release(); // Release audio track resources

                if (wifiStateExtra < 26){
                    playMarioSong();
                    isContinuousFeedbackRunning = false;

                    // UI update when the thread is done
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Update UI after thread completion
                            signalStrengthTextView.append("\n\n  Congratulations, you found the router!");
                            measureDistanceButton.setText("START SEARCH");
                        }
                    });
                }
            }
        }).start();
    }


    private void stopContinuousFeedback() {
        // Implement this method to stop the continuous feedback loop
        isContinuousFeedbackRunning = false; // Update the flag
        measureDistanceButton.setText("START SEARCH");
    }

    private void playMarioSong() {
        if (mediaPlayer != null) {
            mediaPlayer.start(); // Start playing the Mario song
        }
    }

    private int mapValueToFrequency(int value) {
        // Map the value to a frequency between 20 and 60, with 20 having the lowest frequency and 60 having the highest
        // Use a higher power of the normalized value to increase sensitivity
        double normalizedValue = Math.min(1, Math.max(0, 1.0 - (double) (value - 20) / (70 - 20))); // Invert the normalization and normalize the value between 0 and 1
        // Adjust the scaling factor to make changes in value more noticeable in the frequency
        int frequencyRange = 5000 - 20; // Define the frequency range
        return (int) (20 + Math.pow(normalizedValue, 4) * frequencyRange); // Map the normalized value to the frequency range
    }

    private double mapValueToVolume(int value) {
        // Map signal strength to volume level (0.0-1.0)
        double volume = Math.min(100.0, Math.max(0.0, (double) value / 100.0)); // Assuming value is a percentage (0-100)
        return volume;
    }

    private int calculateAudioVolume(int value) {
        // Map value to volume level
        // Normalize the value between 0 and 1
        double normalizedValue = (double) Math.min(1, Math.max(0, 1 - (value - 20) / (70 - 20)));
        // Define the volume range (0-100)
        int volumeRange = 50;
        // Map the normalized value to the volume range
        return (int) (normalizedValue * volumeRange);
    }

    private long calculateVibrationDuration(int value) {
        // Map value to vibration duration
        long normalizedValue = (long) Math.min(1, Math.max(0, (value - 20) / (70 - 20))); // Normalize the value between 0 and 1
        // Vibrate for 1 second to 0.1 second based on value
        int frequencyRange = 1000 - 10;
        return (long) 10 + (long) Math.pow(normalizedValue, 4) * frequencyRange;
    }

    private int calculateVibrationAmplitude(int value) {
        double normalizedValue = (double) Math.min(1, Math.max(0, (value - 20) / (70 - 20))); // Normalize the value between 0 and 1
        int frequencyRange = 1;
        int vibrationAmplitude = (int) (1 - Math.pow(normalizedValue, 4) * frequencyRange); // Calculate vibration amplitude
        Log.d("VibrationAmplitude", "Amplitude calculated: " + vibrationAmplitude); // Log the result
        return vibrationAmplitude;
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
