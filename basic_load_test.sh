#!/bin/bash

# Basic XML Compare Load Testing Script
# Simple, reliable, and focused on core testing

JAR_FILE="target/xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
RESULTS_DIR="basic-load-test-results"
TEST_FILES_DIR="test-files"
LARGE_FILES_DIR="test-files-large"
CSV_FILE="$RESULTS_DIR/test_results.csv"
REPORT_FILE="$RESULTS_DIR/load_test_report.html"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Results
declare -a RESULTS=()

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}Basic XML Compare Load Testing${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""

# Check prerequisites
if [[ ! -f "$JAR_FILE" ]]; then
    echo -e "${RED}❌ JAR file not found: $JAR_FILE${NC}"
    exit 1
fi

if [[ ! -d "$TEST_FILES_DIR" ]]; then
    echo -e "${RED}❌ Test files directory not found: $TEST_FILES_DIR${NC}"
    exit 1
fi

echo -e "${GREEN}✅ JAR file found: $JAR_FILE${NC}"
echo -e "${GREEN}✅ Test files directory: $TEST_FILES_DIR${NC}"
echo -e "${BLUE}ℹ️  Java version: $(java -version 2>&1 | head -1)${NC}"
echo ""

# Create results directory
mkdir -p "$RESULTS_DIR"

# Initialize CSV
echo "test_name,file1,file2,file1_size,file2_size,duration_seconds,status,approach,heap_size,depth,output_size" > "$CSV_FILE"

get_file_size() {
    if [[ -f "$1" ]]; then
        stat -f%z "$1" 2>/dev/null || stat -c%s "$1" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

format_size() {
    local bytes=$1
    if [[ $bytes -lt 1048576 ]]; then
        echo "$(echo "scale=1; $bytes/1024" | bc -l)KB"
    elif [[ $bytes -lt 1073741824 ]]; then
        echo "$(echo "scale=1; $bytes/1048576" | bc -l)MB"
    else
        echo "$(echo "scale=1; $bytes/1073741824" | bc -l)GB"
    fi
}

run_test() {
    local test_name="$1"
    local file1="$2"
    local file2="$3"
    local heap="$4"
    local depth="$5"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "${BLUE}[TEST $TOTAL_TESTS] $test_name (Depth: $depth, Heap: $heap)${NC}"
    
    if [[ ! -f "$file1" || ! -f "$file2" ]]; then
        echo -e "${RED}❌ Files not found${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo ""
        return 1
    fi
    
    local file1_size=$(get_file_size "$file1")
    local file2_size=$(get_file_size "$file2")
    
    echo "   File 1: $(format_size $file1_size) ($(basename "$file1"))"
    echo "   File 2: $(format_size $file2_size) ($(basename "$file2"))"
    
    local output_file="$RESULTS_DIR/result_${test_name}_d${depth}.xls"
    local log_file="$RESULTS_DIR/log_${test_name}_d${depth}.txt"
    
    echo "   Running comparison..."
    local start_time=$(date +%s)
    
    if java -Xmx"$heap" \
        -jar "$JAR_FILE" \
        "$file1" "$file2" "$output_file" \
        > "$log_file" 2>&1; then
        
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        # Check approach (now always StAX streaming)
        local approach="StAX-Streaming"
        if grep -q "StAX streaming comparison" "$log_file" 2>/dev/null; then
            approach="StAX-Streaming"
        fi
        
        # Check output
        local output_size=0
        local differences="None"
        if [[ -f "$output_file" ]]; then
            output_size=$(get_file_size "$output_file")
            if [[ $output_size -gt 0 ]]; then
                differences="Found ($(format_size $output_size))"
            fi
        fi
        
        echo -e "${GREEN}   ✅ SUCCESS: ${duration}s using $approach approach${NC}"
        if [[ "$differences" != "None" ]]; then
            echo "   📄 Differences: $differences"
        fi
        
        PASSED_TESTS=$((PASSED_TESTS + 1))
        RESULTS+=("$test_name,$file1,$file2,$file1_size,$file2_size,$duration,PASSED,$approach,$heap,$depth,$output_size")
        
    else
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        local error="Unknown"
        if grep -q -i "outofmemory" "$log_file" 2>/dev/null; then
            error="OutOfMemory"
        elif grep -q -i "exception" "$log_file" 2>/dev/null; then
            error="Exception"
        fi
        
        echo -e "${RED}   ❌ FAILED: ${duration}s ($error)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        RESULTS+=("$test_name,$file1,$file2,$file1_size,$file2_size,$duration,FAILED,$error,$heap,$depth,0")
    fi
    
    echo ""
}

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}XML Comparison Tests (StAX Streaming)${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""

# Test small files with different depths
depths=(3 5 7 10)

for depth in "${depths[@]}"; do
    echo -e "${YELLOW}--- Testing Depth $depth ---${NC}"
    
    run_test "1MB_identical" "$TEST_FILES_DIR/test_1MB_identical_1.xml" "$TEST_FILES_DIR/test_1MB_identical_2.xml" "512m" "$depth"
    run_test "1MB_different" "$TEST_FILES_DIR/test_1MB_different_1.xml" "$TEST_FILES_DIR/test_1MB_different_2.xml" "512m" "$depth"
    run_test "3MB_identical" "$TEST_FILES_DIR/test_3MB_identical_1.xml" "$TEST_FILES_DIR/test_3MB_identical_2.xml" "512m" "$depth"
    run_test "3MB_different" "$TEST_FILES_DIR/test_3MB_different_1.xml" "$TEST_FILES_DIR/test_3MB_different_2.xml" "512m" "$depth"
    
    if [[ $depth -le 7 ]]; then  # Limit larger files to fewer depths
        run_test "6MB_identical" "$TEST_FILES_DIR/test_6MB_identical_1.xml" "$TEST_FILES_DIR/test_6MB_identical_2.xml" "1g" "$depth"
        run_test "6MB_different" "$TEST_FILES_DIR/test_6MB_different_1.xml" "$TEST_FILES_DIR/test_6MB_different_2.xml" "1g" "$depth"
    fi
    
    if [[ $depth -le 5 ]]; then  # Even fewer depths for largest small files
        run_test "10MB_identical" "$TEST_FILES_DIR/test_10MB_identical_1.xml" "$TEST_FILES_DIR/test_10MB_identical_2.xml" "1g" "$depth"
        run_test "10MB_different" "$TEST_FILES_DIR/test_10MB_different_1.xml" "$TEST_FILES_DIR/test_10MB_different_2.xml" "1g" "$depth"
    fi
done

# Test large files if available  
if [[ -d "$LARGE_FILES_DIR" ]]; then
    echo -e "${CYAN}============================================${NC}"
    echo -e "${CYAN}Large File Tests (StAX Streaming)${NC}"
    echo -e "${CYAN}============================================${NC}"
    echo ""
    
    for depth in 3 5; do
        echo -e "${YELLOW}--- Large Files Depth $depth ---${NC}"
        
        run_test "87MB_identical" "$LARGE_FILES_DIR/large_60MB.xml" "$LARGE_FILES_DIR/large_60MB_identical.xml" "1g" "$depth"
        run_test "87MB_different" "$LARGE_FILES_DIR/large_60MB.xml" "$LARGE_FILES_DIR/large_60MB_modified.xml" "1g" "$depth"
    done
    
    # Extreme tests
    if [[ -f "$LARGE_FILES_DIR/massive_test.xml" && -f "$LARGE_FILES_DIR/massive_test_copy.xml" ]]; then
        echo -e "${CYAN}============================================${NC}"
        echo -e "${CYAN}Extreme File Tests (324MB)${NC}"
        echo -e "${CYAN}============================================${NC}"
        echo ""
        
        run_test "324MB_512m" "$LARGE_FILES_DIR/massive_test.xml" "$LARGE_FILES_DIR/massive_test_copy.xml" "512m" "3"
        run_test "324MB_1g" "$LARGE_FILES_DIR/massive_test.xml" "$LARGE_FILES_DIR/massive_test_copy.xml" "1g" "3"
    fi
fi

# Write results to CSV
for result in "${RESULTS[@]}"; do
    echo "$result" >> "$CSV_FILE"
done

# Generate HTML report
cat > "$REPORT_FILE" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>XML Compare Load Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .header { text-align: center; border-bottom: 2px solid #007acc; padding-bottom: 20px; margin-bottom: 20px; }
        .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin: 20px 0; }
        .stat { background: #f0f8ff; padding: 20px; border-radius: 8px; text-align: center; }
        .stat-number { font-size: 2.5em; font-weight: bold; color: #007acc; }
        .stat-label { color: #666; margin-top: 5px; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background: #007acc; color: white; }
        .passed { color: #28a745; font-weight: bold; }
        .failed { color: #dc3545; font-weight: bold; }
        .duration { text-align: right; font-family: monospace; }
        .size { text-align: right; font-family: monospace; }
        tr:nth-child(even) { background: #f8f9fa; }
        .approach-dom { background: #e3f2fd; padding: 4px 8px; border-radius: 4px; }
        .approach-streaming { background: #f3e5f5; padding: 4px 8px; border-radius: 4px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 XML Compare Load Test Report</h1>
            <p>Generated: $(date)</p>
            <p>Platform: $(uname -s) $(uname -m) | Java: $(java -version 2>&1 | head -1)</p>
        </div>
        
        <div class="stats">
            <div class="stat">
                <div class="stat-number">$TOTAL_TESTS</div>
                <div class="stat-label">Total Tests</div>
            </div>
            <div class="stat">
                <div class="stat-number">$PASSED_TESTS</div>
                <div class="stat-label">Passed</div>
            </div>
            <div class="stat">
                <div class="stat-number">$FAILED_TESTS</div>
                <div class="stat-label">Failed</div>
            </div>
            <div class="stat">
                <div class="stat-number">$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%</div>
                <div class="stat-label">Success Rate</div>
            </div>
        </div>
        
        <h2>📊 Test Results</h2>
        <table>
            <thead>
                <tr>
                    <th>Test Name</th>
                    <th>File 1 Size</th>
                    <th>File 2 Size</th>
                    <th>Depth</th>
                    <th>Duration (s)</th>
                    <th>Status</th>
                    <th>Approach</th>
                    <th>Heap</th>
                    <th>Output Size</th>
                </tr>
            </thead>
            <tbody>
EOF

# Add test results
for result in "${RESULTS[@]}"; do
    IFS=',' read -r test_name file1 file2 file1_size file2_size duration status approach heap depth output_size <<< "$result"
    
    file1_human=$(format_size $file1_size)
    file2_human=$(format_size $file2_size)
    output_human=$(format_size $output_size)
    
    status_class="passed"
    if [[ "$status" == "FAILED" ]]; then
        status_class="failed"
    fi
    
    approach_class="approach-dom"
    if [[ "$approach" == "Streaming" ]]; then
        approach_class="approach-streaming"
    fi
    
    cat >> "$REPORT_FILE" << EOF
                <tr>
                    <td><strong>$test_name</strong></td>
                    <td class="size">$file1_human</td>
                    <td class="size">$file2_human</td>
                    <td>$depth</td>
                    <td class="duration">$duration</td>
                    <td class="$status_class">$status</td>
                    <td><span class="$approach_class">$approach</span></td>
                    <td>$heap</td>
                    <td class="size">$output_human</td>
                </tr>
EOF
done

cat >> "$REPORT_FILE" << EOF
            </tbody>
        </table>
        
        <h2>📁 Generated Files</h2>
        <ul>
            <li><strong>CSV Data:</strong> test_results.csv</li>
            <li><strong>Test Logs:</strong> log_*.txt files</li>
            <li><strong>Result Files:</strong> result_*.xls files</li>
        </ul>
        
        <div style="text-align: center; margin-top: 30px; color: #666;">
            <p>All files preserved in: $RESULTS_DIR/</p>
        </div>
    </div>
</body>
</html>
EOF

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}Load Testing Complete! 🎉${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""
echo -e "${GREEN}📊 Final Results:${NC}"
echo "   Total Tests: $TOTAL_TESTS"
echo "   Passed: $PASSED_TESTS"
echo "   Failed: $FAILED_TESTS"
echo "   Success Rate: $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%"
echo ""
echo -e "${BLUE}📋 Reports Generated:${NC}"
echo "   📊 HTML Report: $REPORT_FILE"
echo "   📈 CSV Data: $CSV_FILE"
echo "   📁 All files: $RESULTS_DIR/"
echo ""
echo -e "${GREEN}🎉 Open $REPORT_FILE in your browser to view detailed results!${NC}"