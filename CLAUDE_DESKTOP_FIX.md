# Claude Desktop Integration Fix

## Issue Identified
The error logs show that Claude Desktop is trying to parse Spring Boot startup banner as JSON, causing parsing errors.

## Root Cause
- Spring Boot outputs a startup banner and logs to STDOUT
- Claude Desktop expects only MCP JSON-RPC messages on STDOUT
- Any non-JSON output breaks the MCP protocol communication

## Solution Applied
Updated application configuration to suppress all console output:

### Fixed Configuration
1. **Disabled Spring Boot banner**: `spring.main.banner-mode=off`
2. **Suppressed startup logs**: `spring.main.log-startup-info=false`
3. **Set minimal logging levels**: All frameworks set to ERROR level
4. **Disabled ANSI colors**: `spring.output.ansi.enabled=never`

## Updated Claude Desktop Configuration

Use this **exact** configuration in your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "xml-comparison": {
      "command": "java",
      "args": [
        "-jar", 
        "/Users/ravikalla/Desktop/projects/xml-compare-master/target/xml-comparison-1.0.0.jar"
      ]
    }
  }
}
```

## Configuration File Locations

- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

## Verification Steps

1. **Update the JAR path** to the exact location: 
   ```
   /Users/ravikalla/Desktop/projects/xml-compare-master/target/xml-comparison-1.0.0.jar
   ```

2. **Test the server manually** (should only output JSON):
   ```bash
   echo '{"jsonrpc": "2.0", "method": "initialize", "id": 1, "params": {"protocolVersion": "2024-11-05", "capabilities": {}, "clientInfo": {"name": "test", "version": "1.0"}}}' | java -jar target/xml-comparison-1.0.0.jar
   ```

3. **Restart Claude Desktop** completely (quit and relaunch)

4. **Check Claude Desktop settings** to verify the MCP server appears

## Expected Tools Available

After successful integration, you should see these tools:

1. **compare_xml_files** - Basic XML comparison
2. **compare_xml_files_custom_path** - XML comparison with custom output path
3. **validate_xml_file** - XML file validation
4. **get_xml_file_info** - XML file information

## Test Commands

Try these commands in Claude Desktop:

```
Compare /Users/ravikalla/Desktop/projects/xml-compare-master/demo-files/config1.xml with /Users/ravikalla/Desktop/projects/xml-compare-master/demo-files/config2.xml in JSON format
```

```
Validate this XML file: /Users/ravikalla/Desktop/projects/xml-compare-master/demo-files/config1.xml
```

## Troubleshooting

If issues persist:

1. **Check Java version**: Ensure Java 17+ is installed
2. **Verify file permissions**: JAR file should be executable
3. **Check path accuracy**: Use absolute paths only
4. **Review Claude Desktop logs**: Look for any remaining parsing errors

## Alternative: Pure Command Line Usage

If Claude Desktop integration still has issues, you can test the MCP server directly:

```bash
java -jar target/xml-comparison-1.0.0.jar
```

Then send MCP JSON-RPC messages directly to test functionality.