@echo off
title TV Detox Brasil - Instalador
chcp 65001 >nul
echo Iniciando TV Detox Brasil...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0instalar.ps1"
pause
