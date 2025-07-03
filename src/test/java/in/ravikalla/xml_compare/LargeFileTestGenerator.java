package in.ravikalla.xml_compare;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Generates large XML test files for performance testing
 */
public class LargeFileTestGenerator {
    
    public static void main(String[] args) {
        System.out.println("=== Large XML File Generator ===");
        
        // Create test directory
        File testDir = new File("test-files");
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
        
        // Generate files of different sizes
        long[] targetSizes = {
            1024 * 1024,       // 1MB
            3 * 1024 * 1024,   // 3MB
            6 * 1024 * 1024,   // 6MB
            10 * 1024 * 1024   // 10MB
        };
        
        String[] sizeLabels = {"1MB", "3MB", "6MB", "10MB"};
        
        for (int i = 0; i < targetSizes.length; i++) {
            try {
                System.out.println("\nGenerating " + sizeLabels[i] + " test files...");
                
                // Generate identical files for baseline test
                String xml1Path = generateXMLFile(testDir, "test_" + sizeLabels[i] + "_identical_1.xml", targetSizes[i], false);
                String xml2Path = generateXMLFile(testDir, "test_" + sizeLabels[i] + "_identical_2.xml", targetSizes[i], false);
                
                // Generate different files for difference detection test
                String xml3Path = generateXMLFile(testDir, "test_" + sizeLabels[i] + "_different_1.xml", targetSizes[i], false);
                String xml4Path = generateXMLFile(testDir, "test_" + sizeLabels[i] + "_different_2.xml", targetSizes[i], true);
                
                File file1 = new File(xml1Path);
                File file3 = new File(xml3Path);
                
                System.out.println("✅ Generated " + sizeLabels[i] + " files:");
                System.out.println("   Identical files: " + formatBytes(file1.length()));
                System.out.println("   Different files: " + formatBytes(file3.length()));
                
            } catch (IOException e) {
                System.err.println("❌ Failed to generate " + sizeLabels[i] + " files: " + e.getMessage());
            }
        }
        
        System.out.println("\n✅ File generation completed!");
        System.out.println("Files are available in: " + testDir.getAbsolutePath());
    }
    
    private static String generateXMLFile(File dir, String filename, long targetSize, boolean makeDifferent) throws IOException {
        File file = new File(dir, filename);
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<TestData>\n");
            writer.write("  <Metadata>\n");
            writer.write("    <GeneratedAt>" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()) + "</GeneratedAt>\n");
            writer.write("    <TargetSize>" + formatBytes(targetSize) + "</TargetSize>\n");
            writer.write("    <Filename>" + filename + "</Filename>\n");
            writer.write("  </Metadata>\n");
            writer.write("  <Schools>\n");
            
            // Calculate how many school elements we need
            String schoolTemplate = generateSchoolTemplate(makeDifferent);
            int estimatedSchoolSize = schoolTemplate.length();
            int numSchools = (int) (targetSize * 0.8 / estimatedSchoolSize); // Use 80% of target size for content
            
            for (int i = 1; i <= numSchools; i++) {
                String schoolXml = schoolTemplate
                    .replace("{SCHOOL_ID}", String.valueOf(i))
                    .replace("{SCHOOL_NAME}", "School_" + i)
                    .replace("{LOCATION}", "Location_" + i)
                    .replace("{PRINCIPAL}", "Principal_" + i)
                    .replace("{PHONE}", "555-" + String.format("%04d", i % 10000))
                    .replace("{EMAIL}", "school" + i + "@example.com");
                
                writer.write(schoolXml);
                
                // Add progress indicator for large files
                if (i % 1000 == 0) {
                    long currentSize = file.length();
                    double progress = (double) currentSize / targetSize * 100;
                    System.out.print("\r  Progress: " + String.format("%.1f%%", progress) + " (" + formatBytes(currentSize) + ")");
                }
            }
            
            writer.write("  </Schools>\n");
            writer.write("</TestData>\n");
        }
        
        System.out.print("\r  Completed: " + formatBytes(file.length()) + "                    \n");
        return file.getAbsolutePath();
    }
    
    private static String generateSchoolTemplate(boolean makeDifferent) {
        String baseTemplate = "    <School id=\"{SCHOOL_ID}\">\n" +
                              "      <Name>{SCHOOL_NAME}</Name>\n" +
                              "      <Location>{LOCATION}</Location>\n" +
                              "      <Principal>{PRINCIPAL}</Principal>\n" +
                              "      <Contact>\n" +
                              "        <Phone>{PHONE}</Phone>\n" +
                              "        <Email>{EMAIL}</Email>\n" +
                              "      </Contact>\n" +
                              "      <Students>\n";
        
        // Add student data
        for (int i = 1; i <= 5; i++) {
            baseTemplate += "        <Student id=\"" + i + "\">\n" +
                           "          <FirstName>Student" + i + "First</FirstName>\n" +
                           "          <LastName>Student" + i + "Last</LastName>\n" +
                           "          <Grade>" + (9 + (i % 4)) + "</Grade>\n" +
                           "          <Age>" + (14 + (i % 4)) + "</Age>\n";
            
            if (makeDifferent && i == 1) {
                // Make the first student different to create differences
                baseTemplate += "          <Status>DIFFERENT</Status>\n";
            } else {
                baseTemplate += "          <Status>Active</Status>\n";
            }
            
            baseTemplate += "        </Student>\n";
        }
        
        baseTemplate += "      </Students>\n" +
                       "      <Classes>\n";
        
        // Add class data
        String[] subjects = {"Math", "Science", "English", "History", "Art"};
        for (String subject : subjects) {
            baseTemplate += "        <Class>\n" +
                           "          <Subject>" + subject + "</Subject>\n" +
                           "          <Teacher>Teacher_" + subject + "_{SCHOOL_ID}</Teacher>\n" +
                           "          <Room>Room_" + subject.charAt(0) + "{SCHOOL_ID}</Room>\n" +
                           "          <Schedule>Daily</Schedule>\n" +
                           "        </Class>\n";
        }
        
        baseTemplate += "      </Classes>\n" +
                       "    </School>\n";
        
        return baseTemplate;
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}