#Requires -Version 7
<#
.SYNOPSIS
  ローカルのエミュレータで UI テストを実行し、結果を HEAD の commit status として記録する。

.DESCRIPTION
  Linux CI の API 37 エミュレータは不安定なため（#101）、targetSdk 引き上げの検証はここで行う。
  システムイメージの取得・AVD の作成・エミュレータの起動と停止まで自動化し、PC を変えても同じ手順で再現できる。
  結果は HEAD の SHA に紐づく commit status（既定: ui-test/api37-local）として PR に残る。
  push すると SHA が変わって status が外れるため、変更後は再実行が必要になる。

.PARAMETER ApiLevel
  検証するエミュレータの API レベル（SDK のパッケージ名どおり。例: 37.0）。

.PARAMETER NoPost
  commit status を投稿しない（動作確認用）。未コミットの変更があっても実行できる。

.PARAMETER KeepEmulator
  実行後にエミュレータを停止しない。
#>
[CmdletBinding()]
param(
    [string]$ApiLevel = '37.0',
    [switch]$NoPost,
    [switch]$KeepEmulator
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$apiMajor = [int]$ApiLevel.Split('.')[0]
$avdName = "costcalc_api$apiMajor"
$systemImage = "system-images;android-$ApiLevel;google_apis;x86_64"
$statusContext = "ui-test/api$apiMajor-local"
$resultsDir = Join-Path $repoRoot 'app/build/outputs/androidTest-results/connected'

# ネイティブコマンドは終了コードで失敗を検知する（$ErrorActionPreference では捕まらない）。
function Invoke-Native {
    param([string]$File, [string[]]$Arguments, [string]$What)
    $output = & $File @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "$What に失敗した (exit=$LASTEXITCODE): $($output -join [Environment]::NewLine)"
    }
    return $output
}

function Get-SdkRoot {
    foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA 'Android/Sdk'))) {
        if ($candidate -and (Test-Path $candidate)) { return $candidate }
    }
    throw 'Android SDK が見つからない。ANDROID_HOME を設定するか README の SDK セットアップを実施する'
}

# 結果 XML の件数を合算する。AGP は APK のインストールに失敗してもテスト 0 件のまま
# BUILD SUCCESSFUL を返すため、Gradle の終了コードだけでは成否を判定できない。
function Get-ResultSummary {
    param([string]$Dir)
    $total = [pscustomobject]@{ Tests = 0; Failures = 0; Errors = 0; Skipped = 0 }
    if (-not (Test-Path $Dir)) { return $total }
    foreach ($file in Get-ChildItem -Path $Dir -Filter 'TEST-*.xml' -Recurse) {
        $suite = ([xml](Get-Content -Raw -Path $file.FullName)).DocumentElement
        $total.Tests += [int]$suite.GetAttribute('tests')
        $total.Failures += [int]$suite.GetAttribute('failures')
        $total.Errors += [int]$suite.GetAttribute('errors')
        $total.Skipped += [int]$suite.GetAttribute('skipped')
    }
    return $total
}

function Wait-EmulatorBoot {
    param([string]$Adb, [string]$Serial, [System.Diagnostics.Process]$Process, [string]$LogPath)
    $deadline = (Get-Date).AddMinutes(5)
    while ((Get-Date) -lt $deadline) {
        if ($Process.HasExited) {
            throw "エミュレータが起動前に終了した (exit=$($Process.ExitCode))。ログ: $LogPath"
        }
        $booted = (& $Adb -s $Serial shell getprop sys.boot_completed 2>$null) -join ''
        if ($booted.Trim() -eq '1') { return }
        Start-Sleep -Seconds 3
    }
    throw "エミュレータが 5 分以内に起動しなかった。ログ: $LogPath"
}

$sdk = Get-SdkRoot
$adb = Join-Path $sdk 'platform-tools/adb.exe'
$emulator = Join-Path $sdk 'emulator/emulator.exe'
$sdkmanager = Join-Path $sdk 'cmdline-tools/latest/bin/sdkmanager.bat'
$avdmanager = Join-Path $sdk 'cmdline-tools/latest/bin/avdmanager.bat'
foreach ($tool in @($adb, $emulator, $sdkmanager, $avdmanager)) {
    if (-not (Test-Path $tool)) { throw "必要なツールが見つからない: $tool（README の SDK セットアップを参照）" }
}

# status の対象は HEAD の SHA。テスト結果と一致させるため、投稿する場合は実行前に検証する。
$headSha = $null
$repoSlug = $null
if (-not $NoPost) {
    Push-Location $repoRoot
    try {
        if (Invoke-Native git @('status', '--porcelain') '作業ツリーの確認') {
            throw '未コミットの変更がある。コミットしてから実行する（変更したまま確認するだけなら -NoPost）'
        }
        $headSha = (Invoke-Native git @('rev-parse', 'HEAD') 'HEAD の取得') -join ''
        if (-not (Invoke-Native git @('branch', '-r', '--contains', $headSha) 'push 済みの確認')) {
            throw "HEAD ($headSha) が push されていない。push してから実行する"
        }
        $repoSlug = (Invoke-Native gh @('repo', 'view', '--json', 'nameWithOwner', '--jq', '.nameWithOwner') 'リポジトリ名の取得') -join ''
    } finally {
        Pop-Location
    }
}

# connectedDebugAndroidTest は接続中の全端末で実行されるため、対象以外が繋がっていると結果が混ざる。
$attached = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' })
if ($attached.Count -gt 0) {
    throw "接続中の端末がある。切断または停止してから実行する: $($attached -join ', ')"
}

if (-not (Test-Path (Join-Path $sdk ($systemImage -replace ';', '/')))) {
    Write-Host "システムイメージを取得する: $systemImage"
    Invoke-Native $sdkmanager @('--install', $systemImage) 'システムイメージの取得' | Out-Null
}

if (-not (@(& $emulator -list-avds) -contains $avdName)) {
    Write-Host "AVD を作成する: $avdName"
    # カスタムハードウェアプロファイルの確認に no と答える。
    $created = 'no' | & $avdmanager create avd -n $avdName -k $systemImage -d pixel_6 2>&1
    if ($LASTEXITCODE -ne 0) { throw "AVD の作成に失敗した: $($created -join [Environment]::NewLine)" }
}

$emulatorLog = Join-Path ([IO.Path]::GetTempPath()) "costcalc-emulator-$avdName.log"
$emulatorProcess = $null
$state = 'error'
$description = ''
try {
    Write-Host "エミュレータを起動する: $avdName（ログ: $emulatorLog）"
    $emulatorArgs = @(
        '-avd', $avdName, '-no-window', '-gpu', 'swiftshader_indirect', '-noaudio',
        '-no-boot-anim', '-memory', '2048', '-no-snapshot'
    )
    $emulatorProcess = Start-Process -FilePath $emulator -ArgumentList $emulatorArgs -WindowStyle Hidden `
        -RedirectStandardOutput $emulatorLog -RedirectStandardError "$emulatorLog.err" -PassThru

    Invoke-Native $adb @('wait-for-device') '端末の接続待ち' | Out-Null
    $serial = ((& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '^emulator-\d+\tdevice$' }) -split "`t")[0]
    if (-not $serial) { throw 'エミュレータの端末を特定できない' }
    Wait-EmulatorBoot -Adb $adb -Serial $serial -Process $emulatorProcess -LogPath $emulatorLog

    # 違う API のイメージで検証した結果を、API 37 の検証として記録しないための確認。
    $actualApi = [int](((Invoke-Native $adb @('-s', $serial, 'shell', 'getprop', 'ro.build.version.sdk') 'API レベルの取得') -join '').Trim())
    if ($actualApi -ne $apiMajor) { throw "エミュレータの API レベルが想定と違う: 期待=$apiMajor 実際=$actualApi" }

    foreach ($setting in 'window_animation_scale', 'transition_animation_scale', 'animator_duration_scale') {
        Invoke-Native $adb @('-s', $serial, 'shell', 'settings', 'put', 'global', $setting, '0') 'アニメーションの無効化' | Out-Null
    }

    Remove-Item -Recurse -Force -Path $resultsDir -ErrorAction SilentlyContinue
    $env:ANDROID_SERIAL = $serial
    Push-Location $repoRoot
    try {
        & (Join-Path $repoRoot 'gradlew.bat') ':app:connectedDebugAndroidTest'
        $gradleExit = $LASTEXITCODE
    } finally {
        Pop-Location
        Remove-Item Env:ANDROID_SERIAL -ErrorAction SilentlyContinue
    }

    $summary = Get-ResultSummary -Dir $resultsDir
    Write-Host ("結果: tests={0} failures={1} errors={2} skipped={3} gradle={4}" -f $summary.Tests, $summary.Failures, $summary.Errors, $summary.Skipped, $gradleExit)

    if ($summary.Tests -eq 0) {
        $state = 'error'
        $description = "UI テストが 1 件も実行されなかった (API $apiMajor, local)"
    } elseif ($gradleExit -ne 0 -or $summary.Failures -gt 0 -or $summary.Errors -gt 0) {
        $state = 'failure'
        $description = "UI テスト失敗: failures=$($summary.Failures) errors=$($summary.Errors) (API $apiMajor, local)"
    } else {
        $state = 'success'
        $description = "UI テスト $($summary.Tests) 件成功 (API $apiMajor, local)"
    }
} finally {
    if ($emulatorProcess -and -not $KeepEmulator) {
        & $adb emu kill 2>&1 | Out-Null
        if (-not $emulatorProcess.WaitForExit(30000)) { Stop-Process -Id $emulatorProcess.Id -Force }
    }
}

Write-Host "判定: $state - $description"
if (-not $NoPost) {
    Invoke-Native gh @(
        'api', "repos/$repoSlug/statuses/$headSha",
        '-f', "state=$state", '-f', "context=$statusContext", '-f', "description=$description"
    ) 'commit status の投稿' | Out-Null
    Write-Host "commit status を投稿した: $statusContext = $state ($headSha)"
}

if ($state -ne 'success') { exit 1 }
