# xml-compare
 * docker run -p 8084:8080 -v <local path>:/usr/src -t ravikalla/xmlcompare /usr/src/&lt;XML1 in local path> /usr/src/&lt;XML2 in local path> /usr/src/<Results.xls in &lt;XML1 in local path>
 <br/>
 <br/>
 Eg:
 * docker run -p 8084:8080 -v /home/ravi/Desktop/Projects/xml-compare/src/main/resources:/usr/src -t ravikalla/xmlcompare /usr/src/XML1.xml /usr/src/XML2.xml /usr/src/Results.xls
