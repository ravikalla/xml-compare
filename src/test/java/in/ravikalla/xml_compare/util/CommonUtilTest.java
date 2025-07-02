package in.ravikalla.xml_compare.util;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CommonUtilTest extends TestCase {
    private final static Logger logger = LogManager.getLogger(CommonUtilTest.class);
    
    public CommonUtilTest(String testName) {
        super(testName);
    }
    
    public static Test suite() {
        return new TestSuite(CommonUtilTest.class);
    }
    
    public void testReadDataFromValidFile() throws IOException {
        String filePath = "src/main/resources/XML1.xml";
        String content = CommonUtil.readDataFromFile(filePath);
        
        assertNotNull("Content should not be null", content);
        assertTrue("Content should contain XML declaration", content.contains("<?xml"));
        assertTrue("Content should contain Schools element", content.contains("<Schools>"));
    }
    
    public void testReadDataFromNonExistentFile() {
        try {
            CommonUtil.readDataFromFile("non_existent_file.xml");
            fail("Should throw IOException for non-existent file");
        } catch (IOException e) {
            assertTrue("Should throw IOException", true);
        }
    }
    
    public void testReadDataFromNullFilePath() {
        try {
            CommonUtil.readDataFromFile(null);
            fail("Should throw exception for null file path");
        } catch (Exception e) {
            assertTrue("Should throw exception for null file path", true);
        }
    }
    
    public void testReadDataFromEmptyFilePath() {
        try {
            CommonUtil.readDataFromFile("");
            fail("Should throw exception for empty file path");
        } catch (Exception e) {
            assertTrue("Should throw exception for empty file path", true);
        }
    }
}