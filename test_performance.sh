#!/bin/bash

echo "=== XML Comparison Performance Test ==="
echo "Testing large file handling with different approaches"
echo ""

JAR_FILE="target/xml-compare-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
TEST_DIR="test-files"
RESULTS_DIR="performance-results"

# Create results directory
mkdir -p "$RESULTS_DIR"

# Test configurations
declare -a SIZES=("1MB" "3MB" "6MB" "10MB")
declare -a TEST_TYPES=("identical" "different")

echo "Java Version:"
java -version
echo ""

echo "JVM Memory Settings:"
echo "  Max Heap: $(java -XX:+PrintFlagsFinal -version 2>&1 | grep MaxHeapSize | awk '{print $4/1024/1024 " MB"}')"
echo ""

# Function to run test and measure performance
run_test() {
    local size=$1
    local type=$2
    local file1="${TEST_DIR}/test_${size}_${type}_1.xml"
    local file2="${TEST_DIR}/test_${size}_${type}_2.xml"
    local output="${RESULTS_DIR}/result_${size}_${type}.xls"
    
    echo "Testing ${size} ${type} files..."
    echo "  File 1: $(ls -lh "$file1" | awk '{print $5}')"
    echo "  File 2: $(ls -lh "$file2" | awk '{print $5}')"
    
    # Measure time and memory
    start_time=$(date +%s.%N)
    
    # Run with memory monitoring
    java -Xmx2g -XX:+PrintGCDetails -XX:+PrintGCTimeStamps \
         -jar "$JAR_FILE" "$file1" "$file2" "$output" 2>&1 | \
         grep -E "(GC|Full GC|Exception|Error|OutOfMemory)" > "${RESULTS_DIR}/gc_${size}_${type}.log"
    
    exit_code=$?
    end_time=$(date +%s.%N)
    duration=$(echo "$end_time - $start_time" | bc -l)
    
    if [ $exit_code -eq 0 ]; then
        result_size=$(ls -lh "$output" 2>/dev/null | awk '{print $5}' || echo "N/A")
        echo "  ✅ SUCCESS - Duration: ${duration}s, Result: ${result_size}"
        
        # Check if streaming was used
        if grep -q "Large files detected" "${RESULTS_DIR}/gc_${size}_${type}.log" 2>/dev/null; then
            echo "  📡 Streaming approach was used"
        else
            echo "  🧠 DOM approach was used"
        fi
    else
        echo "  ❌ FAILED - Duration: ${duration}s, Exit code: ${exit_code}"
        
        # Check for specific error types
        if grep -q "OutOfMemoryError" "${RESULTS_DIR}/gc_${size}_${type}.log" 2>/dev/null; then
            echo "  💥 Out of Memory Error detected"
        fi
        if grep -q "Exception" "${RESULTS_DIR}/gc_${size}_${type}.log" 2>/dev/null; then
            echo "  ⚠️  Exception occurred"
        fi
    fi
    
    # Show GC stats if available
    gc_count=$(grep -c "GC" "${RESULTS_DIR}/gc_${size}_${type}.log" 2>/dev/null || echo "0")
    if [ "$gc_count" -gt 0 ]; then
        echo "  🗑️  Garbage Collections: ${gc_count}"
    fi
    
    echo ""
    return $exit_code
}

# Run tests for each size and type
total_tests=0
failed_tests=0

for size in "${SIZES[@]}"; do
    for type in "${TEST_TYPES[@]}"; do
        total_tests=$((total_tests + 1))
        
        if ! run_test "$size" "$type"; then
            failed_tests=$((failed_tests + 1))
        fi
        
        # Add delay between tests for memory cleanup
        sleep 2
    done
done

echo "=== Performance Test Summary ==="
echo "Total tests: $total_tests"
echo "Failed tests: $failed_tests"
echo "Success rate: $(echo "scale=1; ($total_tests - $failed_tests) * 100 / $total_tests" | bc -l)%"
echo ""

echo "Results and logs are available in: $RESULTS_DIR"
echo ""

# Show file sizes for reference
echo "=== Generated Test File Sizes ==="
for size in "${SIZES[@]}"; do
    file1="${TEST_DIR}/test_${size}_identical_1.xml"
    if [ -f "$file1" ]; then
        actual_size=$(ls -lh "$file1" | awk '{print $5}')
        echo "$size target -> $actual_size actual"
    fi
done

echo ""
echo "Performance test completed!"