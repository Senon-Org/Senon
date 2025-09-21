package com.example.senon.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.senon.R;
import com.example.senon.model.SensorItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/**
 * ViewHolder for sensor items in the RecyclerView
 */
public class SensorViewHolder extends RecyclerView.ViewHolder {
    private final ImageView imageViewSensorIcon;
    private final TextView textViewSensorName;
    private final TextView textViewSensorType;
    private final TextView textViewSensorVendor;
    private final Chip chipStatus;
    private final TextView textViewCurrentValue;
    private final CircularProgressIndicator progressIndicatorTest;

    public SensorViewHolder(@NonNull View itemView) {
        super(itemView);

        // Initialize views
        imageViewSensorIcon = itemView.findViewById(R.id.imageViewSensorIcon);
        textViewSensorName = itemView.findViewById(R.id.textViewSensorName);
        textViewSensorType = itemView.findViewById(R.id.textViewSensorType);
        textViewSensorVendor = itemView.findViewById(R.id.textViewSensorVendor);
        chipStatus = itemView.findViewById(R.id.chipStatus);
        textViewCurrentValue = itemView.findViewById(R.id.textViewCurrentValue);
        progressIndicatorTest = itemView.findViewById(R.id.progressIndicatorTest);
    }

    /**
     * Binds sensor data to the views
     *
     * @param sensorItem          The sensor item to display
     * @param onItemClickListener Click listener for the item
     */
    public void bind(SensorItem sensorItem, OnSensorItemClickListener onItemClickListener) {
        // Set sensor icon
        imageViewSensorIcon.setImageResource(sensorItem.getIconResId());

        // Set sensor name
        textViewSensorName.setText(sensorItem.getSensorName());

        // Set sensor type
        textViewSensorType.setText(sensorItem.getSensorTypeString());

        // Set sensor vendor
        textViewSensorVendor.setText(sensorItem.getSensorVendor());

        // Set current value
        textViewCurrentValue.setText(sensorItem.getCurrentValue());

        // Set status
        updateStatus(sensorItem.isActive());

        // Set click listener
        itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onSensorItemClick(sensorItem, getAdapterPosition());
            }
        });
    }

    /**
     * Updates the status indicator
     *
     * @param isActive Whether the sensor is currently active
     */
    public void updateStatus(boolean isActive) {
        if (isActive) {
            chipStatus.setText("ACTIVE");
            chipStatus.setChipBackgroundColorResource(R.color.success_background);
            chipStatus.setVisibility(View.VISIBLE);
        } else {
            chipStatus.setText("INACTIVE");
            chipStatus.setChipBackgroundColorResource(R.color.error_background);
            chipStatus.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the current sensor value display
     *
     * @param value The new sensor value to display
     */
    public void updateCurrentValue(String value) {
        textViewCurrentValue.setText(value);
    }

    /**
     * Shows or hides the test progress indicator
     *
     * @param show Whether to show the progress indicator
     */
    public void showTestProgress(boolean show) {
        if (show) {
            progressIndicatorTest.setVisibility(View.VISIBLE);
            chipStatus.setVisibility(View.GONE);
        } else {
            progressIndicatorTest.setVisibility(View.GONE);
            chipStatus.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the sensor item with new data
     *
     * @param sensorItem The updated sensor item
     */
    public void updateSensorItem(SensorItem sensorItem) {
        textViewSensorName.setText(sensorItem.getSensorName());
        textViewSensorType.setText(sensorItem.getSensorTypeString());
        textViewSensorVendor.setText(sensorItem.getSensorVendor());
        textViewCurrentValue.setText(sensorItem.getCurrentValue());
        updateStatus(sensorItem.isActive());
        imageViewSensorIcon.setImageResource(sensorItem.getIconResId());
    }

    /**
     * Sets the sensor icon
     *
     * @param iconResId The resource ID of the icon
     */
    public void setSensorIcon(int iconResId) {
        imageViewSensorIcon.setImageResource(iconResId);
    }

    /**
     * Gets the sensor name TextView for external access
     *
     * @return The sensor name TextView
     */
    public TextView getSensorNameTextView() {
        return textViewSensorName;
    }

    /**
     * Gets the current value TextView for external access
     *
     * @return The current value TextView
     */
    public TextView getCurrentValueTextView() {
        return textViewCurrentValue;
    }

    /**
     * Gets the status Chip for external access
     *
     * @return The status Chip
     */
    public Chip getStatusChip() {
        return chipStatus;
    }

    /**
     * Interface for handling sensor item clicks
     */
    public interface OnSensorItemClickListener {
        void onSensorItemClick(SensorItem sensorItem, int position);
    }
}