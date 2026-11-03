# Uso: .\scripts\servidor.ps1 [porta] [-Compilar]
param([int]$Porta = 5000, [switch]$Compilar)
. "$PSScriptRoot\ambiente.ps1"

if ($Compilar) { Build-Projeto }
$cp = Get-Classpath
Write-Host "Java: $Java" -ForegroundColor DarkGray
& $Java -cp $cp br.com.luka.chat.server.Servidor $Porta

