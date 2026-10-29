#!/bin/sh
# Build, test, demo, run all experiments, regenerate plots (needs python3 + matplotlib for plots).
set -e
./build.sh
java -cp out tests.TestRunner
java -cp out app.CityDemo
java -cp out experiments.Benchmark results
python3 scripts/plot.py results
