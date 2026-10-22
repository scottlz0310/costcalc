#!/usr/bin/env bash
# check-runner-cmdline-tools.sh のバージョン判定と通知分岐を検証する。
set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")/.." && pwd)/check-runner-cmdline-tools.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# スタブ gh: issue view は STUB_LAST_BODY に従い、issue comment は本文を記録する。
cat >"$WORK/gh" <<'STUB'
#!/usr/bin/env bash
case "$1 $2" in
  "issue view")
    printf '%s' "${STUB_LAST_BODY:-}"
    ;;
  "issue comment")
    printf '%s' "$5" >"$STUB_POSTED"
    ;;
esac
STUB
chmod +x "$WORK/gh"

failures=0

create_props() {
  local ver="$1"
  local file="$WORK/source.properties"
  cat >"$file" <<EOF
Pkg.Revision=$ver
Pkg.Path=cmdline-tools;latest
Pkg.Desc=Android SDK Command-line Tools
EOF
  echo "$file"
}

# 名前 | バージョン | イベント名 | 直前コメント | 期待する終了コード | 投稿されるか
cases=(
  "ファイル不在||schedule||0|no"
  "12.0（更新なし）|12.0|schedule||0|no"
  "19.0（更新なし）|19.0|schedule||0|no"
  "20.0（22.0未満）|20.0|schedule||0|no"
  "22.0（更新検知・schedule）|22.0|schedule||0|yes"
  "23.0（更新検知・schedule）|23.0|schedule||0|yes"
  "22.0だがPRイベント|22.0|pull_request||0|no"
  "22.0だが直前が同じ通知|22.0|schedule|<!-- runner-cmdline-tools-updated -->前回の通知|0|no"
)

for entry in "${cases[@]}"; do
  IFS='|' read -r name ver event last want_exit want_post <<<"$entry"
  rm -f "$WORK/posted" "$WORK/source.properties"
  status=0
  props_path="$WORK/nonexistent.properties"
  if [[ -n "$ver" ]]; then
    props_path="$(create_props "$ver")"
  fi

  GH_BIN="$WORK/gh" ISSUE=127 RUN_URL="https://example.test/run/100" \
    GITHUB_EVENT_NAME="$event" STUB_LAST_BODY="$last" STUB_POSTED="$WORK/posted" \
    bash "$SCRIPT" "$props_path" >/dev/null 2>&1 || status=$?

  posted=no
  [[ -f "$WORK/posted" ]] && posted=yes
  if [[ "$status" -ne "$want_exit" || "$posted" != "$want_post" ]]; then
    echo "FAIL: ${name} (exit=${status} 期待=${want_exit}, 投稿=${posted} 期待=${want_post})"
    failures=$((failures + 1))
  else
    echo "ok: ${name}"
  fi
done

# 投稿された本文の検証
props="$(create_props '22.0')"
rm -f "$WORK/posted"
GH_BIN="$WORK/gh" ISSUE=127 RUN_URL="https://example.test/run/100" \
  GITHUB_EVENT_NAME="schedule" STUB_POSTED="$WORK/posted" \
  bash "$SCRIPT" "$props" >/dev/null

if grep -q '<!-- runner-cmdline-tools-updated -->' "$WORK/posted" && \
   grep -q 'https://example.test/run/100' "$WORK/posted" && \
   grep -q '22.0' "$WORK/posted"; then
  echo "ok: 本文にマーカー、実行リンク、バージョンを含む"
else
  echo "FAIL: 本文の記載内容が不足している"
  failures=$((failures + 1))
fi

# GITHUB_STEP_SUMMARY の出力検証
summary_file="$WORK/step_summary.md"
GITHUB_STEP_SUMMARY="$summary_file" bash "$SCRIPT" "$props" >/dev/null
if grep -q 'ランナー標準 Command-line Tools 更新検知' "$summary_file" && grep -q '22.0' "$summary_file"; then
  echo "ok: GITHUB_STEP_SUMMARY に正しく出力される"
else
  echo "FAIL: GITHUB_STEP_SUMMARY への出力がない"
  failures=$((failures + 1))
fi

[[ "$failures" -eq 0 ]]
