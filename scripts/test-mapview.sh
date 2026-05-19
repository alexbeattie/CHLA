#!/bin/bash

# MapView.vue Refactoring Test Script
# Run this after EVERY extraction to ensure nothing broke

echo "Testing MapView.vue..."
echo "================================"

cd "$(dirname "$0")/.." || exit 1

# Test 1: Check file exists and get line count
if [ -f "map-frontend/src/views/MapView.vue" ]; then
    LINES=$(wc -l < map-frontend/src/views/MapView.vue)
    echo "MapView.vue exists ($LINES lines)"
else
    echo "MapView.vue not found!"
    exit 1
fi

# Test 2: Check for syntax errors
echo ""
echo "Testing Vue syntax..."
cd map-frontend || exit 1

if npm run build > /tmp/mapview-test.log 2>&1; then
    echo "Build succeeds (no syntax errors)"
else
    echo "Build fails - check /tmp/mapview-test.log"
    tail -20 /tmp/mapview-test.log
    exit 1
fi

# Test 3: Check dev server starts
echo ""
echo "Testing dev server..."
pkill -f "vite.*development" 2>/dev/null
npm run dev > /tmp/mapview-dev.log 2>&1 &
DEV_PID=$!
sleep 8

if curl -s http://localhost:3000 | grep -q "Map Location Finder"; then
    echo "Dev server starts and app loads"
else
    echo "Dev server failed or app doesn't load"
    echo "Check /tmp/mapview-dev.log"
    kill $DEV_PID 2>/dev/null
    exit 1
fi

# Cleanup
kill $DEV_PID 2>/dev/null
sleep 2

echo ""
echo "================================"
echo "ALL TESTS PASSED!"
echo ""
echo "Current state:"
echo " - Lines: $LINES"
echo " - Compiles: YES"
echo " - Runs: YES"
echo ""
echo "Safe to commit!"

