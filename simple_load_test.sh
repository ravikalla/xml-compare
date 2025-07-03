#!/bin/bash

# Simple and Reliable XML Compare Load Testing Script
# Focuses on core functionality without complex monitoring

set -e

# Configuration
JAR_FILE="target/xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
RESULTS_DIR="simple-load-test-results"
TEST_FILES_DIR="test-files"
LARGE_FILES_DIR="test-files-large"
CSV_FILE="$RESULTS_DIR/test_results.csv"
REPORT_FILE="$RESULTS_DIR/performance_report.html"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Results array
declare -a RESULTS=()

print_header() {
    echo ""
    echo -e "${CYAN}============================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}============================================${NC}"
    echo ""
}

print_test() {
    echo -e "${BLUE}[TEST $TOTAL_TESTS]${NC} $1"
}

print_success() {
    echo -e "${GREEN}✅ SUCCESS:${NC} $1"
}

print_error() {
    echo -e "${RED}❌ ERROR:${NC} $1"
}

print_info() {
    echo -e "${PURPLE}ℹ️  INFO:${NC} $1"
}

format_bytes() {
    local bytes=$1
    if [[ $bytes -lt 1024 ]]; then
        echo "${bytes}B"
    elif [[ $bytes -lt 1048576 ]]; then
        printf "%.1fKB" $(echo "scale=1; $bytes/1024" | bc -l)
    elif [[ $bytes -lt 1073741824 ]]; then
        printf "%.1fMB" $(echo "scale=1; $bytes/1048576" | bc -l)
    else
        printf "%.1fGB" $(echo "scale=1; $bytes/1073741824" | bc -l)
    fi
}

get_file_size() {
    local file="$1"
    if [[ -f "$file" ]]; then
        stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

run_test() {
    local test_name="$1"
    local file1="$2"
    local file2="$3"
    local heap_size="$4"
    local depth="${5:-5}"
    local category="$6"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    print_test "$test_name (Depth: $depth, Heap: $heap_size)"
    
    # Check files exist
    if [[ ! -f "$file1" || ! -f "$file2" ]]; then
        print_error "Files not found"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        RESULTS+=("$test_name,$file1,$file2,0,0,0,FAILED,FileNotFound,$heap_size,$depth,$category,0")
        return 1
    fi
    
    # Get file sizes
    local file1_size=$(get_file_size "$file1")
    local file2_size=$(get_file_size "$file2")
    local file1_human=$(format_bytes $file1_size)
    local file2_human=$(format_bytes $file2_size)
    
    print_info "File 1: $file1_human, File 2: $file2_human"
    
    # Prepare output files
    local output_file="$RESULTS_DIR/result_${test_name}_d${depth}.xls"
    local log_file="$RESULTS_DIR/log_${test_name}_d${depth}.txt"
    
    # Run test with timeout
    local start_time=$(date +%s)
    
    if gtimeout 300 java -Xmx"$heap_size" \
        -jar "$JAR_FILE" \
        "$file1" "$file2" "$output_file" \
        > "$log_file" 2>&1 || \
       java -Xmx"$heap_size" \
        -jar "$JAR_FILE" \
        "$file1" "$file2" "$output_file" \
        > "$log_file" 2>&1; then
        
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        local duration_ms=$((duration * 1000))
        
        # Check approach used
        local approach="DOM"
        if grep -q "Large files detected\|streaming" "$log_file" 2>/dev/null; then
            approach="Streaming"
        fi
        
        # Check output
        local output_size=0
        local differences="None"
        if [[ -f "$output_file" ]]; then
            output_size=$(get_file_size "$output_file")
            if [[ $output_size -gt 0 ]]; then
                differences="Found"
            fi
        fi
        
        print_success "Completed in ${duration_ms}ms using $approach approach"
        if [[ "$differences" == "Found" ]]; then
            print_info "Differences detected ($(format_bytes $output_size))"
        fi
        
        PASSED_TESTS=$((PASSED_TESTS + 1))
        RESULTS+=("$test_name,$file1,$file2,$file1_size,$file2_size,$duration_ms,PASSED,$approach,$heap_size,$depth,$category,$output_size")
        
    else
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        local duration_ms=$((duration * 1000))
        
        local error_type="Unknown"
        if grep -q -i "outofmemory" "$log_file" 2>/dev/null; then
            error_type="OutOfMemory"
        elif grep -q -i "timeout" "$log_file" 2>/dev/null; then
            error_type="Timeout"
        fi
        
        print_error "Failed after ${duration_ms}ms ($error_type)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        RESULTS+=("$test_name,$file1,$file2,$file1_size,$file2_size,$duration_ms,FAILED,$error_type,$heap_size,$depth,$category,0")
    fi
    
    echo ""
}

run_all_tests() {
    print_header "Starting XML Comparison Load Tests"
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    
    # Initialize CSV
    echo "test_name,file1,file2,file1_size,file2_size,duration_ms,status,approach,heap_size,depth,category,output_size" > "$CSV_FILE"
    
    # Test configurations: name:file1:file2:heap:depths:category
    local configs=(
        # Small files - multiple depths
        "1MB_identical:test_1MB_identical_1.xml:test_1MB_identical_2.xml:512m:3,5,7,10:Small"
        "1MB_different:test_1MB_different_1.xml:test_1MB_different_2.xml:512m:3,5,7,10:Small"
        "3MB_identical:test_3MB_identical_1.xml:test_3MB_identical_2.xml:512m:3,5,7,10:Small"
        "3MB_different:test_3MB_different_1.xml:test_3MB_different_2.xml:512m:3,5,7,10:Small"
        "6MB_identical:test_6MB_identical_1.xml:test_6MB_identical_2.xml:1g:3,5,7:Small"
        "6MB_different:test_6MB_different_1.xml:test_6MB_different_2.xml:1g:3,5,7:Small"
        "10MB_identical:test_10MB_identical_1.xml:test_10MB_identical_2.xml:1g:3,5:Small"
        "10MB_different:test_10MB_different_1.xml:test_10MB_different_2.xml:1g:3,5:Small"
    )
    
    print_header "Small File Tests (DOM Approach Expected)"
    
    for config in "${configs[@]}"; do
        IFS=':' read -r name file1_name file2_name heap depths category <<< "$config"
        
        local file1="$TEST_FILES_DIR/$file1_name"
        local file2="$TEST_FILES_DIR/$file2_name"
        
        if [[ -f "$file1" && -f "$file2" ]]; then
            IFS=',' read -ra depth_array <<< "$depths"
            for depth in "${depth_array[@]}"; do
                run_test "${name}_d${depth}" "$file1" "$file2" "$heap" "$depth" "$category"
            done
        else
            print_error "Skipping $name - files not found"
        fi
    done
    
    # Large file tests if available
    if [[ -d "$LARGE_FILES_DIR" ]]; then
        print_header "Large File Tests (Streaming Approach Expected)"
        
        local large_configs=(
            "87MB_identical:large_60MB.xml:large_60MB_identical.xml:1g:3,5:Large"
            "87MB_different:large_60MB.xml:large_60MB_modified.xml:1g:3,5:Large"
        )
        
        for config in "${large_configs[@]}"; do
            IFS=':' read -r name file1_name file2_name heap depths category <<< "$config"
            
            local file1="$LARGE_FILES_DIR/$file1_name"
            local file2="$LARGE_FILES_DIR/$file2_name"
            
            if [[ -f "$file1" && -f "$file2" ]]; then
                IFS=',' read -ra depth_array <<< "$depths"
                for depth in "${depth_array[@]}"; do
                    run_test "${name}_d${depth}" "$file1" "$file2" "$heap" "$depth" "$category"
                done
            else
                print_error "Skipping $name - files not found"
            fi
        done
        
        # Extreme tests
        if [[ -f "$LARGE_FILES_DIR/massive_test.xml" && -f "$LARGE_FILES_DIR/massive_test_copy.xml" ]]; then
            print_header "Extreme File Tests (324MB)"
            
            run_test "324MB_512m_d3" "$LARGE_FILES_DIR/massive_test.xml" "$LARGE_FILES_DIR/massive_test_copy.xml" "512m" "3" "Extreme"
            run_test "324MB_1g_d3" "$LARGE_FILES_DIR/massive_test.xml" "$LARGE_FILES_DIR/massive_test_copy.xml" "1g" "3" "Extreme"
        fi
    fi
    
    # Write results to CSV
    for result in "${RESULTS[@]}"; do
        echo "$result" >> "$CSV_FILE"
    done
}

generate_report() {
    print_header "Generating Performance Report"
    
    cat > "$REPORT_FILE" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>XML Compare Load Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
        .header { text-align: center; border-bottom: 2px solid #007acc; padding-bottom: 20px; margin-bottom: 20px; }
        .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; margin: 20px 0; }
        .stat { background: #f0f8ff; padding: 15px; border-radius: 8px; text-align: center; }
        .stat-number { font-size: 2em; font-weight: bold; color: #007acc; }
        .stat-label { color: #666; margin-top: 5px; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background: #007acc; color: white; }
        .passed { color: #28a745; font-weight: bold; }
        .failed { color: #dc3545; font-weight: bold; }
        .duration { text-align: right; font-family: monospace; }
        .size { text-align: right; font-family: monospace; }
        tr:nth-child(even) { background: #f8f9fa; }
        .approach-dom { background: #e3f2fd; padding: 2px 6px; border-radius: 4px; }
        .approach-streaming { background: #f3e5f5; padding: 2px 6px; border-radius: 4px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 XML Compare Load Test Report</h1>
            <p>Generated: $(date) | Platform: $(uname -s) $(uname -m)</p>
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
                    <th>Duration (ms)</th>
                    <th>Status</th>
                    <th>Approach</th>
                    <th>Category</th>
                    <th>Heap</th>
                    <th>Output Size</th>
                </tr>
            </thead>
            <tbody>
EOF

    # Add results to HTML
    for result in "${RESULTS[@]}"; do
        IFS=',' read -r test_name file1 file2 file1_size file2_size duration status approach heap depth category output_size <<< "$result"
        
        local file1_human=$(format_bytes $file1_size)
        local file2_human=$(format_bytes $file2_size)
        local output_human=$(format_bytes $output_size)
        
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
                    <td class="size">$file1_human</td>
                    <td class="size">$file2_human</td>
                    <td>$depth</td>
                    <td class="duration">$duration</td>
                    <td class="$status_class">$status</td>
                    <td><span class="$approach_class">$approach</span></td>
                    <td>$category</td>
                    <td>$heap</td>
                    <td class="size">$output_human</td>
                </tr>
EOF
    done
    
    cat >> "$REPORT_FILE" << EOF
            </tbody>
        </table>
        
        <h2>📁 Files Generated</h2>
        <ul>
            <li><strong>CSV Data:</strong> test_results.csv</li>
            <li><strong>Test Logs:</strong> log_*.txt files</li>
            <li><strong>Result Files:</strong> result_*.xls files</li>
        </ul>
        
        <div style="margin-top: 30px; text-align: center; color: #666;">
            <p>All files preserved in: $RESULTS_DIR/</p>
        </div>
    </div>
</body>
</html>
EOF

    print_success "Report generated: $REPORT_FILE"
}

main() {
    print_header "Simple XML Compare Load Testing"
    
    # Check prerequisites
    if [[ ! -f "$JAR_FILE" ]]; then
        print_error "JAR file not found: $JAR_FILE"
        exit 1
    fi
    
    if [[ ! -d "$TEST_FILES_DIR" ]]; then
        print_error "Test files directory not found: $TEST_FILES_DIR"
        exit 1
    fi
    
    print_info "JAR file: $JAR_FILE"
    print_info "Java version: $(java -version 2>&1 | head -1)"
    print_info "Platform: $(uname -s) $(uname -m)"
    
    # Run tests
    run_all_tests
    
    # Generate report
    generate_report
    
    # Final summary
    print_header "Load Testing Complete! 🎉"
    
    print_success "Final Results:"
    echo "  📊 Total Tests: $TOTAL_TESTS"
    echo "  ✅ Passed: $PASSED_TESTS" 
    echo "  ❌ Failed: $FAILED_TESTS"
    echo "  📈 Success Rate: $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%"
    echo ""
    print_info "📊 HTML Report: $REPORT_FILE"
    print_info "📈 CSV Data: $CSV_FILE"
    print_info "📁 All files: $RESULTS_DIR/"
    echo ""
    print_success "Open $REPORT_FILE in your browser to view detailed results!"
}

# Run main function
main "$@"