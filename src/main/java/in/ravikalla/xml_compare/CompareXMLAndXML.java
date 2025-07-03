package in.ravikalla.xml_compare;
/*
 * Copyright (c) 1995, 2008, Ravi Kalla. All rights reserved.
 * Author : ravi2523096@gmail.com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Ravi Kalla or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import in.ravikalla.xml_compare.util.StreamingXMLComparator;
import in.ravikalla.xml_compare.util.ValidationUtil;

/**
 * 
 * 
 * Compare two XMLs that has elements in random order and write differences in and Excel file.
 * 
 * @author ravi2523096@gmail.com
 * @since 31-May-2016
 * 
 * Current Features:
 * =================
 * 1. Compare XMLs with elements in any order
 * 2. Tested on 6MB XML files.
 * 3. Ignore elements while comparing
 * 4. Trim elements while comparing
 * 5. Auto identification of first level of repeating elements
 * 
 * TODO - New Features:
 * ================
 * 1. Consider prefix for elements
 * 2. Consider attributes
 * 3. Consider a primary key for repeating elements
 *
 * Compile with below command:
 * ===========================
 * $ java -jar xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar <Input XML1 path> <Input XML1 path> <Output XLS path>
 * Eg:
 * ===
 * $ java -jar xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home/ravi/Desktop/Projects/xml-compare/src/main/resources/XML1.xml /home/ravi/Desktop/Projects/xml-compare/src/main/resources/XML2.xml /home/ravi/Desktop/Projects/xml-compare/src/main/resources/Results2.xls
 * 
 * 
 * Docker:
 * =======
 * $ docker build -f Dockerfile -t ravikalla/xmlcompare .
 * $ docker run -p 8084:8080 -v <local path>:/usr/src -t ravikalla/xml-compare /usr/src/<XML1 in local path> /usr/src/<XML2 in local path> /usr/src/<Results.xls in <XML1 in local path>
 * $ docker run -p 8084:8080 -v /home/ravi/Desktop/Projects/xml-compare/src/main/resources:/usr/src -t ravikalla/xml-compare /usr/src/XML1.xml /usr/src/XML2.xml /usr/src/Results.xls
 */

public class CompareXMLAndXML {
	private final static Logger logger = LogManager.getLogger(CompareXMLAndXML.class);

	public static void main(String[] args) {
		logger.debug("Start : CompareXMLAndXML.main(...)");
		String strXMLFileName1 = "XML1.xml";
		String strXMLFileName2 = "XML2.xml";
		String strExcludeElementsFileName = null;
		String strIterateElementsFileName = null;
		String strComparisonResultsFile = "Results.xls";
		String strTrimElements = null;

		try {
			ValidationUtil.validateArguments(args);
			if (null != args && args.length >= 3) {
				strXMLFileName1 = args[0];
				strXMLFileName2 = args[1];
				strComparisonResultsFile = args[2];
				
				ValidationUtil.validateInputFiles(strXMLFileName1, strXMLFileName2);
				ValidationUtil.validateOutputPath(strComparisonResultsFile);
			}
		} catch (IllegalArgumentException e) {
			logger.error("Invalid arguments: " + e.getMessage());
			System.err.println("Error: " + e.getMessage());
			System.exit(1);
		}
		try {
			testCompareXMLAndXMLWriteResults(strXMLFileName1, strXMLFileName2, strExcludeElementsFileName,
					strIterateElementsFileName, strComparisonResultsFile, strTrimElements);
		} catch (IOException e) {
			logger.error("31 : CompareXMLAndXML.main(...) : IOException e : " + e);
		}
		logger.debug("End : CompareXMLAndXML.main(...)");
	}

	public static boolean testCompareXMLAndXMLWriteResults(String strXMLFileName1, String strXMLFileName2,
			String strExcludeElementsFileName, String strIterateElementsFileName, String strComparisonResultsFile,
			String strTrimElements) throws IOException {
		logger.debug("Start : CompareXMLAndXML.testCompareXMLAndXMLWriteResults()" + strXMLFileName1 + " : " + strXMLFileName2);
		
		// Use StAX streaming approach consistently for all file sizes
		logger.info("Using StAX streaming comparison approach for all files.");
		try {
			return StreamingXMLComparator.compareXMLFilesStreaming(strXMLFileName1, strXMLFileName2, strComparisonResultsFile);
		} catch (Exception e) {
			logger.error("StAX streaming comparison failed: " + e.getMessage(), e);
			throw new IOException("XML comparison failed: " + e.getMessage(), e);
		}
	}
}
