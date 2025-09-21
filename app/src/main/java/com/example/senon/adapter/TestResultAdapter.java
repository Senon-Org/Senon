package com.example.senon.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.senon.R;
import com.example.senon.model.TestResult;
import com.example.senon.utils.SensorIconMapper;

import java.util.List;

/**
 * Adapter for displaying test results in RecyclerView
 */
public class TestResultAdapter extends RecyclerView.Adapter<TestResultAdapter.TestResultViewHolder> {

    private List<TestResult> testResults;

    public TestResultAdapter(List<TestResult> testResults) {
        this.testResults = testResults;
    }

    @NonNull
    @Override
    public TestResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_test_result, parent, false);
        return new TestResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TestResultViewHolder holder, int position) {
        TestResult result = testResults.get(position);
        holder.bind(result);
    }

    @Override
    public int getItemCount() {
        return testResults.size();
    }

    public void updateResults(List<TestResult> newResults) {
        this.testResults = newResults;
        notifyDataSetChanged();
    }

    static class TestResultViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewSensorIcon;
        private TextView textViewSensorName;
        private TextView textViewSensorType;
        private TextView textViewTestStatus;
        private TextView textViewTestDuration;
        private TextView textViewSampleData;
        private TextView textViewErrorMessage;
        private TextView textViewAccuracy;
        private View layoutErrorDetails;
        private View layoutSuccessDetails;

        public TestResultViewHolder(@NonNull View itemView) {
            super(itemView);

            imageViewSensorIcon = itemView.findViewById(R.id.imageViewSensorIcon);
            textViewSensorName = itemView.findViewById(R.id.textViewSensorName);
            textViewSensorType = itemView.findViewById(R.id.textViewSensorType);
            textViewTestStatus = itemView.findViewById(R.id.textViewTestStatus);
            textViewTestDuration = itemView.findViewById(R.id.textViewTestDuration);
            textViewSampleData = itemView.findViewById(R.id.textViewSampleData);
            textViewErrorMessage = itemView.findViewById(R.id.textViewErrorMessage);
            textViewAccuracy = itemView.findViewById(R.id.textViewAccuracy);
            layoutErrorDetails = itemView.findViewById(R.id.layoutErrorDetails);
            layoutSuccessDetails = itemView.findViewById(R.id.layoutSuccessDetails);
        }

        public void bind(TestResult result) {
            // Set sensor icon
            int iconResId = SensorIconMapper.getIconForSensorType(result.getSensorTypeInt());
            imageViewSensorIcon.setImageResource(iconResId);

            // Set sensor information
            textViewSensorName.setText(result.getSensorName());
            textViewSensorType.setText(result.getSensorTypeString());
            textViewTestDuration.setText(result.getFormattedDuration());

            // Set status
            if (result.isWorking()) {
                // Success state
                textViewTestStatus.setText("PASS");
                textViewTestStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark));
                textViewTestStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0);

                // Show success details
                layoutSuccessDetails.setVisibility(View.VISIBLE);
                layoutErrorDetails.setVisibility(View.GONE);

                // Set sample data
                if (result.hasSampleData()) {
                    textViewSampleData.setText("Data: " + result.getSampleDataString());
                } else {
                    textViewSampleData.setText("No sample data");
                }

                // Set accuracy
                textViewAccuracy.setText("Accuracy: " + result.getAccuracyString());

            } else {
                // Failure state
                textViewTestStatus.setText("FAIL");
                textViewTestStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark));
                textViewTestStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error, 0, 0, 0);

                // Show error details
                layoutErrorDetails.setVisibility(View.VISIBLE);
                layoutSuccessDetails.setVisibility(View.GONE);

                // Set error message
                if (result.hasError()) {
                    textViewErrorMessage.setText(result.getErrorMessage());
                } else {
                    textViewErrorMessage.setText("Unknown error");
                }
            }
        }
    }
}