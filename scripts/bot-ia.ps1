# Uso: .\scripts\bot-ia.ps1 [-Porta 5000] [-Compilar]
param([int]$Porta = 5000, [switch]$Compilar)
. "$PSScriptRoot\ambiente.ps1"

if ($Compilar) { Build-Projeto }
$cp = Get-Classpath
Write-Host "Iniciando Bot IA do Chat..." -ForegroundColor Magenta

# Se o usuario tiver definido $env:GEMINI_API_KEY ou existir chat.local.properties, ele avisa
if ($env:GEMINI_API_KEY) {
    Write-Host "[Info] Chave GEMINI_API_KEY detectada no ambiente." -ForegroundColor Cyan
} elseif (Test-Path "chat.local.properties") {
    Write-Host "[Info] Configuracao chat.local.properties detectada." -ForegroundColor Cyan
} else {
    Write-Host "[Aviso] Nenhuma chave de API detectada. Executando em Modo Demonstracao / Fallback." -ForegroundColor Yellow
}

& $Java -cp $cp br.com.luka.chat.bot.BotIaCliente "localhost" $Porta "Gemini"

