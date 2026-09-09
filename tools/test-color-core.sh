#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
SWIFT_COMPILER="${SWIFT_COMPILER:-swiftc}"
TEST_DIR=$(mktemp -d)
trap 'rm -rf "$TEST_DIR"' EXIT
"$SWIFT_COMPILER" Sources/Color/ColorPlan.swift Tests/ColorCore/main.swift -o "$TEST_DIR/color-tests"
"$TEST_DIR/color-tests" Tests/ColorCore/python_reference.json
