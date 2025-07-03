package in.ravikalla.xml_compare.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CommonUtil {
	// Maximum file size in bytes (50MB)
	private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
	
	public static String readDataFromFile(String strFileName) throws IOException {
		File file = new File(strFileName);
		long fileSize = file.length();
		
		if (fileSize > MAX_FILE_SIZE) {
			throw new IOException("File size (" + fileSize + " bytes) exceeds maximum allowed size (" + MAX_FILE_SIZE + " bytes). Use streaming approach for large files.");
		}
		
		BufferedReader br = new BufferedReader(new FileReader(strFileName));
		try {
			StringBuilder sb = new StringBuilder((int) fileSize);
			String line = br.readLine();
			while (null != line) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}
	
	public static long getFileSizeInBytes(String strFileName) throws IOException {
		return Files.size(Paths.get(strFileName));
	}
	
	public static boolean isLargeFile(String strFileName) throws IOException {
		return getFileSizeInBytes(strFileName) > MAX_FILE_SIZE;
	}
}
