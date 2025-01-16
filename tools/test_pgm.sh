#!/usr/bin/env bash

cd ..
mvn clean package

# Example test runs
java -jar target/VisionGuard-1.0.jar --help

java -Djava.awt.headless=true -jar target/VisionGuard-1.0.jar "input/" "MASK" "output/" "report/"

java -Djava.awt.headless=true -jar target/VisionGuard-1.0.jar "src/main/resources/images/" "input/" "MASK" "output/" "report/" --ollama=true

# Detects "medium text" image:
java -Djava.awt.headless=true -jar target/VisionGuard-1.0.jar "src/main/resources/images/" "input/" "MASK" "output/" "report/" --minWidth=200 --minHeight=20 --maxWidth=250 --maxHeight=35
