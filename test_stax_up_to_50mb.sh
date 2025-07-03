#!/bin/bash

# Comprehensive StAX Testing Script - Up to 50MB Files

JAR_FILE="target/xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
TEST_FILES_DIR="test-files-50mb"
RESULTS_DIR="stax-test-results"
REPORT_FILE="$RESULTS_DIR/stax_performance_report.html"
CSV_FILE="$RESULTS_DIR/stax_test_data.csv"

# Colors
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
TOTAL_DURATION=0

# Results array
declare -a RESULTS=()

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
    if [[ -f "$1" ]]; then
        stat -f%z "$1" 2>/dev/null || stat -c%s "$1" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

run_stax_test() {
    local test_name="$1"
    local file1="$2"
    local file2="$3"
    local heap_size="$4"
    local expected_result="$5"  # "identical" or "different"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    print_test "$test_name (Heap: $heap_size)"
    
    if [[ ! -f "$file1" || ! -f "$file2" ]]; then
        print_error "Files not found: $file1 or $file2"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
    
    local file1_size=$(get_file_size "$file1")
    local file2_size=$(get_file_size "$file2")
    local file1_human=$(format_bytes $file1_size)
    local file2_human=$(format_bytes $file2_size)
    
    print_info "File 1: $file1_human, File 2: $file2_human"
    
    local output_file="$RESULTS_DIR/result_${test_name}.txt"
    local log_file="$RESULTS_DIR/log_${test_name}.txt"
    
    # Monitor memory before test
    local mem_before=$(ps -o rss= $$ | tr -d ' ' 2>/dev/null || echo "0")
    
    # Run test with timing
    local start_time=$(date +%s)
    
    if java -Xmx"$heap_size" \
        -XX:+UseG1GC \
        -XX:MaxGCPauseMillis=200 \
        -jar "$JAR_FILE" \
        "$file1" "$file2" "$output_file" \
        > "$log_file" 2>&1; then
        
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        TOTAL_DURATION=$((TOTAL_DURATION + duration))
        
        # Monitor memory after test
        local mem_after=$(ps -o rss= $$ | tr -d ' ' 2>/dev/null || echo "0")
        local mem_used=$((mem_after - mem_before))
        
        # Verify StAX usage
        local stax_confirmed="No"
        if grep -q "StAX streaming comparison" "$log_file" 2>/dev/null; then
            stax_confirmed="Yes"
        fi
        
        # Check result correctness
        local result_correct="Unknown"
        local output_size=0
        local differences_found="None"
        
        if [[ -f "$output_file" ]]; then
            output_size=$(get_file_size "$output_file")
            if [[ $output_size -gt 0 ]]; then
                differences_found="Found"
                if [[ "$expected_result" == "different" ]]; then
                    result_correct="Yes"
                else
                    result_correct="No (unexpected differences)"
                fi
            else
                if [[ "$expected_result" == "identical" ]]; then
                    result_correct="Yes"
                else
                    result_correct="No (expected differences not found)"
                fi
            fi
        else
            if [[ "$expected_result" == "identical" ]]; then
                result_correct="Yes"
            else
                result_correct="No (no output file created)"
            fi
        fi
        
        print_success "Completed in ${duration}s - StAX: $stax_confirmed - Result: $result_correct"
        print_info "Memory used: $(format_bytes $((mem_used * 1024))) - Output: $(format_bytes $output_size)"
        
        if [[ "$result_correct" == "Yes" ]]; then
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            FAILED_TESTS=$((FAILED_TESTS + 1))
            print_error "Test validation failed: $result_correct"
        fi
        
        # Store results
        RESULTS+=("$test_name,$file1,$file2,$file1_size,$file2_size,$duration,$mem_used,PASSED,$stax_confirmed,$result_correct,$heap_size,$output_size,$differences_found")
        
    else
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        print_error "Failed after ${duration}s"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        
        local error_type="Unknown"
        if grep -q -i "outofmemory" "$log_file" 2>/dev/null; then
            error_type="OutOfMemory"
        elif grep -q -i "exception" "$log_file" 2>/dev/null; then
            error_type="Exception"
        fi
        
        print_info "Error type: $error_type"
        
        RESULTS+=("$test_name,$file1,$file2,$file1_size,$file2_size,$duration,0,FAILED,Unknown,No,$heap_size,0,$error_type")
    fi
    
    echo ""
}

generate_test_files() {
    print_header "Generating Test Files up to 50MB"
    
    if [[ ! -d "$TEST_FILES_DIR" ]]; then
        print_info "Generating 50MB test files..."
        ./generate_50mb_test_files.sh
    else
        print_info "Test files directory already exists: $TEST_FILES_DIR"
    fi
    
    # Verify files exist
    local file_count=$(ls "$TEST_FILES_DIR"/*.xml 2>/dev/null | wc -l)
    if [[ $file_count -gt 0 ]]; then
        print_success "Found $file_count test files"
    else
        print_error "No test files found! Running generator..."
        ./generate_50mb_test_files.sh
    fi
}

run_comprehensive_tests() {
    print_header "Comprehensive StAX Testing (Up to 50MB)"
    
    # Test file size categories
    local test_configs=(
        "1MB:1MB:512m"
        "5MB:5MB:512m" 
        "10MB:10MB:1g"
        "15MB:15MB:1g"
        "20MB:20MB:1g"
        "25MB:25MB:2g"
        "30MB:30MB:2g"
        "35MB:35MB:2g"
        "40MB:40MB:2g"
        "45MB:45MB:2g"
        "50MB:50MB:2g"
    )
    
    for config in "${test_configs[@]}"; do
        IFS=':' read -r size_label size_dir heap_size <<< "$config"
        
        echo -e "${YELLOW}--- Testing $size_label Files ---${NC}"
        
        # Test identical files
        local file1="$TEST_FILES_DIR/test_${size_label}_identical_1.xml"
        local file2="$TEST_FILES_DIR/test_${size_label}_identical_2.xml"
        
        if [[ -f "$file1" && -f "$file2" ]]; then
            run_stax_test "${size_label}_identical" "$file1" "$file2" "$heap_size" "identical"
        else
            print_error "Identical files not found for $size_label"
        fi
        
        # Test different files
        local file3="$TEST_FILES_DIR/test_${size_label}_different_1.xml"
        local file4="$TEST_FILES_DIR/test_${size_label}_different_2.xml"
        
        if [[ -f "$file3" && -f "$file4" ]]; then
            run_stax_test "${size_label}_different" "$file3" "$file4" "$heap_size" "different"
        else
            print_error "Different files not found for $size_label"
        fi
    done
}

run_stress_tests() {
    print_header "StAX Stress Testing"
    
    # Test with limited memory
    echo -e "${YELLOW}--- Memory Stress Tests ---${NC}"
    
    local stress_configs=(
        "10MB:256m:Memory_Stress_256m"
        "25MB:512m:Memory_Stress_512m" 
        "50MB:1g:Memory_Stress_1g"
    )
    
    for config in "${stress_configs[@]}"; do
        IFS=':' read -r size heap test_name <<< "$config"
        
        local file1="$TEST_FILES_DIR/test_${size}_identical_1.xml"
        local file2="$TEST_FILES_DIR/test_${size}_identical_2.xml"
        
        if [[ -f "$file1" && -f "$file2" ]]; then
            run_stax_test "$test_name" "$file1" "$file2" "$heap" "identical"
        fi
    done
}

generate_report() {
    print_header "Generating Comprehensive Report"
    
    # Create CSV
    echo "test_name,file1,file2,file1_size,file2_size,duration_sec,memory_used_kb,status,stax_confirmed,result_correct,heap_size,output_size,differences" > "$CSV_FILE"
    
    for result in "${RESULTS[@]}"; do
        echo "$result" >> "$CSV_FILE"
    done
    
    # Calculate statistics
    local avg_duration=0
    if [[ $TOTAL_TESTS -gt 0 ]]; then
        avg_duration=$(echo "scale=2; $TOTAL_DURATION / $TOTAL_TESTS" | bc -l)
    fi
    
    local success_rate=0
    if [[ $TOTAL_TESTS -gt 0 ]]; then
        success_rate=$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)
    fi
    
    # Generate HTML report
    cat > "$REPORT_FILE" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>StAX Performance Test Report (Up to 50MB)</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 1400px; margin: 0 auto; background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
        .header { text-align: center; border-bottom: 3px solid #007acc; padding-bottom: 20px; margin-bottom: 30px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 8px; padding: 30px; }
        .header h1 { font-size: 2.5em; margin-bottom: 10px; }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 20px; margin: 30px 0; }
        .stat-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px; text-align: center; }
        .stat-number { font-size: 2.2em; font-weight: bold; margin-bottom: 5px; }
        .stat-label { font-size: 0.9em; opacity: 0.9; }
        .section { background: white; margin: 20px 0; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .section-header { background: #f8f9fa; padding: 20px; border-bottom: 1px solid #e9ecef; }
        .section-title { font-size: 1.4em; color: #333; margin-bottom: 5px; }
        table { width: 100%; border-collapse: collapse; }
        th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #e9ecef; }
        th { background: #667eea; color: white; font-weight: 600; position: sticky; top: 0; }
        .status-passed { color: #28a745; font-weight: bold; }
        .status-failed { color: #dc3545; font-weight: bold; }
        .stax-yes { background: #d4edda; color: #155724; padding: 4px 8px; border-radius: 15px; font-size: 0.85em; }
        .stax-no { background: #f8d7da; color: #721c24; padding: 4px 8px; border-radius: 15px; font-size: 0.85em; }
        .result-yes { background: #d1ecf1; color: #0c5460; padding: 4px 8px; border-radius: 15px; font-size: 0.85em; }
        .result-no { background: #f8d7da; color: #721c24; padding: 4px 8px; border-radius: 15px; font-size: 0.85em; }
        .size-cell, .duration-cell { text-align: right; font-family: 'Courier New', monospace; }
        tr:hover { background: #f8f9fa; }
        .performance-chart { margin: 20px 0; padding: 20px; background: #f8f9fa; border-radius: 8px; }
        .footer { text-align: center; margin-top: 40px; padding: 20px; color: #666; border-top: 1px solid #e9ecef; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 StAX Performance Test Report</h1>
            <p>Comprehensive XML Comparison Testing (Up to 50MB Files)</p>
            <p>Generated: $(date) | Platform: $(uname -s) $(uname -m)</p>
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
                <div class="stat-number">$success_rate%</div>
                <div class="stat-label">Success Rate</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${avg_duration}s</div>
                <div class="stat-label">Avg Duration</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${TOTAL_DURATION}s</div>
                <div class="stat-label">Total Duration</div>
            </div>
        </div>
        
        <div class="section">
            <div class="section-header">
                <div class="section-title">📊 Detailed Test Results</div>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>Test Name</th>
                        <th>File 1 Size</th>
                        <th>File 2 Size</th>
                        <th>Duration (s)</th>
                        <th>Memory (KB)</th>
                        <th>Status</th>
                        <th>StAX Used</th>
                        <th>Result Correct</th>
                        <th>Heap Size</th>
                        <th>Output Size</th>
                    </tr>
                </thead>
                <tbody>
EOF

    # Add test results to HTML
    for result in "${RESULTS[@]}"; do
        IFS=',' read -r test_name file1 file2 file1_size file2_size duration memory status stax_confirmed result_correct heap output_size differences <<< "$result"
        
        local file1_human=$(format_bytes $file1_size)
        local file2_human=$(format_bytes $file2_size)
        local output_human=$(format_bytes $output_size)
        local memory_human=$(format_bytes $((memory * 1024)))
        
        local status_class="status-passed"
        if [[ "$status" == "FAILED" ]]; then
            status_class="status-failed"
        fi
        
        local stax_class="stax-yes"
        if [[ "$stax_confirmed" != "Yes" ]]; then
            stax_class="stax-no"
        fi
        
        local result_class="result-yes"
        if [[ "$result_correct" != "Yes" ]]; then
            result_class="result-no"
        fi
        
        cat >> "$REPORT_FILE" << EOF
                    <tr>
                        <td><strong>$test_name</strong></td>
                        <td class="size-cell">$file1_human</td>
                        <td class="size-cell">$file2_human</td>
                        <td class="duration-cell">$duration</td>
                        <td class="size-cell">$memory_human</td>
                        <td class="$status_class">$status</td>
                        <td><span class="$stax_class">$stax_confirmed</span></td>
                        <td><span class="$result_class">$result_correct</span></td>
                        <td>$heap</td>
                        <td class="size-cell">$output_human</td>
                    </tr>
EOF
    done
    
    cat >> "$REPORT_FILE" << EOF
                </tbody>
            </table>
        </div>
        
        <div class="section">
            <div class="section-header">
                <div class="section-title">📁 Generated Files</div>
            </div>
            <div style="padding: 20px;">
                <ul>
                    <li><strong>CSV Data:</strong> stax_test_data.csv</li>
                    <li><strong>Test Logs:</strong> log_*.txt files</li>
                    <li><strong>Result Files:</strong> result_*.txt files</li>
                    <li><strong>Test Files:</strong> $TEST_FILES_DIR/ directory</li>
                </ul>
            </div>
        </div>
        
        <div class="footer">
            <p>🔬 StAX XML Comparison Testing Suite | All files preserved in $RESULTS_DIR/</p>
            <p>Java Version: $(java -version 2>&1 | head -1)</p>
        </div>
    </div>
</body>
</html>
EOF

    print_success "Report generated: $REPORT_FILE"
    print_info "CSV data: $CSV_FILE"
}

main() {
    print_header "StAX XML Comparison Testing (Up to 50MB)"
    
    # Check prerequisites
    if [[ ! -f "$JAR_FILE" ]]; then
        print_error "JAR file not found: $JAR_FILE"
        print_info "Please run: mvn clean package -DskipTests"
        exit 1
    fi
    
    print_info "JAR file: $JAR_FILE"
    print_info "Java version: $(java -version 2>&1 | head -1)"
    print_info "Platform: $(uname -s) $(uname -m)"
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    
    # Generate test files
    generate_test_files
    
    # Run comprehensive tests
    run_comprehensive_tests
    
    # Run stress tests
    run_stress_tests
    
    # Generate report
    generate_report
    
    # Final summary
    print_header "StAX Testing Complete! 🎉"
    
    print_success "Final Results:"
    echo "  📊 Total Tests: $TOTAL_TESTS"
    echo "  ✅ Passed: $PASSED_TESTS"
    echo "  ❌ Failed: $FAILED_TESTS"
    echo "  📈 Success Rate: $success_rate%"
    echo "  ⏱️  Total Duration: ${TOTAL_DURATION}s"
    echo ""
    print_info "📊 HTML Report: $REPORT_FILE"
    print_info "📈 CSV Data: $CSV_FILE"
    print_info "📁 All files: $RESULTS_DIR/"
    echo ""
    print_success "Open $REPORT_FILE in your browser for detailed analysis!"
}

# Run main function
main "$@"