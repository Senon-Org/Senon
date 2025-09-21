package com.example.senon;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.senon.adapter.TestResultAdapter;
import com.example.senon.databinding.ActivityAutoTestResultsBinding;
import com.example.senon.model.TestResult;
import com.example.senon.utils.PdfReportGenerator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for displaying auto test results
 */
public class AutoTestResultsActivity extends AppCompatActivity {

    public static final String EXTRA_TEST_RESULTS = "test_results";
    public static final String EXTRA_TEST_DURATION = "test_duration";

    private ActivityAutoTestResultsBinding binding;
    private List<TestResult> testResults;
    private long totalTestDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAutoTestResultsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        getIntentData();
        setupRecyclerView();
        updateSummary();
        setupButtons();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Auto Test Results");
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        testResults = (List<TestResult>) intent.getSerializableExtra(EXTRA_TEST_RESULTS);
        totalTestDuration = intent.getLongExtra(EXTRA_TEST_DURATION, 0);

        if (testResults == null) {
            testResults = new ArrayList<>();
        }
    }

    private void setupRecyclerView() {
        TestResultAdapter adapter = new TestResultAdapter(testResults);
        binding.recyclerViewResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewResults.setAdapter(adapter);

        // Show empty state if no results
        if (testResults.isEmpty()) {
            binding.recyclerViewResults.setVisibility(View.GONE);
            binding.textViewEmptyState.setVisibility(View.VISIBLE);
            binding.textViewEmptyState.setText("No test results available");
        } else {
            binding.recyclerViewResults.setVisibility(View.VISIBLE);
            binding.textViewEmptyState.setVisibility(View.GONE);
        }
    }

    private void updateSummary() {
        if (testResults.isEmpty()) {
            binding.cardViewSummary.setVisibility(View.GONE);
            return;
        }

        // Calculate statistics
        int totalSensors = testResults.size();
        int workingSensors = 0;
        int failedSensors = 0;

        for (TestResult result : testResults) {
            if (result.isWorking()) {
                workingSensors++;
            } else {
                failedSensors++;
            }
        }

        // Update summary UI
        binding.textViewTotalSensors.setText(String.valueOf(totalSensors));
        binding.textViewWorkingSensors.setText(String.valueOf(workingSensors));
        binding.textViewFailedSensors.setText(String.valueOf(failedSensors));

        // Calculate success rate
        double successRate = totalSensors > 0 ? (workingSensors * 100.0 / totalSensors) : 0;
        binding.textViewSuccessRate.setText(String.format(Locale.getDefault(), "%.1f%%", successRate));

        // Format test duration
        binding.textViewTestDuration.setText(formatDuration(totalTestDuration));

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
        binding.textViewTestTimestamp.setText(sdf.format(new Date()));

        // Update progress indicator
        binding.progressBarSuccess.setProgress((int) successRate);

        // Set summary card background color based on success rate
        if (successRate >= 80) {
            binding.cardViewSummary.setCardBackgroundColor(getColor(android.R.color.holo_green_light));
        } else if (successRate >= 50) {
            binding.cardViewSummary.setCardBackgroundColor(getColor(android.R.color.holo_orange_light));
        } else {
            binding.cardViewSummary.setCardBackgroundColor(getColor(android.R.color.holo_red_light));
        }
    }

    private void setupButtons() {
        binding.buttonShareResults.setOnClickListener(v -> shareResults());
        binding.buttonGeneratePdf.setOnClickListener(v -> generatePdfReport());
        binding.buttonRetestAll.setOnClickListener(v -> retestAll());
    }

    private void shareResults() {
        if (testResults.isEmpty()) {
            Toast.makeText(this, "No results to share", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("Sensor Test Report\n");
        report.append("==================\n\n");

        // Summary
        int workingSensors = 0;
        for (TestResult result : testResults) {
            if (result.isWorking())
                workingSensors++;
        }

        report.append(String.format("Total Sensors: %d\n", testResults.size()));
        report.append(String.format("Working: %d\n", workingSensors));
        report.append(String.format("Failed: %d\n", testResults.size() - workingSensors));
        report.append(String.format("Success Rate: %.1f%%\n",
                testResults.size() > 0 ? (workingSensors * 100.0 / testResults.size()) : 0));
        report.append(String.format("Test Duration: %s\n\n", formatDuration(totalTestDuration)));

        // Individual results
        report.append("Individual Results:\n");
        report.append("------------------\n");

        for (TestResult result : testResults) {
            report.append(String.format("%s: %s\n",
                    result.getSensorName(),
                    result.isWorking() ? "PASS" : "FAIL"));

            if (!result.isWorking() && result.getErrorMessage() != null) {
                report.append(String.format("  Error: %s\n", result.getErrorMessage()));
            }

            if (result.isWorking() && result.getSampleData() != null) {
                report.append(String.format("  Sample: %s\n", result.getSampleDataString()));
            }

            report.append("\n");
        }

        // Create share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sensor Test Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, report.toString());

        startActivity(Intent.createChooser(shareIntent, "Share Test Results"));
    }

    private void generatePdfReport() {
        if (testResults.isEmpty()) {
            Toast.makeText(this, "No results to generate PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate PDF in background thread (no permissions needed for app directory)
        new Thread(() -> {
            PdfReportGenerator.PdfGenerationResult result = PdfReportGenerator.generateReport(
                    this, testResults, totalTestDuration);

            runOnUiThread(() -> {
                if (result.success) {
                    // Show dialog asking user what to do with the PDF
                    showPdfActionDialog(result.filePath);
                } else {
                    Toast.makeText(this, "Failed to generate PDF: " + result.errorMessage,
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void showPdfActionDialog(String filePath) {
        java.io.File file = new java.io.File(filePath);
        String fileName = file.getName();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("PDF Report Generated")
                .setMessage("Your sensor test report has been created. What would you like to do?")
                .setPositiveButton("View PDF", (dialog, which) -> {
                    openPdfFile(filePath);
                    dialog.dismiss();
                })
                .setNeutralButton("Share PDF", (dialog, which) -> {
                    sharePdfFile(filePath);
                    dialog.dismiss();
                })
                .setNegativeButton("Close", (dialog, which) -> {
                    // Just close dialog, PDF stays in cache
                    dialog.dismiss();
                    Toast.makeText(this, "PDF created temporarily", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(true)
                .show();
    }

    private void sharePdfFile(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            Uri uri = androidx.core.content.FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sensor Test Report");
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share PDF Report"));
        } catch (Exception e) {
            Toast.makeText(this, "Could not share PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openPdfFile(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            Uri uri = androidx.core.content.FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);

            // First try to open with PDF viewer
            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.setDataAndType(uri, "application/pdf");
            pdfIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (pdfIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(pdfIntent);
            } else {
                // No PDF viewer found, try alternative approaches
                showPdfViewerOptions(filePath, uri);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not open PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPdfViewerOptions(String filePath, Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No PDF Viewer Found")
                .setMessage("No PDF viewer app is installed on your device. What would you like to do?")
                .setPositiveButton("Open in Browser", (dialog, which) -> {
                    openPdfInBrowser(uri);
                    dialog.dismiss();
                })
                .setNeutralButton("Share PDF", (dialog, which) -> {
                    sharePdfFile(filePath);
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    private void openPdfInBrowser(Uri uri) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setDataAndType(uri, "*/*");
            browserIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(browserIntent);
                Toast.makeText(this, "Opening PDF in browser...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No app available to open the file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not open in browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void retestAll() {
        // Return to main activity and trigger auto test
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("trigger_auto_test", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format(Locale.getDefault(), "%.1fs", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return String.format(Locale.getDefault(), "%dm %ds", minutes, seconds);
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
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}