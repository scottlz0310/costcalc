#!/usr/bin/env bash
# notify-api37-green.sh の分岐を、gh をスタブに差し替えて確認する（ネットワーク不要）。
set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")/.." && pwd)/notify-api37-green.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# スタブ: issue view は STUB_LAST_BODY / STUB_VIEW_FAIL に従い、issue comment は本文をファイルへ記録する。
cat >"$WORK/gh" <<'STUB'
#!/usr/bin/env bash
case "$1 $2" in
  "issue view")
    [ "${STUB_VIEW_FAIL:-}" = "1" ] && { echo "view failed" >&2; exit 1; }
    printf '%s' "${STUB_LAST_BODY:-}"
    ;;
  "issue comment")
    printf '%s' "$5" >"$STUB_POSTED"
    ;;
esac
STUB
chmod +x "$WORK/gh"

failures=0
# 名前 | 直前コメント本文 | view を失敗させるか | 期待する終了コード | 投稿されるか
cases=(
  "コメントなし|||0|yes"
  "人のコメントが直前|別のコメント||0|yes"
  "直前が同じ通知|<!-- api37-ci-green -->前回の通知||0|no"
  "コメントの取得に失敗||1|1|no"
)

for entry in "${cases[@]}"; do
  IFS='|' read -r name last fail want_exit want_post <<<"$entry"
  rm -f "$WORK/posted"
  status=0
  GH_BIN="$WORK/gh" ISSUE=101 RUN_URL="https://example.test/run/1" \
    STUB_LAST_BODY="$last" STUB_VIEW_FAIL="$fail" STUB_POSTED="$WORK/posted" \
    bash "$SCRIPT" >/dev/null 2>&1 || status=$?

  posted=no
  [ -f "$WORK/posted" ] && posted=yes
  if [ "$status" -ne "$want_exit" ] || [ "$posted" != "$want_post" ]; then
    echo "FAIL: ${name} (exit=${status} 期待=${want_exit}, 投稿=${posted} 期待=${want_post})"
    failures=$((failures + 1))
  else
    echo "ok: ${name}"
  fi
done

# 投稿される本文に、重複防止のマーカーと実行リンクが含まれること。
rm -f "$WORK/posted"
GH_BIN="$WORK/gh" ISSUE=101 RUN_URL="https://example.test/run/1" STUB_POSTED="$WORK/posted" bash "$SCRIPT" >/dev/null
if grep -q '<!-- api37-ci-green -->' "$WORK/posted" && grep -q 'https://example.test/run/1' "$WORK/posted"; then
  echo "ok: 本文にマーカーと実行リンクを含む"
else
  echo "FAIL: 本文にマーカーまたは実行リンクが無い"
  failures=$((failures + 1))
fi

[ "$failures" -eq 0 ]
