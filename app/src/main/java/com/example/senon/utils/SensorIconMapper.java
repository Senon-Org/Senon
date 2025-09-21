package com.example.senon.utils;

import android.hardware.Sensor;

import com.example.senon.R;

/**
 * Utility class to map sensor types to appropriate icons
 */
public class SensorIconMapper {

    /**
     * Maps a sensor type to its corresponding icon resource ID
     *
     * @param sensorType The sensor type constant from Sensor class
     * @return The resource ID of the appropriate icon
     */
    public static int getIconForSensorType(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return android.R.drawable.ic_menu_compass; // Placeholder - will use custom icons
            case Sensor.TYPE_GYROSCOPE:
                return android.R.drawable.ic_menu_rotate;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return android.R.drawable.ic_menu_compass;
            case Sensor.TYPE_LIGHT:
                return android.R.drawable.ic_menu_day;
            case Sensor.TYPE_PROXIMITY:
                return android.R.drawable.ic_menu_view;
            case Sensor.TYPE_PRESSURE:
                return android.R.drawable.ic_menu_info_details;
            case Sensor.TYPE_TEMPERATURE:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return android.R.drawable.ic_dialog_info;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return android.R.drawable.ic_dialog_info;
            case Sensor.TYPE_HEART_RATE:
                return android.R.drawable.ic_menu_agenda;
            case Sensor.TYPE_STEP_COUNTER:
            case Sensor.TYPE_STEP_DETECTOR:
                return android.R.drawable.ic_menu_directions;
            case Sensor.TYPE_ROTATION_VECTOR:
                return android.R.drawable.ic_menu_rotate;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return android.R.drawable.ic_menu_compass;
            case Sensor.TYPE_GRAVITY:
                return android.R.drawable.ic_menu_compass;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                return android.R.drawable.ic_menu_rotate;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return android.R.drawable.ic_menu_compass;
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                return android.R.drawable.ic_menu_directions;
            case Sensor.TYPE_POSE_6DOF:
                return android.R.drawable.ic_menu_rotate;
            case Sensor.TYPE_STATIONARY_DETECT:
                return android.R.drawable.ic_menu_mylocation;
            case Sensor.TYPE_MOTION_DETECT:
                return android.R.drawable.ic_menu_directions;
            case Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT:
                return android.R.drawable.ic_menu_view;
            default:
                return android.R.drawable.ic_menu_help; // Default icon for unknown sensors
        }
    }

    /**
     * Gets a descriptive name for the sensor icon
     *
     * @param sensorType The sensor type constant from Sensor class
     * @return A string describing the icon type
     */
    public static String getIconDescription(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return "Acceleration";
            case Sensor.TYPE_GYROSCOPE:
                return "Rotation";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "Compass";
            case Sensor.TYPE_LIGHT:
                return "Light Bulb";
            case Sensor.TYPE_PROXIMITY:
                return "Proximity";
            case Sensor.TYPE_PRESSURE:
                return "Pressure Gauge";
            case Sensor.TYPE_TEMPERATURE:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "Thermometer";
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "Water Drop";
            case Sensor.TYPE_HEART_RATE:
                return "Heart";
            case Sensor.TYPE_STEP_COUNTER:
            case Sensor.TYPE_STEP_DETECTOR:
                return "Footsteps";
            case Sensor.TYPE_ROTATION_VECTOR:
                return "3D Rotation";
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return "Linear Motion";
            case Sensor.TYPE_GRAVITY:
                return "Gravity";
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                return "Game Rotation";
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return "Magnetic Rotation";
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                return "Motion Detection";
            case Sensor.TYPE_POSE_6DOF:
                return "6DOF Pose";
            case Sensor.TYPE_STATIONARY_DETECT:
                return "Stationary";
            case Sensor.TYPE_MOTION_DETECT:
                return "Motion";
            case Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT:
                return "Off-body Detection";
            default:
                return "Unknown Sensor";
        }
    }

    /**
     * Gets the color resource for a sensor type (for theming)
     *
     * @param sensorType The sensor type constant from Sensor class
     * @return The color resource ID
     */
    public static int getColorForSensorType(int sensorType) {
        switch (getSensorCategory(sensorType)) {
            case MOTION:
                return android.R.color.holo_blue_light;
            case ENVIRONMENT:
                return android.R.color.holo_green_light;
            case POSITION:
                return android.R.color.holo_orange_light;
            case BIOMETRIC:
                return android.R.color.holo_red_light;
            default:
                return android.R.color.darker_gray;
        }
    }

    /**
     * Gets the category of a sensor type
     *
     * @param sensorType The sensor type constant from Sensor class
     * @return The sensor category
     */
    public static SensorCategory getSensorCategory(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_LINEAR_ACCELERATION:
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
            case Sensor.TYPE_SIGNIFICANT_MOTION:
            case Sensor.TYPE_STEP_COUNTER:
            case Sensor.TYPE_STEP_DETECTOR:
            case Sensor.TYPE_MOTION_DETECT:
            case Sensor.TYPE_STATIONARY_DETECT:
                return SensorCategory.MOTION;

            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_PRESSURE:
            case Sensor.TYPE_TEMPERATURE:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
            case Sensor.TYPE_RELATIVE_HUMIDITY:
            case Sensor.TYPE_PROXIMITY:
                return SensorCategory.ENVIRONMENT;

            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_POSE_6DOF:
                return SensorCategory.POSITION;

            case Sensor.TYPE_HEART_RATE:
            case Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT:
                return SensorCategory.BIOMETRIC;

            default:
                return SensorCategory.OTHER;
        }
    }

    /**
     * Gets a human-readable category name
     *
     * @param category The sensor category
     * @return The category name
     */
    public static String getCategoryName(SensorCategory category) {
        switch (category) {
            case MOTION:
                return "Motion Sensors";
            case ENVIRONMENT:
                return "Environmental Sensors";
            case POSITION:
                return "Position Sensors";
            case BIOMETRIC:
                return "Biometric Sensors";
            default:
                return "Other Sensors";
        }
    }

    /**
     * Checks if a sensor type is commonly available on most devices
     *
     * @param sensorType The sensor type constant from Sensor class
     * @return true if the sensor is commonly available
     */
    public static boolean isCommonSensor(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_PROXIMITY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Gets the priority for displaying sensors (lower number = higher priority)
     *
     * @param sensorType The sensor type constant from Sensor class
     * @return The display priority
     */
    public static int getDisplayPriority(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return 1;
            case Sensor.TYPE_GYROSCOPE:
                return 2;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return 3;
            case Sensor.TYPE_LIGHT:
                return 4;
            case Sensor.TYPE_PROXIMITY:
                return 5;
            case Sensor.TYPE_PRESSURE:
                return 6;
            case Sensor.TYPE_TEMPERATURE:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return 7;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return 8;
            case Sensor.TYPE_HEART_RATE:
                return 9;
            case Sensor.TYPE_STEP_COUNTER:
                return 10;
            default:
                return 100; // Lower priority for uncommon sensors
        }
    }

    /**
     * Sensor categories for grouping
     */
    public enum SensorCategory {
        MOTION,
        ENVIRONMENT,
        POSITION,
        BIOMETRIC,
        OTHER
    }
}