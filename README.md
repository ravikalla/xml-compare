[![Issues](https://img.shields.io/github/issues/ravikalla/xml-compare.svg?style=flat-square)](https://github.com/ravikalla/xml-compare/issues)
[![Forks](https://img.shields.io/github/forks/ravikalla/xml-compare.svg?style=flat-square)](https://github.com/ravikalla/xml-compare/network)
[![Stars](https://img.shields.io/github/stars/ravikalla/xml-compare.svg?style=flat-square)](https://github.com/ravikalla/xml-compare/stargazers)
[![Docker Stars](https://img.shields.io/docker/stars/ravikalla/xml-compare.svg)](https://hub.docker.com/r/ravikalla/xml-compare/)
[![Docker Pull](https://img.shields.io/docker/pulls/ravikalla/xml-compare.svg)](https://hub.docker.com/r/ravikalla/xml-compare/)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-blue.svg)](https://maven.apache.org/)

# Compare two XMLs and write differences to an Excel

## Features

- **Fast XML comparison** regardless of element order
- **Excel output** with detailed differences
- **Comprehensive input validation** and error handling
- **Modern dependencies** with security updates (Log4j 2.x, Apache POI 5.x, JUnit 5.x)
- **Docker support** for containerized execution
- **Tested on large files** (6MB+ XML files)

## Recent Updates

- ✅ **Security fixed**: Upgraded Log4j to 2.21.1 (fixes Log4Shell vulnerabilities)
- ✅ **Dependencies updated**: Apache POI 5.2.5, JUnit 5.10.1
- ✅ **Code modernized**: Better error handling, input validation, refactored architecture
- ✅ **Test coverage**: Comprehensive unit tests added

### Run Java code in local

    java -jar xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar <Input XML1 path> <Input XML2 path> <Output XLS path>
 Eg:

    java -jar xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home/ravi/Desktop/Projects/xml-compare/src/main/resources/XML1.xml /home/ravi/Desktop/Projects/xml-compare/src/main/resources/XML2.xml /home/ravi/Desktop/Projects/xml-compare/src/main/resources/Results2.xls

### Run as Docker image
    docker run -p 8084:8080 -v <local path>:/usr/src -t ravikalla/xml-compare /usr/src/<XML1 filename> /usr/src/<XML2 filename> /usr/src/<Results.xls filename>
 Eg:

    docker run -p 8084:8080 -v /home/ravi/Desktop/Projects/xml-compare/src/main/resources:/usr/src -t ravikalla/xml-compare /usr/src/XML1.xml /usr/src/XML2.xml /usr/src/Results.xls

## Usage Notes

- **Input validation**: The tool validates file paths and accessibility before processing
- **Error handling**: Clear error messages for common issues (file not found, permission errors, etc.)
- **Output format**: Results are generated in Excel (.xls) format with separate sheets for matches and mismatches

### Input files:
![https://github.com/ravikalla/screenshots/blob/master/XMLs.png](https://github.com/ravikalla/screenshots/blob/master/XMLs.png)
### Result:
![https://github.com/ravikalla/screenshots/blob/master/ComparisonResults.png](https://github.com/ravikalla/screenshots/blob/master/ComparisonResults.png)

Javadoc in Github pages:
[Javadoc](https://ravikalla.github.io/xml-compare)
