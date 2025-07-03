package in.ravikalla.xml_compare;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

/**
 * Performance tests to determine maximum file size capacity
 */
public class PerformanceTest {
    
    private Path tempDir;
    
    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("xml-performance-test");
    }
    
    @AfterEach  
    void tearDown() throws IOException {
        // Clean up temporary files
        Files.walk(tempDir)
            .map(Path::toFile)
            .forEach(File::delete);
    }
    
    @Test
    void testSmallFile() throws IOException {
        // Test with 1KB file
        testFileSize(1024, "1KB");
    }
    
    @Test
    void testMediumFile() throws IOException {
        // Test with 100KB file
        testFileSize(100 * 1024, "100KB");
    }
    
    @Test
    void testLargeFile() throws IOException {
        // Test with 1MB file
        testFileSize(1024 * 1024, "1MB");
    }
    
    @Test
    void testVeryLargeFile() throws IOException {
        // Test with 5MB file (documented as tested size)
        testFileSize(5 * 1024 * 1024, "5MB");
    }
    
    @Test
    void testExtraLargeFile() throws IOException {
        // Test with 10MB file to push limits
        testFileSize(10 * 1024 * 1024, "10MB");
    }
    
    private void testFileSize(long targetSize, String description) throws IOException {
        System.out.println("\n=== Testing " + description + " file ===");
        
        // Monitor memory before test
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Memory before: " + formatBytes(memoryBefore));
        
        // Generate test XML files
        Path xml1Path = generateXMLFile("test1.xml", targetSize, "TestData1");
        Path xml2Path = generateXMLFile("test2.xml", targetSize, "TestData2");
        Path resultPath = tempDir.resolve("results.xls");
        
        long actualSize1 = Files.size(xml1Path);
        long actualSize2 = Files.size(xml2Path);
        System.out.println("Generated files: " + formatBytes(actualSize1) + " and " + formatBytes(actualSize2));
        
        // Time the comparison
        long startTime = System.currentTimeMillis();
        
        try {
            boolean result = CompareXMLAndXML.testCompareXMLAndXMLWriteResults(
                xml1Path.toString(),
                xml2Path.toString(), 
                null, // excludeElements
                null, // iterateElements
                resultPath.toString(),
                null  // trimElements
            );
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Monitor memory after test
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = memoryAfter - memoryBefore;
            
            // Check result file was created
            assertTrue(Files.exists(resultPath), "Result file should be created");
            long resultSize = Files.size(resultPath);
            
            // Print performance metrics
            System.out.println("Comparison result: " + (result ? "IDENTICAL" : "DIFFERENCES FOUND"));
            System.out.println("Processing time: " + duration + "ms");
            System.out.println("Memory used: " + formatBytes(memoryUsed));
            System.out.println("Memory after: " + formatBytes(memoryAfter));
            System.out.println("Result file size: " + formatBytes(resultSize));
            System.out.println("Memory efficiency: " + String.format("%.2fx", (double)memoryUsed / actualSize1));
            
            // Success if we reach here without OutOfMemoryError
            System.out.println("✅ " + description + " test PASSED");
            
        } catch (OutOfMemoryError e) {
            System.out.println("❌ " + description + " test FAILED - Out of Memory");
            System.out.println("Available memory: " + formatBytes(runtime.maxMemory()));
            throw new AssertionError("Out of memory for " + description + " file", e);
        } catch (Exception e) {
            System.out.println("❌ " + description + " test FAILED - " + e.getMessage());
            throw e;
        }
    }
    
    private Path generateXMLFile(String filename, long targetSize, String baseData) throws IOException {
        Path filePath = tempDir.resolve(filename);
        
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<root>\n");
            
            // Calculate how many elements we need to reach target size
            String elementTemplate = "  <item id=\"%d\">%s_Item_%d</item>\n";
            String sampleElement = String.format(elementTemplate, 1, baseData, 1);
            int elementSize = sampleElement.length();
            int numElements = (int) (targetSize / elementSize);
            
            // Generate elements to reach approximately target size
            for (int i = 1; i <= numElements; i++) {
                writer.write(String.format(elementTemplate, i, baseData, i));
                
                // Add some nested structure for complexity
                if (i % 10 == 0) {
                    writer.write("  <complex id=\"" + i + "\">\n");
                    writer.write("    <nested>" + baseData + "_Nested_" + i + "</nested>\n");
                    writer.write("    <deep>\n");
                    writer.write("      <value>" + baseData + "_Deep_" + i + "</value>\n");
                    writer.write("    </deep>\n");
                    writer.write("  </complex>\n");
                }
            }
            
            writer.write("</root>\n");
        }
        
        return filePath;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}