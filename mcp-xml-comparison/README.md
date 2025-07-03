# XML Comparison MCP Server

A Model Context Protocol (MCP) server that provides XML file comparison functionality using StAX streaming parser. This server can be integrated with Claude Desktop to enable natural language XML file comparison with multiple output formats.

## Features

- 🚀 **High Performance**: StAX streaming parser for memory-efficient processing of large XML files (tested with 60MB+ files)
- 🔄 **Semantic Comparison**: Order-agnostic comparison that ignores element ordering for true semantic analysis
- 📊 **Multiple Output Formats**: Support for text, JSON, and Excel output formats
- ✅ **XML Validation**: Built-in XML file validation and detailed analysis
- 🛡️ **Error Handling**: Comprehensive error handling with detailed error messages
- 🎯 **String-based Processing**: All comparisons treat data as strings (no complex type conversions)
- 🔧 **MCP Integration**: Seamless integration with Claude Desktop

## Tools Available

### 1. `compare_xml_files`
Compare two XML files using sequential (order-sensitive) comparison.

**Parameters:**
- `file1Path` (required): Path to first XML file
- `file2Path` (required): Path to second XML file  
- `outputFormat` (required): Output format - "text", "json", or "excel"

**Example Usage:**
```
Compare the XML files /path/to/file1.xml and /path/to/file2.xml in JSON format
```

### 2. `compare_xml_files_semantic`
Compare two XML files using semantic (order-agnostic) comparison that ignores element ordering.

**Parameters:**
- `file1Path` (required): Path to first XML file
- `file2Path` (required): Path to second XML file  
- `outputFormat` (required): Output format - "text", "json", or "excel"

**Example Usage:**
```
Compare these XML files semantically (ignoring element order) in JSON format:
- /path/to/file1.xml and /path/to/file2.xml
```

### 3. `compare_xml_files_custom_path`
Compare XML files with a custom output file path.

**Parameters:**
- `file1Path` (required): Path to first XML file
- `file2Path` (required): Path to second XML file  
- `outputFormat` (required): Output format - "text", "json", or "excel"
- `customOutputPath` (required): Custom path for output file

**Example Usage:**
```
Compare XML files and save the report to a specific location:
- file1Path: /path/to/file1.xml
- file2Path: /path/to/file2.xml
- outputFormat: text
- customOutputPath: /path/to/my_report.txt
```

### 4. `validate_xml_file`
Validate if a file is well-formed XML.

**Parameters:**
- `filePath` (required): Path to XML file to validate

**Example Usage:**
```
Validate the XML file /path/to/myfile.xml
```

### 5. `get_xml_file_info`
Get detailed information about an XML file.

**Parameters:**
- `filePath` (required): Path to XML file to analyze

**Example Usage:**
```
Get information about the XML file /path/to/myfile.xml
```

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Claude Desktop application

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/ravikalla/xml-compare.git
   cd xml-compare/mcp-xml-comparison
   ```

2. **Build the application:**
   ```bash
   mvn clean package
   ```

3. **Configure Claude Desktop:**
   
   Add the following to your Claude Desktop configuration file:
   
   **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
   **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

   ```json
   {
     "mcpServers": {
       "xml-comparison": {
         "command": "java",
         "args": ["-jar", "/path/to/xml-compare/mcp-xml-comparison/target/xml-comparison-1.0.0.jar"]
       }
     }
   }
   ```

4. **Restart Claude Desktop**

## Usage Examples

Once configured, you can use natural language with Claude to compare XML files:

### Basic XML Comparison
```
Compare these two XML files and show me the differences in text format:
- /Users/myuser/file1.xml
- /Users/myuser/file2.xml
```

### Semantic (Order-Agnostic) Comparison
```
Compare these XML files semantically, ignoring element order:
- /path/to/config1.xml
- /path/to/config2.xml
Output format: JSON
```

### JSON Output
```
Compare /path/to/schema1.xml with /path/to/schema2.xml and generate a JSON report
```

### Excel Report
```
I need an Excel report comparing these XML configuration files:
- /config/old_config.xml  
- /config/new_config.xml
```

### File Validation
```
Check if this XML file is valid: /data/customer_data.xml
```

### File Analysis
```
Give me detailed information about this XML file: /documents/large_file.xml
```

## Output Formats

### Text Format
- Human-readable comparison report
- Lists all differences found
- Includes file sizes and comparison duration
- Saved as `.txt` file

### JSON Format
- Structured JSON object with comparison results
- Programmatically parseable
- Includes all metadata and differences
- Saved as `.json` file

### Excel Format
- Professional Excel workbook
- Summary sheet with overview
- Detailed differences in tabular format
- Saved as `.xlsx` file

## Performance

- **Memory Efficient**: Uses StAX streaming parser to handle large files
- **Fast Processing**: Linear performance scaling
- **Large File Support**: Successfully tested with files up to 92MB
- **Difference Limiting**: Prevents memory issues by limiting to 1000 differences

## Example Response

### Text Format Response:
```
Text report generated successfully at: xml_comparison_20250703_102154.txt
Files match: NO
Differences found: 5
Duration: 245ms
```

### JSON Format Response:
```json
{
  "filesMatch": false,
  "file1Path": "/path/to/file1.xml",
  "file2Path": "/path/to/file2.xml",
  "file1Size": 2048576,
  "file2Size": 2048580,
  "outputFormat": "json",
  "outputFilePath": "xml_comparison_20250703_102154.json",
  "differenceCount": 5,
  "comparisonDurationMs": 245,
  "comparisonTimestamp": "2025-07-03T10:21:54.123",
  "status": "SUCCESS"
}
```

## Testing

Run the test suite:
```bash
mvn test
```

For web mode testing (optional):
```bash
java -jar target/xml-comparison-1.0.0.jar --spring.profiles.active=web
```

## Troubleshooting

### Common Issues

1. **Java Version**: Ensure Java 17+ is installed
2. **File Paths**: Use absolute paths for XML files
3. **Large Files**: Increase JVM heap size for very large files:
   ```bash
   java -Xmx2g -jar target/xml-comparison-1.0.0.jar
   ```
4. **Permissions**: Ensure read/write permissions for input and output directories

### Logging

Enable debug logging by setting:
```bash
java -jar target/xml-comparison-1.0.0.jar --logging.level.com.ravikalla.xmlcomparison=DEBUG
```

## License

MIT License - see LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request