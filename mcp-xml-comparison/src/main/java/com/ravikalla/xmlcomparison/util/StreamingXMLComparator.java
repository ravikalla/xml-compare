package com.ravikalla.xmlcomparison.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * Compare two XML files using canonical (order-agnostic) comparison
<<<<<<< HEAD
     * Based on the existing XMLDataConverter logic but using StAX for memory efficiency
=======
     * For large files, we'll use a simplified approach that normalizes content
>>>>>>> mcp-semantic-comparison
     */
    public static boolean compareXMLFilesCanonical(String xmlFile1, String xmlFile2) 
            throws IOException, XMLStreamException {
        logger.info("Starting StAX canonical comparison of XML files: {} and {}", xmlFile1, xmlFile2);
        
<<<<<<< HEAD
        // Extract normalized element structures from both files
        List<ElementStructure> elements1 = extractElementStructures(xmlFile1);
        List<ElementStructure> elements2 = extractElementStructures(xmlFile2);
        
        // Perform order-agnostic comparison
        return compareElementStructuresCanonically(elements1, elements2);
    }
    
    private static List<ElementStructure> extractElementStructures(String xmlFile) 
            throws IOException, XMLStreamException {
        List<ElementStructure> elements = new ArrayList<>();
=======
        // For large files, use a hash-based approach to avoid memory issues
        String normalizedContent1 = extractNormalizedContent(xmlFile1);
        String normalizedContent2 = extractNormalizedContent(xmlFile2);
        
        boolean result = normalizedContent1.equals(normalizedContent2);
        logger.info("Canonical comparison completed. Files match: {}", result);
        return result;
    }
    
    /**
     * Extract normalized content from XML file for semantic comparison
     * This approach sorts element content to make comparison order-agnostic
     */
    private static String extractNormalizedContent(String xmlFile) throws IOException, XMLStreamException {
        Map<String, List<String>> elementsByName = new HashMap<>();
>>>>>>> mcp-semantic-comparison
        XMLInputFactory factory = XMLInputFactory.newInstance();
        
        try (FileInputStream fis = new FileInputStream(xmlFile)) {
            XMLStreamReader reader = factory.createXMLStreamReader(fis);
            
            while (reader.hasNext()) {
                int event = reader.next();
                
                if (event == XMLStreamConstants.START_ELEMENT) {
<<<<<<< HEAD
                    ElementStructure element = parseElement(reader);
                    if (element != null) {
                        elements.add(element);
                    }
=======
                    String elementName = reader.getLocalName();
                    String elementContent = extractElementContent(reader, elementName);
                    
                    elementsByName.computeIfAbsent(elementName, k -> new ArrayList<>()).add(elementContent);
>>>>>>> mcp-semantic-comparison
                }
            }
            reader.close();
        }
        
<<<<<<< HEAD
        return elements;
=======
        // Sort elements by name and their content for consistent comparison
        StringBuilder normalized = new StringBuilder();
        elementsByName.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                entry.getValue().sort(String::compareTo);
                normalized.append(entry.getKey()).append(":");
                entry.getValue().forEach(content -> normalized.append(content).append("|"));
                normalized.append(";");
            });
        
        return normalized.toString();
    }
    
    private static String extractElementContent(XMLStreamReader reader, String elementName) throws XMLStreamException {
        StringBuilder content = new StringBuilder();
        content.append(elementName).append("(");
        
        // Add attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            content.append(reader.getAttributeLocalName(i)).append("=")
                   .append(reader.getAttributeValue(i)).append(",");
        }
        content.append(")");
        
        // Add text content
        while (reader.hasNext()) {
            int event = reader.next();
            
            if (event == XMLStreamConstants.CHARACTERS) {
                String text = reader.getText().trim();
                if (!text.isEmpty()) {
                    content.append(text);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && 
                      reader.getLocalName().equals(elementName)) {
                break;
            }
        }
        
        return content.toString();
>>>>>>> mcp-semantic-comparison
    }
    
    private static ElementStructure parseElement(XMLStreamReader reader) throws XMLStreamException {
        String name = reader.getLocalName();
        Map<String, String> attributes = new HashMap<>();
        List<ElementStructure> children = new ArrayList<>();
        StringBuilder textContent = new StringBuilder();
        
        // Read attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
        }
        
        // Read element content
        while (reader.hasNext()) {
            int event = reader.next();
            
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    ElementStructure child = parseElement(reader);
                    if (child != null) {
                        children.add(child);
                    }
                    break;
                    
                case XMLStreamConstants.CHARACTERS:
                case XMLStreamConstants.CDATA:
                    String text = reader.getText().trim();
                    if (!text.isEmpty()) {
                        textContent.append(text);
                    }
                    break;
                    
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getLocalName().equals(name)) {
                        return new ElementStructure(name, attributes, children, textContent.toString().trim());
                    }
                    break;
            }
        }
        
        return new ElementStructure(name, attributes, children, textContent.toString().trim());
    }
    
    private static boolean compareElementStructuresCanonically(List<ElementStructure> elements1, 
                                                              List<ElementStructure> elements2) {
        if (elements1.size() != elements2.size()) {
            return false;
        }
        
        // Track which elements in list2 have been matched
        List<Integer> matchedPositions = new ArrayList<>();
        
        // For each element in list1, find a matching element in list2
        for (ElementStructure element1 : elements1) {
            boolean foundMatch = false;
            
            for (int i = 0; i < elements2.size(); i++) {
                if (matchedPositions.contains(i)) {
                    continue; // Already matched
                }
                
                if (elementsEqualCanonically(element1, elements2.get(i))) {
                    matchedPositions.add(i);
                    foundMatch = true;
                    break;
                }
            }
            
            if (!foundMatch) {
                return false;
            }
        }
        
        return matchedPositions.size() == elements1.size();
    }
    
    private static boolean elementsEqualCanonically(ElementStructure elem1, ElementStructure elem2) {
        // Compare element names
        if (!Objects.equals(elem1.name, elem2.name)) {
            return false;
        }
        
        // Compare attributes
        if (!Objects.equals(elem1.attributes, elem2.attributes)) {
            return false;
        }
        
        // Compare text content
        if (!Objects.equals(elem1.textContent, elem2.textContent)) {
            return false;
        }
        
        // Compare children canonically (order-agnostic)
        return compareElementStructuresCanonically(elem1.children, elem2.children);
    }
    
    /**
     * Lightweight element structure for canonical comparison
     */
    private static class ElementStructure {
        final String name;
        final Map<String, String> attributes;
        final List<ElementStructure> children;
        final String textContent;
        
        ElementStructure(String name, Map<String, String> attributes, 
                        List<ElementStructure> children, String textContent) {
            this.name = name;
            this.attributes = new HashMap<>(attributes);
            this.children = new ArrayList<>(children);
            this.textContent = textContent;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            ElementStructure that = (ElementStructure) obj;
            return Objects.equals(name, that.name) &&
                   Objects.equals(attributes, that.attributes) &&
                   Objects.equals(textContent, that.textContent) &&
                   compareElementStructuresCanonically(children, that.children);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, attributes, textContent);
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