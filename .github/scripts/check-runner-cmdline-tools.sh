#!/usr/bin/env bash
# 週次 CI 等でランナー標準の Android Command-line Tools バージョンを検査し、
# 22.0 以上への追従を自己検知して #127 に通知する。
#
# ワークアラウンド差し替え前に退避した source.properties を読み込み、
# 22.0 以上であれば GitHub Actions Notice を出力し、週次実行時に #127 へコメントする。
set -euo pipefail

PROPS_FILE="${1:-${RUNNER_TEMP:-/tmp}/android-command-line-tools-original/source.properties}"
REQUIRED_VERSION="22.0"
GH_BIN="${GH_BIN:-gh}"
ISSUE="${ISSUE:-127}"
RUN_URL="${RUN_URL:-}"
MARKER='<!-- runner-cmdline-tools-updated -->'

if [[ ! -f "$PROPS_FILE" ]]; then
  echo "::warning title=Command-line Tools 検知スキップ::source.properties が見つかりません: $PROPS_FILE"
  exit 0
fi

version="$(grep -E '^Pkg\.Revision=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '\r')"
if [[ -z "$version" ]]; then
  echo "::warning title=Command-line Tools 検知スキップ::Pkg.Revision を読み取れませんでした: $PROPS_FILE"
  exit 0
fi

echo "ランナー標準の Command-line Tools バージョン: $version"

# バージョン比較: $version >= $REQUIRED_VERSION
# sort -V で昇順ソートした先頭が REQUIRED_VERSION なら、$version >= $REQUIRED_VERSION
first_ver="$(printf '%s\n%s\n' "$REQUIRED_VERSION" "$version" | sort -V | head -n1)"
if [[ "$first_ver" == "$REQUIRED_VERSION" ]]; then
  is_updated=true
else
  is_updated=false
fi

if [[ "$is_updated" != "true" ]]; then
  echo "ランナー標準ツールはまだ $REQUIRED_VERSION 未満です（$version）。ワークアラウンドを維持します。"
  exit 0
fi

echo "::notice title=ランナー標準 Command-line Tools 更新検知::ランナー標準の Command-line Tools が $version（>= $REQUIRED_VERSION）に更新されました。#127 に従いワークアラウンドの撤去を検討してください。"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "### :rocket: ランナー標準 Command-line Tools 更新検知"
    echo ""
    echo "ランナー標準の Command-line Tools が **${version}**（>= ${REQUIRED_VERSION}）に更新されました。"
    echo "#127 に従い、\`android-ui-tests.yml\` の一時ワークアラウンド撤去を検討してください。"
  } >> "$GITHUB_STEP_SUMMARY"
fi

# 週次実行時（schedule）かつ GitHub CLI が利用可能な場合に Issue #127 へ通知
EVENT_NAME="${GITHUB_EVENT_NAME:-}"
if [[ "$EVENT_NAME" != "schedule" && -z "${FORCE_NOTIFY:-}" ]]; then
  echo "週次実行（schedule）ではないため Issue への通知はスキップします（event: $EVENT_NAME）。"
  exit 0
fi

if ! command -v "$GH_BIN" >/dev/null 2>&1; then
  echo "gh コマンドが見つからないため Issue へのコメントをスキップします。"
  exit 0
fi

last_body="$("$GH_BIN" issue view "$ISSUE" --json comments --jq '.comments | last | .body // ""')"
if [[ "$last_body" == *"$MARKER"* ]]; then
  echo "直前のコメントが同じ更新通知のため投稿しない"
  exit 0
fi

run_link=""
if [[ -n "$RUN_URL" ]]; then
  run_link="- 実行: ${RUN_URL}"$'\n'
fi

body="${MARKER}
ランナー標準の Android Command-line Tools が **${version}**（>= ${REQUIRED_VERSION}）に更新されたことを検知しました（週次ジョブ）。

${run_link}- 検出バージョン: ${version}

DoD に従い、以下の作業を進めてください：
1. \`android-ui-tests.yml\` の \`cmdline-tools;22.0\` 手動導入およびシンボリックリンク差し替えステップを削除
2. 撤去後も AVD target 検査（\`check-avd-target.sh\`）および API 37 UI テスト 24 件が全件成功することを確認"

"$GH_BIN" issue comment "$ISSUE" --body "$body"
echo "Issue #${ISSUE} に更新検知コメントを投稿しました。"
