package com.example.senon.model;

import android.hardware.Sensor;

/**
 * Data class representing a sensor item with its associated icon and current value
 */
public class SensorItem {
    private final Sensor sensor;
    private final int iconResId;
    private String currentValue;
    private boolean isActive;
    private long lastUpdated;

    public SensorItem(Sensor sensor, int iconResId) {
        this.sensor = sensor;
        this.iconResId = iconResId;
        this.currentValue = "N/A";
        this.isActive = false;
        this.lastUpdated = System.currentTimeMillis();
    }

    public SensorItem(Sensor sensor, int iconResId, String currentValue) {
        this.sensor = sensor;
        this.iconResId = iconResId;
        this.currentValue = currentValue;
        this.isActive = false;
        this.lastUpdated = System.currentTimeMillis();
    }

    public static String getSensorTypeString(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return "Accelerometer";
            case Sensor.TYPE_GYROSCOPE:
                return "Gyroscope";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "Magnetometer";
            case Sensor.TYPE_LIGHT:
                return "Light Sensor";
            case Sensor.TYPE_PROXIMITY:
                return "Proximity Sensor";
            case Sensor.TYPE_PRESSURE:
                return "Pressure Sensor";
            case Sensor.TYPE_TEMPERATURE:
                return "Temperature Sensor";
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "Humidity Sensor";
            case Sensor.TYPE_HEART_RATE:
                return "Heart Rate Sensor";
            case Sensor.TYPE_STEP_COUNTER:
                return "Step Counter";
            case Sensor.TYPE_STEP_DETECTOR:
                return "Step Detector";
            case Sensor.TYPE_ROTATION_VECTOR:
                return "Rotation Vector";
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return "Linear Acceleration";
            case Sensor.TYPE_GRAVITY:
                return "Gravity Sensor";
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "Ambient Temperature";
            default:
                return "Unknown Sensor";
        }
    }

    // Getters
    public Sensor getSensor() {
        return sensor;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    // Setters
    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
        this.lastUpdated = System.currentTimeMillis();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    // Utility methods
    public String getSensorName() {
        return sensor.getName();
    }

    public String getSensorVendor() {
        return sensor.getVendor();
    }

    public int getSensorType() {
        return sensor.getType();
    }

    public String getSensorTypeString() {
        return getSensorTypeString(sensor.getType());
    }

    public float getMaximumRange() {
        return sensor.getMaximumRange();
    }

    public float getResolution() {
        return sensor.getResolution();
    }

    public float getPower() {
        return sensor.getPower();
    }

    public int getVersion() {
        return sensor.getVersion();
    }

    @Override
    public String toString() {
        return "SensorItem{" +
                "sensorName='" + getSensorName() + '\'' +
                ", sensorType='" + getSensorTypeString() + '\'' +
                ", currentValue='" + currentValue + '\'' +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SensorItem that = (SensorItem) obj;
        return sensor.getType() == that.sensor.getType() &&
                sensor.getName().equals(that.sensor.getName());
    }

    @Override
    public int hashCode() {
        return sensor.getName().hashCode() + sensor.getType();
    }
}