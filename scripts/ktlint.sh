#!/usr/bin/env bash
# .ktlint-version の版の ktlint を、PATH に依存せず実行する。引数は、そのまま ktlint に渡す。
#
# インストール先（既定: ~/.local/bin）のバイナリを明示的に実行する。`ktlint` を PATH から探すと、
# PATH の前方にある別の版が実行され、CI とローカルで版が食い違う（#109）。
# 未導入、または版が違う場合は、install-ktlint.sh で入れ直してから実行する（同じ版なら何もしない）。
#
# 使い方:
#   bash scripts/ktlint.sh --format path/to/File.kt
#   bash scripts/ktlint.sh "**/*.kt"
#
# 環境変数: KTLINT_INSTALL_DIR（インストール先。既定: ~/.local/bin）。そのほかは install-ktlint.sh を参照。
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
INSTALL_DIR="${KTLINT_INSTALL_DIR:-$HOME/.local/bin}"

# インストールの出力は、ktlint の出力と混ざらないよう、失敗したときだけ表示する。
if ! install_log="$(KTLINT_INSTALL_DIR="$INSTALL_DIR" bash "$DIR/install-ktlint.sh" 2>&1)"; then
  printf '%s\n' "$install_log" >&2
  echo "ktlint のインストールに失敗した" >&2
  exit 1
fi

exec "$INSTALL_DIR/ktlint" "$@"
