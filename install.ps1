<#
.SYNOPSIS
    Instalador do TV Detox para Windows (Português do Brasil)
.DESCRIPTION
    Instala e define a tela inicial ultraleve TV Detox na sua Android TV / Google TV.
    Garante que o ADB esteja instalado e guia o usuário em todas as etapas.
.PARAMETER IpDaTv
    Endereço IP da TV na rede Wi-Fi (ex: 192.168.15.11).
.PARAMETER Apps
    Lista de nomes de pacotes separados por vírgula (opcional; se omitido, usa o apps.txt).
#>
param(
    [string]$IpDaTv = "",
    [string]$Apps = ""
)

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "    TV Detox Brasil - Instalador Windows  " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Obter o IP da TV
if ([string]::IsNullOrWhiteSpace($IpDaTv)) {
    $IpDaTv = Read-Host "Digite o IP da sua TV (ex: 192.168.15.11)"
}

$IpDaTv = $IpDaTv.Trim()
if ([string]::IsNullOrWhiteSpace($IpDaTv)) {
    Write-Host "[ERRO] Nenhum endereço IP fornecido. Encerrando." -ForegroundColor Red
    exit 1
}

$TV = if ($IpDaTv -match ":\d+$") { $IpDaTv } else { "$($IpDaTv):5555" }

# 2. Localizar ou baixar o ADB
$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
$scriptDir = $PSScriptRoot
if (-not $scriptDir) { $scriptDir = (Get-Location).Path }

if (-not $adbCmd) {
    $localAdb = Join-Path $scriptDir "platform-tools\adb.exe"
    $parentAdb = Join-Path (Split-Path $scriptDir -Parent) "platform-tools\adb.exe"
    
    if (Test-Path $localAdb) {
        $adbExe = $localAdb
    } elseif (Test-Path $parentAdb) {
        $adbExe = $parentAdb
    } else {
        Write-Host "-> ADB nao encontrado. Baixando platform-tools oficial do Google..." -ForegroundColor Yellow
        $zipPath = Join-Path $scriptDir "platform-tools.zip"
        Invoke-WebRequest -Uri "https://dl.google.com/android/repository/platform-tools-latest-windows.zip" -OutFile $zipPath
        Expand-Archive -Path $zipPath -DestinationPath $scriptDir -Force
        Remove-Item $zipPath -Force
        $adbExe = Join-Path $scriptDir "platform-tools\adb.exe"
    }
} else {
    $adbExe = "adb"
}

Write-Host "[OK] ADB pronto: $adbExe" -ForegroundColor Green

# 3. Conectar à TV
Write-Host "-> Conectando a $TV..." -ForegroundColor Cyan
& $adbExe connect $TV | Out-Null

$state = (& $adbExe -s $TV get-state 2>&1) | Out-String
$state = $state.Trim()

if ($state -match "unauthorized") {
    Write-Host ""
    Write-Host "[ATENCAO] Olhe para a tela da sua TV agora!" -ForegroundColor Yellow
    Write-Host "Apareceu uma mensagem: 'Permitir depuracao USB?'" -ForegroundColor Yellow
    Write-Host "Marque 'Sempre permitir a partir deste computador' e clique em OK/Permitir." -ForegroundColor Yellow
    Read-Host "Pressione ENTER apos confirmar na TV"
    $state = (& $adbExe -s $TV get-state 2>&1) | Out-String
}

if ($state -notmatch "device") {
    Write-Host "[ERRO] Nao foi possivel conectar a $TV." -ForegroundColor Red
    Write-Host "Verifique se a TV esta ligada, no mesmo Wi-Fi do computador e com a Depuracao USB ativada." -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] Conectado a TV com sucesso!" -ForegroundColor Green

# 4. Baixar o APK do TV Detox se necessario
$outDir = Join-Path $scriptDir "out"
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$apkPath = Join-Path $outDir "tvhome.apk"

if (-not (Test-Path $apkPath)) {
    Write-Host "-> Baixando a tela inicial ultraleve (tvhome.apk)..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri "https://github.com/Ansub/tv-detox/releases/latest/download/tvhome.apk" -OutFile $apkPath
}

# 5. Instalar o aplicativo na TV
Write-Host "-> Instalando TV Detox na TV..." -ForegroundColor Cyan
$installOut = (& $adbExe -s $TV install -r $apkPath 2>&1) | Out-String

if ($LASTEXITCODE -ne 0 -or $installOut -match "FAILED") {
    if ($installOut -match "UPDATE_INCOMPATIBLE") {
        Write-Host "Substituindo versao anterior assinada com outra chave..." -ForegroundColor Yellow
        & $adbExe -s $TV uninstall com.ansub.tvhome | Out-Null
        & $adbExe -s $TV install $apkPath | Out-Null
    } else {
        Write-Host "[ERRO] Falha ao instalar: $installOut" -ForegroundColor Red
        exit 1
    }
}
Write-Host "[OK] TV Detox instalado com sucesso!" -ForegroundColor Green

# 6. Carregar lista de aplicativos (apps.txt se nao passado)
if ([string]::IsNullOrWhiteSpace($Apps)) {
    $appsFile = Join-Path $scriptDir "apps.txt"
    if (Test-Path $appsFile) {
        $lines = Get-Content $appsFile | Where-Object { $_ -notmatch "^\s*#" -and $_.Trim() -ne "" }
        $Apps = ($lines -join ",").Trim()
    }
}

if (-not [string]::IsNullOrWhiteSpace($Apps)) {
    Write-Host "-> Configurando atalhos na tela inicial..." -ForegroundColor Cyan
    $stamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    & $adbExe -s $TV shell am start -n com.ansub.tvhome/.MainActivity --es stamp "$stamp" --es apps "`"$Apps`"" | Out-Null
}

# 7. Definir TV Detox como Home Padrao
Write-Host "-> Definindo TV Detox como tela inicial padrao..." -ForegroundColor Cyan
$sdk = (& $adbExe -s $TV shell getprop ro.build.version.sdk).Trim()
if ([int]$sdk -ge 29) {
    & $adbExe -s $TV shell cmd role add-role-holder --user 0 android.app.role.HOME com.ansub.tvhome 2>&1 | Out-Null
}
& $adbExe -s $TV shell cmd package set-home-activity com.ansub.tvhome/.MainActivity 2>&1 | Out-Null

# 8. Verificar se o lançador antigo ainda intercepta o botão Home
$currentHome = (& $adbExe -s $TV shell "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME" | Select-Object -Last 1).Trim()

if ($currentHome -notmatch "com\.ansub\.tvhome") {
    Write-Host ""
    Write-Host "A interface original da TV ($currentHome) ainda esta assumindo o botao Home." -ForegroundColor Yellow
    $ans = Read-Host "Deseja desativar o inicializador antigo para que o TV Detox abra no botao Home? (S/N) [Padrao: S]"
    if ([string]::IsNullOrWhiteSpace($ans) -or $ans -match "^[sSyY]") {
        $pkg = $currentHome.Split('/')[0]
        Write-Host "-> Desativando $pkg com seguranca..." -ForegroundColor Cyan
        & $adbExe -s $TV shell "echo $pkg >> /data/local/tmp/tvhome-disabled.txt"
        & $adbExe -s $TV shell pm disable-user --user 0 $pkg | Out-Null
        Start-Sleep -Seconds 15
        & $adbExe -s $TV shell cmd package set-home-activity com.ansub.tvhome/.MainActivity 2>&1 | Out-Null
    }
}

# 9. Finalização
& $adbExe -s $TV shell input keyevent KEYCODE_HOME | Out-Null
Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "      Instalacao Concluida com Sucesso!   " -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host "Aperte o botao HOME no controle da sua TV para ver a nova tela inicial." -ForegroundColor Cyan
