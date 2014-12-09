package lab4.cis542.upenn.edu.infinityvisualizer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener {


    private Button button0, button1, button2;
    private TextView textView;

    private static final int REQUEST_ENABLE_BT = 0;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ConnectedThread CT;

    private ArrayAdapter mArrayAdapter;

    private Visualizer mViz;
    private int sessionID = 0;
    private int priority = 1;
    private double max_mag = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up buttons and views
        button0 = (Button) findViewById(R.id.button0);
        button0.setOnClickListener(this);
        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(this);
        button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(this);




        //// - Set up Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Test to see if device supports it
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "No bluetooth detected on phone", Toast.LENGTH_LONG);
        }

        // Enable it
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(getApplicationContext(),"Turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(),"Already on", Toast.LENGTH_LONG).show();
        }

        // Set up visualizer
        initViz();
        mViz.setEnabled(true);

    }


    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(mReceiver);
        deinitViz();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button0:
                Log.d("what what", "whaaat");

                // Pair with device
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                Log.d("what what", "size: " + pairedDevices.size());

                String ad;
                if (pairedDevices.size() > 0) {
                    // loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        Log.d("what what", "name: " + device.getName());
                        Log.d("what what", "address: " + device.getAddress());

                        ConnectThread ct = new ConnectThread(device);
                        ct.run();
                    }
                }

                // Register the BroadcastReceiver
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
                break;

            case R.id.button1:
                int c = 123;
                int H = 72;
                char buffer[] = {(char)c,(char)H,10,10};
                String s3 = new String(buffer);
                String s = "{HOR";
                Log.d("what what", "Sending the HOR");
//                CT.write(s.getBytes());
                CT.write(s3.getBytes());
                break;

            case R.id.button2:
                String s2 = "{RO";
                Log.d("what what", "Sending the off sequence");
                CT.write(s2.getBytes());
                break;
        }

    }



    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d("what what", "connection started");
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.d("what what", "ran connection");
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }


            ConnectedThread cdT = new ConnectedThread(mmSocket);
            CT = cdT;

        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
//                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };



    private void waveformUpdate(byte[] waveBytes, int sRate) {
        //do stuff
    }

    private void fftUpdate(byte[] fftBytes, int sRate) {
        int n = mViz.getCaptureSize();
        byte rfk;
        byte ifk;
        double rmsVal;
        double rmsMax = 0;
        double[] fftMags = new double[n/2];
        double max_alpha = .95; //.8 is a good number
        int bucket_offset = 0;

        rfk = fftBytes[0];
        ifk = fftBytes[0];
        fftMags[0] = (double)Math.sqrt(rfk*rfk);
        for(int i = 1; i <n/2; i++) {
            rfk = fftBytes[i*2];
            ifk = fftBytes[i*2+1];
            rmsVal = Math.sqrt(rfk*rfk + ifk*ifk);
            if(rmsVal>rmsMax){
                rmsMax = rmsVal;
            }
            fftMags[i] = rmsVal;
        }

        int max_range = (int)Math.round(.03*((double)n/2.0));
        int bucket;
        int bucketSize = (int)Math.floor((max_range-bucket_offset)/3);

        double[] sum = new double[3];
        double num_r = 0;
        double num_g = 0;
        double num_b = 0;

        double r_max = 0;
        double g_max = 0;
        double b_max = 0;

        for(int j = bucket_offset; j<n/2; j++){
            if(j<bucketSize){
                bucket = 0;
            } else if(j>2*bucketSize) {
                bucket = 2;
            } else {
                bucket = 1;
            }
            switch (bucket) {
                case 0:
                    num_r++;
                    if(fftMags[j]>r_max) {
                        r_max = fftMags[j];
                    }
                    break;
                case 1:
                    num_g++;
                    if(fftMags[j]>g_max) {
                        g_max = fftMags[j];
                    }
                    break;
                case 2:
                    num_b++;
                    if(fftMags[j]>b_max) {
                        b_max = fftMags[j];
                    }
                    break;
            }
            sum[bucket] = sum[bucket] + fftMags[j];
        }

        TextView textView1 = (TextView) findViewById(R.id.textView3);
        textView1.setText(Double.toString(bucketSize));
        TextView textView2 = (TextView) findViewById(R.id.textView2);
        textView2.setText(Double.toString(num_g));
        TextView textView3 = (TextView) findViewById(R.id.textView4);
        textView3.setText(Double.toString(num_b));

        double maxSum = 1;
        double mix_sum = .2;

        sum[0] = mix_sum*(sum[0]/num_r)+(1-mix_sum)*r_max;
        sum[1] = mix_sum*(sum[1]/num_g)+(1-mix_sum)*g_max;
        sum[2] = mix_sum*(sum[2]/num_b)+(1-mix_sum)*b_max;

        for (int j = 0; j<3;j++) {
            if(sum[j]>maxSum){
                maxSum = sum[j];
            }
        }

        if(maxSum>max_mag) {
            max_mag = maxSum;
        } else {
            max_mag = max_alpha * max_mag + (1 - max_alpha) * 1.2* maxSum;
        }

        double r_val_f = 100*(sum[0])/max_mag;
        double g_val_f = 100*(sum[1])/max_mag;
        double b_val_f = 100*(sum[2])/max_mag;

        int r_val = (int)r_val_f;
        int g_val = (int)g_val_f;
        int b_val = (int)b_val_f;

        ProgressBar pg1 = (ProgressBar) findViewById(R.id.progressBar1);
        pg1.setProgress(r_val);
        ProgressBar pg2 = (ProgressBar) findViewById(R.id.progressBar2);
        pg2.setProgress(g_val);
        ProgressBar pg3 = (ProgressBar) findViewById(R.id.progressBar3);
        pg3.setProgress(b_val);

    }

    private void initViz() {
        mViz = new Visualizer(sessionID);
        mViz.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mViz.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {

                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate){
                        waveformUpdate(bytes,samplingRate);
                    }

                    public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                        fftUpdate(bytes,samplingRate);
                    }
                }, (Visualizer.getMaxCaptureRate()/2), true, true);
    }

    private void deinitViz() {
        mViz.setEnabled(false);
        mViz.release();

    }




}
