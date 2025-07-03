#!/bin/bash

# =============================================================================
# XML Compare Comprehensive Load Testing Script
# =============================================================================
# This script runs comprehensive load tests on the XML comparison application
# using various file sizes to test both DOM and Streaming approaches.
# =============================================================================

set -e  # Exit on any error

# Configuration
JAR_FILE="target/xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
RESULTS_DIR="load-test-results"
TEST_FILES_DIR="test-files"
LARGE_FILES_DIR="test-files-large"
LOG_FILE="$RESULTS_DIR/comprehensive_test.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# =============================================================================
# Utility Functions
# =============================================================================

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

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

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_info() {
    echo -e "${PURPLE}[INFO]${NC} $1"
}

format_duration() {
    local duration=$1
    if [[ -z "$duration" ]]; then
        echo "0.00s"
        return
    fi
    
    # Convert to integer for comparison (multiply by 100 to keep 2 decimal places)
    local duration_int=$(echo "$duration * 100" | bc -l | cut -d. -f1)
    
    if [[ $duration_int -lt 6000 ]]; then  # less than 60 seconds
        printf "%.2fs" "$duration"
    else
        local minutes=$(echo "$duration / 60" | bc -l)
        local seconds=$(echo "$duration % 60" | bc -l)
        printf "%.0fm %.2fs" "$minutes" "$seconds"
    fi
}

check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check if JAR file exists
    if [[ ! -f "$JAR_FILE" ]]; then
        print_error "JAR file not found: $JAR_FILE"
        print_info "Please run: mvn clean package -DskipTests"
        exit 1
    fi
    print_success "JAR file found: $JAR_FILE"
    
    # Check if test directories exist
    if [[ ! -d "$TEST_FILES_DIR" ]]; then
        print_error "Test files directory not found: $TEST_FILES_DIR"
        print_info "Please run the test file generator first"
        exit 1
    fi
    print_success "Small test files directory found: $TEST_FILES_DIR"
    
    if [[ ! -d "$LARGE_FILES_DIR" ]]; then
        print_warning "Large test files directory not found: $LARGE_FILES_DIR"
        print_info "Large file tests will be skipped"
    else
        print_success "Large test files directory found: $LARGE_FILES_DIR"
    fi
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    print_success "Results directory ready: $RESULTS_DIR"
    
    # Check Java version and memory
    echo ""
    print_info "Java Environment:"
    java -version 2>&1 | head -1
    local max_heap=$(java -XX:+PrintFlagsFinal -version 2>&1 | grep MaxHeapSize | awk '{print int($4/1024/1024) " MB"}')
    print_info "Maximum heap size: $max_heap"
}

run_xml_comparison() {
    local file1="$1"
    local file2="$2"
    local output_file="$3"
    local test_name="$4"
    local heap_size="${5:-2g}"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    print_test "Running: $test_name"
    print_info "File 1: $(ls -lh "$file1" 2>/dev/null | awk '{print $5}') - $(basename "$file1")"
    print_info "File 2: $(ls -lh "$file2" 2>/dev/null | awk '{print $5}') - $(basename "$file2")"
    print_info "Output: $output_file"
    print_info "Heap Size: $heap_size"
    
    # Prepare log file for this test
    local test_log="$RESULTS_DIR/${test_name}_test.log"
    
    # Run the comparison with timing
    local start_time=$(date +%s.%N)
    
    if java -Xmx"$heap_size" -Xlog:gc \
       -jar "$JAR_FILE" "$file1" "$file2" "$output_file" \
       > "$test_log" 2>&1; then
        
        local end_time=$(date +%s.%N)
        local duration=$(echo "$end_time - $start_time" | bc -l)
        
        # Check if output file was created
        if [[ -f "$output_file" ]]; then
            local output_size=$(ls -lh "$output_file" | awk '{print $5}')
            print_success "Test completed in $(format_duration $duration) - Output: $output_size"
        else
            print_success "Test completed in $(format_duration $duration) - Files identical (no output file)"
        fi
        
        # Check which approach was used
        if grep -q "Large files detected" "$test_log" 2>/dev/null; then
            print_info "🔄 Streaming approach used"
        else
            print_info "🧠 DOM approach used"
        fi
        
        # Check for GC activity
        local gc_count=$(grep -c "GC" "$test_log" 2>/dev/null || echo "0")
        if [[ $gc_count -gt 0 ]]; then
            print_info "🗑️  Garbage collections: $gc_count"
        fi
        
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log "PASS: $test_name - Duration: $(format_duration $duration)"
        
        return 0
    else
        local end_time=$(date +%s.%N)
        local duration=$(echo "$end_time - $start_time" | bc -l)
        
        print_error "Test failed in $(format_duration $duration)"
        
        # Check for specific error types
        if grep -q -i "outofmemory" "$test_log" 2>/dev/null; then
            print_error "💥 Out of Memory Error detected"
        fi
        if grep -q -i "exception" "$test_log" 2>/dev/null; then
            print_error "⚠️  Exception occurred - check $test_log"
        fi
        
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "FAIL: $test_name - Duration: $(format_duration $duration) - Check $test_log"
        
        return 1
    fi
}

# =============================================================================
# Test Suites
# =============================================================================

run_small_file_tests() {
    print_header "Small File Tests (DOM Approach)"
    
    local sizes=("1MB" "3MB" "6MB" "10MB")
    local types=("identical" "different")
    
    for size in "${sizes[@]}"; do
        for type in "${types[@]}"; do
            local file1="$TEST_FILES_DIR/test_${size}_${type}_1.xml"
            local file2="$TEST_FILES_DIR/test_${size}_${type}_2.xml"
            local output="$RESULTS_DIR/result_${size}_${type}.xls"
            
            if [[ -f "$file1" && -f "$file2" ]]; then
                run_xml_comparison "$file1" "$file2" "$output" "${size}_${type}" "1g"
                echo ""
            else
                print_warning "Skipping $size $type test - files not found"
                echo ""
            fi
        done
    done
}

run_large_file_tests() {
    if [[ ! -d "$LARGE_FILES_DIR" ]]; then
        print_warning "Skipping large file tests - directory not found"
        return
    fi
    
    print_header "Large File Tests (Streaming Approach)"
    
    # Test with 87MB files
    local large_files=(
        "large_60MB.xml:large_60MB_identical.xml:87MB_identical"
        "large_60MB.xml:large_60MB_modified.xml:87MB_different"
    )
    
    for test_config in "${large_files[@]}"; do
        IFS=':' read -r file1_name file2_name test_name <<< "$test_config"
        
        local file1="$LARGE_FILES_DIR/$file1_name"
        local file2="$LARGE_FILES_DIR/$file2_name"
        local output="$RESULTS_DIR/result_${test_name}.txt"
        
        if [[ -f "$file1" && -f "$file2" ]]; then
            run_xml_comparison "$file1" "$file2" "$output" "$test_name" "1g"
            echo ""
        else
            print_warning "Skipping $test_name test - files not found"
            echo ""
        fi
    done
}

run_extreme_tests() {
    if [[ ! -d "$LARGE_FILES_DIR" ]]; then
        print_warning "Skipping extreme tests - directory not found"
        return
    fi
    
    print_header "Extreme Load Tests"
    
    # Test with 324MB files
    local file1="$LARGE_FILES_DIR/massive_test.xml"
    local file2="$LARGE_FILES_DIR/massive_test_copy.xml"
    
    if [[ -f "$file1" && -f "$file2" ]]; then
        print_test "Testing with 324MB files and limited heap (512MB)"
        local output="$RESULTS_DIR/result_extreme_324MB.txt"
        run_xml_comparison "$file1" "$file2" "$output" "extreme_324MB_512heap" "512m"
        echo ""
        
        print_test "Testing with 324MB files and comfortable heap (2GB)"
        local output2="$RESULTS_DIR/result_extreme_324MB_2g.txt"
        run_xml_comparison "$file1" "$file2" "$output2" "extreme_324MB_2gheap" "2g"
        echo ""
    else
        print_warning "Skipping extreme tests - 324MB files not found"
        print_info "Run generate_very_large_files.sh to create extreme test files"
        echo ""
    fi
}

run_stress_tests() {
    print_header "Stress Tests (Multiple Heap Sizes)"
    
    # Use medium-sized files for stress testing
    local file1="$TEST_FILES_DIR/test_10MB_identical_1.xml"
    local file2="$TEST_FILES_DIR/test_10MB_identical_2.xml"
    
    if [[ ! -f "$file1" || ! -f "$file2" ]]; then
        print_warning "Skipping stress tests - 10MB test files not found"
        return
    fi
    
    local heap_sizes=("256m" "512m" "1g" "2g")
    
    for heap in "${heap_sizes[@]}"; do
        local output="$RESULTS_DIR/result_stress_${heap}.xls"
        run_xml_comparison "$file1" "$file2" "$output" "stress_10MB_${heap}" "$heap"
        echo ""
    done
}

generate_summary_report() {
    print_header "Test Summary Report"
    
    local report_file="$RESULTS_DIR/test_summary_report.txt"
    
    {
        echo "XML Compare Load Test Summary Report"
        echo "Generated: $(date)"
        echo "=================================="
        echo ""
        echo "Test Statistics:"
        echo "  Total Tests: $TOTAL_TESTS"
        echo "  Passed: $PASSED_TESTS"
        echo "  Failed: $FAILED_TESTS"
        echo "  Success Rate: $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%"
        echo ""
        echo "Environment:"
        echo "  Java Version: $(java -version 2>&1 | head -1)"
        echo "  Max Heap: $(java -XX:+PrintFlagsFinal -version 2>&1 | grep MaxHeapSize | awk '{print int($4/1024/1024) " MB"}')"
        echo "  Platform: $(uname -s) $(uname -m)"
        echo ""
        echo "Generated Files:"
        echo "=================="
        find "$RESULTS_DIR" -name "result_*" -type f | while read -r file; do
            echo "  $(basename "$file"): $(ls -lh "$file" | awk '{print $5}')"
        done
        echo ""
        echo "Test Logs:"
        echo "=========="
        find "$RESULTS_DIR" -name "*_test.log" -type f | while read -r file; do
            echo "  $(basename "$file")"
        done
        echo ""
        echo "Detailed Results:"
        echo "================"
        cat "$LOG_FILE" 2>/dev/null || echo "No detailed log available"
        
    } > "$report_file"
    
    print_success "Summary report generated: $report_file"
    
    # Display summary to console
    echo ""
    print_info "Final Results:"
    echo "  📊 Total Tests: $TOTAL_TESTS"
    echo "  ✅ Passed: $PASSED_TESTS"
    echo "  ❌ Failed: $FAILED_TESTS"
    echo "  📈 Success Rate: $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%"
    echo ""
    
    if [[ $FAILED_TESTS -gt 0 ]]; then
        print_warning "Some tests failed. Check logs in $RESULTS_DIR for details."
    else
        print_success "All tests passed! 🎉"
    fi
    
    echo ""
    print_info "All results and logs are available in: $RESULTS_DIR"
    print_info "Summary report: $report_file"
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    print_header "XML Compare Comprehensive Load Testing"
    
    # Initialize log
    log "Starting comprehensive load testing"
    
    # Check prerequisites
    check_prerequisites
    
    # Run test suites
    run_small_file_tests
    run_large_file_tests
    run_extreme_tests
    run_stress_tests
    
    # Generate final report
    generate_summary_report
    
    log "Load testing completed"
}

# Execute main function
main "$@"