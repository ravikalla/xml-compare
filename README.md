[![Codacy Badge](https://api.codacy.com/project/badge/Grade/fb2e5b1e69484d3c979351671a5f7225)](https://www.codacy.com/app/ravikalla/xml-compare?utm_source=github.com&utm_medium=referral&utm_content=ravikalla/xml-compare&utm_campaign=badger)
[![Build Status](https://travis-ci.org/ravikalla/xml-compare.svg?branch=main)](https://travis-ci.org/ravikalla/xml-compare)
[![Issue Count](https://codeclimate.com/github/ravikalla/xml-compare/badges/issue_count.svg)](https://codeclimate.com/github/ravikalla/xml-compare)
[![Issues](https://img.shields.io/github/issues/ravikalla/xml-compare.svg?style=flat-square)](https://github.com/ravikalla/xml-compare/issues)
[![Docker Stars](https://img.shields.io/docker/stars/ravikalla/xml-compare.svg)](https://hub.docker.com/r/ravikalla/xml-compare/)
[![Docker Pull](https://img.shields.io/docker/pulls/ravikalla/xml-compare.svg)](https://hub.docker.com/r/ravikalla/xml-compare/)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Join the chat at https://gitter.im/capitalone/Hygieia](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/XML-Compare/Lobby?utm_source=share-link&utm_medium=link&utm_campaign=share-link)

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
