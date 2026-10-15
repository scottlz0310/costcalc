#!/usr/bin/env bash
# ktlint CLI をインストールする。
#
# 版の定義は、リポジトリ直下の .ktlint-version だけ。CI（ci.yml）も、ローカル（lefthook の pre-commit）も、
# このスクリプトで入れるため、版が食い違わない。Renovate は .ktlint-version を追従する（renovate.json）。
#
# 完全性の確認: ダウンロードしたバイナリの SHA-256 を、GitHub のリリースアセットの digest と照合する。
# digest は、リポジトリには固定せず、実行時に GitHub API から取得する（版の更新で SHA を手で書き換えない）。
# なお、リリースそのものが改ざんされた場合は検知できない。
#
# 既に同じ版が入っている場合はスキップする。
#
# 環境変数:
#   KTLINT_INSTALL_DIR  インストール先（既定: ~/.local/bin）
#   GITHUB_TOKEN        GitHub API の認証に使う（任意。CI では、未認証のレート制限を避けるために渡す）
#   KTLINT_VERSION_FILE 版ファイルのパス（既定: リポジトリ直下の .ktlint-version。テスト用）
#   CURL_BIN            curl の実行ファイル（既定: curl。テスト用）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION_FILE="${KTLINT_VERSION_FILE:-$ROOT/.ktlint-version}"
INSTALL_DIR="${KTLINT_INSTALL_DIR:-$HOME/.local/bin}"
CURL_BIN="${CURL_BIN:-curl}"
REPO="pinterest/ktlint"
BINARY="$INSTALL_DIR/ktlint"

if [ ! -f "$VERSION_FILE" ]; then
  echo "ktlint の版の定義が見つからない: $VERSION_FILE" >&2
  exit 1
fi
KTLINT_VERSION="$(tr -d '[:space:]' <"$VERSION_FILE")"
if [[ ! "$KTLINT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "ktlint の版の書式が不正: '$KTLINT_VERSION'（例: 1.8.0）" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq が必要（GitHub API の応答から digest を取り出すため）。インストールしてから再実行する" >&2
  exit 1
fi

if [ -f "$BINARY" ] && "$BINARY" --version 2>/dev/null | grep -qxF "ktlint version $KTLINT_VERSION"; then
  echo "ktlint $KTLINT_VERSION is already installed."
  exit 0
fi

auth=()
if [ -n "${GITHUB_TOKEN:-}" ]; then
  auth=(-H "Authorization: Bearer $GITHUB_TOKEN")
fi

release_json="$("$CURL_BIN" -fsSL ${auth[@]+"${auth[@]}"} -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/$REPO/releases/tags/$KTLINT_VERSION")"
digest="$(printf '%s' "$release_json" | jq -r '[.assets[]? | select(.name == "ktlint") | .digest] | first // empty')"
if [[ ! "$digest" =~ ^sha256:[0-9a-f]{64}$ ]]; then
  echo "リリース $KTLINT_VERSION の ktlint の digest を取得できない（得られた値: '${digest}'）" >&2
  exit 1
fi
expected="${digest#sha256:}"

TMPFILE="$(mktemp)"
trap 'rm -f "$TMPFILE"' EXIT
echo "Installing ktlint $KTLINT_VERSION to $BINARY ..."
"$CURL_BIN" -fsSLo "$TMPFILE" "https://github.com/$REPO/releases/download/$KTLINT_VERSION/ktlint"

actual="$(sha256sum "$TMPFILE" | awk '{print $1}')"
if [ "$actual" != "$expected" ]; then
  echo "SHA-256 が一致しない。中止する（期待: $expected / 実際: $actual）" >&2
  exit 1
fi

mkdir -p "$INSTALL_DIR"
mv "$TMPFILE" "$BINARY"
chmod +x "$BINARY"
trap - EXIT
echo "Done. Make sure $INSTALL_DIR is in your PATH."
