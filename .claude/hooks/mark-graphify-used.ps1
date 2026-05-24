# PostToolUse-Hook für Bash: setzt das Session-Flag, sobald ein graphify-Befehl
# gelaufen ist. Danach lässt graphify-research-reminder.ps1 Grep/Glob durch.
#
# Erwartet auf stdin den Hook-Input von Claude Code (JSON mit session_id
# und tool_input.command).

$ErrorActionPreference = 'SilentlyContinue'

$raw = [Console]::In.ReadToEnd()
$payload = $null
try {
    $payload = $raw | ConvertFrom-Json
} catch {
    exit 0
}

$sessionId = $payload.session_id
if (-not $sessionId) { exit 0 }

$command = $null
try { $command = $payload.tool_input.command } catch {}
if (-not $command) { exit 0 }

# Nur echte graphify-Aufrufe markieren — keine zufälligen Substrings.
if ($command -notmatch '(^|[\s;&|])graphify(\s|$)') { exit 0 }

$flagDir = Join-Path $env:TEMP "claude-erp-graphify-flags"
if (-not (Test-Path $flagDir)) {
    New-Item -ItemType Directory -Force -Path $flagDir | Out-Null
}
$flagFile = Join-Path $flagDir ("session-$sessionId.flag")
Set-Content -Path $flagFile -Value (Get-Date).ToString('o') -Encoding UTF8

exit 0
