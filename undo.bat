@echo off
title TV Detox Brasil - Restaurar Padrao
chcp 65001 >nul
echo Restaurando configuracao original da TV...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0desfazer.ps1"
pause
