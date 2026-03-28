#!/bin/bash
# Build sql-cli jar and copy to jar/ directory for npm packaging
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
JAR_DIR="$PROJECT_DIR/jar"

echo "Building sql-cli.jar..."
cd "$PROJECT_DIR"
./gradlew clean shadowJar -q

mkdir -p "$JAR_DIR"
cp "$PROJECT_DIR/build/libs/sql-cli.jar" "$JAR_DIR/sql-cli.jar"

echo "Done: jar/sql-cli.jar"
ls -lh "$JAR_DIR/sql-cli.jar"
