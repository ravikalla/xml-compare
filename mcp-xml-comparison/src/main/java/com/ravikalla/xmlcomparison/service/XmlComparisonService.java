package com.ravikalla.xmlcomparison.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravikalla.xmlcomparison.model.ComparisonResult;
import com.ravikalla.xmlcomparison.model.XmlFileInfo;
import com.ravikalla.xmlcomparison.util.StreamingXMLComparator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class XmlComparisonService {

    private static final Logger logger = LoggerFactory.getLogger(XmlComparisonService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(name = "compare_xml_files", description = "Compare two XML files using StAX streaming parser and return results in specified format (text, json, excel)")
    public String compareXmlFiles(String file1Path, String file2Path, String outputFormat) {
        return compareXmlFilesWithCustomPath(file1Path, file2Path, outputFormat, null);
    }

    @Tool(name = "compare_xml_files_custom_path", description = "Compare two XML files using StAX streaming parser and return results in specified format with custom output file path")
    public String compareXmlFilesWithCustomPath(String file1Path, String file2Path, String outputFormat, String customOutputPath) {
        logger.info("Comparing XML files: {} vs {} with output format: {}", file1Path, file2Path, outputFormat);
        
        try {
            // Validate input parameters
            if (!isValidOutputFormat(outputFormat)) {
                return createErrorResponse("Invalid output format. Supported formats: text, json, excel");
            }
            
            if (!Files.exists(Paths.get(file1Path))) {
                return createErrorResponse("File 1 does not exist: " + file1Path);
            }
            
            if (!Files.exists(Paths.get(file2Path))) {
                return createErrorResponse("File 2 does not exist: " + file2Path);
            }
            
            // Perform comparison
            String tempOutputFile = customOutputPath != null ? customOutputPath : generateTempOutputPath();
            StreamingXMLComparator.ComparisonResultData resultData = 
                StreamingXMLComparator.compareXMLFilesStreaming(file1Path, file2Path, tempOutputFile);
            
            // Create comparison result
            ComparisonResult result = createComparisonResult(file1Path, file2Path, resultData, outputFormat);
            
            // Generate output based on format
            String outputPath = generateOutput(result, outputFormat, customOutputPath);
            result.setOutputFilePath(outputPath);
            
            // Return formatted response
            return formatResponse(result, outputFormat);
            
        } catch (Exception e) {
            logger.error("Error comparing XML files: {}", e.getMessage(), e);
            return createErrorResponse("Comparison failed: " + e.getMessage());
        }
    }

    @Tool(name = "validate_xml_file", description = "Validate if a file is a well-formed XML file")
    public String validateXmlFile(String filePath) {
        logger.info("Validating XML file: {}", filePath);
        
        try {
            if (!Files.exists(Paths.get(filePath))) {
                return createJsonResponse(new XmlFileInfo(filePath, "File does not exist"));
            }
            
            XmlFileInfo info = analyzeXmlFile(filePath);
            return createJsonResponse(info);
            
        } catch (Exception e) {
            logger.error("Error validating XML file: {}", e.getMessage(), e);
            return createJsonResponse(new XmlFileInfo(filePath, "Validation failed: " + e.getMessage()));
        }
    }

    @Tool(name = "get_xml_file_info", description = "Get basic information about an XML file (size, element count, structure)")
    public String getXmlFileInfo(String filePath) {
        logger.info("Getting XML file info: {}", filePath);
        
        try {
            if (!Files.exists(Paths.get(filePath))) {
                return createJsonResponse(new XmlFileInfo(filePath, "File does not exist"));
            }
            
            XmlFileInfo info = analyzeXmlFile(filePath);
            return createJsonResponse(info);
            
        } catch (Exception e) {
            logger.error("Error getting XML file info: {}", e.getMessage(), e);
            return createJsonResponse(new XmlFileInfo(filePath, "Analysis failed: " + e.getMessage()));
        }
    }

    private boolean isValidOutputFormat(String format) {
        return format != null && 
               (format.equalsIgnoreCase("text") || 
                format.equalsIgnoreCase("json") || 
                format.equalsIgnoreCase("excel"));
    }

    private String generateTempOutputPath() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "xml_comparison_" + timestamp + ".txt";
    }

    private ComparisonResult createComparisonResult(String file1Path, String file2Path, 
                                                  StreamingXMLComparator.ComparisonResultData resultData, 
                                                  String outputFormat) throws IOException {
        ComparisonResult result = new ComparisonResult();
        result.setFile1Path(file1Path);
        result.setFile2Path(file2Path);
        result.setFile1Size(Files.size(Paths.get(file1Path)));
        result.setFile2Size(Files.size(Paths.get(file2Path)));
        result.setFilesMatch(resultData.filesMatch);
        result.setDifferences(resultData.differences);
        result.setComparisonDurationMs(resultData.durationMs);
        result.setOutputFormat(outputFormat);
        
        return result;
    }

    private String generateOutput(ComparisonResult result, String format, String customOutputPath) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputPath;
        
        if (customOutputPath != null) {
            outputPath = customOutputPath;
        } else {
            switch (format.toLowerCase()) {
                case "json":
                    outputPath = "xml_comparison_" + timestamp + ".json";
                    break;
                case "excel":
                    outputPath = "xml_comparison_" + timestamp + ".xlsx";
                    break;
                default:
                    outputPath = "xml_comparison_" + timestamp + ".txt";
            }
        }
        
        switch (format.toLowerCase()) {
            case "json":
                generateJsonOutput(result, outputPath);
                break;
            case "excel":
                generateExcelOutput(result, outputPath);
                break;
            default:
                generateTextOutput(result, outputPath);
        }
        
        return outputPath;
    }

    private void generateTextOutput(ComparisonResult result, String outputPath) throws IOException {
        try (var writer = Files.newBufferedWriter(Paths.get(outputPath))) {
            writer.write("XML Comparison Report\n");
            writer.write("====================\n\n");
            writer.write("Comparison Timestamp: " + result.getComparisonTimestamp() + "\n");
            writer.write("File 1: " + result.getFile1Path() + " (" + formatBytes(result.getFile1Size()) + ")\n");
            writer.write("File 2: " + result.getFile2Path() + " (" + formatBytes(result.getFile2Size()) + ")\n");
            writer.write("Files Match: " + (result.isFilesMatch() ? "YES" : "NO") + "\n");
            writer.write("Comparison Duration: " + result.getComparisonDurationMs() + "ms\n");
            writer.write("Total Differences: " + result.getDifferenceCount() + "\n\n");
            
            if (result.getDifferences() != null && !result.getDifferences().isEmpty()) {
                writer.write("Differences Found:\n");
                writer.write("-----------------\n");
                for (int i = 0; i < result.getDifferences().size(); i++) {
                    writer.write((i + 1) + ". " + result.getDifferences().get(i) + "\n");
                }
            }
        }
    }

    private void generateJsonOutput(ComparisonResult result, String outputPath) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), result);
    }

    private void generateExcelOutput(ComparisonResult result, String outputPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("XML Comparison Results");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Create summary section
            int rowNum = 0;
            Row summaryHeaderRow = sheet.createRow(rowNum++);
            summaryHeaderRow.createCell(0).setCellValue("XML Comparison Summary");
            summaryHeaderRow.getCell(0).setCellStyle(headerStyle);
            
            sheet.createRow(rowNum++).createCell(0).setCellValue("Comparison Timestamp: " + result.getComparisonTimestamp());
            sheet.createRow(rowNum++).createCell(0).setCellValue("File 1: " + result.getFile1Path());
            sheet.createRow(rowNum++).createCell(0).setCellValue("File 1 Size: " + formatBytes(result.getFile1Size()));
            sheet.createRow(rowNum++).createCell(0).setCellValue("File 2: " + result.getFile2Path());
            sheet.createRow(rowNum++).createCell(0).setCellValue("File 2 Size: " + formatBytes(result.getFile2Size()));
            sheet.createRow(rowNum++).createCell(0).setCellValue("Files Match: " + (result.isFilesMatch() ? "YES" : "NO"));
            sheet.createRow(rowNum++).createCell(0).setCellValue("Duration (ms): " + result.getComparisonDurationMs());
            sheet.createRow(rowNum++).createCell(0).setCellValue("Total Differences: " + result.getDifferenceCount());
            
            // Add differences section if any
            if (result.getDifferences() != null && !result.getDifferences().isEmpty()) {
                rowNum++; // Empty row
                Row differencesHeaderRow = sheet.createRow(rowNum++);
                differencesHeaderRow.createCell(0).setCellValue("Differences");
                differencesHeaderRow.getCell(0).setCellStyle(headerStyle);
                
                Row headerRow = sheet.createRow(rowNum++);
                headerRow.createCell(0).setCellValue("#");
                headerRow.createCell(1).setCellValue("Description");
                headerRow.getCell(0).setCellStyle(headerStyle);
                headerRow.getCell(1).setCellStyle(headerStyle);
                
                for (int i = 0; i < result.getDifferences().size(); i++) {
                    Row dataRow = sheet.createRow(rowNum++);
                    dataRow.createCell(0).setCellValue(i + 1);
                    dataRow.createCell(1).setCellValue(result.getDifferences().get(i));
                }
            }
            
            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }
        }
    }

    private XmlFileInfo analyzeXmlFile(String filePath) {
        XmlFileInfo info = new XmlFileInfo(filePath);
        
        try {
            File file = new File(filePath);
            info.setFileSizeBytes(file.length());
            
            XMLInputFactory factory = XMLInputFactory.newInstance();
            try (FileInputStream fis = new FileInputStream(file)) {
                XMLStreamReader reader = factory.createXMLStreamReader(fis);
                
                int elementCount = 0;
                int maxDepth = 0;
                int currentDepth = 0;
                boolean foundRoot = false;
                
                while (reader.hasNext()) {
                    int event = reader.next();
                    
                    switch (event) {
                        case XMLStreamConstants.START_DOCUMENT:
                            String encoding = reader.getCharacterEncodingScheme();
                            String version = reader.getVersion();
                            info.setEncoding(encoding != null ? encoding : "UTF-8");
                            info.setVersion(version != null ? version : "1.0");
                            break;
                            
                        case XMLStreamConstants.START_ELEMENT:
                            elementCount++;
                            currentDepth++;
                            maxDepth = Math.max(maxDepth, currentDepth);
                            
                            if (!foundRoot) {
                                info.setRootElement(reader.getLocalName());
                                foundRoot = true;
                            }
                            break;
                            
                        case XMLStreamConstants.END_ELEMENT:
                            currentDepth--;
                            break;
                    }
                }
                
                info.setElementCount(elementCount);
                info.setDepth(maxDepth);
                info.setValidXml(true);
                
                reader.close();
            }
            
        } catch (Exception e) {
            info.setValidXml(false);
            info.setErrorMessage("XML parsing failed: " + e.getMessage());
        }
        
        return info;
    }

    private String formatResponse(ComparisonResult result, String format) {
        switch (format.toLowerCase()) {
            case "json":
                return createJsonResponse(result);
            case "excel":
                return "Excel report generated successfully at: " + result.getOutputFilePath() + 
                       "\nFiles match: " + (result.isFilesMatch() ? "YES" : "NO") + 
                       "\nDifferences found: " + result.getDifferenceCount() + 
                       "\nDuration: " + result.getComparisonDurationMs() + "ms";
            default:
                return "Text report generated successfully at: " + result.getOutputFilePath() + 
                       "\nFiles match: " + (result.isFilesMatch() ? "YES" : "NO") + 
                       "\nDifferences found: " + result.getDifferenceCount() + 
                       "\nDuration: " + result.getComparisonDurationMs() + "ms";
        }
    }

    private String createErrorResponse(String errorMessage) {
        ComparisonResult errorResult = new ComparisonResult(errorMessage);
        return createJsonResponse(errorResult);
    }

    private String createJsonResponse(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (Exception e) {
            logger.error("Error creating JSON response: {}", e.getMessage());
            return "{\"error\": \"Failed to create JSON response: " + e.getMessage() + "\"}";
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}