package in.ravikalla.xml_compare.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class XmlProcessor {
    private static final Logger logger = LogManager.getLogger(XmlProcessor.class);

    private XmlProcessor() {
        // Utility class - private constructor
    }

    public static String replaceEscapeCharacters(String xmlContent) {
        logger.debug("Start : XmlProcessor.replaceEscapeCharacters(...)");
        
        if (xmlContent == null) {
            logger.debug("End : XmlProcessor.replaceEscapeCharacters(...) - null input");
            return null;
        }

        String processedXml = xmlContent
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("<\\?.*?\\?>", "");
        
        logger.debug("End : XmlProcessor.replaceEscapeCharacters(...)");
        return processedXml;
    }
}