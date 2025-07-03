#!/bin/bash

# Generate test files up to 50MB for StAX comparison testing

TEST_DIR="test-files-50mb"
LOG_FILE="$TEST_DIR/generation.log"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}Generating Test Files up to 50MB${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# Create test directory
mkdir -p "$TEST_DIR"
echo "$(date): Starting test file generation" > "$LOG_FILE"

# File size targets (approximate)
FILE_SIZES="1MB:1000 5MB:5000 10MB:10000 15MB:15000 20MB:20000 25MB:25000 30MB:30000 35MB:35000 40MB:40000 45MB:45000 50MB:50000"

generate_xml_file() {
    local filename="$1"
    local element_count="$2"
    local make_different="$3"
    
    echo "Generating $filename with $element_count elements..."
    
    cat > "$filename" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<TestData>
  <Metadata>
    <GeneratedAt>$(date -Iseconds)</GeneratedAt>
    <ElementCount>$element_count</ElementCount>
    <FileType>$(basename "$filename")</FileType>
    <TestPurpose>StAX Performance Testing</TestPurpose>
  </Metadata>
  <Schools>
EOF

    for ((i=1; i<=element_count; i++)); do
        cat >> "$filename" << EOF
    <School id="$i">
      <Name>School_$i</Name>
      <Location>Location_$i</Location>
      <Principal>Principal_$i</Principal>
      <Contact>
        <Phone>555-$(printf "%04d" $((i % 10000)))</Phone>
        <Email>school$i@example.com</Email>
        <Website>https://school$i.edu</Website>
      </Contact>
      <Students>
EOF

        for ((j=1; j<=5; j++)); do
            local status="Active"
            if [[ "$make_different" == "true" && $i -eq 1 && $j -eq 1 ]]; then
                status="MODIFIED_FOR_DIFFERENCE_TEST"
            fi
            
            cat >> "$filename" << EOF
        <Student id="$j">
          <FirstName>Student${j}First_${i}</FirstName>
          <LastName>Student${j}Last_${i}</LastName>
          <Grade>$((9 + (j % 4)))</Grade>
          <Age>$((14 + (j % 4)))</Age>
          <Status>$status</Status>
          <GPA>$(echo "scale=2; 3.0 + ($j * 0.2)" | bc)</GPA>
        </Student>
EOF
        done

        cat >> "$filename" << EOF
      </Students>
      <Classes>
EOF

        local subjects=("Mathematics" "Science" "English" "History" "Art" "Physics" "Chemistry" "Biology")
        for subject in "${subjects[@]}"; do
            cat >> "$filename" << EOF
        <Class>
          <Subject>$subject</Subject>
          <Teacher>Teacher_${subject}_${i}</Teacher>
          <Room>Room_${subject:0:1}${i}</Room>
          <Schedule>Daily</Schedule>
          <Credits>3</Credits>
          <MaxStudents>30</MaxStudents>
        </Class>
EOF
        done

        cat >> "$filename" << EOF
      </Classes>
      <Facilities>
        <Library>
          <Books>$((1000 + i * 10))</Books>
          <Computers>$((20 + i % 50))</Computers>
        </Library>
        <Gymnasium>
          <Capacity>$((500 + i * 5))</Capacity>
          <Equipment>Standard</Equipment>
        </Gymnasium>
      </Facilities>
    </School>
EOF

        # Progress indicator
        if [[ $((i % 1000)) -eq 0 ]]; then
            local current_size=$(ls -lh "$filename" 2>/dev/null | awk '{print $5}' || echo "calculating...")
            echo "  Progress: $i/$element_count elements - Current size: $current_size"
        fi
    done

    cat >> "$filename" << EOF
  </Schools>
</TestData>
EOF

    local final_size=$(ls -lh "$filename" | awk '{print $5}')
    echo "  ✅ Completed: $final_size"
    echo "$(date): Generated $filename - $final_size" >> "$LOG_FILE"
}

# Generate files for each target size
for size_config in $FILE_SIZES; do
    IFS=':' read -r size_label element_count <<< "$size_config"
    
    echo -e "\n${YELLOW}Generating $size_label files...${NC}"
    
    # Generate identical files
    echo -e "${BLUE}Creating identical pair for $size_label${NC}"
    generate_xml_file "$TEST_DIR/test_${size_label}_identical_1.xml" "$element_count" "false"
    cp "$TEST_DIR/test_${size_label}_identical_1.xml" "$TEST_DIR/test_${size_label}_identical_2.xml"
    
    # Generate different files
    echo -e "${BLUE}Creating different pair for $size_label${NC}"
    generate_xml_file "$TEST_DIR/test_${size_label}_different_1.xml" "$element_count" "false"
    generate_xml_file "$TEST_DIR/test_${size_label}_different_2.xml" "$element_count" "true"
done

echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}Generation Complete!${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

echo -e "${GREEN}📁 Generated Files:${NC}"
ls -lh "$TEST_DIR"/*.xml | while read -r line; do
    echo "  $line"
done

echo ""
echo -e "${GREEN}📊 Summary:${NC}"
total_files=$(ls "$TEST_DIR"/*.xml 2>/dev/null | wc -l)
total_size=$(du -sh "$TEST_DIR" | awk '{print $1}')
echo "  Total files: $total_files"
echo "  Total size: $total_size"
echo "  Log file: $LOG_FILE"

echo ""
echo -e "${GREEN}🎯 Ready for StAX testing up to 50MB!${NC}"