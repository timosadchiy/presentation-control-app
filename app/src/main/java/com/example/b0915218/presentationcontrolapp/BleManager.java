package com.example.b0915218.presentationcontrolapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleManager {

    private final static String CONTROL_SERVICE_UUID = "81771959-f027-41f7-b4c2-0d07baa729ec";
    private final static String CONTROL_CHARACTERISTIC_UUID = "c88a4a74-f5f4-4b0c-9bfa-5ae1420994f9";
    private final static long SCAN_PERIOD = 10000;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic controlCharacteristic = null;
    private Context context;
    private boolean deviceConected;
    private List<ControlCharacteristicConnectedEventListener>
            controlCharacteristicConnectedEventListeners =
            new ArrayList<ControlCharacteristicConnectedEventListener>();

    public BleManager(Context cont) throws BleNotSupportedException {
        context = cont;
        deviceConected = false;
        mHandler = new Handler();
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new BleNotSupportedException();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    connectToDevice(device);
                }
            };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    fireDisconnected();
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(UUID.fromString(CONTROL_SERVICE_UUID));
            if (service == null) {
                gatt.disconnect();
                return;
            }
            Log.i("onServicesDiscovered", service.toString());
            controlCharacteristic = service.getCharacteristic(UUID.fromString(CONTROL_CHARACTERISTIC_UUID));
            Log.i("controlCharacteristic", controlCharacteristic.toString());
            fireConnected();
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(context, false, gattCallback);
            scanLeDevice(false);
        }
    }

    public void resume() throws BleNotEnabled {
        if (deviceConected) {
            return;
        }
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            throw new BleNotEnabled();
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                final ScanFilter filter = new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(CONTROL_SERVICE_UUID)).build();
                filters = new ArrayList<ScanFilter>() {{
                    add(filter);
                }};
            }
            scanLeDevice(true);
        }
    }

    public void pause() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    public void close() {
        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
        }
    }

    public void sendSwipeLeft() throws ControlCharacteristicNotConnected {
        if (controlCharacteristic == null) {
            throw new ControlCharacteristicNotConnected();
        }
        byte[] data = "0".getBytes(Charset.forName("UTF-8"));
        controlCharacteristic.setValue(data);
        mGatt.writeCharacteristic(controlCharacteristic);
    }

    public void sendSwipeRight() throws ControlCharacteristicNotConnected {
        if (controlCharacteristic == null) {
            throw new ControlCharacteristicNotConnected();
        }
        byte[] data = "1".getBytes(Charset.forName("UTF-8"));
        controlCharacteristic.setValue(data);
        mGatt.writeCharacteristic(controlCharacteristic);
    }

    public boolean registerControlCharacteristicConnectedEventListener(ControlCharacteristicConnectedEventListener listener) {
        Boolean r = controlCharacteristicConnectedEventListeners.add(listener);
        return r;
    }

    public void unregisterControlCharacteristicConnectedEventListeners() {
        controlCharacteristicConnectedEventListeners.clear();
    }

    public void unregisterControlCharacteristicConnectedEventListener(ControlCharacteristicConnectedEventListener listener) {
        controlCharacteristicConnectedEventListeners.remove(listener);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);
                    }
                    if (mGatt == null) {
                        fireNotFound();
                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                UUID[] uuids = {UUID.fromString(CONTROL_SERVICE_UUID)};
                mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
            fireSearching();
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                if (mLEScanner != null) {
                    mLEScanner.stopScan(mScanCallback);
                }
            }
        }
    }

    private void fireSearching() {
        for (ControlCharacteristicConnectedEventListener listener : controlCharacteristicConnectedEventListeners) {
            listener.onSearching();
        }
    }

    private void fireConnected() {
        deviceConected = true;
        for (ControlCharacteristicConnectedEventListener listener : controlCharacteristicConnectedEventListeners) {
            listener.onConnect();
        }
    }

    private void fireDisconnected() {
        deviceConected = false;
        for (ControlCharacteristicConnectedEventListener listener : controlCharacteristicConnectedEventListeners) {
            listener.onDisconnect();
        }
    }

    private void fireNotFound() {
        for (ControlCharacteristicConnectedEventListener listener : controlCharacteristicConnectedEventListeners) {
            listener.onNotFound();
        }
    }

}
