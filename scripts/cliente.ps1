# Uso: .\scripts\cliente.ps1 [-Compilar]
param([switch]$Compilar)
. "$PSScriptRoot\ambiente.ps1"

if ($Compilar) { Build-Projeto }
$cp = Get-Classpath
# Console em UTF-8 para exibir acentos corretamente.
[Console]::OutputEncoding = [Text.Encoding]::UTF8
[Console]::InputEncoding = [Text.Encoding]::UTF8
& $Java "-Dstdout.encoding=UTF-8" "-Dstdin.encoding=UTF-8" -cp $cp br.com.luka.chat.client.ClienteChat

