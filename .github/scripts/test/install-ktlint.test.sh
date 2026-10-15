#!/usr/bin/env bash
# scripts/install-ktlint.sh の分岐を、curl をスタブに差し替えて確認する（ネットワーク不要）。
set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")/../../.." && pwd)/scripts/install-ktlint.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# 偽の ktlint（版を表示するだけのスクリプト）とその SHA-256。
FAKE_BINARY=$'#!/usr/bin/env bash\necho "ktlint version 9.9.9"\n'
FAKE_SHA="$(printf '%s' "$FAKE_BINARY" | sha256sum | awk '{print $1}')"
OTHER_SHA="$(printf '%s' 'tampered' | sha256sum | awk '{print $1}')"

# スタブ: リリース API には STUB_RELEASE_JSON を返し、ダウンロードには偽のバイナリを書き出す。呼び出しを記録する。
cat >"$WORK/curl" <<'STUB'
#!/usr/bin/env bash
echo "$*" >>"$STUB_CALLS"
url="${*: -1}"
out=""
args=("$@")
for ((i = 0; i < ${#args[@]}; i++)); do
  case "${args[$i]}" in
    -*o) out="${args[$((i + 1))]}" ;;
  esac
done
case "$url" in
  */releases/tags/*) printf '%s' "$STUB_RELEASE_JSON" ;;
  */releases/download/*/ktlint) printf '%s' "$STUB_BINARY" >"$out" ;;
esac
STUB
chmod +x "$WORK/curl"

release_json() { printf '{"assets":[{"name":"other","digest":"sha256:%s"},{"name":"ktlint","digest":%s}]}' "$OTHER_SHA" "$1"; }

failures=0
# 名前 | 版ファイルの内容 | digest（JSON の値） | 事前にインストール済みか | 期待する終了コード | 期待: インストールされるか | 期待: curl が呼ばれるか
cases=(
  "digest 一致|9.9.9|\"sha256:$FAKE_SHA\"|no|0|yes|yes"
  "digest 不一致|9.9.9|\"sha256:$OTHER_SHA\"|no|1|no|yes"
  "digest が null|9.9.9|null|no|1|no|yes"
  "digest の書式が不正|9.9.9|\"md5:abc\"|no|1|no|yes"
  "既に同じ版が入っている|9.9.9|\"sha256:$FAKE_SHA\"|yes|0|yes|no"
  "版の書式が不正|latest|\"sha256:$FAKE_SHA\"|no|1|no|no"
  "版ファイルが空|||no|1|no|no"
)

for entry in "${cases[@]}"; do
  IFS='|' read -r name version digest preinstalled want_exit want_installed want_curl <<<"$entry"
  dir="$WORK/case"
  rm -rf "$dir"
  mkdir -p "$dir/bin"
  printf '%s\n' "$version" >"$dir/version"
  : >"$dir/calls"
  if [ "$preinstalled" = "yes" ]; then
    printf '%s' "$FAKE_BINARY" >"$dir/bin/ktlint"
    chmod +x "$dir/bin/ktlint"
  fi

  status=0
  CURL_BIN="$WORK/curl" KTLINT_VERSION_FILE="$dir/version" KTLINT_INSTALL_DIR="$dir/bin" \
    STUB_CALLS="$dir/calls" STUB_RELEASE_JSON="$(release_json "${digest:-null}")" STUB_BINARY="$FAKE_BINARY" \
    bash "$SCRIPT" >/dev/null 2>&1 || status=$?

  installed=no
  [ -x "$dir/bin/ktlint" ] && installed=yes
  called=no
  [ -s "$dir/calls" ] && called=yes

  if [ "$status" -ne "$want_exit" ] || [ "$installed" != "$want_installed" ] || [ "$called" != "$want_curl" ]; then
    echo "FAIL: ${name} (exit=${status} 期待=${want_exit}, インストール=${installed} 期待=${want_installed}, curl=${called} 期待=${want_curl})"
    failures=$((failures + 1))
  else
    echo "ok: ${name}"
  fi
done

# 版ファイルが存在しない場合。
status=0
CURL_BIN="$WORK/curl" KTLINT_VERSION_FILE="$WORK/no-such-file" KTLINT_INSTALL_DIR="$WORK/none" \
  STUB_CALLS="$WORK/calls2" bash "$SCRIPT" >/dev/null 2>&1 || status=$?
if [ "$status" -eq 1 ]; then
  echo "ok: 版ファイルが存在しない"
else
  echo "FAIL: 版ファイルが存在しない (exit=${status} 期待=1)"
  failures=$((failures + 1))
fi

[ "$failures" -eq 0 ]
