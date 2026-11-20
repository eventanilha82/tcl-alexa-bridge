#!/usr/bin/env bash
set -euo pipefail
BRIDGE_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRIDGE_TEST_OUT="$BRIDGE_ROOT/build/compatibility-tests"
mkdir -p "$BRIDGE_TEST_OUT/classes"
BRIDGE_DEPENDENCIES="$(python3 "$BRIDGE_ROOT/scripts/ensure-dependencies.py")"
javac --release 8 -cp "$BRIDGE_DEPENDENCIES" -d "$BRIDGE_TEST_OUT/classes" \
  "$BRIDGE_ROOT/app/src/local/tclbridge/CoreReflection.java" \
  "$BRIDGE_ROOT/app/src/local/tclbridge/DexSdkCompat.java" \
  "$BRIDGE_ROOT/app/src/local/tclbridge/DexOrderRepair.java" \
  "$BRIDGE_ROOT/tests/CoreReflectionTest.java" \
  "$BRIDGE_ROOT/tests/DexSdkCompatTest.java" \
  "$BRIDGE_ROOT/tests/DexOrderRepairTest.java"
java -cp "$BRIDGE_TEST_OUT/classes" local.tclbridge.CoreReflectionTest
java -cp "$BRIDGE_TEST_OUT/classes:$BRIDGE_DEPENDENCIES" local.tclbridge.DexOrderRepairTest "$@"
if [[ "$#" -gt 0 ]]; then
  python3 "$BRIDGE_ROOT/scripts/test-core-contract.py" "$@"
fi
BRIDGE_TEST_INDEX=0
for BRIDGE_TEST_APK in "$@"; do
  BRIDGE_TEST_INDEX=$((BRIDGE_TEST_INDEX+1))
  java -cp "$BRIDGE_TEST_OUT/classes" local.tclbridge.DexSdkCompatTest \
    "$BRIDGE_TEST_APK" "$BRIDGE_TEST_OUT/sdk-patched-$BRIDGE_TEST_INDEX.dex"
done
