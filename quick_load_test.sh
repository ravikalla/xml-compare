#!/bin/bash

# Quick Load Test Script with Performance Reporting
# Generates comprehensive test reports with file sizes, timing, and results

JAR_FILE="target/xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
RESULTS_DIR="quick-test-results"
REPORT_FILE="$RESULTS_DIR/performance_report.html"
CSV_FILE="$RESULTS_DIR/test_results.csv"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}=== Quick XML Compare Load Test ===${NC}"
echo "Starting comprehensive load testing..."

# Create results directory
mkdir -p "$RESULTS_DIR"

# Initialize CSV
echo "test_name,file1,file2,file1_size,file2_size,duration_ms,status,approach,output_file,output_size,heap_size" > "$CSV_FILE"

# Test counter
TOTAL=0
PASSED=0

run_test() {
    local name="$1"
    local file1="$2"
    local file2="$3"
    local heap="$4"
    local output="$RESULTS_DIR/result_${name}.xls"
    
    TOTAL=$((TOTAL + 1))
    
    echo -e "\n${BLUE}[TEST $TOTAL]${NC} $name"
    echo "  File 1: $(ls -lh "$file1" 2>/dev/null | awk '{print $5}') - $(basename "$file1")"
    echo "  File 2: $(ls -lh "$file2" 2>/dev/null | awk '{print $5}') - $(basename "$file2")"
    
    # Get file sizes
    local size1=$(stat -f%z "$file1" 2>/dev/null || echo "0")
    local size2=$(stat -f%z "$file2" 2>/dev/null || echo "0")
    
    # Time the execution
    local start=$(date +%s%3N)
    
    if java -Xmx"$heap" -jar "$JAR_FILE" "$file1" "$file2" "$output" > "$RESULTS_DIR/${name}_log.txt" 2>&1; then
        local end=$(date +%s%3N)
        local duration=$((end - start))
        
        # Check approach used
        local approach="DOM"
        if grep -q "Large files detected" "$RESULTS_DIR/${name}_log.txt" 2>/dev/null; then
            approach="Streaming"
        fi
        
        # Check output
        local output_size="0"
        local output_desc="Files identical"
        if [[ -f "$output" ]]; then
            output_size=$(stat -f%z "$output" 2>/dev/null || echo "0")
            output_desc="$(ls -lh "$output" | awk '{print $5}')"
        fi
        
        echo -e "  ${GREEN}✅ PASSED${NC} - ${duration}ms - $approach approach - Output: $output_desc"
        PASSED=$((PASSED + 1))
        
        # Add to CSV
        echo "$name,$file1,$file2,$size1,$size2,$duration,PASSED,$approach,$output,$output_size,$heap" >> "$CSV_FILE"
        
    else
        local end=$(date +%s%3N)
        local duration=$((end - start))
        
        echo -e "  ${RED}❌ FAILED${NC} - ${duration}ms"
        
        # Add to CSV
        echo "$name,$file1,$file2,$size1,$size2,$duration,FAILED,Error,$output,0,$heap" >> "$CSV_FILE"
    fi
}

# Run tests if files exist
echo -e "\n${YELLOW}Testing Small Files (DOM approach)...${NC}"

if [[ -d "test-files" ]]; then
    run_test "1MB_identical" "test-files/test_1MB_identical_1.xml" "test-files/test_1MB_identical_2.xml" "512m"
    run_test "1MB_different" "test-files/test_1MB_different_1.xml" "test-files/test_1MB_different_2.xml" "512m"
    run_test "3MB_identical" "test-files/test_3MB_identical_1.xml" "test-files/test_3MB_identical_2.xml" "512m"
    run_test "3MB_different" "test-files/test_3MB_different_1.xml" "test-files/test_3MB_different_2.xml" "512m"
    run_test "6MB_identical" "test-files/test_6MB_identical_1.xml" "test-files/test_6MB_identical_2.xml" "1g"
    run_test "6MB_different" "test-files/test_6MB_different_1.xml" "test-files/test_6MB_different_2.xml" "1g"
    run_test "10MB_identical" "test-files/test_10MB_identical_1.xml" "test-files/test_10MB_identical_2.xml" "1g"
    run_test "10MB_different" "test-files/test_10MB_different_1.xml" "test-files/test_10MB_different_2.xml" "1g"
fi

echo -e "\n${YELLOW}Testing Large Files (Streaming approach)...${NC}"

if [[ -d "test-files-large" ]]; then
    run_test "87MB_identical" "test-files-large/large_60MB.xml" "test-files-large/large_60MB_identical.xml" "1g"
    run_test "87MB_different" "test-files-large/large_60MB.xml" "test-files-large/large_60MB_modified.xml" "1g"
    
    if [[ -f "test-files-large/massive_test.xml" && -f "test-files-large/massive_test_copy.xml" ]]; then
        run_test "324MB_identical_512m" "test-files-large/massive_test.xml" "test-files-large/massive_test_copy.xml" "512m"
        run_test "324MB_identical_1g" "test-files-large/massive_test.xml" "test-files-large/massive_test_copy.xml" "1g"
    fi
fi

# Generate HTML report
cat > "$REPORT_FILE" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>XML Compare Load Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .header { text-align: center; border-bottom: 2px solid #007acc; padding-bottom: 20px; margin-bottom: 30px; }
        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }
        .stat-card { background: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; border-left: 4px solid #007acc; }
        .stat-number { font-size: 2em; font-weight: bold; color: #007acc; }
        .stat-label { color: #666; margin-top: 5px; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background: #007acc; color: white; position: sticky; top: 0; }
        .passed { color: #28a745; font-weight: bold; }
        .failed { color: #dc3545; font-weight: bold; }
        .duration { text-align: right; font-family: monospace; }
        .size { text-align: right; font-family: monospace; }
        .approach-dom { background: #e3f2fd; padding: 4px 8px; border-radius: 4px; }
        .approach-streaming { background: #f3e5f5; padding: 4px 8px; border-radius: 4px; }
        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; text-align: center; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 XML Compare Load Test Report</h1>
            <p>Generated: $(date)</p>
            <p>Platform: $(uname -s) $(uname -m) | Java: $(java -version 2>&1 | head -1)</p>
        </div>
        
        <div class="summary">
            <div class="stat-card">
                <div class="stat-number">$TOTAL</div>
                <div class="stat-label">Total Tests</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">$PASSED</div>
                <div class="stat-label">Passed</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">$((TOTAL - PASSED))</div>
                <div class="stat-label">Failed</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">$(echo "scale=1; $PASSED * 100 / $TOTAL" | bc -l)%</div>
                <div class="stat-label">Success Rate</div>
            </div>
        </div>
        
        <h2>📊 Detailed Test Results</h2>
        <table>
            <thead>
                <tr>
                    <th>Test Name</th>
                    <th>File 1</th>
                    <th>File 2</th>
                    <th>Duration</th>
                    <th>Status</th>
                    <th>Approach</th>
                    <th>Output</th>
                    <th>Heap Size</th>
                </tr>
            </thead>
            <tbody>
EOF

# Add test results to HTML
while IFS=',' read -r test_name file1 file2 file1_size file2_size duration status approach output output_size heap_size; do
    if [[ "$test_name" != "test_name" ]]; then  # Skip header
        # Format sizes
        local f1_human=$(echo $file1_size | awk '{if($1>=1073741824) printf "%.1fGB", $1/1073741824; else if($1>=1048576) printf "%.1fMB", $1/1048576; else if($1>=1024) printf "%.1fKB", $1/1024; else printf "%dB", $1}')
        local f2_human=$(echo $file2_size | awk '{if($1>=1073741824) printf "%.1fGB", $1/1073741824; else if($1>=1048576) printf "%.1fMB", $1/1048576; else if($1>=1024) printf "%.1fKB", $1/1024; else printf "%dB", $1}')
        local out_human="None"
        if [[ $output_size -gt 0 ]]; then
            out_human=$(echo $output_size | awk '{if($1>=1048576) printf "%.1fMB", $1/1048576; else if($1>=1024) printf "%.1fKB", $1/1024; else printf "%dB", $1}')
        fi
        
        local status_class="passed"
        if [[ "$status" == "FAILED" ]]; then
            status_class="failed"
        fi
        
        local approach_class="approach-dom"
        if [[ "$approach" == "Streaming" ]]; then
            approach_class="approach-streaming"
        fi
        
        cat >> "$REPORT_FILE" << EOF
                <tr>
                    <td><strong>$test_name</strong></td>
                    <td class="size">$f1_human</td>
                    <td class="size">$f2_human</td>
                    <td class="duration">${duration}ms</td>
                    <td class="$status_class">$status</td>
                    <td><span class="$approach_class">$approach</span></td>
                    <td class="size">$out_human</td>
                    <td>$heap_size</td>
                </tr>
EOF
    fi
done < "$CSV_FILE"

cat >> "$REPORT_FILE" << EOF
            </tbody>
        </table>
        
        <h2>📁 Generated Files</h2>
        <ul>
            <li><strong>CSV Data:</strong> <code>test_results.csv</code></li>
            <li><strong>Test Logs:</strong> <code>*_log.txt</code> files</li>
            <li><strong>Comparison Results:</strong> <code>result_*.xls</code> files</li>
        </ul>
        
        <div class="footer">
            <p>Report generated by XML Compare Load Testing Suite</p>
            <p>All test files and results are preserved in the <code>$RESULTS_DIR</code> directory</p>
        </div>
    </div>
</body>
</html>
EOF

# Final summary
echo -e "\n${GREEN}=== Load Test Complete! ===${NC}"
echo "📊 Total Tests: $TOTAL"
echo "✅ Passed: $PASSED"
echo "❌ Failed: $((TOTAL - PASSED))"
echo "📈 Success Rate: $(echo "scale=1; $PASSED * 100 / $TOTAL" | bc -l)%"
echo ""
echo "📋 Reports Generated:"
echo "  📊 HTML Report: $REPORT_FILE"
echo "  📈 CSV Data: $CSV_FILE"
echo "  📁 All files: $RESULTS_DIR/"
echo ""
echo "🎉 Open the HTML report in your browser to see detailed results!"