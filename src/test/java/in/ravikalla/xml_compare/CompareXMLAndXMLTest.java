package in.ravikalla.xml_compare;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CompareXMLAndXMLTest extends TestCase {
    private final static Logger logger = LogManager.getLogger(CompareXMLAndXMLTest.class);
    
    public CompareXMLAndXMLTest(String testName) {
        super(testName);
    }
    
    public static Test suite() {
        return new TestSuite(CompareXMLAndXMLTest.class);
    }
    
    public void testCompareIdenticalXMLs() throws IOException {
        String xml1 = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        String xml2 = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        
        String tempFile1 = createTempXMLFile(xml1, "identical1.xml");
        String tempFile2 = createTempXMLFile(xml2, "identical2.xml");
        String resultFile = getTempFilePath("identical_result.xls");
        
        try {
            boolean result = CompareXMLAndXML.testCompareXMLAndXMLWriteResults(
                tempFile1, tempFile2, null, null, resultFile, null);
            assertTrue("Identical XMLs should return true", result);
            assertTrue("Result file should be created", new File(resultFile).exists());
        } finally {
            cleanupTempFiles(tempFile1, tempFile2, resultFile);
        }
    }
    
    public void testCompareDifferentXMLs() throws IOException {
        String xml1 = "<?xml version=\"1.0\"?><root><item>test1</item></root>";
        String xml2 = "<?xml version=\"1.0\"?><root><item>test2</item></root>";
        
        String tempFile1 = createTempXMLFile(xml1, "different1.xml");
        String tempFile2 = createTempXMLFile(xml2, "different2.xml");
        String resultFile = getTempFilePath("different_result.xls");
        
        try {
            boolean result = CompareXMLAndXML.testCompareXMLAndXMLWriteResults(
                tempFile1, tempFile2, null, null, resultFile, null);
            assertFalse("Different XMLs should return false", result);
            assertTrue("Result file should be created", new File(resultFile).exists());
        } finally {
            cleanupTempFiles(tempFile1, tempFile2, resultFile);
        }
    }
    
    public void testCompareXMLsWithReorderedElements() throws IOException {
        String xml1 = "<?xml version=\"1.0\"?><Schools><School><SchoolName>School1</SchoolName><Students><Student>Student1</Student></Students></School></Schools>";
        String xml2 = "<?xml version=\"1.0\"?><Schools><School><Students><Student>Student1</Student></Students><SchoolName>School1</SchoolName></School></Schools>";
        
        String tempFile1 = createTempXMLFile(xml1, "reordered1.xml");
        String tempFile2 = createTempXMLFile(xml2, "reordered2.xml");
        String resultFile = getTempFilePath("reordered_result.xls");
        
        try {
            boolean result = CompareXMLAndXML.testCompareXMLAndXMLWriteResults(
                tempFile1, tempFile2, null, null, resultFile, null);
            assertTrue("XMLs with reordered elements should be considered identical", result);
        } finally {
            cleanupTempFiles(tempFile1, tempFile2, resultFile);
        }
    }
    
    public void testCompareWithProvidedXMLs() throws IOException {
        String xml1Path = "src/main/resources/XML1.xml";
        String xml2Path = "src/main/resources/XML2.xml";
        String resultFile = getTempFilePath("provided_result.xls");
        
        try {
            boolean result = CompareXMLAndXML.testCompareXMLAndXMLWriteResults(
                xml1Path, xml2Path, null, null, resultFile, null);
            assertFalse("Provided test XMLs should have differences", result);
            assertTrue("Result file should be created", new File(resultFile).exists());
        } finally {
            cleanupTempFiles(resultFile);
        }
    }
    
    public void testCompareEmptyXMLs() throws IOException {
        String xml1 = "<?xml version=\"1.0\"?><root></root>";
        String xml2 = "<?xml version=\"1.0\"?><root></root>";
        
        String tempFile1 = createTempXMLFile(xml1, "empty1.xml");
        String tempFile2 = createTempXMLFile(xml2, "empty2.xml");
        String resultFile = getTempFilePath("empty_result.xls");
        
        try {
            boolean result = CompareXMLAndXML.testCompareXMLAndXMLWriteResults(
                tempFile1, tempFile2, null, null, resultFile, null);
            assertTrue("Empty XMLs should be considered identical", result);
        } finally {
            cleanupTempFiles(tempFile1, tempFile2, resultFile);
        }
    }
    
    public void testCompareWithNonExistentFile() {
        String nonExistentFile = "non_existent_file.xml";
        String validFile = "src/main/resources/XML1.xml";
        String resultFile = getTempFilePath("error_result.xls");
        
        try {
            CompareXMLAndXML.testCompareXMLAndXMLWriteResults(
                nonExistentFile, validFile, null, null, resultFile, null);
            fail("Should throw IOException for non-existent file");
        } catch (IOException e) {
            assertTrue("Should throw IOException", true);
        } finally {
            cleanupTempFiles(resultFile);
        }
    }
    
    private String createTempXMLFile(String xmlContent, String fileName) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String filePath = tempDir + File.separator + fileName;
        Files.write(Paths.get(filePath), xmlContent.getBytes());
        return filePath;
    }
    
    private String getTempFilePath(String fileName) {
        String tempDir = System.getProperty("java.io.tmpdir");
        return tempDir + File.separator + fileName;
    }
    
    private void cleanupTempFiles(String... filePaths) {
        for (String filePath : filePaths) {
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
                File paramsFile = new File(filePath + "_Params");
                if (paramsFile.exists()) {
                    paramsFile.delete();
                }
            } catch (Exception e) {
                logger.warn("Failed to cleanup temp file: " + filePath, e);
            }
        }
    }
}