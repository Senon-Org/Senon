package com.example.senon;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import com.example.senon.model.TestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Background service for testing all sensors automatically
 */
public class AutoSensorTester {

    private static final int TEST_DURATION_MS = 3000; // 3 seconds per sensor
    private static final int SAMPLE_COLLECTION_TIMEOUT_MS = 5000; // 5 seconds timeout

    private SensorManager sensorManager;
    private Handler mainHandler;
    private List<TestResult> testResults;
    private boolean isTesting = false;

    public AutoSensorTester(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        testResults = new ArrayList<>();
    }

    public void testAllSensors(List<Sensor> sensors, TestCallback callback) {
        if (isTesting) {
            callback.onTestError("Testing already in progress");
            return;
        }

        if (sensors == null || sensors.isEmpty()) {
            callback.onTestError("No sensors to test");
            return;
        }

        isTesting = true;
        testResults.clear();

        // Start testing in background thread
        new Thread(() -> {
            try {
                mainHandler.post(() -> callback.onTestStarted(sensors.size()));

                for (int i = 0; i < sensors.size(); i++) {
                    if (!isTesting) break; // Allow cancellation

                    Sensor sensor = sensors.get(i);
                    TestResult result = testSensor(sensor);
                    testResults.add(result);

                    final int progress = i + 1;
                    mainHandler.post(() -> callback.onSensorTested(result, progress, sensors.size()));

                    // Add a longer delay between tests to avoid overwhelming the sensor system
                    // This is especially important for Xiaomi devices
                    try {
                        Thread.sleep(500); // Increased from 100ms to 500ms
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                isTesting = false;
                mainHandler.post(() -> callback.onTestCompleted(new ArrayList<>(testResults)));

            } catch (Exception e) {
                isTesting = false;
                mainHandler.post(() -> callback.onTestError("Testing failed: " + e.getMessage()));
            }
        }).start();
    }

    private TestResult testSensor(Sensor sensor) {
        long startTime = System.currentTimeMillis();

        try {
            // Check if sensor is null or not available
            if (sensor == null) {
                return new TestResult(
                        sensor,
                        false,
                        "Sensor is null",
                        null,
                        System.currentTimeMillis() - startTime,
                        0
                );
            }

            // Check if sensor manager is available
            if (sensorManager == null) {
                return new TestResult(
                        sensor,
                        false,
                        "SensorManager is not available",
                        null,
                        System.currentTimeMillis() - startTime,
                        0
                );
            }

            SensorTestListener listener = new SensorTestListener();

            // Try multiple registration attempts with different delays
            boolean registered = false;
            String lastError = "";

            // First attempt with SENSOR_DELAY_NORMAL
            registered = sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );

            if (!registered) {
                // Second attempt with SENSOR_DELAY_UI (faster)
                registered = sensorManager.registerListener(
                        listener,
                        sensor,
                        SensorManager.SENSOR_DELAY_UI
                );
                lastError = "Failed with NORMAL delay, ";
            }

            if (!registered) {
                // Third attempt with SENSOR_DELAY_GAME (even faster)
                registered = sensorManager.registerListener(
                        listener,
                        sensor,
                        SensorManager.SENSOR_DELAY_GAME
                );
                lastError += "Failed with UI delay, ";
            }

            if (!registered) {
                return new TestResult(
                        sensor,
                        false,
                        lastError + "Failed to register sensor listener - sensor may not be available or accessible",
                        null,
                        System.currentTimeMillis() - startTime,
                        0
                );
            }

            // Wait for data or timeout
            boolean hasData = listener.waitForData(SAMPLE_COLLECTION_TIMEOUT_MS);

            // Always unregister listener, even if no data received
            try {
                sensorManager.unregisterListener(listener);
            } catch (Exception unregisterException) {
                // Log but don't fail the test for unregister issues
            }

            long testDuration = System.currentTimeMillis() - startTime;

            if (hasData) {
                return new TestResult(
                        sensor,
                        true,
                        null,
                        listener.getSampleData(),
                        testDuration,
                        listener.getAccuracy()
                );
            } else {
                return new TestResult(
                        sensor,
                        false,
                        "Sensor registered successfully but no data received within " + (SAMPLE_COLLECTION_TIMEOUT_MS / 1000) + " seconds",
                        null,
                        testDuration,
                        0
                );
            }

        } catch (SecurityException e) {
            return new TestResult(
                    sensor,
                    false,
                    "Permission denied: " + e.getMessage() + " - Check sensor permissions",
                    null,
                    System.currentTimeMillis() - startTime,
                    0
            );
        } catch (Exception e) {
            return new TestResult(
                    sensor,
                    false,
                    "Test error: " + e.getMessage(),
                    null,
                    System.currentTimeMillis() - startTime,
                    0
            );
        }
    }

    public void cancelTesting() {
        isTesting = false;
    }

    public boolean isTesting() {
        return isTesting;
    }

    public List<TestResult> getLastResults() {
        return new ArrayList<>(testResults);
    }

    public interface TestCallback {
        void onTestStarted(int totalSensors);

        void onSensorTested(TestResult result, int progress, int total);

        void onTestCompleted(List<TestResult> results);

        void onTestError(String error);
    }

    private static class SensorTestListener implements SensorEventListener {
        private float[] sampleData;
        private int accuracy = 0;
        private CountDownLatch dataLatch = new CountDownLatch(1);
        private boolean hasReceivedData = false;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!hasReceivedData) {
                sampleData = event.values.clone();
                hasReceivedData = true;
                dataLatch.countDown();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            this.accuracy = accuracy;
        }

        public boolean waitForData(long timeoutMs) {
            try {
                return dataLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        public float[] getSampleData() {
            return sampleData;
        }

        public int getAccuracy() {
            return accuracy;
        }
    }
}