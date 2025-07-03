# XML Comparison MCP Server - Playground Ready

This MCP server is ready for the MCP Playground! Test XML file comparison with semantic analysis capabilities.

## Quick Start for Playground

### 1. Installation Commands
```bash
# Clone and build
git clone https://github.com/ravikalla/xml-compare.git
cd xml-compare/mcp-xml-comparison
mvn clean package -DskipTests

# Run the server
java -jar target/xml-comparison-1.0.0.jar
```

### 2. MCP Configuration
```json
{
  "mcpServers": {
    "xml-comparison": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/xml-comparison-1.0.0.jar"]
    }
  }
}
```

## Demo Commands for Playground

### Basic File Comparison
```
Compare two XML files:
- file1Path: "/path/to/demo-files/config1.xml"
- file2Path: "/path/to/demo-files/config2.xml" 
- outputFormat: "text"
```

### Semantic (Order-Agnostic) Comparison
```
Perform semantic comparison ignoring element order:
- Use tool: compare_xml_files_semantic
- file1Path: "/path/to/demo-files/config1.xml"
- file2Path: "/path/to/demo-files/config1_copy.xml"
- outputFormat: "json"
```

### XML Validation
```
Validate XML file:
- Use tool: validate_xml_file
- filePath: "/path/to/demo-files/config1.xml"
```

### File Analysis
```
Get detailed XML file information:
- Use tool: get_xml_file_info
- filePath: "/path/to/demo-files/config2.xml"
```

## Sample Test Files

The server includes demo XML files for testing:

1. **config1.xml** - Sample configuration file
2. **config1_copy.xml** - Identical copy (for semantic comparison testing)
3. **config2.xml** - Modified version with differences

## Key Features to Demonstrate

1. **Performance**: Handles large XML files efficiently
2. **Semantic Comparison**: Shows order-agnostic comparison capabilities
3. **Multiple Outputs**: Text, JSON, and Excel format support
4. **Validation**: XML well-formedness checking
5. **Analysis**: Detailed file structure information

## Expected Outputs

### Regular Comparison
- Shows all differences including order changes
- Reports element-by-element mismatches

### Semantic Comparison  
- Ignores element ordering
- Focuses on actual content differences
- Much fewer false positives from reordering

### Validation Response
```json
{
  "filePath": "/path/to/file.xml",
  "validXml": true,
  "fileSizeBytes": 2048,
  "elementCount": 15,
  "depth": 4,
  "rootElement": "configuration",
  "encoding": "UTF-8",
  "version": "1.0"
}
```

## Performance Characteristics

- **Memory Efficient**: Uses streaming parser
- **Large File Support**: Tested with 60MB+ files  
- **Fast Processing**: Typical comparisons under 300ms
- **Error Resilient**: Comprehensive error handling

## Playground Integration Notes

- All file paths should be absolute
- Server runs in STDIO mode by default
- Supports natural language interaction through Claude
- No complex dependencies - just Java 17+

Ready for MCP Playground testing! 🚀