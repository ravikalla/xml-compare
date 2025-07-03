package com.ravikalla.xmlcomparison;

import com.ravikalla.xmlcomparison.service.XmlComparisonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("web") // Use web profile for testing
class XmlComparisonMcpApplicationTest {

    @Autowired
    private XmlComparisonService xmlComparisonService;

    @Test
    void contextLoads() {
        assertNotNull(xmlComparisonService);
    }

    @Test
    void validateXmlFileWithNonExistentFile() {
        String result = xmlComparisonService.validateXmlFile("/non/existent/file.xml");
        assertNotNull(result);
        assertTrue(result.contains("File does not exist"));
    }

    @Test
    void getXmlFileInfoWithNonExistentFile() {
        String result = xmlComparisonService.getXmlFileInfo("/non/existent/file.xml");
        assertNotNull(result);
        assertTrue(result.contains("File does not exist"));
    }

}