package com.example.senon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

/**
 * PowerManager handles battery optimization and power saving restrictions
 * to ensure the sensor testing app can run without interruptions
 */
public class PowerOptimizationManager {

    private static final String TAG = "PowerOptimizationManager";

    /**
     * Check if the app is whitelisted from battery optimizations
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return true; // Assume true for older versions
    }

    /**
     * Request to ignore battery optimizations for intensive sensor testing
     */
    @SuppressLint("BatteryLife")
    public static void requestIgnoreBatteryOptimizations(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(activity)) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);

                    Toast.makeText(activity,
                            "Please allow this app to ignore battery optimizations for accurate sensor testing",
                            Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    // Fallback to general battery optimization settings
                    openBatteryOptimizationSettings(activity);
                }
            }
        }
    }

    /**
     * Open battery optimization settings page
     */
    public static void openBatteryOptimizationSettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            activity.startActivity(intent);

            Toast.makeText(activity,
                    "Please find '" + activity.getString(R.string.app_name) + "' and disable battery optimization",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            // Fallback to general settings
            openGeneralSettings(activity);
        }
    }

    /**
     * Open device-specific battery optimization settings (avoiding auto-start)
     */
    public static void openDeviceSpecificPowerSettings(Activity activity) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();

        try {
            Intent intent = null;

            switch (manufacturer) {
                case "xiaomi":
                case "redmi":
                    // MIUI Battery Saver settings (not auto-start)
                    try {
                        intent = new Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST");
                        intent.setClassName("com.miui.powerkeeper",
                                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity");
                        intent.putExtra("package_name", activity.getPackageName());
                    } catch (Exception e) {
                        // Fallback to general battery optimization
                        openBatteryOptimizationSettings(activity);
                        return;
                    }
                    break;

                case "huawei":
                case "honor":
                    // Huawei Battery Optimization (not protected apps)
                    try {
                        intent = new Intent("huawei.intent.action.HSM_BATTERY_OPTIMIZATION");
                        intent.setClassName("com.huawei.systemmanager",
                                "com.huawei.systemmanager.power.ui.HwPowerManagerActivity");
                    } catch (Exception e) {
                        openBatteryOptimizationSettings(activity);
                        return;
                    }
                    break;

                case "samsung":
                    // Samsung Battery Optimization
                    intent = new Intent("samsung.android.sm.ACTION_BATTERY_OPTIMIZATION_DETAIL");
                    intent.putExtra("package_name", activity.getPackageName());
                    break;

                case "oppo":
                case "oneplus":
                    // ColorOS Battery Optimization (not auto-launch)
                    try {
                        intent = new Intent("com.coloros.safecenter.permission.startup");
                        intent.setClassName("com.coloros.safecenter",
                                "com.coloros.safecenter.permission.startup.StartupAppListActivity");
                    } catch (Exception e) {
                        openBatteryOptimizationSettings(activity);
                        return;
                    }
                    break;

                case "vivo":
                    // Vivo Battery Optimization
                    try {
                        intent = new Intent("com.vivo.abe.action.BACKGROUND_MANAGER");
                    } catch (Exception e) {
                        openBatteryOptimizationSettings(activity);
                        return;
                    }
                    break;

                default:
                    // Generic battery optimization settings
                    openBatteryOptimizationSettings(activity);
                    return;
            }

            if (intent != null) {
                activity.startActivity(intent);
                showManufacturerSpecificInstructions(activity, manufacturer);
            }

        } catch (Exception e) {
            // Fallback to general battery optimization settings
            openBatteryOptimizationSettings(activity);
        }
    }

    /**
     * Show manufacturer-specific instructions
     */
    private static void showManufacturerSpecificInstructions(Activity activity, String manufacturer) {
        String message = "";

        switch (manufacturer) {
            case "xiaomi":
            case "redmi":
                message = "Disable battery optimization for " + activity.getString(R.string.app_name) +
                        " to prevent sensor testing interruptions";
                break;

            case "huawei":
            case "honor":
                message = "Allow " + activity.getString(R.string.app_name) +
                        " to run without battery restrictions for accurate sensor testing";
                break;

            case "samsung":
                message = "Disable battery optimization for " + activity.getString(R.string.app_name) +
                        " in Device Care settings";
                break;

            case "oppo":
            case "oneplus":
                message = "Allow " + activity.getString(R.string.app_name) +
                        " to run in background without restrictions";
                break;

            case "vivo":
                message = "Allow " + activity.getString(R.string.app_name) +
                        " to run in background for accurate sensor testing";
                break;
        }

        if (!message.isEmpty()) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Open general device settings as fallback
     */
    private static void openGeneralSettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);

            Toast.makeText(activity,
                    "Please disable battery optimization and allow background activity",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(activity,
                    "Please manually disable battery optimization in device settings",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Get device manufacturer for power management customization
     */
    public static String getDeviceManufacturer() {
        return Build.MANUFACTURER.toLowerCase();
    }

    /**
     * Check if device has aggressive power management
     */
    public static boolean hasAggressivePowerManagement() {
        String manufacturer = getDeviceManufacturer();
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("huawei") ||
                manufacturer.contains("honor") ||
                manufacturer.contains("oppo") ||
                manufacturer.contains("oneplus") ||
                manufacturer.contains("vivo") ||
                manufacturer.contains("realme");
    }

    /**
     * Show power optimization dialog with device-specific guidance
     */
    public static void showPowerOptimizationDialog(Activity activity) {
        if (hasAggressivePowerManagement()) {
            String manufacturer = getDeviceManufacturer();
            String message = "For accurate sensor testing on " + Build.MANUFACTURER +
                    " devices, please disable battery optimization to prevent interruptions during testing.";

            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();

            // Automatically open device-specific battery optimization settings
            openDeviceSpecificPowerSettings(activity);
        } else {
            requestIgnoreBatteryOptimizations(activity);
        }
    }
}