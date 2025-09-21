package com.example.senon;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.senon.databinding.ActivityHardwareTestBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for testing hardware components like GPS, Camera, and Microphone
 */
public class HardwareTestActivity extends AppCompatActivity implements LocationListener {

    // Audio recording parameters
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private ActivityHardwareTestBinding binding;
    private LocationManager locationManager;
    private CameraManager cameraManager;
    private AudioRecord audioRecord;
    private Handler uiHandler;
    // Test states
    private boolean isLocationTestRunning = false;
    private boolean isCameraTestRunning = false;
    private boolean isMicTestRunning = false;
    // Test data
    private long locationTestStartTime;
    private long cameraTestStartTime;
    private long micTestStartTime;
    private int locationUpdatesCount = 0;
    private int cameraCount = 0;
    private double micLevel = 0.0;
    private int bufferSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHardwareTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupToolbar();
        setupButtons();
        checkHardwareAvailability();

        // Handle specific test type from intent
        String testType = getIntent().getStringExtra("test_type");
        if (testType != null) {
            showSpecificTest(testType);
        }
    }

    private void showSpecificTest(String testType) {
        // Hide all sections first
        findViewById(R.id.cardViewLocationSection).setVisibility(View.GONE);
        findViewById(R.id.cardViewCameraSection).setVisibility(View.GONE);
        findViewById(R.id.cardViewMicrophoneSection).setVisibility(View.GONE);

        // Show only the requested section and update toolbar
        switch (testType.toLowerCase()) {
            case "location":
                findViewById(R.id.cardViewLocationSection).setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Location Test");
                    getSupportActionBar().setSubtitle("GPS Testing");
                }
                break;
            case "camera":
                findViewById(R.id.cardViewCameraSection).setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Camera Test");
                    getSupportActionBar().setSubtitle("Camera Testing");
                }
                break;
            case "microphone":
                findViewById(R.id.cardViewMicrophoneSection).setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Microphone Test");
                    getSupportActionBar().setSubtitle("Audio Testing");
                }
                break;
            default:
                // Show all sections if test type is not recognized
                findViewById(R.id.cardViewLocationSection).setVisibility(View.VISIBLE);
                findViewById(R.id.cardViewCameraSection).setVisibility(View.VISIBLE);
                findViewById(R.id.cardViewMicrophoneSection).setVisibility(View.VISIBLE);
                break;
        }
    }

    private void initializeComponents() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        uiHandler = new Handler(Looper.getMainLooper());

        // Calculate buffer size for audio recording
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Hardware Test");
            getSupportActionBar().setSubtitle("Location, Camera & Microphone");
        }
    }

    private void setupButtons() {
        // Location test buttons
        binding.buttonStartLocationTest.setOnClickListener(v -> startLocationTest());
        binding.buttonStopLocationTest.setOnClickListener(v -> stopLocationTest());

        // Camera test buttons
        binding.buttonStartCameraTest.setOnClickListener(v -> startCameraTest());
        binding.buttonStopCameraTest.setOnClickListener(v -> stopCameraTest());

        // Microphone test buttons
        binding.buttonStartMicTest.setOnClickListener(v -> startMicrophoneTest());
        binding.buttonStopMicTest.setOnClickListener(v -> stopMicrophoneTest());

        // Initially disable stop buttons
        binding.buttonStopLocationTest.setEnabled(false);
        binding.buttonStopCameraTest.setEnabled(false);
        binding.buttonStopMicTest.setEnabled(false);
    }

    private void checkHardwareAvailability() {
        // Check Location availability
        if (locationManager != null) {
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (gpsEnabled || networkEnabled) {
                binding.textViewLocationStatus.setText("Available");
                binding.textViewLocationStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                binding.textViewLocationStatus.setText("Disabled");
                binding.textViewLocationStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
            }
        } else {
            binding.textViewLocationStatus.setText("Not Available");
            binding.textViewLocationStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }

        // Check Camera availability
        if (cameraManager != null) {
            try {
                String[] cameraIds = cameraManager.getCameraIdList();
                cameraCount = cameraIds.length;
                if (cameraCount > 0) {
                    binding.textViewCameraStatus.setText(cameraCount + " Camera(s) Available");
                    binding.textViewCameraStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                } else {
                    binding.textViewCameraStatus.setText("No Cameras");
                    binding.textViewCameraStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                }
            } catch (CameraAccessException e) {
                binding.textViewCameraStatus.setText("Access Error");
                binding.textViewCameraStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            }
        } else {
            binding.textViewCameraStatus.setText("Not Available");
            binding.textViewCameraStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }

        // Check Microphone availability
        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
            binding.textViewMicStatus.setText("Available");
            binding.textViewMicStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            binding.textViewMicStatus.setText("Not Available");
            binding.textViewMicStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }
    }

    // Location Testing Methods
    private void startLocationTest() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permissions not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        if (locationManager == null) {
            Toast.makeText(this, "Location manager not available", Toast.LENGTH_SHORT).show();
            return;
        }

        isLocationTestRunning = true;
        locationTestStartTime = System.currentTimeMillis();
        locationUpdatesCount = 0;

        // Request location updates
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
            }

            // Update UI
            binding.buttonStartLocationTest.setEnabled(false);
            binding.buttonStopLocationTest.setEnabled(true);
            binding.textViewLocationTestStatus.setText("TESTING");
            binding.textViewLocationTestStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            binding.cardViewLocationData.setVisibility(View.VISIBLE);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            binding.textViewLocationStartTime.setText("Started: " + sdf.format(new Date()));

            Toast.makeText(this, "Location test started", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            isLocationTestRunning = false;
        }
    }

    private void stopLocationTest() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }

        isLocationTestRunning = false;

        // Update UI
        binding.buttonStartLocationTest.setEnabled(true);
        binding.buttonStopLocationTest.setEnabled(false);
        binding.textViewLocationTestStatus.setText("STOPPED");
        binding.textViewLocationTestStatus.setTextColor(getColor(android.R.color.holo_red_dark));

        // Calculate test duration
        long duration = System.currentTimeMillis() - locationTestStartTime;
        binding.textViewLocationDuration.setText("Duration: " + formatDuration(duration));
        binding.textViewLocationUpdates.setText("Updates: " + locationUpdatesCount);

        Toast.makeText(this, "Location test stopped", Toast.LENGTH_SHORT).show();
    }

    // Camera Testing Methods
    private void startCameraTest() {
        if (cameraManager == null) {
            Toast.makeText(this, "Camera manager not available", Toast.LENGTH_SHORT).show();
            return;
        }

        isCameraTestRunning = true;
        cameraTestStartTime = System.currentTimeMillis();

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            StringBuilder cameraInfo = new StringBuilder();

            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                cameraInfo.append("Camera ").append(cameraId).append(": ");
                if (facing != null) {
                    switch (facing) {
                        case CameraCharacteristics.LENS_FACING_FRONT:
                            cameraInfo.append("Front");
                            break;
                        case CameraCharacteristics.LENS_FACING_BACK:
                            cameraInfo.append("Back");
                            break;
                        case CameraCharacteristics.LENS_FACING_EXTERNAL:
                            cameraInfo.append("External");
                            break;
                        default:
                            cameraInfo.append("Unknown");
                            break;
                    }
                }
                cameraInfo.append("\n");
            }

            // Update UI
            binding.buttonStartCameraTest.setEnabled(false);
            binding.buttonStopCameraTest.setEnabled(true);
            binding.textViewCameraTestStatus.setText("TESTING");
            binding.textViewCameraTestStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            binding.cardViewCameraData.setVisibility(View.VISIBLE);
            binding.textViewCameraInfo.setText(cameraInfo.toString());

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            binding.textViewCameraStartTime.setText("Started: " + sdf.format(new Date()));

            Toast.makeText(this, "Camera test started", Toast.LENGTH_SHORT).show();

        } catch (CameraAccessException e) {
            Toast.makeText(this, "Camera access error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isCameraTestRunning = false;
        }
    }

    private void stopCameraTest() {
        isCameraTestRunning = false;

        // Update UI
        binding.buttonStartCameraTest.setEnabled(true);
        binding.buttonStopCameraTest.setEnabled(false);
        binding.textViewCameraTestStatus.setText("STOPPED");
        binding.textViewCameraTestStatus.setTextColor(getColor(android.R.color.holo_red_dark));

        // Calculate test duration
        long duration = System.currentTimeMillis() - cameraTestStartTime;
        binding.textViewCameraDuration.setText("Duration: " + formatDuration(duration));
        binding.textViewCameraCount.setText("Cameras: " + cameraCount);

        Toast.makeText(this, "Camera test stopped", Toast.LENGTH_SHORT).show();
    }

    // Microphone Testing Methods
    private void startMicrophoneTest() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(this, "AudioRecord initialization failed", Toast.LENGTH_SHORT).show();
                return;
            }

            isMicTestRunning = true;
            micTestStartTime = System.currentTimeMillis();

            audioRecord.startRecording();

            // Update UI
            binding.buttonStartMicTest.setEnabled(false);
            binding.buttonStopMicTest.setEnabled(true);
            binding.textViewMicTestStatus.setText("TESTING");
            binding.textViewMicTestStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            binding.cardViewMicData.setVisibility(View.VISIBLE);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            binding.textViewMicStartTime.setText("Started: " + sdf.format(new Date()));

            // Start monitoring audio levels
            startAudioLevelMonitoring();

            Toast.makeText(this, "Microphone test started", Toast.LENGTH_SHORT).show();

        } catch (SecurityException e) {
            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            isMicTestRunning = false;
        }
    }

    private void stopMicrophoneTest() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        isMicTestRunning = false;

        // Update UI
        binding.buttonStartMicTest.setEnabled(true);
        binding.buttonStopMicTest.setEnabled(false);
        binding.textViewMicTestStatus.setText("STOPPED");
        binding.textViewMicTestStatus.setTextColor(getColor(android.R.color.holo_red_dark));

        // Calculate test duration
        long duration = System.currentTimeMillis() - micTestStartTime;
        binding.textViewMicDuration.setText("Duration: " + formatDuration(duration));

        Toast.makeText(this, "Microphone test stopped", Toast.LENGTH_SHORT).show();
    }

    private void startAudioLevelMonitoring() {
        new Thread(() -> {
            short[] buffer = new short[bufferSize];

            while (isMicTestRunning && audioRecord != null) {
                int read = audioRecord.read(buffer, 0, bufferSize);
                if (read > 0) {
                    // Calculate RMS (Root Mean Square) for audio level
                    double sum = 0;
                    for (int i = 0; i < read; i++) {
                        sum += buffer[i] * buffer[i];
                    }
                    double rms = Math.sqrt(sum / read);
                    micLevel = 20 * Math.log10(rms / 32767.0); // Convert to dB

                    // Update UI on main thread
                    uiHandler.post(() -> {
                        if (isMicTestRunning) {
                            binding.textViewMicLevel.setText(String.format(Locale.getDefault(), "Level: %.1f dB", micLevel));
                        }
                    });
                }

                try {
                    Thread.sleep(100); // Update every 100ms
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    // LocationListener implementation
    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (isLocationTestRunning) {
            locationUpdatesCount++;

            String locationInfo = String.format(Locale.getDefault(),
                    "Lat: %.6f\nLon: %.6f\nAccuracy: %.1fm\nProvider: %s",
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy(),
                    location.getProvider());

            binding.textViewLocationData.setText(locationInfo);
            binding.textViewLocationUpdates.setText("Updates: " + locationUpdatesCount);

            // Update duration
            long duration = System.currentTimeMillis() - locationTestStartTime;
            binding.textViewLocationDuration.setText("Duration: " + formatDuration(duration));
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Toast.makeText(this, provider + " enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Toast.makeText(this, provider + " disabled", Toast.LENGTH_SHORT).show();
    }

    // Utility methods
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
        // Stop all tests when activity is paused
        if (isLocationTestRunning) {
            stopLocationTest();
        }
        if (isCameraTestRunning) {
            stopCameraTest();
        }
        if (isMicTestRunning) {
            stopMicrophoneTest();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        if (binding != null) {
            binding = null;
        }
    }
}