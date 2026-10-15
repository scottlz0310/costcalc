#!/usr/bin/env bash
# 非必須の API 37 ジョブが緑になったとき、必須化を判断できるよう #101 に通知する。
#
# 緑が続く間は毎週同じコメントが増えるため、直前のコメントが同じ通知なら投稿しない。
# 人のコメントが挟まれば、次に緑になった時にまた通知する。
set -euo pipefail

GH_BIN="${GH_BIN:-gh}"
ISSUE="${ISSUE:?通知先の Issue 番号を ISSUE で指定する}"
RUN_URL="${RUN_URL:?実行へのリンクを RUN_URL で指定する}"
MARKER='<!-- api37-ci-green -->'

last_body="$("$GH_BIN" issue view "$ISSUE" --json comments --jq '.comments | last | .body // ""')"
if [[ "$last_body" == *"$MARKER"* ]]; then
  echo "直前のコメントが同じ通知のため投稿しない"
  exit 0
fi

body="${MARKER}
Linux CI の API 37 の UI テストが全件成功しました（週次ジョブ）。

- 実行: ${RUN_URL}

必須にできるか判断してください。判断の目安は次のとおりです。
- 複数回連続で緑になっているか（初回 0 件・再試行で失敗する等の不安定さが残っていないか）
- 必須にしたら、\`scripts/run-ui-tests-local.ps1\` によるローカル検証の運用を廃止する"

"$GH_BIN" issue comment "$ISSUE" --body "$body"
