// MyBluetoothService.java
package nos.example.devicefinder;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.TextView;


import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

public class MyBluetoothService {

    private static final String TAG = "MY_APP_DEBUG_TAG";

    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final byte[] mmBuffer;
        private final Handler handler;
        private final BluetoothDevice mmDevice;
        private BufferedReader mmReader;
        private final Context mContext;
        private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        public ConnectedThread(BluetoothSocket socket, Handler handler, BluetoothDevice device, Context context) {
            this.handler = handler;
            this.mmDevice = device;
            this.mContext = context; // Store the context for permission checks

            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            BluetoothSocket tmpSocket = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();

                // Check if the Bluetooth permission is declared in the manifest file
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                    tmpSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                } else {
                    // Log an error if the permission is missing
                    Log.e("ConnectThread", "BLUETOOTH permission is missing in AndroidManifest.xml");
                }
            } catch (IOException e) {
                // Handle errors
                Log.e(TAG, "Error occurred when creating streams or socket", e);
            } catch (SecurityException se) {
                // Handle SecurityException due to missing permission
                Log.e(TAG, "SecurityException: BLUETOOTH permission is not granted", se);
            }

            // Assign the values only once
            this.mmInStream = tmpIn;
            this.mmOutStream = tmpOut;
            this.mmBuffer = new byte[1024];
            this.mmSocket = tmpSocket;
        }

        public void run() {
            // Initialize input stream and reader before entering the loop
            try {
                mmInStream = mmSocket.getInputStream();
                mmReader = new BufferedReader(new InputStreamReader(mmInStream));
            } catch (IOException e) {
                // Handle errors
                Log.e(TAG, "Error occurred when creating input stream or reader", e);
                return;
            }

            while (true) {
                try {
                    // Read data from the input stream
                    String distanceData = mmReader.readLine(); // Read distance data from the input stream

                    // Parse the distance data and update UI
                    parseDistanceData(distanceData);
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }



        private void parseDistanceData(String distanceData) {
            // Parse the received distance data based on the protocol
            // For example, assuming the data is in the format "DISTANCE: X meters"
            String[] parts = distanceData.split(":");
            if (parts.length == 2) {
                String distanceValue = parts[1].trim(); // Extract the distance value
                // Convert distanceValue to appropriate format if needed
                // Update UI with the distance value
                updateUIWithDistance(distanceValue);
            }
        }

        private void updateUIWithDistance(String distanceValue) {
            // Assuming you have a TextView named distanceTextView in your UI layout
            // You should replace R.id.distanceTextView with the ID of your TextView
            TextView distanceTextView = ((Activity) mContext).findViewById(R.id.distanceTextView);

            // Set the received distance value to the TextView
            distanceTextView.setText("Distance from device: " + distanceValue + " meters");
        }


        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                Message writtenMsg = handler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                Message writeErrorMsg =
                        handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                writeErrorMsg.getData().putString("toast",
                        "Couldn't send data to the other device");
                handler.sendMessage(writeErrorMsg);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    public interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 1;
        int MESSAGE_TOAST = 2;
    }
}
