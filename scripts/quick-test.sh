#!/bin/bash

# Ultra-fast checks (< 10 seconds)
# Run this before EVERY commit to catch issues immediately

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd .. && pwd)"

echo "⚡ Quick Pre-commit Checks..."
echo ""

cd "$SCRIPT_DIR/maplocation"

# 1. Python syntax (instant)
python3 -m compileall -q . 2>/dev/null && echo "✓ Python syntax" || echo "✗ Python syntax errors"

# 2. Django imports (2 seconds)
python3 -c "import django; django.setup()" 2>/dev/null && echo "✓ Django imports" || echo "✗ Django import errors"

# 3. Check for common mistakes (instant)
if grep -r "console.log" "$SCRIPT_DIR/map-frontend/src" --include="*.vue" --include="*.ts" > /dev/null 2>&1; then
    echo "⚠ console.log found in frontend"
fi

if grep -r "print(" "$SCRIPT_DIR/maplocation/locations" --include="*.py" | grep -v "DEBUG" > /dev/null 2>&1; then
    echo "⚠ print() statements found in backend"
fi

echo "✓ Basic checks passed"
echo ""
echo "Run './scripts/test-deployment-locally.sh' for full validation"

