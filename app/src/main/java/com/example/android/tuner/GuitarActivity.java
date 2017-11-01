package com.example.android.tuner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GuitarActivity extends AppCompatActivity {
    private final String DEVICE_ADDRESS = "20:15:04:16:50:67";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Context toasty = GuitarActivity.this;
    int duration = Toast.LENGTH_SHORT;
    boolean deviceConnected = false;
    byte buffer[];
    boolean stopThread;
    private PdService pdService = null;
    private PdUiDispatcher dispatcher;
    private float centerPitch;

    // PD related functions
    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = ((PdService.PdBinder) service).getService();
            try {
                initPd();
                loadPatch();
            } catch (IOException e) {
                Log.e("serviceConnectedFail", e.toString());
                //finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {// this method will never be called
        }
    };

    private void initPd() throws IOException {
        // Configure the audio glue
        AudioParameters.init(this);
        int sampleRate = AudioParameters.suggestSampleRate();
        pdService.initAudio(sampleRate, 1, 2, 10.0f);
        //startPd(); // should not be listening all the time

        // Create and install the dispatcher
        dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
        dispatcher.addListener("pitch", new PdListener.Adapter() {
            @Override
            public void receiveFloat(String source, final float x) {
                float delta = centerPitch - x;
                Toast.makeText(toasty, "current pitch =" + x + ", delta = " + delta, Toast.LENGTH_SHORT).show();
                if (delta >= 5 && delta <= -5) {
                    done();
                } else if (delta > 5) {
                    boolean tooLow = false;
                    turnPeg(tooLow);
                } else if (delta < -5) {
                    boolean tooLow = true;
                    turnPeg(tooLow);
                }
                pdService.stopAudio();
            }
        });
    }

    private void startPd() {
        if (!pdService.isRunning()) {
            Intent intent = new Intent(toasty, GuitarActivity.class);
            pdService.startAudio(intent, R.drawable.icon, "GuitarTuner", "Return to GuitarTuner.");
        }
    }

    private void done() {
        try {
            outputStream.write("D".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
//        Toast.makeText(toasty, "In tune", duration).show();
//        unbindService(pdConnection);
    }

    private void turnPeg(boolean low) {
        if (low == true) {
            try {
                outputStream.write("L".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(toasty, "Tuning up", duration).show();
        } else {
            try {
                outputStream.write("H".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(toasty, "Tuning down", duration).show();
        }
    }

    private void loadPatch() throws IOException {
        File dir = getFilesDir();
        IoUtils.extractZipResource(
                getResources().openRawResource(R.raw.tuner), dir, true);
        File patchFile = new File(dir, "tuner.pd");
        PdBase.openPatch(patchFile.getAbsolutePath());
    }

    // Android lifecycle functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guitar);

        getSupportActionBar().setTitle("Guitar");
        // No sé com fer per a que es pugui traduir
        initSystemServices(); //microphone
        start();
    }

    private void initSystemServices() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (pdService == null) return;
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    // Start the PD service if not taling
                    //startPd(); // should not be listening all the time
                } else {
                    // Stop the PD service if talking
                    pdService.stopAudio();
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void start() {
        // Initialize BTooth
        if (BTinit()) {
            if (BTconnect()) {
                deviceConnected = true;
            }
        }
        ListenToArduino listenToArduino = new ListenToArduino();
        //Connect to the the device in a new thread
        new Thread(listenToArduino).start();

        // start the PD service in background
        bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
    }

    private boolean BTinit() {
        boolean found = false;
        BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (myBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device does not Support Bluetooth", Toast.LENGTH_SHORT).show();
        }

        int REQUEST_ENABLE_BT = 1;
        if (!myBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();

        if (pairedDevices.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please Pair the Device first", Toast.LENGTH_SHORT).show();
        } else {
            for (BluetoothDevice iterator : pairedDevices) {
                if (iterator.getAddress().equals(DEVICE_ADDRESS)) {
                    device = iterator;
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    private boolean BTconnect() {
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }

        if (connected) {
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return connected;
    }

    private void triggerNote(int n) {
        PdBase.sendFloat("midinote", n);
        PdBase.sendBang("trigger");
        centerPitch = n;
    }

    public void pressString1(View view) {
        triggerNote(40);
        pressString("E", "1");
    }

    public void pressString2(View view) {
        triggerNote(45);
        pressString("A", "2");
    }

    public void pressString3(View view) {
        triggerNote(50);
        pressString("D", "3");
    }

    public void pressString4(View view) {
        triggerNote(55);
        pressString("G", "4");
    }

    public void pressString5(View view) {
        triggerNote(59);
        pressString("B", "5");
    }

    public void pressString6(View view) {
        triggerNote(64);
        pressString("E'", "6");
    }

    private void pressString(String noteName, String noteCode) {
        String pressed = "Tuning to " + noteName;
        Toast.makeText(toasty, pressed, duration).show();

        try {
            outputStream.write(noteCode.getBytes());
            outputStream.write("P".getBytes());
            //run(); // already listening to arduino
        } catch (IOException e) {
            e.printStackTrace();
        }

        //bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
    }


    // The Handler that gets information back from the Socket
    public static final int MESSAGE_WRITE = 1;
    public static final int MESSAGE_READ = 2;
    String readMessage;

//    private final Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case MESSAGE_WRITE:
//                    //Do something when writing
//                    break;
//                case MESSAGE_READ:
//                    //Get the bytes from the msg.obj
//                    byte[] readBuf = (byte[]) msg.obj;
//                    // construct a string from the valid bytes in the buffer
//                    readMessage = new String(readBuf, 0, msg.arg1);
//                    break;
//            }
//        }
//    };


    public class ListenToArduino implements Runnable {
        @Override
        public void run() {
            int bytes; // bytes returned from read()
            byte[] buffer = new byte[512]; // buffer store for the stream
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    int byteCount = inputStream.available();
                    if (byteCount>=8) {
                        bytes = inputStream.read(buffer);
                        // Send the obtained bytes to the UI activity
                        //handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                        String str = new String(buffer, "UTF-8");
                       // Toast.makeText(toasty, str, duration).show();
                        String order = str.substring(0,6);
                        if (order.equals("picked")) {
                            //startPd();
                            sendRandomTurnCommand();
                        }
                        //break;
                    }
                } catch (IOException e) {
                    Log.e("SendReceiveBytes", "Error reading from btInputStream");
                    break;
                }
            }
        }
    }

    private void sendRandomTurnCommand(){
        Random r = new Random();
        int Low = -100;
        int High = 100;
        int delta = r.nextInt(High-Low) + Low;
        if (delta <= 5 && delta >= -5) {
            Toast.makeText(toasty, "This string is in tune!", duration).show();
            done();
        } else if (delta > 5) {
            turnPeg(false);
        } else if (delta < -5) {
            turnPeg(true);
        }
        try {
            TimeUnit.MILLISECONDS.sleep(300);
        } catch (Exception e) {

        }
    }
}
    /**
    void beginListenForData() {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable() {

            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopThread) {
                    try {
                        int byteCount = inputStream.available();
                        if (byteCount > 0) {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String recMes = new String(rawBytes, "UTF-8");
                            handler.post(new Runnable() {

                                public void run() {
                                    if (recMes == "picked") {
                                        Toast.makeText(toasty, "Checking tuning", duration).show();
                                    }
                                }

                            });
                        }
                    }
                    catch (IOException ex) {
                            stopThread = true;
                    }
                }
            }

        });
        thread.start();
        }






 send "string1" to Arduino
 boolean done = false;
 while (done == false) {
 wait for orders
 receive "listen" from Arduino
 listen for sound
 if (sound > tune) send "tune down" to Arduino
 else if (sound < tune) send "tune up" to Arduino
 else if (sound == tune) {
 send "done" to Arduino;
 done = true;
 }
 }**/