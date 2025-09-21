package com.example.senon.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.senon.R;
import com.example.senon.model.SensorItem;
import com.example.senon.utils.SensorIconMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Adapter for displaying sensor items in a RecyclerView
 */
public class SensorAdapter extends RecyclerView.Adapter<SensorViewHolder> {
    private final Context context;
    private final List<SensorItem> sensorItems;
    private final List<SensorItem> filteredSensorItems;
    private SensorViewHolder.OnSensorItemClickListener onItemClickListener;
    private boolean showOnlyActiveSensors = false;
    private SensorIconMapper.SensorCategory filterCategory = null;

    public SensorAdapter(Context context) {
        this.context = context;
        this.sensorItems = new ArrayList<>();
        this.filteredSensorItems = new ArrayList<>();
    }

    @NonNull
    @Override
    public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sensor, parent, false);
        return new SensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
        SensorItem sensorItem = filteredSensorItems.get(position);
        holder.bind(sensorItem, onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return filteredSensorItems.size();
    }

    /**
     * Sets the list of sensor items
     *
     * @param sensorItems The list of sensor items to display
     */
    public void setSensorItems(List<SensorItem> sensorItems) {
        this.sensorItems.clear();
        this.sensorItems.addAll(sensorItems);
        sortSensorItems();
        applyFilters();
        notifyDataSetChanged();
    }

    /**
     * Adds a sensor item to the list
     *
     * @param sensorItem The sensor item to add
     */
    public void addSensorItem(SensorItem sensorItem) {
        sensorItems.add(sensorItem);
        sortSensorItems();
        applyFilters();
        notifyDataSetChanged();
    }

    /**
     * Updates a specific sensor item
     *
     * @param position   The position of the item to update
     * @param sensorItem The updated sensor item
     */
    public void updateSensorItem(int position, SensorItem sensorItem) {
        if (position >= 0 && position < filteredSensorItems.size()) {
            filteredSensorItems.set(position, sensorItem);
            notifyItemChanged(position);
        }
    }

    /**
     * Updates a sensor item by sensor type
     *
     * @param sensorType The sensor type to update
     * @param newValue   The new value to set
     */
    public void updateSensorValue(int sensorType, String newValue) {
        for (int i = 0; i < filteredSensorItems.size(); i++) {
            SensorItem item = filteredSensorItems.get(i);
            if (item.getSensorType() == sensorType) {
                item.setCurrentValue(newValue);
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * Updates the active status of a sensor
     *
     * @param sensorType The sensor type to update
     * @param isActive   The new active status
     */
    public void updateSensorStatus(int sensorType, boolean isActive) {
        for (int i = 0; i < filteredSensorItems.size(); i++) {
            SensorItem item = filteredSensorItems.get(i);
            if (item.getSensorType() == sensorType) {
                item.setActive(isActive);
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * Shows test progress for a specific sensor
     *
     * @param sensorType   The sensor type being tested
     * @param showProgress Whether to show progress
     */
    public void showSensorTestProgress(int sensorType, boolean showProgress) {
        for (int i = 0; i < filteredSensorItems.size(); i++) {
            SensorItem item = filteredSensorItems.get(i);
            if (item.getSensorType() == sensorType) {
                // Update the item and notify change
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * Clears all sensor items
     */
    public void clearSensorItems() {
        sensorItems.clear();
        filteredSensorItems.clear();
        notifyDataSetChanged();
    }

    /**
     * Gets the sensor item at a specific position
     *
     * @param position The position
     * @return The sensor item at the position
     */
    public SensorItem getSensorItem(int position) {
        if (position >= 0 && position < filteredSensorItems.size()) {
            return filteredSensorItems.get(position);
        }
        return null;
    }

    /**
     * Gets all sensor items
     *
     * @return List of all sensor items
     */
    public List<SensorItem> getAllSensorItems() {
        return new ArrayList<>(sensorItems);
    }

    /**
     * Gets filtered sensor items
     *
     * @return List of filtered sensor items
     */
    public List<SensorItem> getFilteredSensorItems() {
        return new ArrayList<>(filteredSensorItems);
    }

    /**
     * Sets the click listener for sensor items
     *
     * @param listener The click listener
     */
    public void setOnItemClickListener(SensorViewHolder.OnSensorItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    /**
     * Filters sensors to show only active ones
     *
     * @param showOnlyActive Whether to show only active sensors
     */
    public void filterByActiveStatus(boolean showOnlyActive) {
        this.showOnlyActiveSensors = showOnlyActive;
        applyFilters();
        notifyDataSetChanged();
    }

    /**
     * Filters sensors by category
     *
     * @param category The category to filter by (null for no filter)
     */
    public void filterByCategory(SensorIconMapper.SensorCategory category) {
        this.filterCategory = category;
        applyFilters();
        notifyDataSetChanged();
    }

    /**
     * Applies current filters to the sensor list
     */
    private void applyFilters() {
        filteredSensorItems.clear();

        for (SensorItem item : sensorItems) {
            boolean includeItem = true;

            // Apply active status filter
            if (showOnlyActiveSensors && !item.isActive()) {
                includeItem = false;
            }

            // Apply category filter
            if (filterCategory != null) {
                SensorIconMapper.SensorCategory itemCategory =
                        SensorIconMapper.getSensorCategory(item.getSensorType());
                if (itemCategory != filterCategory) {
                    includeItem = false;
                }
            }

            if (includeItem) {
                filteredSensorItems.add(item);
            }
        }
    }

    /**
     * Sorts sensor items by display priority and name
     */
    private void sortSensorItems() {
        Collections.sort(sensorItems, new Comparator<SensorItem>() {
            @Override
            public int compare(SensorItem o1, SensorItem o2) {
                // First sort by priority (lower number = higher priority)
                int priority1 = SensorIconMapper.getDisplayPriority(o1.getSensorType());
                int priority2 = SensorIconMapper.getDisplayPriority(o2.getSensorType());

                if (priority1 != priority2) {
                    return Integer.compare(priority1, priority2);
                }

                // Then sort by name
                return o1.getSensorName().compareToIgnoreCase(o2.getSensorName());
            }
        });
    }

    /**
     * Searches for sensors by name or type
     *
     * @param query The search query
     */
    public void search(String query) {
        filteredSensorItems.clear();

        if (query == null || query.trim().isEmpty()) {
            applyFilters();
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (SensorItem item : sensorItems) {
                if (item.getSensorName().toLowerCase().contains(lowerQuery) ||
                        item.getSensorTypeString().toLowerCase().contains(lowerQuery) ||
                        item.getSensorVendor().toLowerCase().contains(lowerQuery)) {

                    // Apply other filters
                    boolean includeItem = true;
                    if (showOnlyActiveSensors && !item.isActive()) {
                        includeItem = false;
                    }
                    if (filterCategory != null) {
                        SensorIconMapper.SensorCategory itemCategory =
                                SensorIconMapper.getSensorCategory(item.getSensorType());
                        if (itemCategory != filterCategory) {
                            includeItem = false;
                        }
                    }

                    if (includeItem) {
                        filteredSensorItems.add(item);
                    }
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Gets the number of active sensors
     *
     * @return The count of active sensors
     */
    public int getActiveSensorCount() {
        int count = 0;
        for (SensorItem item : sensorItems) {
            if (item.isActive()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the total number of sensors
     *
     * @return The total count of sensors
     */
    public int getTotalSensorCount() {
        return sensorItems.size();
    }
}