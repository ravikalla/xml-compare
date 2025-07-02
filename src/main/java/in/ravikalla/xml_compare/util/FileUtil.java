package in.ravikalla.xml_compare.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileUtil {
    private static final Logger logger = LogManager.getLogger(FileUtil.class);

    private FileUtil() {
        // Utility class - private constructor
    }

    public static List<String> readTextFileToList(String fileName) {
        logger.debug("Start : FileUtil.readTextFileToList(...)");
        
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.debug("End : FileUtil.readTextFileToList(...) - empty filename");
            return new ArrayList<>();
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException in readTextFileToList: " + e.getMessage());
            throw new RuntimeException("File not found: " + fileName, e);
        } catch (IOException e) {
            logger.error("IOException in readTextFileToList: " + e.getMessage());
            throw new RuntimeException("Error reading file: " + fileName, e);
        }
        
        logger.debug("End : FileUtil.readTextFileToList(...)");
        return lines;
    }

    public static void writeParametersToFile(String fileName, String content) {
        logger.debug("Start : FileUtil.writeParametersToFile(...)");
        
        try (PrintWriter out = new PrintWriter(fileName)) {
            out.println(content);
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException in writeParametersToFile: " + e.getMessage());
            throw new RuntimeException("Cannot create file: " + fileName, e);
        }
        
        logger.debug("End : FileUtil.writeParametersToFile(...)");
    }
}