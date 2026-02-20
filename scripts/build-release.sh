#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VUE_DIR="$ROOT_DIR/vue"
SPRING_DIR="$ROOT_DIR/spring"
STATIC_DIR="$SPRING_DIR/src/main/resources/static"
RELEASE_DIR="$SPRING_DIR/target/release/windows"

echo "[1/5] Build Vue app"
(
  cd "$VUE_DIR"
  npm ci
  npm run build
)

echo "[2/5] Copy Vue dist into Spring static resources"
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR"
cp -R "$VUE_DIR/dist/." "$STATIC_DIR/"

echo "[3/5] Build Spring Boot jar"
(
  cd "$SPRING_DIR"
  ./mvnw -q clean package -DskipTests
)

echo "[4/5] Prepare Windows runtime package"
mkdir -p "$RELEASE_DIR"
JAR_PATH="$(ls -t "$SPRING_DIR"/target/*.jar | head -n 1)"
cp "$JAR_PATH" "$RELEASE_DIR/flippers.jar"

cat > "$RELEASE_DIR/run.bat" <<'BAT'
@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%jre\bin\java.exe" (
  "%SCRIPT_DIR%jre\bin\java.exe" -jar "%SCRIPT_DIR%flippers.jar"
) else (
  java -jar "%SCRIPT_DIR%flippers.jar"
)
pause
BAT

cat > "$RELEASE_DIR/README.txt" <<'TXT'
1. Put Java runtime at ".\jre" (optional).
2. Run "run.bat".
3. Open http://localhost:8080
TXT

echo "[5/5] Done"
echo "Output: $RELEASE_DIR"
