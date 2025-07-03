package com.ravikalla.xmlcomparison.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class XmlFileInfo {
    
    private String filePath;
    private long fileSizeBytes;
    private String fileSizeHuman;
    private boolean isValidXml;
    private String rootElement;
    private int elementCount;
    private int depth;
    private String encoding;
    private String version;
    private String errorMessage;
    
    public XmlFileInfo() {}
    
    public XmlFileInfo(String filePath) {
        this.filePath = filePath;
    }
    
    public XmlFileInfo(String filePath, String errorMessage) {
        this.filePath = filePath;
        this.errorMessage = errorMessage;
        this.isValidXml = false;
    }

    // Getters and Setters
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
        this.fileSizeHuman = formatBytes(fileSizeBytes);
    }

    public String getFileSizeHuman() {
        return fileSizeHuman;
    }

    public void setFileSizeHuman(String fileSizeHuman) {
        this.fileSizeHuman = fileSizeHuman;
    }

    public boolean isValidXml() {
        return isValidXml;
    }

    public void setValidXml(boolean validXml) {
        isValidXml = validXml;
    }

    public String getRootElement() {
        return rootElement;
    }

    public void setRootElement(String rootElement) {
        this.rootElement = rootElement;
    }

    public int getElementCount() {
        return elementCount;
    }

    public void setElementCount(int elementCount) {
        this.elementCount = elementCount;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    @Override
    public String toString() {
        return "XmlFileInfo{" +
                "filePath='" + filePath + '\'' +
                ", fileSizeHuman='" + fileSizeHuman + '\'' +
                ", isValidXml=" + isValidXml +
                ", rootElement='" + rootElement + '\'' +
                ", elementCount=" + elementCount +
                ", depth=" + depth +
                '}';
    }
}