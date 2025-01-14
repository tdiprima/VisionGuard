#!/usr/bin/env bash

cd ..
mvn clean package

JAR="target/VisionGuard-1.0.jar"
INPUT="src/main/resources/images/"
OUTPUT="output/"
REPORT="report/"
ACTION="BURN"

java -jar "$JAR" --help

java -jar "$JAR" "$INPUT" "$ACTION" "$OUTPUT" "$REPORT" --ollama=true

# Detects "medium text" image:
java -jar "$JAR" "$INPUT" "$ACTION" "$OUTPUT" "$REPORT" --minWidth=200 --minHeight=20 --maxWidth=250 --maxHeight=35

java -Djava.library.path=/usr/local/lib -jar "$JAR" "$INPUT" "$ACTION" "$OUTPUT" "$REPORT"
