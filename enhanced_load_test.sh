#!/bin/bash

# =============================================================================
# Enhanced XML Compare Load Testing Script with Performance Monitoring
# =============================================================================
# This script runs comprehensive load tests with detailed performance metrics
# including memory usage, CPU usage, and timing for each comparison.
# =============================================================================

set -e  # Exit on any error

# Configuration
JAR_FILE="target/xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
RESULTS_DIR="enhanced-test-results"
TEST_FILES_DIR="test-files"
LARGE_FILES_DIR="test-files-large"
REPORT_FILE="$RESULTS_DIR/detailed_performance_report.html"
CSV_REPORT="$RESULTS_DIR/performance_data.csv"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test results array
declare -a TEST_RESULTS=()

# =============================================================================
# Utility Functions
# =============================================================================

print_header() {
    echo -e "\n${CYAN}================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}================================${NC}\n"
}

print_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_info() {
    echo -e "${PURPLE}[INFO]${NC} $1"
}

format_bytes() {
    local bytes=$1
    if [[ $bytes -lt 1024 ]]; then
        echo "${bytes}B"
    elif [[ $bytes -lt 1048576 ]]; then
        echo "$(echo "scale=1; $bytes/1024" | bc)KB"
    elif [[ $bytes -lt 1073741824 ]]; then
        echo "$(echo "scale=1; $bytes/1048576" | bc)MB"
    else
        echo "$(echo "scale=1; $bytes/1073741824" | bc)GB"
    fi
}

get_file_size_bytes() {
    local file="$1"
    if [[ -f "$file" ]]; then
        stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

monitor_process() {
    local pid=$1
    local output_file=$2
    
    # Initialize monitoring data
    local max_memory=0
    local max_cpu=0
    local memory_samples=0
    local cpu_samples=0
    
    echo "timestamp,memory_mb,cpu_percent" > "$output_file"
    
    while kill -0 "$pid" 2>/dev/null; do
        # Get memory usage in KB, convert to MB
        local memory_kb=$(ps -o rss= -p "$pid" 2>/dev/null | tr -d ' ' || echo "0")
        local memory_mb=$(echo "scale=2; $memory_kb/1024" | bc -l)
        
        # Get CPU usage percentage
        local cpu_percent=$(ps -o pcpu= -p "$pid" 2>/dev/null | tr -d ' ' || echo "0")
        
        # Update maximums
        if (( $(echo "$memory_mb > $max_memory" | bc -l) )); then
            max_memory=$memory_mb
        fi
        if (( $(echo "$cpu_percent > $max_cpu" | bc -l) )); then
            max_cpu=$cpu_percent
        fi
        
        # Record sample
        echo "$(date +%s),$memory_mb,$cpu_percent" >> "$output_file"
        
        memory_samples=$((memory_samples + 1))
        cpu_samples=$((cpu_samples + 1))
        
        sleep 0.5
    done
    
    # Return max values
    echo "$max_memory:$max_cpu"
}

run_enhanced_comparison() {
    local file1="$1"
    local file2="$2"
    local output_file="$3"
    local test_name="$4"
    local heap_size="${5:-1g}"
    
    print_test "Running: $test_name"
    
    # Get file information
    local file1_size=$(get_file_size_bytes "$file1")
    local file2_size=$(get_file_size_bytes "$file2")
    local file1_size_human=$(format_bytes $file1_size)
    local file2_size_human=$(format_bytes $file2_size)
    
    print_info "File 1: $file1_size_human - $(basename "$file1")"
    print_info "File 2: $file2_size_human - $(basename "$file2")"
    print_info "Heap Size: $heap_size"
    
    # Prepare monitoring files
    local monitor_file="$RESULTS_DIR/${test_name}_monitor.csv"
    local test_log="$RESULTS_DIR/${test_name}_test.log"
    
    # Start the Java process in background and capture PID
    local start_time=$(date +%s.%N)
    
    java -Xmx"$heap_size" -jar "$JAR_FILE" "$file1" "$file2" "$output_file" \
         > "$test_log" 2>&1 &
    local java_pid=$!
    
    # Monitor the process
    local max_stats=$(monitor_process $java_pid "$monitor_file")
    
    # Wait for Java process to complete
    wait $java_pid
    local exit_code=$?
    
    local end_time=$(date +%s.%N)
    local duration=$(echo "$end_time - $start_time" | bc -l)
    
    # Parse monitoring results
    IFS=':' read -r max_memory max_cpu <<< "$max_stats"
    
    # Check results
    local status="FAILED"
    local output_size=0
    local output_size_human="N/A"
    local approach="Unknown"
    local differences_found=0
    
    if [[ $exit_code -eq 0 ]]; then
        status="PASSED"
        
        # Check if output file was created and get its size
        if [[ -f "$output_file" ]]; then
            output_size=$(get_file_size_bytes "$output_file")
            output_size_human=$(format_bytes $output_size)
            
            # Try to count differences from output file
            if [[ "$output_file" == *.txt ]]; then
                differences_found=$(grep -c "Text content mismatch\|Element name mismatch\|Attribute.*mismatch" "$output_file" 2>/dev/null || echo "0")
            elif [[ -f "$output_file" ]]; then
                differences_found="Unknown"
            fi
        else
            output_size_human="No differences"
            differences_found=0
        fi
        
        # Determine approach used
        if grep -q "Large files detected" "$test_log" 2>/dev/null; then
            approach="Streaming"
        else
            approach="DOM"
        fi
        
        print_success "Completed in ${duration}s - $approach approach"
        print_info "Peak Memory: ${max_memory}MB, Peak CPU: ${max_cpu}%"
        print_info "Output: $output_size_human"
    else
        print_error "Failed with exit code $exit_code"
        print_info "Peak Memory: ${max_memory}MB, Peak CPU: ${max_cpu}%"
        
        # Check for specific errors
        if grep -q -i "outofmemory" "$test_log" 2>/dev/null; then
            approach="DOM (OOM)"
        fi
    fi
    
    # Store results for reporting
    TEST_RESULTS+=("$test_name|$file1|$file2|$file1_size|$file2_size|$file1_size_human|$file2_size_human|$duration|$max_memory|$max_cpu|$status|$approach|$output_file|$output_size|$output_size_human|$differences_found|$heap_size")
    
    echo ""
    return $exit_code
}

# =============================================================================
# Test Execution Functions
# =============================================================================

run_all_tests() {
    print_header "Starting Enhanced Load Tests"
    
    # Small file tests (DOM approach expected)
    local small_tests=(
        "test_1MB_identical_1.xml:test_1MB_identical_2.xml:1MB_identical:512m"
        "test_1MB_different_1.xml:test_1MB_different_2.xml:1MB_different:512m"
        "test_3MB_identical_1.xml:test_3MB_identical_2.xml:3MB_identical:512m"
        "test_3MB_different_1.xml:test_3MB_different_2.xml:3MB_different:512m"
        "test_6MB_identical_1.xml:test_6MB_identical_2.xml:6MB_identical:1g"
        "test_6MB_different_1.xml:test_6MB_different_2.xml:6MB_different:1g"
        "test_10MB_identical_1.xml:test_10MB_identical_2.xml:10MB_identical:1g"
        "test_10MB_different_1.xml:test_10MB_different_2.xml:10MB_different:1g"
    )
    
    print_header "Small File Tests (DOM Approach Expected)"
    for test_config in "${small_tests[@]}"; do
        IFS=':' read -r file1_name file2_name test_name heap_size <<< "$test_config"
        
        local file1="$TEST_FILES_DIR/$file1_name"
        local file2="$TEST_FILES_DIR/$file2_name"
        local output="$RESULTS_DIR/result_${test_name}.xls"
        
        if [[ -f "$file1" && -f "$file2" ]]; then
            run_enhanced_comparison "$file1" "$file2" "$output" "$test_name" "$heap_size"
        else
            print_error "Files not found for $test_name test"
            echo ""
        fi
    done
    
    # Large file tests (Streaming approach expected)
    if [[ -d "$LARGE_FILES_DIR" ]]; then
        print_header "Large File Tests (Streaming Approach Expected)"
        
        local large_tests=(
            "large_60MB.xml:large_60MB_identical.xml:87MB_identical:1g"
            "large_60MB.xml:large_60MB_modified.xml:87MB_different:1g"
            "massive_test.xml:massive_test_copy.xml:324MB_identical_1g:1g"
            "massive_test.xml:massive_test_copy.xml:324MB_identical_512m:512m"
        )
        
        for test_config in "${large_tests[@]}"; do
            IFS=':' read -r file1_name file2_name test_name heap_size <<< "$test_config"
            
            local file1="$LARGE_FILES_DIR/$file1_name"
            local file2="$LARGE_FILES_DIR/$file2_name"
            local output="$RESULTS_DIR/result_${test_name}.txt"
            
            if [[ -f "$file1" && -f "$file2" ]]; then
                run_enhanced_comparison "$file1" "$file2" "$output" "$test_name" "$heap_size"
            else
                print_error "Files not found for $test_name test"
                echo ""
            fi
        done
    else
        print_error "Large files directory not found - skipping large file tests"
    fi
}

generate_detailed_report() {
    print_header "Generating Detailed Performance Report"
    
    # Create CSV header
    echo "test_name,file1_path,file2_path,file1_size_bytes,file2_size_bytes,file1_size_human,file2_size_human,duration_seconds,peak_memory_mb,peak_cpu_percent,status,approach,output_file,output_size_bytes,output_size_human,differences_found,heap_size" > "$CSV_REPORT"
    
    # Create HTML report
    cat > "$REPORT_FILE" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>XML Compare Performance Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f0f0f0; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
        .summary { background: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0; }
        .error { background: #ffebee; padding: 15px; border-radius: 5px; margin: 20px 0; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .passed { color: green; font-weight: bold; }
        .failed { color: red; font-weight: bold; }
        .duration { text-align: right; }
        .size { text-align: right; }
        .memory { text-align: right; }
        .cpu { text-align: right; }
        .chart { margin: 20px 0; }
    </style>
</head>
<body>
    <div class="header">
        <h1>XML Compare Performance Test Report</h1>
        <p>Generated: $(date)</p>
        <p>Platform: $(uname -s) $(uname -m)</p>
        <p>Java Version: $(java -version 2>&1 | head -1)</p>
    </div>
EOF
    
    # Process results and add to reports
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    local total_duration=0
    
    for result in "${TEST_RESULTS[@]}"; do
        IFS='|' read -r test_name file1 file2 file1_size file2_size file1_size_human file2_size_human duration max_memory max_cpu status approach output_file output_size output_size_human differences_found heap_size <<< "$result"
        
        # Add to CSV
        echo "$test_name,$file1,$file2,$file1_size,$file2_size,$file1_size_human,$file2_size_human,$duration,$max_memory,$max_cpu,$status,$approach,$output_file,$output_size,$output_size_human,$differences_found,$heap_size" >> "$CSV_REPORT"
        
        total_tests=$((total_tests + 1))
        if [[ "$status" == "PASSED" ]]; then
            passed_tests=$((passed_tests + 1))
        else
            failed_tests=$((failed_tests + 1))
        fi
        
        total_duration=$(echo "$total_duration + $duration" | bc -l)
    done
    
    # Add summary to HTML
    cat >> "$REPORT_FILE" << EOF
    <div class="summary">
        <h2>Test Summary</h2>
        <p><strong>Total Tests:</strong> $total_tests</p>
        <p><strong>Passed:</strong> $passed_tests</p>
        <p><strong>Failed:</strong> $failed_tests</p>
        <p><strong>Success Rate:</strong> $(echo "scale=1; $passed_tests * 100 / $total_tests" | bc -l)%</p>
        <p><strong>Total Duration:</strong> $(printf "%.2f" $total_duration)s</p>
    </div>
    
    <h2>Detailed Results</h2>
    <table>
        <tr>
            <th>Test Name</th>
            <th>File 1 Size</th>
            <th>File 2 Size</th>
            <th>Duration (s)</th>
            <th>Peak Memory (MB)</th>
            <th>Peak CPU (%)</th>
            <th>Approach</th>
            <th>Status</th>
            <th>Output Size</th>
            <th>Differences</th>
            <th>Heap Size</th>
        </tr>
EOF
    
    # Add each test result to HTML table
    for result in "${TEST_RESULTS[@]}"; do
        IFS='|' read -r test_name file1 file2 file1_size file2_size file1_size_human file2_size_human duration max_memory max_cpu status approach output_file output_size output_size_human differences_found heap_size <<< "$result"
        
        local status_class="passed"
        if [[ "$status" == "FAILED" ]]; then
            status_class="failed"
        fi
        
        cat >> "$REPORT_FILE" << EOF
        <tr>
            <td>$test_name</td>
            <td class="size">$file1_size_human</td>
            <td class="size">$file2_size_human</td>
            <td class="duration">$(printf "%.3f" $duration)</td>
            <td class="memory">$(printf "%.1f" $max_memory)</td>
            <td class="cpu">$(printf "%.1f" $max_cpu)</td>
            <td>$approach</td>
            <td class="$status_class">$status</td>
            <td class="size">$output_size_human</td>
            <td>$differences_found</td>
            <td>$heap_size</td>
        </tr>
EOF
    done
    
    # Close HTML
    cat >> "$REPORT_FILE" << 'EOF'
    </table>
    
    <h2>Files Generated</h2>
    <ul>
        <li><strong>CSV Data:</strong> performance_data.csv</li>
        <li><strong>Test Logs:</strong> *_test.log files</li>
        <li><strong>Performance Monitoring:</strong> *_monitor.csv files</li>
        <li><strong>Comparison Results:</strong> result_*.xls and result_*.txt files</li>
    </ul>
    
</body>
</html>
EOF
    
    print_success "Reports generated:"
    print_info "📊 HTML Report: $REPORT_FILE"
    print_info "📈 CSV Data: $CSV_REPORT"
    print_info "📁 All files in: $RESULTS_DIR"
    
    # Display summary
    echo ""
    print_info "📋 Summary:"
    echo "   Total Tests: $total_tests"
    echo "   Passed: $passed_tests"
    echo "   Failed: $failed_tests"
    echo "   Success Rate: $(echo "scale=1; $passed_tests * 100 / $total_tests" | bc -l)%"
    echo "   Total Duration: $(printf "%.2f" $total_duration)s"
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    print_header "Enhanced XML Compare Load Testing with Performance Monitoring"
    
    # Check prerequisites
    if [[ ! -f "$JAR_FILE" ]]; then
        print_error "JAR file not found: $JAR_FILE"
        print_info "Please run: mvn clean package -DskipTests"
        exit 1
    fi
    
    if [[ ! -d "$TEST_FILES_DIR" ]]; then
        print_error "Test files directory not found: $TEST_FILES_DIR"
        exit 1
    fi
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    
    # Run tests
    run_all_tests
    
    # Generate reports
    generate_detailed_report
    
    print_header "Load Testing Complete! 🎉"
}

# Execute main function
main "$@"