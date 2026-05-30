#!/usr/bin/env bash
# ktlint CLI をバージョン固定でインストールする。
# 既に同バージョンが存在する場合はスキップする。
set -euo pipefail

KTLINT_VERSION="1.5.0"
INSTALL_DIR="${KTLINT_INSTALL_DIR:-$HOME/.local/bin}"
BINARY="$INSTALL_DIR/ktlint"

if [ -f "$BINARY" ] && "$BINARY" --version 2>/dev/null | grep -qF "$KTLINT_VERSION"; then
  echo "ktlint $KTLINT_VERSION is already installed."
  exit 0
fi

mkdir -p "$INSTALL_DIR"
echo "Installing ktlint $KTLINT_VERSION to $BINARY ..."
curl -sSLo "$BINARY" \
  "https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint"
chmod +x "$BINARY"
echo "Done. Make sure $INSTALL_DIR is in your PATH."
