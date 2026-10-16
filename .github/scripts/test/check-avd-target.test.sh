#!/usr/bin/env bash
set -euo pipefail
SCRIPT="$(cd "$(dirname "$0")/.." && pwd)/check-avd-target.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/device.avd"
echo 'hw.cpu.ncore=4' > "$WORK/device.avd/config.ini"
for entry in '37.0|37.0|0' '36|36|0' '37.0|0|1' '37.0||1'; do
  IFS='|' read -r expected actual want <<< "$entry"
  printf 'path = %s/device.avd\r\ntarget = android-%s\r\n' "$WORK" "$actual" > "$WORK/device.ini"
  status=0
  DIAG_DIR="$WORK/diag" bash "$SCRIPT" "android-$expected" "$WORK/device.ini" > "$WORK/output" 2>&1 || status=$?
  test "$status" -eq "$want"
  cmp "$WORK/device.ini" "$WORK/diag/avd/avd.ini"
  cmp "$WORK/device.avd/config.ini" "$WORK/diag/avd/config.ini"
  echo "ok: expected=$expected actual=$actual exit=$status"
done
printf 'path=%s/device.avd\n' "$WORK" > "$WORK/device.ini"
if DIAG_DIR="$WORK/diag" bash "$SCRIPT" android-37.0 "$WORK/device.ini" > "$WORK/output" 2>&1; then
  echo 'FAIL: target の欠落を検出しなかった'; exit 1
fi
if DIAG_DIR="$WORK/diag" bash "$SCRIPT" android-37.0 "$WORK/missing.ini" > "$WORK/output" 2>&1; then
  echo 'FAIL: 設定ファイルの欠落を検出しなかった'; exit 1
fi
echo 'ok: target・設定ファイルの欠落は失敗する'
