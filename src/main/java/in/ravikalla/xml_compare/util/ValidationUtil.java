package in.ravikalla.xml_compare.util;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValidationUtil {
    private static final Logger logger = LogManager.getLogger(ValidationUtil.class);

    private ValidationUtil() {
        // Utility class - private constructor
    }

    public static void validateInputFiles(String xml1Path, String xml2Path) {
        logger.debug("Start : ValidationUtil.validateInputFiles(...)");
        
        if (xml1Path == null || xml1Path.trim().isEmpty()) {
            throw new IllegalArgumentException("First XML file path cannot be null or empty");
        }
        
        if (xml2Path == null || xml2Path.trim().isEmpty()) {
            throw new IllegalArgumentException("Second XML file path cannot be null or empty");
        }
        
        File xml1File = new File(xml1Path);
        if (!xml1File.exists()) {
            throw new IllegalArgumentException("First XML file does not exist: " + xml1Path);
        }
        
        if (!xml1File.isFile()) {
            throw new IllegalArgumentException("First XML path is not a file: " + xml1Path);
        }
        
        if (!xml1File.canRead()) {
            throw new IllegalArgumentException("Cannot read first XML file: " + xml1Path);
        }
        
        File xml2File = new File(xml2Path);
        if (!xml2File.exists()) {
            throw new IllegalArgumentException("Second XML file does not exist: " + xml2Path);
        }
        
        if (!xml2File.isFile()) {
            throw new IllegalArgumentException("Second XML path is not a file: " + xml2Path);
        }
        
        if (!xml2File.canRead()) {
            throw new IllegalArgumentException("Cannot read second XML file: " + xml2Path);
        }
        
        logger.debug("End : ValidationUtil.validateInputFiles(...) - validation passed");
    }

    public static void validateOutputPath(String outputPath) {
        logger.debug("Start : ValidationUtil.validateOutputPath(...)");
        
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output file path cannot be null or empty");
        }
        
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        
        if (parentDir != null && !parentDir.exists()) {
            throw new IllegalArgumentException("Output directory does not exist: " + parentDir.getAbsolutePath());
        }
        
        if (parentDir != null && !parentDir.canWrite()) {
            throw new IllegalArgumentException("Cannot write to output directory: " + parentDir.getAbsolutePath());
        }
        
        logger.debug("End : ValidationUtil.validateOutputPath(...) - validation passed");
    }

    public static void validateArguments(String[] args) {
        logger.debug("Start : ValidationUtil.validateArguments(...)");
        
        if (args == null) {
            throw new IllegalArgumentException("Arguments array cannot be null");
        }
        
        if (args.length < 3) {
            throw new IllegalArgumentException(
                "Usage: java -jar xml-compare.jar <XML1_PATH> <XML2_PATH> <OUTPUT_PATH>\n" +
                "  XML1_PATH: Path to first XML file\n" +
                "  XML2_PATH: Path to second XML file\n" +
                "  OUTPUT_PATH: Path for output Excel file"
            );
        }
        
        logger.debug("End : ValidationUtil.validateArguments(...) - validation passed");
    }
}