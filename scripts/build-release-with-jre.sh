#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SPRING_DIR="$ROOT_DIR/spring"
RELEASE_DIR="$SPRING_DIR/target/release/windows"
JRE_DIR="$RELEASE_DIR/jre"

JRE_ZIP="${1:-${JRE_ZIP:-}}"
if [[ -z "$JRE_ZIP" ]]; then
  echo "Usage: $0 /path/to/windows-jre.zip"
  echo "or:    JRE_ZIP=/path/to/windows-jre.zip $0"
  exit 1
fi
if [[ ! -f "$JRE_ZIP" ]]; then
  echo "Failed: JRE zip not found: $JRE_ZIP"
  exit 1
fi
if ! command -v unzip >/dev/null 2>&1; then
  echo "Failed: unzip command is required."
  exit 1
fi

echo "[1/3] Build bundled release package"
"$SCRIPT_DIR/build-release.sh"

echo "[2/3] Extract JRE zip into release package"
TMP_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

rm -rf "$JRE_DIR"
mkdir -p "$JRE_DIR"

unzip -q "$JRE_ZIP" -d "$TMP_DIR"

JAVA_BIN="$(find "$TMP_DIR" -type f \( -iname "java.exe" -o -name "java" \) | head -n 1 || true)"
if [[ -z "$JAVA_BIN" ]]; then
  echo "Failed: java executable not found in zip: $JRE_ZIP"
  exit 1
fi

JAVA_HOME_DIR="$(cd "$(dirname "$JAVA_BIN")/.." && pwd)"
cp -R "$JAVA_HOME_DIR/." "$JRE_DIR/"

if [[ ! -f "$JRE_DIR/bin/java.exe" && ! -f "$JRE_DIR/bin/java" ]]; then
  echo "Failed: extracted JRE does not contain bin/java(.exe)."
  exit 1
fi

echo "[3/3] Done"
echo "Output: $RELEASE_DIR"
