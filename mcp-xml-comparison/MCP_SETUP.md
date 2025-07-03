# MCP Server Setup Guide

## Quick Start

1. **Run the demo setup script:**
   ```bash
   ./demo_setup.sh
   ```

2. **Configure Claude Desktop** by adding this to your `claude_desktop_config.json`:
   ```json
   {
     "mcpServers": {
       "xml-comparison": {
         "command": "java",
         "args": ["-jar", "/Users/ravikalla/Desktop/projects/xml-compare-master/mcp-xml-comparison/target/xml-comparison-1.0.0.jar"]
       }
     }
   }
   ```

3. **Restart Claude Desktop** and try these example commands:

   - **Compare different files (text format):**
     ```
     Compare /Users/ravikalla/Desktop/projects/xml-compare-master/mcp-xml-comparison/demo-files/config1.xml with /Users/ravikalla/Desktop/projects/xml-compare-master/mcp-xml-comparison/demo-files/config2.xml in text format
     ```

   - **Compare identical files (JSON format):**
     ```
     Compare /Users/ravikalla/Desktop/projects/xml-compare-master/mcp-xml-comparison/demo-files/config1.xml with /Users/ravikalla/Desktop/projects/xml-compare-master/mcp-xml-comparison/demo-files/config1_copy.xml in JSON format
     ```

   - **Generate Excel report:**
     ```
     Compare these XML config files and create an Excel report: /Users/ravikalla/Desktop/projects/xml-compare-master/mcp-xml-comparison/demo-files/config1.xml and /Users/ravikalla/Desktop/projects/xml-compare-master/mcp-xml-comparison/demo-files/config2.xml
     ```

   - **Validate XML file:**
     ```
     Validate this XML file: /Users/ravikalla/Desktop/projects/xml-compare-master/mcp-xml-comparison/demo-files/config1.xml
     ```

   - **Get file information:**
     ```
     Get detailed information about this XML file: /Users/ravikalla/Desktop/projects/xml-compare-master/mcp-xml-comparison/demo-files/config1.xml
     ```

## Configuration File Locations

- **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

## Available Tools

1. **compare_xml_files** - Basic comparison with auto-generated output path
2. **compare_xml_files_custom_path** - Comparison with custom output file path  
3. **validate_xml_file** - Check if XML file is well-formed
4. **get_xml_file_info** - Get detailed XML file information

## Output Formats

- **text** - Human-readable comparison report (.txt)
- **json** - Structured JSON object (.json)
- **excel** - Professional Excel workbook (.xlsx)

## Demo Files

The setup script creates three demo files:
- `config1.xml` - Original configuration
- `config2.xml` - Modified configuration (database name, cache settings, log level changed)
- `config1_copy.xml` - Identical copy of config1.xml

## Troubleshooting

1. **Ensure Java 17+ is installed**
2. **Use absolute file paths** in your requests
3. **Check file permissions** for read/write access
4. **Verify JAR path** in Claude Desktop configuration