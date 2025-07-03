#!/bin/bash

echo "=== Generating Very Large XML Files for Streaming Test ==="

# Create test directory
mkdir -p test-files-large

# Generate 60MB file (above 50MB limit)
echo "Generating 60MB XML file..."

cat > test-files-large/large_60MB_1.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<TestData>
  <Metadata>
    <GeneratedAt>2024-07-02T14:15:00</GeneratedAt>
    <TargetSize>60MB</TargetSize>
    <Filename>large_60MB_1.xml</Filename>
  </Metadata>
  <Schools>
EOF

# Generate approximately 60MB of XML content
for i in {1..20000}; do
    cat >> test-files-large/large_60MB_1.xml << EOF
    <School id="$i">
      <Name>School_$i</Name>
      <Location>Location_$i</Location>
      <Principal>Principal_$i</Principal>
      <Contact>
        <Phone>555-$(printf "%04d" $((i % 10000)))</Phone>
        <Email>school$i@example.com</Email>
      </Contact>
      <Students>
        <Student id="1">
          <FirstName>Student1First</FirstName>
          <LastName>Student1Last</LastName>
          <Grade>9</Grade>
          <Age>14</Age>
          <Status>Active</Status>
        </Student>
        <Student id="2">
          <FirstName>Student2First</FirstName>
          <LastName>Student2Last</LastName>
          <Grade>10</Grade>
          <Age>15</Age>
          <Status>Active</Status>
        </Student>
        <Student id="3">
          <FirstName>Student3First</FirstName>
          <LastName>Student3Last</LastName>
          <Grade>11</Grade>
          <Age>16</Age>
          <Status>Active</Status>
        </Student>
      </Students>
      <Classes>
        <Class>
          <Subject>Math</Subject>
          <Teacher>Teacher_Math_$i</Teacher>
          <Room>Room_M$i</Room>
          <Schedule>Daily</Schedule>
        </Class>
        <Class>
          <Subject>Science</Subject>
          <Teacher>Teacher_Science_$i</Teacher>
          <Room>Room_S$i</Room>
          <Schedule>Daily</Schedule>
        </Class>
      </Classes>
    </School>
EOF

    # Progress indicator
    if [ $((i % 1000)) -eq 0 ]; then
        current_size=$(ls -lh test-files-large/large_60MB_1.xml 2>/dev/null | awk '{print $5}' || echo "0")
        echo "Progress: $i/20000 schools - Current size: $current_size"
    fi
done

echo "  </Schools>" >> test-files-large/large_60MB_1.xml
echo "</TestData>" >> test-files-large/large_60MB_1.xml

# Create identical copy
cp test-files-large/large_60MB_1.xml test-files-large/large_60MB_2.xml

# Create different version
cp test-files-large/large_60MB_1.xml test-files-large/large_60MB_different.xml
# Modify one student to create differences
sed -i 's/<Status>Active<\/Status>/<Status>DIFFERENT<\/Status>/' test-files-large/large_60MB_different.xml | head -1

echo ""
echo "Generated files:"
ls -lh test-files-large/
echo ""
echo "Files ready for streaming test!"