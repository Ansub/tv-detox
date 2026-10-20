<#
.SYNOPSIS
    Desinstalador e Restaurador do TV Detox para Windows (Português do Brasil)
.DESCRIPTION
    Reativa todos os aplicativos que foram desativados e remove o TV Detox,
    restaurando a tela inicial de fábrica da sua TV.
#>
param(
    [string]$IpDaTv = ""
)

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "    TV Detox Brasil - Restaurador Padrão  " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

if ([string]::IsNullOrWhiteSpace($IpDaTv)) {
    $IpDaTv = Read-Host "Digite o IP da sua TV (ex: 192.168.15.11)"
}

$IpDaTv = $IpDaTv.Trim()
if ([string]::IsNullOrWhiteSpace($IpDaTv)) {
    Write-Host "[ERRO] Nenhum endereço IP fornecido. Encerrando." -ForegroundColor Red
    exit 1
}

$TV = if ($IpDaTv -match ":\d+$") { $IpDaTv } else { "$($IpDaTv):5555" }

# Localizar ADB
$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
$scriptDir = $PSScriptRoot
if (-not $scriptDir) { $scriptDir = (Get-Location).Path }

if (-not $adbCmd) {
    $localAdb = Join-Path $scriptDir "platform-tools\adb.exe"
    $parentAdb = Join-Path (Split-Path $scriptDir -Parent) "platform-tools\adb.exe"
    if (Test-Path $localAdb) { $adbExe = $localAdb }
    elseif (Test-Path $parentAdb) { $adbExe = $parentAdb }
    else { $adbExe = "adb" }
} else {
    $adbExe = "adb"
}

Write-Host "-> Conectando a $TV..." -ForegroundColor Cyan
& $adbExe connect $TV | Out-Null

$state = (& $adbExe -s $TV get-state 2>&1) | Out-String
if ($state -notmatch "device") {
    Write-Host "[ERRO] Nao foi possivel comunicar com $TV." -ForegroundColor Red
    exit 1
}

Write-Host "-> Lendo lista de aplicativos desativados..." -ForegroundColor Cyan
$rawList = (& $adbExe -s $TV shell "if [ -e /data/local/tmp/tvhome-disabled.txt ]; then cat /data/local/tmp/tvhome-disabled.txt; fi" 2>&1) | Out-String
$pkgs = $rawList -split "`r?`n" | Where-Object { $_.Trim() -ne "" } | Select-Object -Unique

foreach ($pkg in $pkgs) {
    Write-Host "  Reativando: $pkg..." -ForegroundColor Gray
    & $adbExe -s $TV shell pm enable $pkg | Out-Null
}

# Setupwraith
& $adbExe -s $TV shell pm enable com.google.android.tungsten.setupwraith 2>&1 | Out-Null

Write-Host "-> Removendo TV Detox..." -ForegroundColor Cyan
& $adbExe -s $TV shell rm -f /data/local/tmp/tvhome-disabled.txt | Out-Null
& $adbExe -s $TV uninstall com.ansub.tvhome 2>&1 | Out-Null
& $adbExe -s $TV shell input keyevent KEYCODE_HOME | Out-Null

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "      Restauracao Concluida com Sucesso!  " -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host "Sua TV voltou a configuracao original de fabrica." -ForegroundColor Cyan
