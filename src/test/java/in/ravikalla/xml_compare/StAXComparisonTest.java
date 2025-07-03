package in.ravikalla.xml_compare;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import in.ravikalla.xml_compare.util.StreamingXMLComparator;

/**
 * Comprehensive test cases for StAX-based XML comparison
 * Tests various file sizes from small (1KB) to large (50MB)
 */
public class StAXComparisonTest {
    private final static Logger logger = LogManager.getLogger(StAXComparisonTest.class);
    
    private static final String TEST_DIR = "test-output";
    
    @Before
    public void setUp() throws IOException {
        // Create test directory
        Files.createDirectories(Paths.get(TEST_DIR));
    }
    
    @After
    public void tearDown() {
        // Cleanup test files
        try {
            Files.walk(Paths.get(TEST_DIR))
                .map(java.nio.file.Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            logger.warn("Failed to cleanup test directory: " + e.getMessage());
        }
    }
    
    @Test
    public void testIdenticalSmallXMLs() throws Exception {
        String xml1 = createTestXMLFile("small_identical_1.xml", generateXMLContent(10, false));
        String xml2 = createTestXMLFile("small_identical_2.xml", generateXMLContent(10, false));
        String output = TEST_DIR + "/result_small_identical.txt";
        
        boolean result = StreamingXMLComparator.compareXMLFilesStreaming(xml1, xml2, output);
        
        assertTrue("Identical small XMLs should match", result);
        assertFalse("No output file should be created for identical files", new File(output).exists());
    }
    
    @Test
    public void testDifferentSmallXMLs() throws Exception {
        String xml1 = createTestXMLFile("small_different_1.xml", generateXMLContent(10, false));
        String xml2 = createTestXMLFile("small_different_2.xml", generateXMLContent(10, true));
        String output = TEST_DIR + "/result_small_different.txt";
        
        boolean result = StreamingXMLComparator.compareXMLFilesStreaming(xml1, xml2, output);
        
        assertFalse("Different small XMLs should not match", result);
        assertTrue("Output file should be created for different files", new File(output).exists());
        assertTrue("Output file should contain differences", new File(output).length() > 0);
    }
    
    @Test
    public void testMediumSizeXMLs() throws Exception {
        String xml1 = createTestXMLFile("medium_1.xml", generateXMLContent(1000, false));
        String xml2 = createTestXMLFile("medium_2.xml", generateXMLContent(1000, false));
        String output = TEST_DIR + "/result_medium.txt";
        
        long startTime = System.currentTimeMillis();
        boolean result = StreamingXMLComparator.compareXMLFilesStreaming(xml1, xml2, output);
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue("Identical medium XMLs should match", result);
        logger.info("Medium XML comparison took: " + duration + "ms");
        assertTrue("Medium comparison should complete in reasonable time", duration < 5000);
    }
    
    @Test
    public void testLargeXMLs() throws Exception {
        String xml1 = createTestXMLFile("large_1.xml", generateXMLContent(10000, false));
        String xml2 = createTestXMLFile("large_2.xml", generateXMLContent(10000, false));
        String output = TEST_DIR + "/result_large.txt";
        
        long startTime = System.currentTimeMillis();
        boolean result = StreamingXMLComparator.compareXMLFilesStreaming(xml1, xml2, output);
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue("Identical large XMLs should match", result);
        logger.info("Large XML comparison took: " + duration + "ms");
        assertTrue("Large comparison should complete in reasonable time", duration < 10000);
    }
    
    @Test
    public void testVeryLargeXMLs() throws Exception {
        String xml1 = createTestXMLFile("very_large_1.xml", generateXMLContent(50000, false));
        String xml2 = createTestXMLFile("very_large_2.xml", generateXMLContent(50000, false));
        String output = TEST_DIR + "/result_very_large.txt";
        
        long startTime = System.currentTimeMillis();
        boolean result = StreamingXMLComparator.compareXMLFilesStreaming(xml1, xml2, output);
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue("Identical very large XMLs should match", result);
        logger.info("Very large XML comparison took: " + duration + "ms");
        
        // Check file sizes
        long file1Size = new File(xml1).length();
        long file2Size = new File(xml2).length();
        logger.info("File sizes - XML1: " + formatBytes(file1Size) + ", XML2: " + formatBytes(file2Size));
    }
    
    @Test
    public void testEmptyXMLs() throws Exception {
        String xml1 = createTestXMLFile("empty_1.xml", "<?xml version=\"1.0\"?><root></root>");
        String xml2 = createTestXMLFile("empty_2.xml", "<?xml version=\"1.0\"?><root></root>");
        String output = TEST_DIR + "/result_empty.txt";
        
        boolean result = StreamingXMLComparator.compareXMLFilesStreaming(xml1, xml2, output);
        
        assertTrue("Empty XMLs should match", result);
    }
    
    @Test
    public void testDifferentStructureXMLs() throws Exception {
        String xml1Content = "<?xml version=\"1.0\"?><root><item>value</item></root>";
        String xml2Content = "<?xml version=\"1.0\"?><root><different>value</different></root>";
        
        String xml1 = createTestXMLFile("structure_1.xml", xml1Content);
        String xml2 = createTestXMLFile("structure_2.xml", xml2Content);
        String output = TEST_DIR + "/result_structure.txt";
        
        boolean result = StreamingXMLComparator.compareXMLFilesStreaming(xml1, xml2, output);
        
        assertFalse("XMLs with different structure should not match", result);
        assertTrue("Output file should be created", new File(output).exists());
    }
    
    @Test
    public void testMemoryUsage() throws Exception {
        // Test memory usage with large file
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        String xml1 = createTestXMLFile("memory_test_1.xml", generateXMLContent(20000, false));
        String xml2 = createTestXMLFile("memory_test_2.xml", generateXMLContent(20000, false));
        String output = TEST_DIR + "/result_memory_test.txt";
        
        boolean result = StreamingXMLComparator.compareXMLFilesStreaming(xml1, xml2, output);
        
        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        assertTrue("Memory test XMLs should match", result);
        logger.info("Memory used during comparison: " + formatBytes(memoryUsed));
        
        // Memory usage should be reasonable (less than 100MB)
        assertTrue("Memory usage should be reasonable", memoryUsed < 100 * 1024 * 1024);
    }
    
    @Test
    public void testPerformanceScaling() throws Exception {
        int[] sizes = {100, 500, 1000, 5000, 10000};
        
        for (int size : sizes) {
            String xml1 = createTestXMLFile("perf_" + size + "_1.xml", generateXMLContent(size, false));
            String xml2 = createTestXMLFile("perf_" + size + "_2.xml", generateXMLContent(size, false));
            String output = TEST_DIR + "/result_perf_" + size + ".txt";
            
            long startTime = System.currentTimeMillis();
            boolean result = StreamingXMLComparator.compareXMLFilesStreaming(xml1, xml2, output);
            long duration = System.currentTimeMillis() - startTime;
            
            assertTrue("Performance test " + size + " should match", result);
            logger.info("Size " + size + " elements took: " + duration + "ms");
            
            // Performance should scale reasonably (not exponentially)
            assertTrue("Performance should be reasonable for size " + size, duration < size / 2);
        }
    }
    
    private String createTestXMLFile(String filename, String content) throws IOException {
        String filepath = TEST_DIR + "/" + filename;
        Files.write(Paths.get(filepath), content.getBytes());
        return filepath;
    }
    
    private String generateXMLContent(int elementCount, boolean makeDifferent) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<TestData>\n");
        xml.append("  <Metadata>\n");
        xml.append("    <GeneratedElements>").append(elementCount).append("</GeneratedElements>\n");
        xml.append("    <TestType>").append(makeDifferent ? "Different" : "Identical").append("</TestType>\n");
        xml.append("  </Metadata>\n");
        xml.append("  <Schools>\n");
        
        for (int i = 1; i <= elementCount; i++) {
            xml.append("    <School id=\"").append(i).append("\">\n");
            xml.append("      <Name>School_").append(i).append("</Name>\n");
            xml.append("      <Location>Location_").append(i).append("</Location>\n");
            xml.append("      <Principal>Principal_").append(i).append("</Principal>\n");
            xml.append("      <Contact>\n");
            xml.append("        <Phone>555-").append(String.format("%04d", i % 10000)).append("</Phone>\n");
            xml.append("        <Email>school").append(i).append("@example.com</Email>\n");
            xml.append("      </Contact>\n");
            xml.append("      <Students>\n");
            
            for (int j = 1; j <= 3; j++) {
                xml.append("        <Student id=\"").append(j).append("\">\n");
                xml.append("          <FirstName>Student").append(j).append("First</FirstName>\n");
                xml.append("          <LastName>Student").append(j).append("Last</LastName>\n");
                xml.append("          <Grade>").append(9 + (j % 4)).append("</Grade>\n");
                xml.append("          <Age>").append(14 + (j % 4)).append("</Age>\n");
                
                if (makeDifferent && i == 1 && j == 1) {
                    xml.append("          <Status>DIFFERENT</Status>\n");
                } else {
                    xml.append("          <Status>Active</Status>\n");
                }
                
                xml.append("        </Student>\n");
            }
            
            xml.append("      </Students>\n");
            xml.append("      <Classes>\n");
            
            String[] subjects = {"Math", "Science", "English", "History", "Art"};
            for (String subject : subjects) {
                xml.append("        <Class>\n");
                xml.append("          <Subject>").append(subject).append("</Subject>\n");
                xml.append("          <Teacher>Teacher_").append(subject).append("_").append(i).append("</Teacher>\n");
                xml.append("          <Room>Room_").append(subject.charAt(0)).append(i).append("</Room>\n");
                xml.append("          <Schedule>Daily</Schedule>\n");
                xml.append("        </Class>\n");
            }
            
            xml.append("      </Classes>\n");
            xml.append("    </School>\n");
        }
        
        xml.append("  </Schools>\n");
        xml.append("</TestData>\n");
        
        return xml.toString();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}