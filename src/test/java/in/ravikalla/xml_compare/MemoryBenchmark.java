package in.ravikalla.xml_compare;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Memory benchmark utility to find maximum file size capacity
 */
public class MemoryBenchmark {
    
    public static void main(String[] args) {
        System.out.println("=== XML Comparison Memory Benchmark ===");
        
        // Print JVM memory configuration
        Runtime runtime = Runtime.getRuntime();
        System.out.println("JVM Max Memory: " + formatBytes(runtime.maxMemory()));
        System.out.println("JVM Total Memory: " + formatBytes(runtime.totalMemory()));
        System.out.println("JVM Free Memory: " + formatBytes(runtime.freeMemory()));
        System.out.println();
        
        // Test different file sizes
        long[] testSizes = {
            1024,              // 1KB
            10 * 1024,         // 10KB  
            100 * 1024,        // 100KB
            1024 * 1024,       // 1MB
            5 * 1024 * 1024,   // 5MB (documented limit)
            10 * 1024 * 1024,  // 10MB
            20 * 1024 * 1024,  // 20MB
            50 * 1024 * 1024   // 50MB (if memory allows)
        };
        
        for (long size : testSizes) {
            try {
                testFileSize(size);
            } catch (OutOfMemoryError e) {
                System.out.println("❌ MEMORY LIMIT REACHED at " + formatBytes(size));
                System.out.println("Maximum practical file size: " + formatBytes(size / 2));
                break;
            } catch (Exception e) {
                System.out.println("❌ ERROR at " + formatBytes(size) + ": " + e.getMessage());
                break;
            }
        }
    }
    
    private static void testFileSize(long targetSize) throws IOException {
        System.out.println("\n--- Testing " + formatBytes(targetSize) + " ---");
        
        // Create temporary directory
        Path tempDir = Files.createTempDirectory("benchmark");
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // Memory before
            runtime.gc();
            try {
                Thread.sleep(100); // Allow GC to complete
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            
            // Generate test files
            long startTime = System.currentTimeMillis();
            Path xml1 = generateTestXML(tempDir, "test1.xml", targetSize);
            Path xml2 = generateTestXML(tempDir, "test2.xml", targetSize);
            Path result = tempDir.resolve("result.xls");
            
            long fileGenTime = System.currentTimeMillis() - startTime;
            
            // Test comparison
            startTime = System.currentTimeMillis();
            
            boolean comparisonResult = CompareXMLAndXML.testCompareXMLAndXMLWriteResults(
                xml1.toString(),
                xml2.toString(),
                null,
                null, 
                result.toString(),
                null
            );
            
            long comparisonTime = System.currentTimeMillis() - startTime;
            
            // Memory after
            long memAfter = runtime.totalMemory() - runtime.freeMemory();
            long memUsed = memAfter - memBefore;
            
            // Results
            long actualSize = Files.size(xml1);
            long resultSize = Files.exists(result) ? Files.size(result) : 0;
            
            System.out.println("File generation: " + fileGenTime + "ms");
            System.out.println("Comparison time: " + comparisonTime + "ms");
            System.out.println("Actual file size: " + formatBytes(actualSize));
            System.out.println("Memory used: " + formatBytes(memUsed));
            System.out.println("Memory multiplier: " + String.format("%.1fx", (double)memUsed / actualSize));
            System.out.println("Result file size: " + formatBytes(resultSize));
            System.out.println("Comparison result: " + (comparisonResult ? "IDENTICAL" : "DIFFERENCES"));
            System.out.println("✅ SUCCESS");
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw e;
        } finally {
            // Cleanup
            try {
                Files.walk(tempDir)
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }
    
    private static Path generateTestXML(Path dir, String filename, long targetSize) throws IOException {
        Path filePath = dir.resolve(filename);
        
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<Schools>\n");
            
            // Calculate elements needed
            String template = "  <School id=\"%d\">\n" +
                             "    <SchoolName>School_%d</SchoolName>\n" +
                             "    <Location>Location_%d</Location>\n" +
                             "    <Students>\n" +
                             "      <Student id=\"1\">Student_%d_1</Student>\n" +
                             "      <Student id=\"2\">Student_%d_2</Student>\n" +
                             "    </Students>\n" +
                             "  </School>\n";
            
            String sample = String.format(template, 1, 1, 1, 1, 1);
            int elementSize = sample.length();
            int numElements = (int) (targetSize / elementSize);
            
            for (int i = 1; i <= numElements; i++) {
                writer.write(String.format(template, i, i, i, i, i));
            }
            
            writer.write("</Schools>\n");
        }
        
        return filePath;
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}