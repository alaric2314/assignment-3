#!/bin/sh
# Compiles all sources into ./out (requires a JDK, Java 11+).
set -e
rm -rf out && mkdir out
javac -d out $(find src -name '*.java')
echo "compiled into ./out"
