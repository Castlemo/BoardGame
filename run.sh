#!/bin/bash

# 컴파일
echo "Compiling..."
javac -d out -encoding UTF-8 $(find src -name "*.java")

# 실행
echo "Starting game..."
java -cp out com.marblegame.Main
