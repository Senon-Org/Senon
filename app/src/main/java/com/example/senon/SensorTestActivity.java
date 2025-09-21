package com.example.senon;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.senon.databinding.ActivitySensorTestBinding;
import com.example.senon.utils.SensorIconMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for testing individual sensors
 */
public class SensorTestActivity extends AppCompatActivity implements SensorEventListener {

    private ActivitySensorTestBinding binding;
    private SensorManager sensorManager;
    private Sensor currentSensor;
    private Handler uiHandler;
    private boolean isTestRunning = false;
    private long testStartTime;
    private int sampleCount = 0;
    private float[] lastValues;

    // Intent extras
    private int sensorType;
    private String sensorName;
    private String sensorVendor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySensorTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get intent extras
        getIntentExtras();

        // Initialize components
        initializeComponents();

        // Setup UI
        setupToolbar();
        setupSensorInfo();
        setupButtons();

        // Find and setup the sensor
        setupSensor();
    }

    private void getIntentExtras() {
        sensorType = getIntent().getIntExtra("sensor_type", -1);
        sensorName = getIntent().getStringExtra("sensor_name");
        sensorVendor = getIntent().getStringExtra("sensor_vendor");
    }

    private void initializeComponents() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        uiHandler = new Handler(Looper.getMainLooper());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sensor Test");
            getSupportActionBar().setSubtitle(sensorName != null ? sensorName : "Unknown Sensor");
        }
    }

    private void setupSensorInfo() {
        // Set sensor icon
        int iconResId = SensorIconMapper.getIconForSensorType(sensorType);
        binding.imageViewSensorIcon.setImageResource(iconResId);

        // Set sensor information
        binding.textViewSensorName.setText(sensorName != null ? sensorName : "Unknown Sensor");
        binding.textViewSensorType.setText(SensorIconMapper.getSensorCategory(sensorType).name());
        binding.textViewSensorVendor.setText(sensorVendor != null ? sensorVendor : "Unknown Vendor");

        // Initially hide real-time data
        binding.cardRealTimeData.setVisibility(View.GONE);
    }

    private void setupButtons() {
        binding.buttonStartTest.setOnClickListener(v -> startSensorTest());
        binding.buttonStopTest.setOnClickListener(v -> stopSensorTest());
        binding.buttonResetTest.setOnClickListener(v -> resetTest());

        // Initially disable stop button
        binding.buttonStopTest.setEnabled(false);
    }

    private void setupSensor() {
        if (sensorManager == null) {
            showError("Sensor manager not available");
            return;
        }

        currentSensor = sensorManager.getDefaultSensor(sensorType);
        if (currentSensor == null) {
            showError("Sensor not available on this device");
            return;
        }

        // Display sensor specifications
        updateSensorSpecs();
    }

    private void updateSensorSpecs() {
        if (currentSensor == null) return;

        StringBuilder specs = new StringBuilder();
        specs.append("Maximum Range: ").append(currentSensor.getMaximumRange()).append("\n");
        specs.append("Resolution: ").append(currentSensor.getResolution()).append("\n");
        specs.append("Power: ").append(currentSensor.getPower()).append(" mA\n");
        specs.append("Version: ").append(currentSensor.getVersion()).append("\n");
        specs.append("Min Delay: ").append(currentSensor.getMinDelay()).append(" μs");

        binding.textViewSensorSpecs.setText(specs.toString());
    }

    private void startSensorTest() {
        if (currentSensor == null) {
            showError("No sensor available for testing");
            return;
        }

        android.util.Log.d("SensorTestActivity", "Attempting to start test for sensor: " + currentSensor.getName());

        if (sensorManager == null) {
            android.util.Log.e("SensorTestActivity", "SensorManager is null!");
            showError("Sensor system not available");
            return;
        }

        android.util.Log.d("SensorTestActivity", "Found sensor: " + currentSensor.getName() +
                " (Vendor: " + currentSensor.getVendor() + ", Version: " + currentSensor.getVersion() +
                ", Max Range: " + currentSensor.getMaximumRange() + ", Resolution: " + currentSensor.getResolution() + ")");

        boolean registered = sensorManager.registerListener(this, currentSensor, SensorManager.SENSOR_DELAY_UI);
        android.util.Log.d("SensorTestActivity", "Sensor registration result: " + registered);

        if (registered) {
            isTestRunning = true;
            testStartTime = System.currentTimeMillis();
            sampleCount = 0;

            // Update UI
            binding.buttonStartTest.setEnabled(false);
            binding.buttonStopTest.setEnabled(true);
            binding.chipTestStatus.setText("TESTING");
            binding.chipTestStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
            binding.cardRealTimeData.setVisibility(View.VISIBLE);
            binding.progressIndicator.setVisibility(View.VISIBLE);

            // Update test info
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            binding.textViewStartTime.setText("Started: " + sdf.format(new Date()));

            Toast.makeText(this, "Sensor test started", Toast.LENGTH_SHORT).show();
            android.util.Log.d("SensorTestActivity", "Sensor test started successfully");

            // Set a timeout to check if we're receiving data
            uiHandler.postDelayed(() -> {
                if (isTestRunning && sampleCount == 0) {
                    android.util.Log.w("SensorTestActivity", "No sensor data received after 5 seconds");
                    binding.textViewRealTimeData.setText("Warning: No sensor data received. Try moving your device or check if the sensor is working properly.");
                }
            }, 5000);
        } else {
            android.util.Log.e("SensorTestActivity", "Failed to register sensor listener for: " + currentSensor.getName());
            showError("Failed to start sensor monitoring. The sensor might be in use by another app or there may be a system issue.");
        }
    }

    private void stopSensorTest() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        isTestRunning = false;

        // Update UI
        binding.buttonStartTest.setEnabled(true);
        binding.buttonStopTest.setEnabled(false);
        binding.chipTestStatus.setText("STOPPED");
        binding.chipTestStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
        binding.progressIndicator.setVisibility(View.GONE);

        // Calculate test duration
        long duration = System.currentTimeMillis() - testStartTime;
        binding.textViewDuration.setText("Duration: " + formatDuration(duration));
        binding.textViewSampleCount.setText("Samples: " + sampleCount);

        Toast.makeText(this, "Sensor test stopped", Toast.LENGTH_SHORT).show();
    }

    private void resetTest() {
        stopSensorTest();

        // Reset UI
        binding.cardRealTimeData.setVisibility(View.GONE);
        binding.chipTestStatus.setText("READY");
        binding.chipTestStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
        binding.textViewRealTimeData.setText("No data");
        binding.textViewStartTime.setText("");
        binding.textViewDuration.setText("");
        binding.textViewSampleCount.setText("");
        binding.textViewAccuracy.setText("");

        sampleCount = 0;
        lastValues = null;

        Toast.makeText(this, "Test reset", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isTestRunning || event == null || event.values == null) {
            if (!isTestRunning) {
                android.util.Log.d("SensorTestActivity", "Sensor data received but test not running");
            }
            if (event == null) {
                android.util.Log.w("SensorTestActivity", "Received null sensor event");
            }
            if (event != null && event.values == null) {
                android.util.Log.w("SensorTestActivity", "Received sensor event with null values");
            }
            return;
        }

        sampleCount++;
        lastValues = event.values.clone();

        // Log first few samples for debugging
        if (sampleCount <= 3) {
            android.util.Log.d("SensorTestActivity", "Sample " + sampleCount + " received: " +
                    java.util.Arrays.toString(event.values));
        }

        // Format sensor values
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 0; i < event.values.length; i++) {
            if (i > 0) valueBuilder.append("\n");
            valueBuilder.append(String.format(Locale.getDefault(), "Axis %d: %.4f", i, event.values[i]));
        }

        // Update UI on main thread
        uiHandler.post(() -> {
            binding.textViewRealTimeData.setText(valueBuilder.toString());
            binding.textViewSampleCount.setText("Samples: " + sampleCount);

            // Update test duration
            long duration = System.currentTimeMillis() - testStartTime;
            binding.textViewDuration.setText("Duration: " + formatDuration(duration));
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String accuracyText;
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                accuracyText = "High";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                accuracyText = "Medium";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                accuracyText = "Low";
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                accuracyText = "Unreliable";
                break;
            default:
                accuracyText = "Unknown";
                break;
        }

        uiHandler.post(() -> binding.textViewAccuracy.setText("Accuracy: " + accuracyText));
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return String.format(Locale.getDefault(), "%dm %ds", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%.1fs", milliseconds / 1000.0);
        }
    }

    private void showError(String message) {
        binding.chipTestStatus.setText("ERROR");
        binding.chipTestStatus.setChipBackgroundColorResource(android.R.color.holo_red_dark);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isTestRunning && sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isTestRunning && currentSensor != null && sensorManager != null) {
            sensorManager.registerListener(this, currentSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (binding != null) {
            binding = null;
        }
    }
}