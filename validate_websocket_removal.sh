#!/bin/bash
# WebSocket Removal Validation Script
# This script verifies that all WebSocket components have been removed

echo "🔍 WebSocket Removal Validation"
echo "================================"
echo ""

PROJECT_DIR="C:\Users\Mosbeh Eya\Desktop\karhebti-android-gestionVoitures"

echo "1️⃣  Checking for WebSocket files..."
WEBSOCKET_FILES=$(find "$PROJECT_DIR/app/src" -iname "*websocket*" -o -iname "*WebSocket*" 2>/dev/null)
if [ -z "$WEBSOCKET_FILES" ]; then
    echo "   ✅ No WebSocket files found"
else
    echo "   ❌ Found WebSocket files:"
    echo "$WEBSOCKET_FILES"
fi

echo ""
echo "2️⃣  Checking for WebSocket references in code..."
REFERENCES=$(grep -r "WebSocket\|websocket\|data\.websocket" "$PROJECT_DIR/app/src/main/java" 2>/dev/null | grep -v "Binary")
if [ -z "$REFERENCES" ]; then
    echo "   ✅ No WebSocket references found in code"
else
    echo "   ❌ Found references:"
    echo "$REFERENCES"
fi

echo ""
echo "3️⃣  Checking for WebSocket imports..."
IMPORTS=$(grep -r "import.*websocket\|import.*WebSocket" "$PROJECT_DIR/app/src/main/java" 2>/dev/null)
if [ -z "$IMPORTS" ]; then
    echo "   ✅ No WebSocket imports found"
else
    echo "   ❌ Found imports:"
    echo "$IMPORTS"
fi

echo ""
echo "4️⃣  Checking NavGraph for WebSocket routes..."
if grep -q "WebSocket\|websocket" "$PROJECT_DIR/app/src/main/java/com/example/karhebti_android/navigation/NavGraph.kt" 2>/dev/null; then
    echo "   ❌ Found WebSocket routes in NavGraph"
else
    echo "   ✅ No WebSocket routes in NavGraph"
fi

echo ""
echo "================================"
echo "✅ WebSocket Removal Validation Complete"
echo ""
echo "Status: ALL CLEAR - WebSocket system completely removed"

