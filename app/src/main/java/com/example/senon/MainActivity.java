package com.example.senon;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.senon.adapter.SensorAdapter;
import com.example.senon.adapter.SensorViewHolder;
import com.example.senon.databinding.ActivityMainBinding;
import com.example.senon.model.SensorItem;
import com.example.senon.utils.SensorIconMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SensorViewHolder.OnSensorItemClickListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int BATTERY_OPTIMIZATION_REQUEST_CODE = 1002;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.NFC,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    private ActivityMainBinding binding;
    private SensorManager sensorManager;
    private SensorAdapter sensorAdapter;
    private List<Sensor> availableSensors;
    private Map<Integer, Sensor> activeSensors;
    private Handler uiHandler;
    private boolean isAutoTesting = false;

    // Hardware testing components
    private CameraManager cameraManager;
    private AudioManager audioManager;
    private BluetoothAdapter bluetoothAdapter;
    private NfcAdapter nfcAdapter;
    private Vibrator vibrator;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private int touchCount = 0;
    private AutoSensorTester autoSensorTester;
    private long autoTestStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("MainActivity", "onCreate started");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize components
        initializeComponents();

        // Setup UI
        setupToolbar();
        setupRecyclerView();
        setupFAB();
        setupHardwareTestingCards();
        checkHardwareAvailability();

        // Check permissions and initialize sensors
        if (checkPermissions()) {
            android.util.Log.d("MainActivity", "Permissions granted, initializing sensors");
            initializeSensors();
            // Check power optimization after permissions are granted
            checkPowerOptimization();
        } else {
            android.util.Log.d("MainActivity", "Requesting permissions");
            requestPermissions();
        }

        // Check if auto test should be triggered
        if (getIntent().getBooleanExtra("trigger_auto_test", false)) {
            android.util.Log.d("MainActivity", "Auto test trigger requested");
            // Small delay to ensure UI is ready
            uiHandler.postDelayed(() -> {
                if (!isAutoTesting) {
                    startAutoSensorTest();
                }
            }, 500);
        }

        android.util.Log.d("MainActivity", "onCreate completed");
    }

    private void initializeComponents() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        availableSensors = new ArrayList<>();
        activeSensors = new HashMap<>();
        uiHandler = new Handler(Looper.getMainLooper());

        // Initialize hardware testing components
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Initialize vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager != null) {
                vibrator = vibratorManager.getDefaultVibrator();
            }
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        // Initialize network components
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sensor Display");
        }
    }

    private void setupRecyclerView() {
        sensorAdapter = new SensorAdapter(this);
        sensorAdapter.setOnItemClickListener(this);

        binding.recyclerViewSensors.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSensors.setAdapter(sensorAdapter);
        binding.recyclerViewSensors.setHasFixedSize(true);
    }

    private void setupFAB() {
        binding.fabAutoTest.setOnClickListener(v -> {
            if (isAutoTesting) {
                // Stop auto testing
                stopAutoSensorTest();
            } else {
                startAutoSensorTest();
            }
        });
    }

    private void stopAutoSensorTest() {
        if (autoSensorTester != null) {
            autoSensorTester.cancelTesting();
        }

        isAutoTesting = false;
        binding.progressIndicator.setVisibility(View.GONE);
        binding.fabAutoTest.setEnabled(true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(availableSensors.size() + " generic sensors found");
        }

        Toast.makeText(this, "Auto test stopped", Toast.LENGTH_SHORT).show();
    }

    private void showAutoTestResults(List<com.example.senon.model.TestResult> results, long totalDuration) {
        Intent intent = new Intent(this, AutoTestResultsActivity.class);
        intent.putExtra(AutoTestResultsActivity.EXTRA_TEST_RESULTS, new ArrayList<>(results));
        intent.putExtra(AutoTestResultsActivity.EXTRA_TEST_DURATION, totalDuration);
        startActivity(intent);
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                initializeSensors();
                // Check power optimization after permissions are granted
                checkPowerOptimization();
            } else {
                Toast.makeText(this, "Some permissions were denied. App functionality may be limited.",
                        Toast.LENGTH_LONG).show();
                initializeSensors(); // Initialize anyway with available sensors
                // Still check power optimization even with limited permissions
                checkPowerOptimization();
            }
        }
    }

    private void initializeSensors() {
        if (sensorManager == null) {
            android.util.Log.e("MainActivity", "SensorManager is null - sensor service not available");
            showEmptyState(true);
            return;
        }

        android.util.Log.d("MainActivity", "SensorManager initialized successfully");

        // Get all available sensors
        List<Sensor> allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        android.util.Log.d("MainActivity", "Total sensors found: " + allSensors.size());

        if (allSensors.isEmpty()) {
            android.util.Log.w("MainActivity", "No sensors found on device");
            showEmptyState(true);
            return;
        }

        // Filter to only generic/standard Android sensors
        availableSensors = new ArrayList<>();
        for (Sensor sensor : allSensors) {
            android.util.Log.d("MainActivity", "Sensor found: " + sensor.getName() +
                    " (Type: " + sensor.getType() + ", Vendor: " + sensor.getVendor() + ")");
            if (isGenericSensor(sensor)) {
                availableSensors.add(sensor);
                android.util.Log.d("MainActivity", "Added generic sensor: " + sensor.getName());
            }
        }

        android.util.Log.d("MainActivity", "Generic sensors found: " + availableSensors.size());

        if (availableSensors.isEmpty()) {
            android.util.Log.w("MainActivity", "No generic sensors found after filtering");
            showEmptyState(true);
            return;
        }

        // Test a few common sensors to see if they can be registered
        testSensorRegistration();

        // Create sensor items
        List<SensorItem> sensorItems = new ArrayList<>();
        for (Sensor sensor : availableSensors) {
            int iconResId = SensorIconMapper.getIconForSensorType(sensor.getType());
            SensorItem sensorItem = new SensorItem(sensor, iconResId);
            sensorItems.add(sensorItem);
        }

        // Update adapter
        sensorAdapter.setSensorItems(sensorItems);
        showEmptyState(false);

        // Update sensor count chip
        binding.chipSensorCount.setText(availableSensors.size() + " generic sensors found");

        // Update toolbar subtitle
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(availableSensors.size() + " generic sensors found");
        }
    }

    private boolean isGenericSensor(Sensor sensor) {
        String sensorName = sensor.getName().toLowerCase();
        String vendor = sensor.getVendor().toLowerCase();

        // Filter out brand-specific sensors
        if (sensorName.contains("samsung") || sensorName.contains("aois") ||
                sensorName.contains("scontext") || sensorName.contains("grip") ||
                sensorName.contains("hover") || sensorName.contains("flip") ||
                sensorName.contains("call gesture") || sensorName.contains("pocket") ||
                sensorName.contains("hall") || sensorName.contains("protos") ||
                sensorName.contains("vdis") || sensorName.contains("supersteady") ||
                sensorName.contains("tcs3701") || sensorName.contains("isg5320a")) {
            return false;
        }

        // Filter out vendor-specific sensors
        if (vendor.contains("samsung") &&
                (sensorName.contains("seamless") || sensorName.contains("auto brightness") ||
                        sensorName.contains("motion") || sensorName.contains("gesture"))) {
            return false;
        }

        // Only include standard Android sensor types
        int sensorType = sensor.getType();
        return sensorType == Sensor.TYPE_ACCELEROMETER ||
                sensorType == Sensor.TYPE_GYROSCOPE ||
                sensorType == Sensor.TYPE_MAGNETIC_FIELD ||
                sensorType == Sensor.TYPE_LIGHT ||
                sensorType == Sensor.TYPE_PROXIMITY ||
                sensorType == Sensor.TYPE_PRESSURE ||
                sensorType == Sensor.TYPE_ROTATION_VECTOR ||
                sensorType == Sensor.TYPE_LINEAR_ACCELERATION ||
                sensorType == Sensor.TYPE_GRAVITY ||
                sensorType == Sensor.TYPE_AMBIENT_TEMPERATURE ||
                sensorType == Sensor.TYPE_RELATIVE_HUMIDITY ||
                sensorType == Sensor.TYPE_STEP_COUNTER ||
                sensorType == Sensor.TYPE_STEP_DETECTOR ||
                sensorType == Sensor.TYPE_SIGNIFICANT_MOTION ||
                sensorType == Sensor.TYPE_GAME_ROTATION_VECTOR ||
                sensorType == Sensor.TYPE_GYROSCOPE_UNCALIBRATED ||
                sensorType == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED ||
                sensorType == Sensor.TYPE_ACCELEROMETER_UNCALIBRATED ||
                sensorType == Sensor.TYPE_HEART_RATE ||
                sensorType == Sensor.TYPE_STATIONARY_DETECT ||
                sensorType == Sensor.TYPE_MOTION_DETECT ||
                sensorType == Sensor.TYPE_HEART_BEAT;
    }

    private void showEmptyState(boolean show) {
        if (show) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewSensors.setVisibility(View.GONE);
            binding.fabAutoTest.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewSensors.setVisibility(View.VISIBLE);
            binding.fabAutoTest.setVisibility(View.VISIBLE);
        }
    }

    private void startAutoSensorTest() {
        android.util.Log.d("MainActivity", "Starting auto sensor test");

        if (availableSensors.isEmpty()) {
            android.util.Log.w("MainActivity", "No sensors available for testing");
            Toast.makeText(this, "No sensors available for testing", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("MainActivity", "Testing " + availableSensors.size() + " sensors");

        if (autoSensorTester == null) {
            autoSensorTester = new AutoSensorTester(this);
        }

        isAutoTesting = true;
        autoTestStartTime = System.currentTimeMillis();
        binding.fabAutoTest.setEnabled(false);
        binding.progressIndicator.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Starting auto test for " + availableSensors.size() + " sensors...", Toast.LENGTH_SHORT).show();

        // Start auto testing
        autoSensorTester.testAllSensors(availableSensors, new AutoSensorTester.TestCallback() {
            @Override
            public void onTestStarted(int totalSensors) {
                runOnUiThread(() -> {
                    binding.progressIndicator.setMax(totalSensors);
                    binding.progressIndicator.setProgress(0);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle("Testing sensors... 0/" + totalSensors);
                    }
                });
            }

            @Override
            public void onSensorTested(com.example.senon.model.TestResult result, int progress, int total) {
                runOnUiThread(() -> {
                    binding.progressIndicator.setProgress(progress);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle("Testing sensors... " + progress + "/" + total);
                    }
                });
            }

            @Override
            public void onTestCompleted(List<com.example.senon.model.TestResult> results) {
                runOnUiThread(() -> {
                    isAutoTesting = false;
                    binding.fabAutoTest.setEnabled(true);
                    binding.progressIndicator.setVisibility(View.GONE);

                    long totalDuration = System.currentTimeMillis() - autoTestStartTime;

                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(availableSensors.size() + " generic sensors found");
                    }

                    // Show results
                    showAutoTestResults(results, totalDuration);
                });
            }

            @Override
            public void onTestError(String error) {
                runOnUiThread(() -> {
                    isAutoTesting = false;
                    binding.fabAutoTest.setEnabled(true);
                    binding.progressIndicator.setVisibility(View.GONE);

                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(availableSensors.size() + " generic sensors found");
                    }

                    Toast.makeText(MainActivity.this, "Auto test failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }


    @Override
    public void onSensorItemClick(SensorItem sensorItem, int position) {
        // Navigate to individual sensor test activity
        Intent intent = new Intent(this, SensorTestActivity.class);
        intent.putExtra("sensor_type", sensorItem.getSensorType());
        intent.putExtra("sensor_name", sensorItem.getSensorName());
        intent.putExtra("sensor_vendor", sensorItem.getSensorVendor());
        startActivity(intent);
    }

    private void startSensorMonitoring(Sensor sensor) {
        if (sensorManager != null && sensor != null) {
            boolean registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
            if (registered) {
                activeSensors.put(sensor.getType(), sensor);
                sensorAdapter.updateSensorStatus(sensor.getType(), true);
            }
        }
    }

    private void stopSensorMonitoring(Sensor sensor) {
        if (sensorManager != null && sensor != null) {
            sensorManager.unregisterListener(this, sensor);
            activeSensors.remove(sensor.getType());
            sensorAdapter.updateSensorStatus(sensor.getType(), false);
        }
    }

    private void stopAllSensorMonitoring() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            activeSensors.clear();

            // Update all sensor statuses
            for (Sensor sensor : availableSensors) {
                sensorAdapter.updateSensorStatus(sensor.getType(), false);
            }
        }
    }

    private void testSensorRegistration() {
        android.util.Log.d("MainActivity", "Testing sensor registration...");

        // Test common sensors
        int[] sensorTypes = {
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_GYROSCOPE,
                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_LIGHT,
                Sensor.TYPE_PROXIMITY,
                Sensor.TYPE_PRESSURE,
                Sensor.TYPE_TEMPERATURE,
                Sensor.TYPE_RELATIVE_HUMIDITY,
                Sensor.TYPE_AMBIENT_TEMPERATURE,
                Sensor.TYPE_ROTATION_VECTOR,
                Sensor.TYPE_LINEAR_ACCELERATION,
                Sensor.TYPE_GRAVITY
        };

        String[] sensorNames = {
                "Accelerometer",
                "Gyroscope",
                "Magnetometer",
                "Light",
                "Proximity",
                "Pressure",
                "Temperature",
                "Humidity",
                "Ambient Temperature",
                "Rotation Vector",
                "Linear Acceleration",
                "Gravity"
        };

        for (int i = 0; i < sensorTypes.length; i++) {
            final int index = i; // Make effectively final for inner class
            Sensor sensor = sensorManager.getDefaultSensor(sensorTypes[i]);
            if (sensor != null) {
                android.util.Log.d("MainActivity", sensorNames[i] + " sensor available: " + sensor.getName() +
                        " (Vendor: " + sensor.getVendor() + ", Version: " + sensor.getVersion() + ")");

                // Test registration
                boolean registered = sensorManager.registerListener(new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        android.util.Log.d("MainActivity", "Test registration successful for " + sensorNames[index]);
                        sensorManager.unregisterListener(this);
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    }
                }, sensor, SensorManager.SENSOR_DELAY_NORMAL);

                android.util.Log.d("MainActivity", sensorNames[i] + " registration test: " +
                        (registered ? "SUCCESS" : "FAILED"));
            } else {
                android.util.Log.w("MainActivity", sensorNames[i] + " sensor NOT AVAILABLE");
            }
        }

        // List all available sensors
        List<Sensor> allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        android.util.Log.d("MainActivity", "Total sensors available: " + allSensors.size());
        for (Sensor sensor : allSensors) {
            android.util.Log.d("MainActivity", "Available sensor: " + sensor.getName() +
                    " (Type: " + sensor.getType() + ", Vendor: " + sensor.getVendor() + ")");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event != null && event.values != null) {
            // Format sensor values
            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 0; i < Math.min(event.values.length, 3); i++) {
                if (i > 0) valueBuilder.append(", ");
                valueBuilder.append(String.format("%.2f", event.values[i]));
            }

            String formattedValue = valueBuilder.toString();

            // Update the adapter with new values
            uiHandler.post(() -> sensorAdapter.updateSensorValue(event.sensor.getType(), formattedValue));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_start_monitoring) {
            startMonitoringAllSensors();
            return true;
        } else if (itemId == R.id.action_stop_monitoring) {
            stopAllSensorMonitoring();
            return true;
        } else if (itemId == R.id.action_refresh) {
            initializeSensors();
            checkHardwareAvailability();
            return true;
        } else if (itemId == R.id.action_power_settings) {
            PowerOptimizationManager.showPowerOptimizationDialog(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Check and request power optimization exemption for intensive sensor testing
     */
    private void checkPowerOptimization() {
        // Only check on devices with aggressive power management
        if (PowerOptimizationManager.hasAggressivePowerManagement()) {
            if (!PowerOptimizationManager.isIgnoringBatteryOptimizations(this)) {
                // Show informative message about power optimization
                uiHandler.postDelayed(() -> {
                    Toast.makeText(this,
                            "For accurate sensor testing, please allow this app to ignore battery optimizations",
                            Toast.LENGTH_LONG).show();

                    // Show device-specific power optimization dialog
                    PowerOptimizationManager.showPowerOptimizationDialog(this);
                }, 1000); // Delay to avoid overwhelming the user with dialogs
            }
        }
    }

    private void setupHardwareTestingCards() {
        // Location testing
        binding.cardLocation.setOnClickListener(v -> testLocation());

        // Camera testing
        binding.cardCamera.setOnClickListener(v -> testCamera());

        // Microphone testing
        binding.cardMicrophone.setOnClickListener(v -> testMicrophone());

        // Vibration testing
        binding.cardVibration.setOnClickListener(v -> testVibration());

        // Bluetooth testing
        binding.cardBluetooth.setOnClickListener(v -> testBluetooth());

        // Speakers testing
        binding.cardSpeakers.setOnClickListener(v -> testSpeakers());

        // Screen testing
        binding.cardScreen.setOnClickListener(v -> testScreen());

        // Touch testing
        binding.cardTouch.setOnClickListener(v -> testTouch());

        // NFC testing
        binding.cardNFC.setOnClickListener(v -> testNFC());

        // WiFi testing
        binding.cardWiFi.setOnClickListener(v -> testWiFi());

        // Internet testing
        binding.cardInternet.setOnClickListener(v -> testInternet());
    }

    private void checkHardwareAvailability() {
        // Check Location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            binding.textLocationStatus.setText("Available");
            binding.textLocationStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            binding.textLocationStatus.setText("No Permission");
            binding.textLocationStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
        }

        // Check Camera
        if (cameraManager != null && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            binding.textCameraStatus.setText("Available");
            binding.textCameraStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            binding.textCameraStatus.setText("Not Available");
            binding.textCameraStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }

        // Check Microphone
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            binding.textMicrophoneStatus.setText("Available");
            binding.textMicrophoneStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            binding.textMicrophoneStatus.setText("Not Available");
            binding.textMicrophoneStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }

        // Check Vibration
        if (vibrator != null && vibrator.hasVibrator()) {
            binding.textVibrationStatus.setText("Available");
            binding.textVibrationStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            binding.textVibrationStatus.setText("Not Available");
            binding.textVibrationStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }

        // Check Bluetooth
        if (bluetoothAdapter != null) {
            binding.textBluetoothStatus.setText(bluetoothAdapter.isEnabled() ? "Enabled" : "Disabled");
            binding.textBluetoothStatus.setTextColor(bluetoothAdapter.isEnabled() ?
                    getColor(android.R.color.holo_green_dark) : getColor(android.R.color.holo_orange_dark));
        } else {
            binding.textBluetoothStatus.setText("Not Available");
            binding.textBluetoothStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }

        // Check Speakers
        if (audioManager != null) {
            binding.textSpeakersStatus.setText("Available");
            binding.textSpeakersStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            binding.textSpeakersStatus.setText("Not Available");
            binding.textSpeakersStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }

        // Check Screen
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        binding.textScreenStatus.setText(metrics.widthPixels + "x" + metrics.heightPixels);
        binding.textScreenStatus.setTextColor(getColor(android.R.color.holo_green_dark));

        // Check Touch
        binding.textTouchStatus.setText("Tap to Test");
        binding.textTouchStatus.setTextColor(getColor(android.R.color.holo_blue_bright));

        // Check NFC
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            binding.textNFCStatus.setText("Enabled");
            binding.textNFCStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else if (nfcAdapter != null) {
            binding.textNFCStatus.setText("Disabled");
            binding.textNFCStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
        } else {
            binding.textNFCStatus.setText("Not Available");
            binding.textNFCStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }

        // Check WiFi
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            binding.textWiFiStatus.setText("Connected");
            binding.textWiFiStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else if (wifiManager != null) {
            binding.textWiFiStatus.setText("Disabled");
            binding.textWiFiStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
        } else {
            binding.textWiFiStatus.setText("Not Available");
            binding.textWiFiStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }

        // Check Internet
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                binding.textInternetStatus.setText("Connected");
                binding.textInternetStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                binding.textInternetStatus.setText("No Connection");
                binding.textInternetStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            }
        } else {
            binding.textInternetStatus.setText("Not Available");
            binding.textInternetStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }
    }

    // Hardware testing methods
    private void testLocation() {
        Intent intent = new Intent(this, HardwareTestActivity.class);
        intent.putExtra("test_type", "location");
        startActivity(intent);
    }

    private void testCamera() {
        Intent intent = new Intent(this, HardwareTestActivity.class);
        intent.putExtra("test_type", "camera");
        startActivity(intent);
    }

    private void testMicrophone() {
        Intent intent = new Intent(this, HardwareTestActivity.class);
        intent.putExtra("test_type", "microphone");
        startActivity(intent);
    }

    private void testVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
            Toast.makeText(this, "Vibration test completed", Toast.LENGTH_SHORT).show();
            binding.textVibrationStatus.setText("Working");
            binding.textVibrationStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            Toast.makeText(this, "Vibration not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void testBluetooth() {
        if (bluetoothAdapter != null) {
            String status = bluetoothAdapter.isEnabled() ? "Bluetooth is ON" : "Bluetooth is OFF";
            Toast.makeText(this, status + "\nAddress: " + bluetoothAdapter.getAddress(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void testSpeakers() {
        if (audioManager != null) {
            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            Toast.makeText(this, "Speaker Volume: " + volume + "/" + maxVolume, Toast.LENGTH_SHORT).show();
            binding.textSpeakersStatus.setText("Volume: " + volume + "/" + maxVolume);
            binding.textSpeakersStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            Toast.makeText(this, "Audio system not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void testScreen() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;
        int densityDpi = metrics.densityDpi;
        String screenInfo = String.format("Resolution: %dx%d\nDensity: %.1f (DPI: %d)",
                metrics.widthPixels, metrics.heightPixels, density, densityDpi);
        Toast.makeText(this, screenInfo, Toast.LENGTH_LONG).show();
    }

    private void testTouch() {
        touchCount++;
        binding.textTouchStatus.setText("Touches: " + touchCount);
        binding.textTouchStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        Toast.makeText(this, "Touch registered! Count: " + touchCount, Toast.LENGTH_SHORT).show();
    }

    private void testNFC() {
        if (nfcAdapter != null) {
            String status = nfcAdapter.isEnabled() ? "NFC is enabled" : "NFC is disabled";
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "NFC not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void testWiFi() {
        if (wifiManager != null) {
            String ssid = "Unknown";
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    ssid = wifiInfo.getSSID();
                }
            }
            String status = wifiManager.isWifiEnabled() ? "WiFi ON\nSSID: " + ssid : "WiFi OFF";
            Toast.makeText(this, status, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "WiFi not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void testInternet() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://www.google.com");
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.connect();
                int responseCode = connection.getResponseCode();

                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(this, "Internet connection working!", Toast.LENGTH_SHORT).show();
                        binding.textInternetStatus.setText("Connected");
                        binding.textInternetStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                    } else {
                        Toast.makeText(this, "Internet connection issues", Toast.LENGTH_SHORT).show();
                        binding.textInternetStatus.setText("Issues");
                        binding.textInternetStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                    binding.textInternetStatus.setText("No Connection");
                    binding.textInternetStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                });
            }
        }).start();
    }

    private void startMonitoringAllSensors() {
        for (Sensor sensor : availableSensors) {
            startSensorMonitoring(sensor);
        }
        Toast.makeText(this, "Started monitoring all sensors", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume sensor monitoring for active sensors
        for (Sensor sensor : activeSensors.values()) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause sensor monitoring to save battery
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAllSensorMonitoring();
        if (binding != null) {
            binding = null;
        }
    }
}