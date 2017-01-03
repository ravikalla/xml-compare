package in.ravikalla.xml_compare.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CommonUtil {
	public static String readDataFromFile(String strFileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(strFileName));
		try {
			StringBuilder sb = new StringBuilder();
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
}
