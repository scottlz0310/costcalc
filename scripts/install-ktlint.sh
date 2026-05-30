#!/usr/bin/env bash
# ktlint CLI をバージョン固定でインストールする。
# 既に同バージョンが存在する場合はスキップする。
set -euo pipefail

KTLINT_VERSION="1.5.0"
# sha256sum of ktlint 1.5.0 binary from GitHub Releases
KTLINT_SHA256="a16be01dcc480aab2f55f444b620142152f66e31564b3b9376506d624c28a2ad"
INSTALL_DIR="${KTLINT_INSTALL_DIR:-$HOME/.local/bin}"
BINARY="$INSTALL_DIR/ktlint"

if [ -f "$BINARY" ] && "$BINARY" --version 2>/dev/null | grep -qF "$KTLINT_VERSION"; then
  echo "ktlint $KTLINT_VERSION is already installed."
  exit 0
fi

mkdir -p "$INSTALL_DIR"
echo "Installing ktlint $KTLINT_VERSION to $BINARY ..."
TMPFILE="$(mktemp)"
curl -sSLo "$TMPFILE" \
  "https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint"
echo "$KTLINT_SHA256  $TMPFILE" | sha256sum -c --quiet || {
  echo "SHA-256 verification failed. Aborting." >&2
  rm -f "$TMPFILE"
  exit 1
}
mv "$TMPFILE" "$BINARY"
chmod +x "$BINARY"
echo "Done. Make sure $INSTALL_DIR is in your PATH."
