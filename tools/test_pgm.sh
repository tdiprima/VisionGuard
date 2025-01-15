#!/usr/bin/env bash

cd ..
mvn clean package

java -jar target/VisionGuard-1.0.jar --help

java -Djava.awt.headless=true -jar target/VisionGuard-1.0.jar "input/" "MASK" "output/" "result/"
