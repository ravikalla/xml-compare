package com.ravikalla.xmlcomparison.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComparisonResult {
    
    private boolean filesMatch;
    private String file1Path;
    private String file2Path;
    private long file1Size;
    private long file2Size;
    private String outputFormat;
    private String outputFilePath;
    private List<String> differences;
    private int differenceCount;
    private long comparisonDurationMs;
    private String comparisonTimestamp;
    private String status;
    private String errorMessage;
    
    public ComparisonResult() {
        this.comparisonTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.status = "SUCCESS";
    }
    
    public ComparisonResult(String errorMessage) {
        this();
        this.status = "ERROR";
        this.errorMessage = errorMessage;
        this.filesMatch = false;
    }

    // Getters and Setters
    public boolean isFilesMatch() {
        return filesMatch;
    }

    public void setFilesMatch(boolean filesMatch) {
        this.filesMatch = filesMatch;
    }

    public String getFile1Path() {
        return file1Path;
    }

    public void setFile1Path(String file1Path) {
        this.file1Path = file1Path;
    }

    public String getFile2Path() {
        return file2Path;
    }

    public void setFile2Path(String file2Path) {
        this.file2Path = file2Path;
    }

    public long getFile1Size() {
        return file1Size;
    }

    public void setFile1Size(long file1Size) {
        this.file1Size = file1Size;
    }

    public long getFile2Size() {
        return file2Size;
    }

    public void setFile2Size(long file2Size) {
        this.file2Size = file2Size;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public List<String> getDifferences() {
        return differences;
    }

    public void setDifferences(List<String> differences) {
        this.differences = differences;
        this.differenceCount = differences != null ? differences.size() : 0;
    }

    public int getDifferenceCount() {
        return differenceCount;
    }

    public void setDifferenceCount(int differenceCount) {
        this.differenceCount = differenceCount;
    }

    public long getComparisonDurationMs() {
        return comparisonDurationMs;
    }

    public void setComparisonDurationMs(long comparisonDurationMs) {
        this.comparisonDurationMs = comparisonDurationMs;
    }

    public String getComparisonTimestamp() {
        return comparisonTimestamp;
    }

    public void setComparisonTimestamp(String comparisonTimestamp) {
        this.comparisonTimestamp = comparisonTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "ComparisonResult{" +
                "filesMatch=" + filesMatch +
                ", file1Path='" + file1Path + '\'' +
                ", file2Path='" + file2Path + '\'' +
                ", outputFormat='" + outputFormat + '\'' +
                ", differenceCount=" + differenceCount +
                ", comparisonDurationMs=" + comparisonDurationMs +
                ", status='" + status + '\'' +
                '}';
    }
}