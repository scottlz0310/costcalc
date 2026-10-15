#!/usr/bin/env bash
# scripts/ktlint.sh が、PATH ではなく、インストール先のバイナリを実行することを確認する（ネットワーク不要）。
set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")/../../.." && pwd)/scripts/ktlint.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

mkdir -p "$WORK/installed" "$WORK/path-first"

# インストール先の ktlint（--version では版を表示し、それ以外は実行を示す文字列と引数を表示する）。
cat >"$WORK/installed/ktlint" <<'EOF'
#!/usr/bin/env bash
if [ "${1:-}" = "--version" ]; then echo "ktlint version 9.9.9"; else echo "INSTALLED-RAN $*"; fi
EOF
# PATH の前方にある、別の（旧）版の ktlint。これが実行されてはならない。
cat >"$WORK/path-first/ktlint" <<'EOF'
#!/usr/bin/env bash
echo "OLD-FROM-PATH-RAN $*"
EOF
chmod +x "$WORK/installed/ktlint" "$WORK/path-first/ktlint"

printf '9.9.9\n' >"$WORK/version"
printf 'not-a-version\n' >"$WORK/bad-version"

failures=0
# 名前 | 版ファイル | 期待する終了コード | 期待する出力（部分一致） | 出力に含まれてはならない文字列
cases=(
  "PATH の前方に旧版があっても、インストール先を実行する|$WORK/version|0|INSTALLED-RAN --format A.kt|OLD-FROM-PATH-RAN"
  "インストールに失敗したら、ktlint を実行しない|$WORK/bad-version|1||RAN"
)

for entry in "${cases[@]}"; do
  IFS='|' read -r name version_file want_exit want_output forbidden <<<"$entry"
  status=0
  output="$(
    PATH="$WORK/path-first:$PATH" KTLINT_VERSION_FILE="$version_file" KTLINT_INSTALL_DIR="$WORK/installed" \
      bash "$SCRIPT" --format A.kt 2>&1
  )" || status=$?

  ok=yes
  [ "$status" -eq "$want_exit" ] || ok=no
  if [ -n "$want_output" ] && [[ "$output" != *"$want_output"* ]]; then ok=no; fi
  if [ -n "$forbidden" ] && [[ "$output" == *"$forbidden"* ]]; then ok=no; fi

  if [ "$ok" = yes ]; then
    echo "ok: ${name}"
  else
    echo "FAIL: ${name} (exit=${status} 期待=${want_exit}, 出力=${output})"
    failures=$((failures + 1))
  fi
done

[ "$failures" -eq 0 ]
