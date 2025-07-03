package com.ravikalla.xmlcomparison.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streaming XML comparator for large XML files that cannot fit in memory.
 * Uses StAX (Streaming API for XML) to process XML files incrementally.
 */
public class StreamingXMLComparator {
    private final static Logger logger = LoggerFactory.getLogger(StreamingXMLComparator.class);
    
    /**
     * Compare two XML files using StAX streaming approach
     * @param xmlFile1 Path to first XML file
     * @param xmlFile2 Path to second XML file
     * @param outputFile Path to output results file (optional, can be null)
     * @return ComparisonResultData containing comparison results
     * @throws IOException if file reading fails
     * @throws XMLStreamException if XML parsing fails
     */
    public static ComparisonResultData compareXMLFilesStreaming(String xmlFile1, String xmlFile2, String outputFile) 
            throws IOException, XMLStreamException {
        logger.info("Starting StAX streaming comparison of XML files: {} and {}", xmlFile1, xmlFile2);
        
        long startTime = System.currentTimeMillis();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        List<String> differences = new ArrayList<>();
        
        try (FileInputStream fis1 = new FileInputStream(xmlFile1);
             FileInputStream fis2 = new FileInputStream(xmlFile2)) {
            
            XMLStreamReader reader1 = factory.createXMLStreamReader(fis1);
            XMLStreamReader reader2 = factory.createXMLStreamReader(fis2);
            
            boolean filesMatch = true;
            int elementCount = 0;
            
            while (reader1.hasNext() && reader2.hasNext()) {
                int event1 = reader1.next();
                int event2 = reader2.next();
                
                if (event1 != event2) {
                    differences.add("Event type mismatch at element " + elementCount + 
                                  ": " + getEventTypeString(event1) + " vs " + getEventTypeString(event2));
                    filesMatch = false;
                    break;
                }
                
                switch (event1) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (!compareStartElements(reader1, reader2, elementCount, differences)) {
                            filesMatch = false;
                        }
                        elementCount++;
                        break;
                        
                    case XMLStreamConstants.CHARACTERS:
                        if (!compareCharacters(reader1, reader2, elementCount, differences)) {
                            filesMatch = false;
                        }
                        break;
                        
                    case XMLStreamConstants.END_ELEMENT:
                        if (!compareEndElements(reader1, reader2, elementCount, differences)) {
                            filesMatch = false;
                        }
                        break;
                }
                
                // Limit memory usage by processing differences in batches
                if (differences.size() > 1000) {
                    logger.warn("Too many differences found (>1000). Stopping comparison to prevent memory issues.");
                    filesMatch = false;
                    break;
                }
            }
            
            // Check if one file has more content than the other
            if (reader1.hasNext() || reader2.hasNext()) {
                differences.add("Files have different lengths");
                filesMatch = false;
            }
            
            reader1.close();
            reader2.close();
            
            // Write results to output file if specified
            if (outputFile != null && !differences.isEmpty()) {
                writeStreamingResults(outputFile, differences);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Streaming comparison completed. Files match: {}, Differences found: {}, Duration: {}ms", 
                       filesMatch, differences.size(), duration);
            
            return new ComparisonResultData(filesMatch, differences, duration, elementCount);
            
        } catch (OutOfMemoryError e) {
            logger.error("Out of memory during streaming comparison. Consider increasing heap size.", e);
            throw new IOException("Out of memory during XML comparison", e);
        }
    }
    
    private static boolean compareStartElements(XMLStreamReader reader1, XMLStreamReader reader2, 
                                              int elementCount, List<String> differences) {
        String name1 = reader1.getLocalName();
        String name2 = reader2.getLocalName();
        
        if (!name1.equals(name2)) {
            differences.add("Element name mismatch at position " + elementCount + 
                          ": '" + name1 + "' vs '" + name2 + "'");
            return false;
        }
        
        // Compare attributes
        int attrCount1 = reader1.getAttributeCount();
        int attrCount2 = reader2.getAttributeCount();
        
        if (attrCount1 != attrCount2) {
            differences.add("Attribute count mismatch for element '" + name1 + 
                          "' at position " + elementCount + 
                          ": " + attrCount1 + " vs " + attrCount2);
            return false;
        }
        
        return true;
    }
    
    private static boolean compareCharacters(XMLStreamReader reader1, XMLStreamReader reader2, 
                                           int elementCount, List<String> differences) {
        String text1 = reader1.getText().trim();
        String text2 = reader2.getText().trim();
        
        if (!text1.equals(text2) && !text1.isEmpty() && !text2.isEmpty()) {
            differences.add("Text content mismatch at element " + elementCount + 
                          ": '" + text1 + "' vs '" + text2 + "'");
            return false;
        }
        
        return true;
    }
    
    private static boolean compareEndElements(XMLStreamReader reader1, XMLStreamReader reader2, 
                                            int elementCount, List<String> differences) {
        String name1 = reader1.getLocalName();
        String name2 = reader2.getLocalName();
        
        if (!name1.equals(name2)) {
            differences.add("End element name mismatch at position " + elementCount + 
                          ": '" + name1 + "' vs '" + name2 + "'");
            return false;
        }
        
        return true;
    }
    
    private static String getEventTypeString(int eventType) {
        switch (eventType) {
            case XMLStreamConstants.START_ELEMENT: return "START_ELEMENT";
            case XMLStreamConstants.END_ELEMENT: return "END_ELEMENT";
            case XMLStreamConstants.CHARACTERS: return "CHARACTERS";
            case XMLStreamConstants.START_DOCUMENT: return "START_DOCUMENT";
            case XMLStreamConstants.END_DOCUMENT: return "END_DOCUMENT";
            default: return "UNKNOWN(" + eventType + ")";
        }
    }
    
    private static void writeStreamingResults(String outputFile, List<String> differences) 
            throws IOException {
        logger.info("Writing streaming comparison results to: {}", outputFile);
        
        try (java.io.FileWriter writer = new java.io.FileWriter(outputFile)) {
            writer.write("Streaming XML Comparison Results\n");
            writer.write("================================\n\n");
            writer.write("Total differences found: " + differences.size() + "\n\n");
            
            for (int i = 0; i < differences.size(); i++) {
                writer.write((i + 1) + ". " + differences.get(i) + "\n");
            }
        }
    }
    
    /**
     * Data class to hold comparison results
     */
    public static class ComparisonResultData {
        public final boolean filesMatch;
        public final List<String> differences;
        public final long durationMs;
        public final int elementCount;
        
        public ComparisonResultData(boolean filesMatch, List<String> differences, long durationMs, int elementCount) {
            this.filesMatch = filesMatch;
            this.differences = differences;
            this.durationMs = durationMs;
            this.elementCount = elementCount;
        }
    }
}