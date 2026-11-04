# Uso: .\scripts\cliente-gui.ps1 [-Compilar]
param([switch]$Compilar)
. "$PSScriptRoot\ambiente.ps1"

if ($Compilar) { Build-Projeto }
$cp = Get-Classpath
Write-Host "Iniciando Interface Grafica do Chat..." -ForegroundColor Green
& $Java -cp $cp br.com.luka.chat.client.ClienteChatGui

