package com.example.senon.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.example.senon.model.TestResult;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for generating professional PDF reports of sensor test results
 */
public class PdfReportGenerator {

    private static final String TAG = "PdfReportGenerator";

    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(63, 81, 181); // Material Blue
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(76, 175, 80); // Material Green
    private static final DeviceRgb ERROR_COLOR = new DeviceRgb(244, 67, 54); // Material Red
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(245, 245, 245);
    private static final DeviceRgb WHITE_COLOR = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb BLACK_COLOR = new DeviceRgb(0, 0, 0);
    private static final DeviceRgb ORANGE_COLOR = new DeviceRgb(255, 152, 0);
    private static final DeviceRgb GRAY_COLOR = new DeviceRgb(128, 128, 128);

    public static PdfGenerationResult generateReport(Context context, List<TestResult> testResults,
                                                     long totalDuration) {
        try {
            // Create temporary PDF file in cache directory
            File cacheDir = context.getCacheDir();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "SensorTestReport_" + timestamp + ".pdf";
            File pdfFile = new File(cacheDir, fileName);

            Log.i(TAG, "Creating temporary PDF at: " + pdfFile.getAbsolutePath());

            // Create PDF document
            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Set up fonts
            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Add title
            Paragraph title = new Paragraph("Sensor Test Report")
                    .setFont(titleFont)
                    .setFontSize(24)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20)
                    .setFontColor(HEADER_COLOR);
            document.add(title);

            // Add generation info
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm:ss", Locale.getDefault());
            Paragraph generatedInfo = new Paragraph("Generated on " + dateFormat.format(new Date()))
                    .setFont(normalFont)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20)
                    .setFontColor(GRAY_COLOR);
            document.add(generatedInfo);

            // Add device information section
            addDeviceInformationSection(document, context, headerFont, normalFont);

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

            double successRate = totalSensors > 0 ? (workingSensors * 100.0 / totalSensors) : 0;

            // Add summary section
            addSummarySection(document, headerFont, normalFont, totalSensors, workingSensors,
                    failedSensors, successRate, totalDuration);

            // Add detailed results table
            addDetailedResultsTable(document, headerFont, normalFont, testResults);

            // Add footer
            addFooter(document, normalFont);

            document.close();

            Log.i(TAG, "PDF successfully generated at: " + pdfFile.getAbsolutePath());
            Log.i(TAG, "PDF file size: " + pdfFile.length() + " bytes");

            return new PdfGenerationResult(true, pdfFile.getAbsolutePath(), null);

        } catch (Exception e) {
            return new PdfGenerationResult(false, null, "Failed to generate PDF: " + e.getMessage());
        }
    }

    private static void addSummarySection(Document document, PdfFont headerFont, PdfFont normalFont,
                                          int totalSensors, int workingSensors, int failedSensors,
                                          double successRate, long totalDuration) throws IOException {

        // Summary header
        Paragraph summaryHeader = new Paragraph("Test Summary")
                .setFont(headerFont)
                .setFontSize(16)
                .setMarginTop(20)
                .setMarginBottom(15)
                .setFontColor(HEADER_COLOR);
        document.add(summaryHeader);

        // Summary table
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Header row
        summaryTable.addHeaderCell(createHeaderCell("Total Sensors", headerFont));
        summaryTable.addHeaderCell(createHeaderCell("Working", headerFont));
        summaryTable.addHeaderCell(createHeaderCell("Failed", headerFont));
        summaryTable.addHeaderCell(createHeaderCell("Success Rate", headerFont));

        // Data row
        summaryTable.addCell(createDataCell(String.valueOf(totalSensors), normalFont, BLACK_COLOR));
        summaryTable.addCell(createDataCell(String.valueOf(workingSensors), normalFont, SUCCESS_COLOR));
        summaryTable.addCell(createDataCell(String.valueOf(failedSensors), normalFont, ERROR_COLOR));
        summaryTable.addCell(createDataCell(String.format("%.1f%%", successRate), normalFont,
                successRate >= 80 ? SUCCESS_COLOR : (successRate >= 50 ? ORANGE_COLOR : ERROR_COLOR)));

        document.add(summaryTable);

        // Test duration
        Paragraph durationInfo = new Paragraph("Test Duration: " + formatDuration(totalDuration))
                .setFont(normalFont)
                .setFontSize(12)
                .setMarginBottom(20);
        document.add(durationInfo);
    }

    private static void addDetailedResultsTable(Document document, PdfFont headerFont, PdfFont normalFont,
                                                List<TestResult> testResults) throws IOException {

        // Detailed results header
        Paragraph detailsHeader = new Paragraph("Detailed Test Results")
                .setFont(headerFont)
                .setFontSize(16)
                .setMarginTop(20)
                .setMarginBottom(15)
                .setFontColor(HEADER_COLOR);
        document.add(detailsHeader);

        // Results table
        Table resultsTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1, 1, 3}))
                .setWidth(UnitValue.createPercentValue(100));

        // Header row
        resultsTable.addHeaderCell(createHeaderCell("Sensor Name", headerFont));
        resultsTable.addHeaderCell(createHeaderCell("Type", headerFont));
        resultsTable.addHeaderCell(createHeaderCell("Status", headerFont));
        resultsTable.addHeaderCell(createHeaderCell("Duration", headerFont));
        resultsTable.addHeaderCell(createHeaderCell("Details", headerFont));

        // Data rows
        for (int i = 0; i < testResults.size(); i++) {
            TestResult result = testResults.get(i);
            DeviceRgb rowColor = (i % 2 == 0) ? WHITE_COLOR : LIGHT_GRAY;

            resultsTable.addCell(createDataCell(result.getSensorName(), normalFont, BLACK_COLOR)
                    .setBackgroundColor(rowColor));
            resultsTable.addCell(createDataCell(result.getSensorTypeString(), normalFont, BLACK_COLOR)
                    .setBackgroundColor(rowColor));
            resultsTable.addCell(createDataCell(result.isWorking() ? "PASS" : "FAIL", normalFont,
                    result.isWorking() ? SUCCESS_COLOR : ERROR_COLOR)
                    .setBackgroundColor(rowColor));
            resultsTable.addCell(createDataCell(result.getFormattedDuration(), normalFont, BLACK_COLOR)
                    .setBackgroundColor(rowColor));

            String details;
            if (result.isWorking()) {
                details = "Sample: " + result.getSampleDataString() + "\nAccuracy: " + result.getAccuracyString();
            } else {
                details = "Error: " + (result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error");
            }

            resultsTable.addCell(createDataCell(details, normalFont, BLACK_COLOR)
                    .setBackgroundColor(rowColor));
        }

        document.add(resultsTable);
    }

    private static void addDeviceInformationSection(Document document, Context context, PdfFont headerFont,
                                                    PdfFont normalFont) throws IOException {
        // Device information header
        Paragraph deviceHeader = new Paragraph("Device Information")
                .setFont(headerFont)
                .setFontSize(16)
                .setMarginTop(10)
                .setMarginBottom(15)
                .setFontColor(HEADER_COLOR);
        document.add(deviceHeader);

        // Get display metrics
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }

        // Device information table
        Table deviceTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Add device information rows
        addDeviceInfoRow(deviceTable, "Device Model", Build.MODEL, headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "Manufacturer", Build.MANUFACTURER, headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "Brand", Build.BRAND, headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "Product", Build.PRODUCT, headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "Android Version", Build.VERSION.RELEASE, headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "API Level", String.valueOf(Build.VERSION.SDK_INT), headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "Build ID", Build.ID, headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "Hardware", Build.HARDWARE, headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "Board", Build.BOARD, headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "Bootloader", Build.BOOTLOADER, headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "Screen Resolution",
                displayMetrics.widthPixels + " x " + displayMetrics.heightPixels + " pixels", headerFont, normalFont);
        addDeviceInfoRow(deviceTable, "Screen Density",
                displayMetrics.densityDpi + " dpi (" + getDensityString(displayMetrics.densityDpi) + ")", headerFont,
                normalFont);
        addDeviceInfoRow(deviceTable, "Available Processors",
                String.valueOf(Runtime.getRuntime().availableProcessors()), headerFont, normalFont);

        document.add(deviceTable);
    }

    private static void addDeviceInfoRow(Table table, String label, String value, PdfFont headerFont,
                                         PdfFont normalFont) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(headerFont).setFontColor(HEADER_COLOR))
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(8));
        table.addCell(new Cell()
                .add(new Paragraph(value != null ? value : "Unknown").setFont(normalFont).setFontColor(BLACK_COLOR))
                .setPadding(8));
    }

    private static String getDensityString(int densityDpi) {
        if (densityDpi <= 120)
            return "ldpi";
        else if (densityDpi <= 160)
            return "mdpi";
        else if (densityDpi <= 240)
            return "hdpi";
        else if (densityDpi <= 320)
            return "xhdpi";
        else if (densityDpi <= 480)
            return "xxhdpi";
        else if (densityDpi <= 640)
            return "xxxhdpi";
        else
            return "ultra-high";
    }

    private static void addFooter(Document document, PdfFont normalFont) {
        Paragraph footer = new Paragraph("\n\nGenerated by Senson")
                .setFont(normalFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30)
                .setFontColor(GRAY_COLOR);
        document.add(footer);
    }

    private static Cell createHeaderCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontColor(WHITE_COLOR))
                .setBackgroundColor(HEADER_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(10);
    }

    private static Cell createDataCell(String text, PdfFont font, DeviceRgb textColor) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontColor(textColor))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    private static String formatDuration(long milliseconds) {
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

    public static class PdfGenerationResult {
        public final boolean success;
        public final String filePath;
        public final String errorMessage;

        public PdfGenerationResult(boolean success, String filePath, String errorMessage) {
            this.success = success;
            this.filePath = filePath;
            this.errorMessage = errorMessage;
        }
    }
}