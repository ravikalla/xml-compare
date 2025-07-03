#!/bin/bash

# Demo Setup Script for XML Comparison MCP Server

echo "🚀 Setting up XML Comparison MCP Server Demo"
echo "==========================================="

# Create demo XML files
echo "📄 Creating demo XML files..."
mkdir -p demo-files

# Create first XML file
cat > demo-files/config1.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Database>
        <Host>localhost</Host>
        <Port>5432</Port>
        <Name>production_db</Name>
        <User>admin</User>
    </Database>
    <Cache>
        <Enabled>true</Enabled>
        <TTL>3600</TTL>
        <MaxSize>1000</MaxSize>
    </Cache>
    <Logging>
        <Level>INFO</Level>
        <File>/var/log/app.log</File>
    </Logging>
</Configuration>
EOF

# Create second XML file (slightly different)
cat > demo-files/config2.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Database>
        <Host>localhost</Host>
        <Port>5432</Port>
        <Name>staging_db</Name>
        <User>admin</User>
    </Database>
    <Cache>
        <Enabled>false</Enabled>
        <TTL>1800</TTL>
        <MaxSize>500</MaxSize>
    </Cache>
    <Logging>
        <Level>DEBUG</Level>
        <File>/var/log/app.log</File>
    </Logging>
</Configuration>
EOF

# Create identical XML files for testing
cp demo-files/config1.xml demo-files/config1_copy.xml

echo "✅ Demo files created:"
echo "   - demo-files/config1.xml (original)"
echo "   - demo-files/config2.xml (modified - database name, cache settings, log level)"
echo "   - demo-files/config1_copy.xml (identical to config1.xml)"

echo ""
echo "🔧 MCP Server JAR Location:"
echo "   $(pwd)/target/xml-comparison-1.0.0.jar"

echo ""
echo "📖 Claude Desktop Configuration:"
echo "Add this to your claude_desktop_config.json:"
echo ""
echo "{"
echo "  \"mcpServers\": {"
echo "    \"xml-comparison\": {"
echo "      \"command\": \"java\","
echo "      \"args\": [\"-jar\", \"$(pwd)/target/xml-comparison-1.0.0.jar\"]"
echo "    }"
echo "  }"
echo "}"

echo ""
echo "🎯 Example Commands to try in Claude Desktop:"
echo ""
echo "1. Compare different files (text format):"
echo "   \"Compare $(pwd)/demo-files/config1.xml with $(pwd)/demo-files/config2.xml in text format\""
echo ""
echo "2. Compare identical files (JSON format):"
echo "   \"Compare $(pwd)/demo-files/config1.xml with $(pwd)/demo-files/config1_copy.xml in JSON format\""
echo ""
echo "3. Generate Excel report:"
echo "   \"Compare these XML config files and create an Excel report: $(pwd)/demo-files/config1.xml and $(pwd)/demo-files/config2.xml\""
echo ""
echo "4. Validate XML file:"
echo "   \"Validate this XML file: $(pwd)/demo-files/config1.xml\""
echo ""
echo "5. Get file information:"
echo "   \"Get detailed information about this XML file: $(pwd)/demo-files/config1.xml\""

echo ""
echo "🎉 Setup complete! Configure Claude Desktop and try the examples above."