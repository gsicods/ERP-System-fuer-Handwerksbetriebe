# PreToolUse-Hook für Grep|Glob: erzwingt graphify-Nutzung VOR Raw-Search.
#
# Logik:
#   1. Hook liest Hook-Input-JSON von stdin (enthält session_id).
#   2. Wenn pro Session noch KEIN graphify-Aufruf gesehen wurde -> exit 2
#      (Permission denied, mit klarer Begründung -> Claude reagiert darauf).
#   3. Sobald irgendwann ein Bash mit "graphify " gelaufen ist, wird ein
#      Session-Flag gesetzt (siehe mark-graphify-used.ps1) und der Hook
#      lässt Grep/Glob durch.
#   4. Bei freigeschaltetem Flag immer noch additionalContext mit Erinnerung.

$ErrorActionPreference = 'SilentlyContinue'

$graphFile = "c:\dev\ERP-System-fuer-Handwerksbetriebe\graphify-out\graph.json"
if (-not (Test-Path $graphFile)) { exit 0 }  # Kein Graph -> kein Zwang.

# Hook-Input einlesen (Claude Code übergibt JSON mit session_id auf stdin)
$raw = [Console]::In.ReadToEnd()
$sessionId = $null
try {
    $payload = $raw | ConvertFrom-Json
    $sessionId = $payload.session_id
} catch {
    # ohne Session-ID -> nur sanfter Reminder, kein Block
}

$flagDir = Join-Path $env:TEMP "claude-erp-graphify-flags"
$flagFile = if ($sessionId) { Join-Path $flagDir ("session-$sessionId.flag") } else { $null }
$graphifyAlreadyUsed = ($flagFile -and (Test-Path $flagFile))

$reminder  = "GRAPHIFY ZUERST: Dieser Workspace hat einen Knowledge-Graphen.`n"
$reminder += "  graphify query `"<Frage>`"        -> fokussierter Subgraph (viel kleiner als grep)`n"
$reminder += "  graphify path  `"<A>`" `"<B>`"    -> Beziehung zwischen zwei Komponenten`n"
$reminder += "  graphify explain `"<Konzept>`"    -> erklaerter Konzept-Auszug`n"
$reminder += "  graphify-out/wiki/index.md        -> breite Navigation`n"
$reminder += "Greife auf Grep/Glob nur zurueck, wenn graphify nicht reicht."

if ($sessionId -and -not $graphifyAlreadyUsed) {
    # Harter Block: Claude bekommt exit 2 + stderr -> muss zuerst graphify aufrufen.
    [Console]::Error.WriteLine("[graphify-gate] BLOCKIERT: In dieser Session wurde noch kein graphify-Befehl aufgerufen.")
    [Console]::Error.WriteLine("")
    [Console]::Error.WriteLine($reminder)
    [Console]::Error.WriteLine("")
    [Console]::Error.WriteLine("Wenn graphify keine Antwort liefert: trotzdem zuerst 'graphify query ...' laufen lassen")
    [Console]::Error.WriteLine("(Beweis, dass nichts kommt) - danach setzt mark-graphify-used.ps1 das Flag und")
    [Console]::Error.WriteLine("Grep/Glob ist fuer den Rest der Session frei.")
    exit 2
}

# Flag gesetzt (oder keine Session-ID) -> Reminder als additionalContext mitgeben, aber durchlassen.
$output = [ordered]@{
    hookSpecificOutput = [ordered]@{
        hookEventName     = "PreToolUse"
        additionalContext = $reminder
    }
} | ConvertTo-Json -Compress

Write-Output $output
exit 0
