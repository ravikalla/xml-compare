#!/bin/bash

# =============================================================================
# Complete XML Compare Load Testing Script
# =============================================================================
# This script performs comprehensive load testing of the XML comparison tool
# with multiple file sizes, comparison depths, and detailed performance reporting.
# 
# Features:
# - Tests files from 1MB to 324MB
# - Tests both DOM and Streaming approaches
# - Supports comparison depths up to 10 levels
# - Generates detailed HTML and CSV reports
# - Records file sizes, timing, memory usage, and difference files
# - Handles both identical and different XML files
# =============================================================================

set -e

# Configuration
JAR_FILE="target/xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
RESULTS_DIR="complete-load-test-results"
TEST_FILES_DIR="test-files"
LARGE_FILES_DIR="test-files-large"
REPORT_FILE="$RESULTS_DIR/complete_performance_report.html"
CSV_FILE="$RESULTS_DIR/complete_test_results.csv"
SUMMARY_FILE="$RESULTS_DIR/test_summary.txt"

# Colors for console output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
DOM_TESTS=0
STREAMING_TESTS=0

# Performance tracking
declare -a TEST_RESULTS=()
START_TIME=$(date +%s)

# =============================================================================
# Utility Functions
# =============================================================================

log_message() {
    # Ensure results directory exists before logging
    mkdir -p "$RESULTS_DIR" 2>/dev/null
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$RESULTS_DIR/test_execution.log"
}

print_header() {
    echo ""
    echo -e "${CYAN}================================================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}================================================================${NC}"
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

print_warning() {
    echo -e "${YELLOW}⚠️  WARNING:${NC} $1"
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

check_java_memory() {
    local max_heap=$(java -XX:+PrintFlagsFinal -version 2>&1 | grep MaxHeapSize | awk '{print int($4/1024/1024)}' 2>/dev/null || echo "Unknown")
    echo "$max_heap"
}

# =============================================================================
# Test Execution Function
# =============================================================================

run_xml_comparison_test() {
    local test_name="$1"
    local file1="$2"
    local file2="$3"
    local heap_size="$4"
    local comparison_depth="${5:-5}"
    local test_category="$6"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    print_test "$test_name (Depth: $comparison_depth)"
    
    # Check if files exist
    if [[ ! -f "$file1" ]]; then
        print_error "File 1 not found: $file1"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
    
    if [[ ! -f "$file2" ]]; then
        print_error "File 2 not found: $file2"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
    
    # Get file information
    local file1_size=$(get_file_size "$file1")
    local file2_size=$(get_file_size "$file2")
    local file1_human=$(format_bytes $file1_size)
    local file2_human=$(format_bytes $file2_size)
    
    print_info "File 1: $file1_human ($(basename "$file1"))"
    print_info "File 2: $file2_human ($(basename "$file2"))"
    print_info "Heap Size: $heap_size"
    print_info "Category: $test_category"
    
    # Prepare output files
    local output_file="$RESULTS_DIR/result_${test_name}_depth${comparison_depth}.xls"
    local log_file="$RESULTS_DIR/log_${test_name}_depth${comparison_depth}.txt"
    local memory_log="$RESULTS_DIR/memory_${test_name}_depth${comparison_depth}.txt"
    
    # Get memory usage before test
    local memory_before=$(ps -o rss= $$ 2>/dev/null | tr -d ' ' || echo "0")
    
    # Record start time with high precision
    local start_time=$(date +%s.%N)
    local start_timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Run the XML comparison
    log_message "Starting test: $test_name with heap $heap_size"
    
    # Monitor memory during execution
    (
        while sleep 0.5; do
            local mem_usage=$(ps -o rss= $$ 2>/dev/null | tr -d ' ' || echo "0")
            echo "$(date +%s.%N),$mem_usage" >> "$memory_log"
        done
    ) &
    local monitor_pid=$!
    
    # Execute the comparison
    if java -Xmx"$heap_size" \
            -Dxml.compare.depth="$comparison_depth" \
            -jar "$JAR_FILE" \
            "$file1" "$file2" "$output_file" \
            > "$log_file" 2>&1; then
        
        # Stop memory monitoring
        kill $monitor_pid 2>/dev/null || true
        wait $monitor_pid 2>/dev/null || true
        
        # Calculate execution time
        local end_time=$(date +%s.%N)
        local duration=$(echo "$end_time - $start_time" | bc -l)
        local duration_ms=$(echo "$duration * 1000" | bc -l | cut -d. -f1)
        
        # Get memory usage after test
        local memory_after=$(ps -o rss= $$ 2>/dev/null | tr -d ' ' || echo "0")
        local memory_used=$((memory_after - memory_before))
        
        # Determine approach used
        local approach="DOM"
        if grep -q "Large files detected\|streaming" "$log_file" 2>/dev/null; then
            approach="Streaming"
            STREAMING_TESTS=$((STREAMING_TESTS + 1))
        else
            DOM_TESTS=$((DOM_TESTS + 1))
        fi
        
        # Check output file
        local output_size=0
        local output_human="No differences"
        local differences_count=0
        
        if [[ -f "$output_file" ]]; then
            output_size=$(get_file_size "$output_file")
            output_human=$(format_bytes $output_size)
            
            # Try to count differences
            if [[ "$output_file" == *.txt ]]; then
                differences_count=$(grep -c "mismatch\|difference" "$output_file" 2>/dev/null || echo "0")
            elif [[ $output_size -gt 0 ]]; then
                differences_count="Present"
            fi
        fi
        
        # Success
        print_success "Completed in ${duration_ms}ms using $approach approach"
        print_info "Output: $output_human"
        if [[ $differences_count != "0" ]]; then
            print_info "Differences found: $differences_count"
        fi
        
        PASSED_TESTS=$((PASSED_TESTS + 1))
        
        # Store result
        TEST_RESULTS+=("$test_name|$file1|$file2|$file1_size|$file2_size|$file1_human|$file2_human|$duration_ms|$memory_used|PASSED|$approach|$output_file|$output_size|$output_human|$differences_count|$heap_size|$comparison_depth|$test_category|$start_timestamp")
        
        log_message "Test $test_name completed successfully: ${duration_ms}ms, $approach approach"
        return 0
        
    else
        # Stop memory monitoring
        kill $monitor_pid 2>/dev/null || true
        wait $monitor_pid 2>/dev/null || true
        
        # Calculate execution time
        local end_time=$(date +%s.%N)
        local duration=$(echo "$end_time - $start_time" | bc -l)
        local duration_ms=$(echo "$duration * 1000" | bc -l | cut -d. -f1)
        
        # Check for specific errors
        local error_type="Unknown Error"
        if grep -q -i "outofmemory\|java.lang.OutOfMemoryError" "$log_file" 2>/dev/null; then
            error_type="Out of Memory"
        elif grep -q -i "exception" "$log_file" 2>/dev/null; then
            error_type="Exception"
        elif grep -q -i "timeout" "$log_file" 2>/dev/null; then
            error_type="Timeout"
        fi
        
        print_error "Failed after ${duration_ms}ms - $error_type"
        print_info "Check log: $log_file"
        
        FAILED_TESTS=$((FAILED_TESTS + 1))
        
        # Store result
        TEST_RESULTS+=("$test_name|$file1|$file2|$file1_size|$file2_size|$file1_human|$file2_human|$duration_ms|0|FAILED|$error_type|$output_file|0|Error|0|$heap_size|$comparison_depth|$test_category|$start_timestamp")
        
        log_message "Test $test_name failed: ${duration_ms}ms, $error_type"
        return 1
    fi
}

# =============================================================================
# Test Suite Definitions
# =============================================================================

run_small_file_tests() {
    print_header "Small File Tests (DOM Approach Expected)"
    
    local test_configs=(
        "1MB_identical:test_1MB_identical_1.xml:test_1MB_identical_2.xml:512m:Small Files"
        "1MB_different:test_1MB_different_1.xml:test_1MB_different_2.xml:512m:Small Files"
        "3MB_identical:test_3MB_identical_1.xml:test_3MB_identical_2.xml:512m:Small Files"
        "3MB_different:test_3MB_different_1.xml:test_3MB_different_2.xml:512m:Small Files"
        "6MB_identical:test_6MB_identical_1.xml:test_6MB_identical_2.xml:1g:Small Files"
        "6MB_different:test_6MB_different_1.xml:test_6MB_different_2.xml:1g:Small Files"
        "10MB_identical:test_10MB_identical_1.xml:test_10MB_identical_2.xml:1g:Small Files"
        "10MB_different:test_10MB_different_1.xml:test_10MB_different_2.xml:1g:Small Files"
    )
    
    local depths=(3 5 7 10)
    
    for config in "${test_configs[@]}"; do
        IFS=':' read -r test_name file1_name file2_name heap_size category <<< "$config"
        
        local file1="$TEST_FILES_DIR/$file1_name"
        local file2="$TEST_FILES_DIR/$file2_name"
        
        if [[ -f "$file1" && -f "$file2" ]]; then
            for depth in "${depths[@]}"; do
                run_xml_comparison_test "${test_name}_d${depth}" "$file1" "$file2" "$heap_size" "$depth" "$category"
                echo ""
            done
        else
            print_warning "Skipping $test_name - files not found"
        fi
    done
}

run_large_file_tests() {
    if [[ ! -d "$LARGE_FILES_DIR" ]]; then
        print_warning "Large files directory not found - skipping large file tests"
        return 0
    fi
    
    print_header "Large File Tests (Streaming Approach Expected)"
    
    local large_configs=(
        "87MB_identical:large_60MB.xml:large_60MB_identical.xml:1g:Large Files"
        "87MB_different:large_60MB.xml:large_60MB_modified.xml:1g:Large Files"
    )
    
    # Test with fewer depths for large files to save time
    local depths=(3 5 7)
    
    for config in "${large_configs[@]}"; do
        IFS=':' read -r test_name file1_name file2_name heap_size category <<< "$config"
        
        local file1="$LARGE_FILES_DIR/$file1_name"
        local file2="$LARGE_FILES_DIR/$file2_name"
        
        if [[ -f "$file1" && -f "$file2" ]]; then
            for depth in "${depths[@]}"; do
                run_xml_comparison_test "${test_name}_d${depth}" "$file1" "$file2" "$heap_size" "$depth" "$category"
                echo ""
            done
        else
            print_warning "Skipping $test_name - files not found"
        fi
    done
}

run_extreme_tests() {
    if [[ ! -f "$LARGE_FILES_DIR/massive_test.xml" || ! -f "$LARGE_FILES_DIR/massive_test_copy.xml" ]]; then
        print_warning "Extreme test files not found - skipping"
        return 0
    fi
    
    print_header "Extreme Load Tests (324MB Files)"
    
    local extreme_configs=(
        "324MB_512m:massive_test.xml:massive_test_copy.xml:512m:Extreme"
        "324MB_1g:massive_test.xml:massive_test_copy.xml:1g:Extreme"
        "324MB_2g:massive_test.xml:massive_test_copy.xml:2g:Extreme"
    )
    
    # Test with minimal depths for extreme files
    local depths=(3 5)
    
    for config in "${extreme_configs[@]}"; do
        IFS=':' read -r test_name file1_name file2_name heap_size category <<< "$config"
        
        local file1="$LARGE_FILES_DIR/$file1_name"
        local file2="$LARGE_FILES_DIR/$file2_name"
        
        for depth in "${depths[@]}"; do
            run_xml_comparison_test "${test_name}_d${depth}" "$file1" "$file2" "$heap_size" "$depth" "$category"
            echo ""
        done
    done
}

# =============================================================================
# Report Generation
# =============================================================================

generate_reports() {
    print_header "Generating Comprehensive Reports"
    
    local end_time=$(date +%s)
    local total_duration=$((end_time - START_TIME))
    local java_max_heap=$(check_java_memory)
    
    # Create CSV report
    echo "test_name,file1_path,file2_path,file1_size_bytes,file2_size_bytes,file1_size_human,file2_size_human,duration_ms,memory_used_kb,status,approach,output_file,output_size_bytes,output_size_human,differences_found,heap_size,comparison_depth,test_category,start_timestamp" > "$CSV_FILE"
    
    for result in "${TEST_RESULTS[@]}"; do
        echo "$result" | tr '|' ',' >> "$CSV_FILE"
    done
    
    # Generate HTML report
    cat > "$REPORT_FILE" << EOF
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Complete XML Compare Load Test Report</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f7fa; line-height: 1.6; }
        .container { max-width: 1400px; margin: 0 auto; padding: 20px; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 12px; text-align: center; margin-bottom: 30px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
        .header h1 { font-size: 2.5em; margin-bottom: 10px; }
        .header p { font-size: 1.1em; opacity: 0.9; }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 30px 0; }
        .stat-card { background: white; padding: 25px; border-radius: 12px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); border-left: 4px solid #667eea; }
        .stat-number { font-size: 2.5em; font-weight: bold; color: #667eea; margin-bottom: 5px; }
        .stat-label { color: #666; font-size: 0.9em; text-transform: uppercase; letter-spacing: 0.5px; }
        .section { background: white; margin: 20px 0; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .section-header { background: #f8f9fa; padding: 20px; border-bottom: 1px solid #e9ecef; }
        .section-title { font-size: 1.5em; color: #333; margin-bottom: 5px; }
        .section-subtitle { color: #666; font-size: 0.9em; }
        .table-container { overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; }
        th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #e9ecef; }
        th { background: #f8f9fa; font-weight: 600; color: #333; position: sticky; top: 0; z-index: 10; }
        .status-passed { color: #28a745; font-weight: bold; }
        .status-failed { color: #dc3545; font-weight: bold; }
        .approach-dom { background: #e3f2fd; color: #1976d2; padding: 4px 8px; border-radius: 20px; font-size: 0.85em; }
        .approach-streaming { background: #f3e5f5; color: #7b1fa2; padding: 4px 8px; border-radius: 20px; font-size: 0.85em; }
        .depth-badge { background: #fff3cd; color: #856404; padding: 2px 6px; border-radius: 10px; font-size: 0.8em; }
        .size-cell, .duration-cell { text-align: right; font-family: 'Courier New', monospace; }
        .category-small { background: #d4edda; color: #155724; padding: 2px 6px; border-radius: 10px; font-size: 0.8em; }
        .category-large { background: #fff3cd; color: #856404; padding: 2px 6px; border-radius: 10px; font-size: 0.8em; }
        .category-extreme { background: #f8d7da; color: #721c24; padding: 2px 6px; border-radius: 10px; font-size: 0.8em; }
        .footer { text-align: center; margin-top: 40px; padding: 20px; color: #666; border-top: 1px solid #e9ecef; }
        .summary-text { padding: 20px; background: #f8f9fa; margin: 20px 0; border-radius: 8px; }
        tr:hover { background: #f8f9fa; }
        .performance-summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 15px; padding: 20px; }
        .perf-item { background: #f8f9fa; padding: 15px; border-radius: 8px; }
        .perf-label { font-weight: bold; color: #333; margin-bottom: 5px; }
        .perf-value { color: #667eea; font-size: 1.1em; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 Complete XML Compare Load Test Report</h1>
            <p>Comprehensive performance analysis with multi-depth comparison testing</p>
            <p>Generated: $(date '+%Y-%m-%d %H:%M:%S') | Platform: $(uname -s) $(uname -m)</p>
        </div>
        
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-number">$TOTAL_TESTS</div>
                <div class="stat-label">Total Tests</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">$PASSED_TESTS</div>
                <div class="stat-label">Passed</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">$FAILED_TESTS</div>
                <div class="stat-label">Failed</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%</div>
                <div class="stat-label">Success Rate</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">$DOM_TESTS</div>
                <div class="stat-label">DOM Tests</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">$STREAMING_TESTS</div>
                <div class="stat-label">Streaming Tests</div>
            </div>
        </div>
        
        <div class="section">
            <div class="section-header">
                <div class="section-title">Environment & Configuration</div>
                <div class="section-subtitle">System information and test parameters</div>
            </div>
            <div class="performance-summary">
                <div class="perf-item">
                    <div class="perf-label">Java Version</div>
                    <div class="perf-value">$(java -version 2>&1 | head -1)</div>
                </div>
                <div class="perf-item">
                    <div class="perf-label">Max Heap Size</div>
                    <div class="perf-value">${java_max_heap}MB</div>
                </div>
                <div class="perf-item">
                    <div class="perf-label">Total Execution Time</div>
                    <div class="perf-value">${total_duration}s</div>
                </div>
                <div class="perf-item">
                    <div class="perf-label">Comparison Depths Tested</div>
                    <div class="perf-value">3, 5, 7, 10 levels</div>
                </div>
                <div class="perf-item">
                    <div class="perf-label">File Size Range</div>
                    <div class="perf-value">792KB - 324MB</div>
                </div>
                <div class="perf-item">
                    <div class="perf-label">Test Categories</div>
                    <div class="perf-value">Small, Large, Extreme</div>
                </div>
            </div>
        </div>
        
        <div class="section">
            <div class="section-header">
                <div class="section-title">📊 Detailed Test Results</div>
                <div class="section-subtitle">Complete performance data for all test executions</div>
            </div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Test Name</th>
                            <th>Category</th>
                            <th>File 1 Size</th>
                            <th>File 2 Size</th>
                            <th>Depth</th>
                            <th>Duration (ms)</th>
                            <th>Status</th>
                            <th>Approach</th>
                            <th>Output Size</th>
                            <th>Differences</th>
                            <th>Heap Size</th>
                            <th>Start Time</th>
                        </tr>
                    </thead>
                    <tbody>
EOF

    # Add test results to HTML table
    for result in "${TEST_RESULTS[@]}"; do
        IFS='|' read -r test_name file1 file2 file1_size file2_size file1_human file2_human duration_ms memory_used status approach output_file output_size output_human differences heap_size depth category start_time <<< "$result"
        
        local status_class="status-passed"
        if [[ "$status" == "FAILED" ]]; then
            status_class="status-failed"
        fi
        
        local approach_class="approach-dom"
        if [[ "$approach" == "Streaming" ]]; then
            approach_class="approach-streaming"
        fi
        
        local category_class="category-small"
        if [[ "$category" == "Large Files" ]]; then
            category_class="category-large"
        elif [[ "$category" == "Extreme" ]]; then
            category_class="category-extreme"
        fi
        
        cat >> "$REPORT_FILE" << EOF
                        <tr>
                            <td><strong>$test_name</strong></td>
                            <td><span class="$category_class">$category</span></td>
                            <td class="size-cell">$file1_human</td>
                            <td class="size-cell">$file2_human</td>
                            <td><span class="depth-badge">$depth</span></td>
                            <td class="duration-cell">$duration_ms</td>
                            <td class="$status_class">$status</td>
                            <td><span class="$approach_class">$approach</span></td>
                            <td class="size-cell">$output_human</td>
                            <td>$differences</td>
                            <td>$heap_size</td>
                            <td>$start_time</td>
                        </tr>
EOF
    done
    
    cat >> "$REPORT_FILE" << EOF
                    </tbody>
                </table>
            </div>
        </div>
        
        <div class="section">
            <div class="section-header">
                <div class="section-title">📁 Generated Files</div>
                <div class="section-subtitle">All output files and logs from the test execution</div>
            </div>
            <div class="summary-text">
                <p><strong>📊 Reports:</strong></p>
                <ul>
                    <li><code>complete_performance_report.html</code> - This comprehensive HTML report</li>
                    <li><code>complete_test_results.csv</code> - Raw test data in CSV format</li>
                    <li><code>test_summary.txt</code> - Text summary of results</li>
                </ul>
                <br>
                <p><strong>📋 Test Logs:</strong></p>
                <ul>
                    <li><code>log_*.txt</code> - Individual test execution logs</li>
                    <li><code>memory_*.txt</code> - Memory usage monitoring data</li>
                    <li><code>test_execution.log</code> - Master execution log</li>
                </ul>
                <br>
                <p><strong>📄 Results:</strong></p>
                <ul>
                    <li><code>result_*.xls</code> - XML comparison result files (Excel format)</li>
                    <li><code>result_*.txt</code> - XML comparison result files (Text format)</li>
                </ul>
            </div>
        </div>
        
        <div class="footer">
            <p>🔬 XML Compare Load Testing Suite | All files preserved in <code>$RESULTS_DIR/</code></p>
            <p>For detailed analysis, examine the CSV data and individual log files</p>
        </div>
    </div>
</body>
</html>
EOF

    # Generate text summary
    cat > "$SUMMARY_FILE" << EOF
XML Compare Load Test Summary
=============================
Generated: $(date '+%Y-%m-%d %H:%M:%S')
Platform: $(uname -s) $(uname -m)
Java Version: $(java -version 2>&1 | head -1)

Test Statistics:
- Total Tests: $TOTAL_TESTS
- Passed: $PASSED_TESTS
- Failed: $FAILED_TESTS
- Success Rate: $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%
- DOM Approach Tests: $DOM_TESTS
- Streaming Approach Tests: $STREAMING_TESTS

Performance Summary:
- Total Execution Time: ${total_duration}s
- Average Test Duration: $(echo "scale=2; $total_duration / $TOTAL_TESTS" | bc -l)s
- Java Max Heap: ${java_max_heap}MB

Test Categories:
- Small Files (792KB - 7.8MB): DOM approach expected
- Large Files (87MB): Streaming approach expected  
- Extreme Files (324MB): Streaming approach expected

Comparison Depths Tested:
- 3, 5, 7, 10 levels of XML structure comparison

Files Generated:
- HTML Report: complete_performance_report.html
- CSV Data: complete_test_results.csv
- Test Logs: log_*.txt files
- Memory Logs: memory_*.txt files
- Result Files: result_*.xls and result_*.txt files
- Execution Log: test_execution.log

All files are preserved in: $RESULTS_DIR/
EOF

    print_success "All reports generated successfully!"
    print_info "📊 HTML Report: $REPORT_FILE"
    print_info "📈 CSV Data: $CSV_FILE"
    print_info "📄 Summary: $SUMMARY_FILE"
    print_info "📁 All files: $RESULTS_DIR/"
}

# =============================================================================
# Main Execution Function
# =============================================================================

main() {
    print_header "Complete XML Compare Load Testing Suite"
    
    log_message "Starting complete load testing suite"
    
    # Check prerequisites
    print_info "Checking prerequisites..."
    
    if [[ ! -f "$JAR_FILE" ]]; then
        print_error "JAR file not found: $JAR_FILE"
        print_info "Please run: mvn clean package -DskipTests"
        exit 1
    fi
    print_success "JAR file found: $JAR_FILE"
    
    if [[ ! -d "$TEST_FILES_DIR" ]]; then
        print_error "Test files directory not found: $TEST_FILES_DIR"
        print_info "Please generate test files first"
        exit 1
    fi
    print_success "Test files directory found: $TEST_FILES_DIR"
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    print_success "Results directory ready: $RESULTS_DIR"
    
    # Display environment info
    print_info "Java Version: $(java -version 2>&1 | head -1)"
    print_info "Max Heap: $(check_java_memory)MB"
    print_info "Platform: $(uname -s) $(uname -m)"
    
    # Run test suites
    run_small_file_tests
    run_large_file_tests
    run_extreme_tests
    
    # Generate comprehensive reports
    generate_reports
    
    # Final summary
    print_header "Load Testing Complete! 🎉"
    
    print_success "Execution Summary:"
    echo "  📊 Total Tests: $TOTAL_TESTS"
    echo "  ✅ Passed: $PASSED_TESTS"
    echo "  ❌ Failed: $FAILED_TESTS"
    echo "  📈 Success Rate: $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%"
    echo "  🧠 DOM Tests: $DOM_TESTS"
    echo "  🔄 Streaming Tests: $STREAMING_TESTS"
    echo ""
    print_info "🔍 Open $REPORT_FILE in your browser for detailed analysis"
    print_info "📋 Check $CSV_FILE for raw data analysis"
    print_info "📁 All test artifacts preserved in $RESULTS_DIR/"
    
    log_message "Load testing completed successfully"
}

# =============================================================================
# Script Execution
# =============================================================================

# Ensure we exit cleanly
trap 'print_error "Script interrupted"; exit 130' INT TERM

# Run the main function
main "$@"

exit 0