package com.example.b0915218.presentationcontrolapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.notifications.VibrationType;
import com.osacci.microsoftbandgestures.BandGestureClientManager;
import com.osacci.microsoftbandgestures.BandGestureManager;
import com.osacci.microsoftbandgestures.IntentionLockEvent;
import com.osacci.microsoftbandgestures.IntentionLockEventListener;
import com.osacci.microsoftbandgestures.PalmSideSwipeEventListener;

public class MainActivity extends AppCompatActivity {
    private int REQUEST_ENABLE_BT = 1;

    private BandClient client = null;
    private BandGestureManager bandGestureManger = null;
    private BleManager bleManager = null;
    private InterfaceController interfaceController;

    private boolean intentionLocked = false;
    private boolean sendOnlyIfIntentionLocked = false;
    private SharedPreferences sharedPreferences;

    private PalmSideSwipeEventListener palmSideSwipeRightEventListener = new PalmSideSwipeEventListener() {
        @Override
        public void onSwipe() {
            interfaceController.setSwipeRight();
            if (sendOnlyIfIntentionLocked && !intentionLocked) {
                return;
            }
            if (bleManager == null) {
                return;
            }
            try {
                bleManager.sendSwipeRight();
            } catch (ControlCharacteristicNotConnected e) {

            }
            try {
                client.getNotificationManager().vibrate(VibrationType.NOTIFICATION_ONE_TONE).await();
            } catch (BandIOException e) {

            } catch (InterruptedException e) {

            } catch (BandException e) {

            }
        }
    };

    private PalmSideSwipeEventListener palmSideSwipeLeftEventListener = new PalmSideSwipeEventListener() {
        @Override
        public void onSwipe() {
            interfaceController.setSwipeLeft();
            if (sendOnlyIfIntentionLocked && !intentionLocked) {
                return;
            }
            if (bleManager == null) {
                return;
            }
            try {
                bleManager.sendSwipeLeft();
            } catch (ControlCharacteristicNotConnected e) {

            }
            try {
                client.getNotificationManager().vibrate(VibrationType.NOTIFICATION_ONE_TONE).await();
            } catch (BandIOException e) {

            } catch (InterruptedException e) {

            } catch (BandException e) {

            }
        }
    };

    private IntentionLockEventListener intentionLockEventListener = new IntentionLockEventListener() {
        @Override
        public void onChange(IntentionLockEvent event) {
            intentionLocked = event.getLocked();
            VibrationType t = event.getLocked() ?
                    VibrationType.NOTIFICATION_ONE_TONE : VibrationType.NOTIFICATION_TWO_TONE;
            if (event.getLocked()) {
                interfaceController.setIntentionLocked();
            } else {
                interfaceController.setIntentionUnlocked();
            }
            try {
                client.getNotificationManager().vibrate(t).await();
            } catch (BandIOException e) {

            } catch (InterruptedException e) {

            } catch (BandException e) {

            }
        }
    };

    private ControlCharacteristicConnectedEventListener controlCharacteristicConnectedEventListener
            = new ControlCharacteristicConnectedEventListener() {
        @Override
        public void onSearching() {
            interfaceController.setLookingForDevice();
        }

        @Override
        public void onConnect() {
            interfaceController.setDeviceConnected();
        }

        @Override
        public void onDisconnect() {
            interfaceController.setDeviceDisconnected();
        }

        @Override
        public void onNotFound() {
            interfaceController.setDeviceNotFound();
        }
    };

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    sendOnlyIfIntentionLocked = sharedPreferences.getBoolean("intention_lock", true);
                    setBandLocation();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        interfaceController = new InterfaceController(this);
        interfaceController.startScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    bleManager.resume();
                } catch (BleNotEnabled e) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        });
        sendOnlyIfIntentionLocked = sharedPreferences.getBoolean("intention_lock", true);
        Log.i("intentionLock", Boolean.toString(sendOnlyIfIntentionLocked));

        try {
            bleManager = new BleManager(this);
            bleManager.registerControlCharacteristicConnectedEventListener(controlCharacteristicConnectedEventListener);
        } catch (BleNotSupportedException e) {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new BandSubscriptionTask().execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bandGestureManger != null) {
            try {
                bandGestureManger.unregisterPalmSideSwipeLeftEventListener(palmSideSwipeLeftEventListener);
                bandGestureManger.unregisterPalmSideSwipeRightEventListener(palmSideSwipeRightEventListener);
                bandGestureManger.unregisterIntentionLockEventListener(intentionLockEventListener);
            } catch (BandIOException e) {
//                setAccelerometerText(e.getMessage());
            }
        }

        bleManager.pause();
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        bleManager.close();
        super.onDestroy();
    }

    // BLE
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class BandSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    setBandGestureManger();
                } else {
                    interfaceController.setInstructionsText(getResources().getString(R.string.band_error_not_connected));
                }
            } catch (BandException e) {
                String exceptionMessage = "";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = getResources().getString(R.string.band_error_sdk_not_supported);
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = getResources().getString(R.string.band_error_mh_not_installed);
                        break;
                    default:
                        exceptionMessage = getResources().getString(R.string.band_error_unknown);
                        break;
                }
                interfaceController.setInstructionsText(exceptionMessage);

            } catch (Exception e) {
                interfaceController.setInstructionsText(e.getMessage());
            }
            return null;
        }
    }

    private void setBandGestureManger() throws BandException, BleNotEnabled {
        bandGestureManger = BandGestureClientManager.getGestureManager(client.getSensorManager());
        setBandLocation();
        bandGestureManger.registerPalmSideSwipeLeftEventListener(palmSideSwipeLeftEventListener);
        bandGestureManger.registerPalmSideSwipeRightEventListener(palmSideSwipeRightEventListener);
        bandGestureManger.registerIntentionLockEventListener(intentionLockEventListener);
        bleManager.resume();
    }

    private void setBandLocation() {
        if (bandGestureManger == null) {
            return;
        }
        int bandLocation =  Integer.valueOf(sharedPreferences.getString("band_location", "0"));
        switch (bandLocation) {
            case 0:
                bandGestureManger.setLeftHandOutside();
                break;
            case 1:
                bandGestureManger.setLeftHandInside();
                break;
            case 2:
                bandGestureManger.setRightHandOutside();
                break;
            case 3:
                bandGestureManger.setRightHandInside();
                break;
            default:
                bandGestureManger.setLeftHandOutside();
                break;
        }
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                interfaceController.setInstructionsText(getResources().getString(R.string.band_error_not_paired));
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        interfaceController.setInstructionsText(getResources().getString(R.string.band_message_connecting));
        return ConnectionState.CONNECTED == client.connect().await();
    }

}
