#!/usr/bin/env bash
# 古い avdmanager が android-37.0 を android-0 と記録した AVD でテストしない。
set -euo pipefail

expected="${1:?期待する target（android-37.0 等）を指定する}"
ini="${2:?AVD の .ini ファイルを指定する}"
diag="${DIAG_DIR:-ui-test-diagnostics}/avd"
mkdir -p "$diag"

if [ ! -f "$ini" ]; then
  echo "::error::AVD の設定が見つからない: $ini"
  exit 1
fi
cp "$ini" "$diag/avd.ini"
# Java properties の空白・CRLF にも対応する。
read_property() {
  sed -n "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*//p" "$ini" | tr -d '\r' | sed 's/[[:space:]]*$//'
}
actual="$(read_property target)"
avd_path="$(read_property path)"
if [ -f "$avd_path/config.ini" ]; then
  cp "$avd_path/config.ini" "$diag/config.ini"
fi
printf 'AVD target: expected=%s actual=%s\n' "$expected" "$actual"
if [ "$actual" != "$expected" ]; then
  echo "::error::AVD target が不正。Command-line Tools のバージョンと生成された AVD を確認する"
  exit 1
fi
