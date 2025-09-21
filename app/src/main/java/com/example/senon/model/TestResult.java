package com.example.senon.model;

import android.hardware.Sensor;

import java.io.Serializable;

/**
 * Data class representing the result of a sensor test
 */
public class TestResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private final transient Sensor sensor;
    private final String sensorName;
    private final String sensorVendor;
    private final int sensorType;
    private final String sensorStringType;
    private final boolean isWorking;
    private final String errorMessage;
    private final float[] sampleData;
    private final long testDuration;
    private final int accuracy;
    private final long timestamp;
    private final String testStatus;

    public TestResult(Sensor sensor, boolean isWorking, String errorMessage,
                      float[] sampleData, long testDuration, int accuracy) {
        this.sensor = sensor;
        this.sensorName = sensor != null ? sensor.getName() : "Unknown";
        this.sensorVendor = sensor != null ? sensor.getVendor() : "Unknown";
        this.sensorType = sensor != null ? sensor.getType() : -1;
        this.sensorStringType = sensor != null ? sensor.getStringType() : "Unknown";
        this.isWorking = isWorking;
        this.errorMessage = errorMessage;
        this.sampleData = sampleData != null ? sampleData.clone() : null;
        this.testDuration = testDuration;
        this.accuracy = accuracy;
        this.timestamp = System.currentTimeMillis();
        this.testStatus = determineTestStatus();
    }

    public static TestResult createFailedTest(Sensor sensor, String errorMessage, long testDuration) {
        return new Builder()
                .setSensor(sensor)
                .setWorking(false)
                .setErrorMessage(errorMessage)
                .setTestDuration(testDuration)
                .build();
    }

    public static TestResult createSuccessfulTest(Sensor sensor, float[] sampleData,
                                                  long testDuration, int accuracy) {
        return new Builder()
                .setSensor(sensor)
                .setWorking(true)
                .setSampleData(sampleData)
                .setTestDuration(testDuration)
                .setAccuracy(accuracy)
                .build();
    }

    // Getters
    public Sensor getSensor() {
        return sensor;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public float[] getSampleData() {
        return sampleData != null ? sampleData.clone() : null;
    }

    public long getTestDuration() {
        return testDuration;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTestStatus() {
        return testStatus;
    }

    // Utility methods
    public String getSensorName() {
        return sensorName;
    }

    public String getSensorTypeString() {
        return sensorStringType != null ? sensorStringType : "Type " + sensorType;
    }

    public String getSensorVendor() {
        return sensorVendor;
    }

    public int getSensorTypeInt() {
        return sensorType;
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    public boolean hasSampleData() {
        return sampleData != null && sampleData.length > 0;
    }

    public String getFormattedDuration() {
        if (testDuration < 1000) {
            return testDuration + "ms";
        } else {
            return String.format("%.1fs", testDuration / 1000.0);
        }
    }

    public String getAccuracyString() {
        switch (accuracy) {
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return "High";
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return "Medium";
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return "Low";
            case android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE:
                return "Unreliable";
            default:
                return "Unknown";
        }
    }

    public String getSampleDataString() {
        if (sampleData == null || sampleData.length == 0) {
            return "No data";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(sampleData.length, 3); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.2f", sampleData[i]));
        }
        if (sampleData.length > 3) {
            sb.append("...");
        }
        return sb.toString();
    }

    private String determineTestStatus() {
        if (isWorking) {
            if (accuracy >= android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                return "PASS";
            } else {
                return "PASS (Low Accuracy)";
            }
        } else {
            return "FAIL";
        }
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "sensor=" + sensorName +
                ", isWorking=" + isWorking +
                ", errorMessage='" + errorMessage + '\'' +
                ", sampleData=" + getSampleDataString() +
                ", testDuration=" + testDuration +
                ", accuracy=" + accuracy +
                ", timestamp=" + timestamp +
                '}';
    }

    // Builder pattern for easier construction
    public static class Builder {
        private Sensor sensor;
        private boolean isWorking = false;
        private String errorMessage = null;
        private float[] sampleData = null;
        private long testDuration = 0;
        private int accuracy = 0;

        public Builder setSensor(Sensor sensor) {
            this.sensor = sensor;
            return this;
        }

        public Builder setWorking(boolean working) {
            this.isWorking = working;
            return this;
        }

        public Builder setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder setSampleData(float[] sampleData) {
            this.sampleData = sampleData;
            return this;
        }

        public Builder setTestDuration(long testDuration) {
            this.testDuration = testDuration;
            return this;
        }

        public Builder setAccuracy(int accuracy) {
            this.accuracy = accuracy;
            return this;
        }

        public TestResult build() {
            if (sensor == null) {
                throw new IllegalStateException("Sensor must be set");
            }
            return new TestResult(sensor, isWorking, errorMessage, sampleData, testDuration, accuracy);
        }
    }
}