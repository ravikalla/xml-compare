[![Codacy Badge](https://api.codacy.com/project/badge/Grade/fb2e5b1e69484d3c979351671a5f7225)](https://www.codacy.com/app/ravikalla/xml-compare?utm_source=github.com&utm_medium=referral&utm_content=ravikalla/xml-compare&utm_campaign=badger)
[![Build Status](https://travis-ci.org/ravikalla/xml-compare.svg?branch=master)](https://travis-ci.org/ravikalla/xml-compare)
# Compare two XMLs and write differences to an Excel

### Run Java code in local
 * java -jar xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar &lt;Input XML1 path> &lt;Input XML1 path> &lt;Output XLS path>
 <br/>
 <br/>
 Eg:
 * java -jar xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home/ravi/Desktop/Projects/xml-compare/src/main/resources/XML1.xml /home/ravi/Desktop/Projects/xml-compare/src/main/resources/XML2.xml /home/ravi/Desktop/Projects/xml-compare/src/main/resources/Results2.xls

### Run as Docker image
 * docker run -p 8084:8080 -v <local path>:/usr/src -t ravikalla/xml-compare /usr/src/&lt;XML1 in local path> /usr/src/&lt;XML2 in local path> /usr/src/&lt;Results.xls in XML1 in local path>
 <br/>
 <br/>
 Eg:
 * docker run -p 8084:8080 -v /home/ravi/Desktop/Projects/xml-compare/src/main/resources:/usr/src -t ravikalla/xml-compare /usr/src/XML1.xml /usr/src/XML2.xml /usr/src/Results.xls

### Input files:
![https://github.com/ravikalla/screenshots/blob/master/XMLs.png](https://github.com/ravikalla/screenshots/blob/master/XMLs.png)
### Result:
![https://github.com/ravikalla/screenshots/blob/master/ComparisonResults.png](https://github.com/ravikalla/screenshots/blob/master/ComparisonResults.png)
