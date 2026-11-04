# Funcoes comuns: localiza um Java 21+ e compila o projeto se necessario.
$ErrorActionPreference = 'Stop'
$Raiz = Split-Path -Parent $PSScriptRoot
Set-Location $Raiz

function Get-VersaoJava($exe) {
    # java -version escreve no stderr; nao tratar isso como erro.
    $ErrorActionPreference = 'Continue'
    $saida = & $exe -version 2>&1 | Out-String
    if ($saida -match 'version "(1\.)?(\d+)') { return [int]$Matches[2] }
    return 0
}

function Find-Java {
    $candidatos = @()
    if ($env:JAVA_HOME) { $candidatos += Join-Path $env:JAVA_HOME 'bin\java.exe' }
    $candidatos += "$env:USERPROFILE\Downloads\jdk-26_windows-x64_bin\jdk-26.0.2.1\bin\java.exe"
    
    # Procura JREs embutidos em extensoes do VS Code (Temurin / RedHat Java)
    $vscodeJdks = Get-ChildItem -Path "$env:USERPROFILE\.vscode\extensions" -Filter "java.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    if ($vscodeJdks) { $candidatos += $vscodeJdks }

    $noPath = Get-Command java -ErrorAction SilentlyContinue
    if ($noPath) { $candidatos += $noPath.Source }

    foreach ($exe in $candidatos) {
        if ((Test-Path $exe) -and (Get-VersaoJava $exe) -ge 21) { return $exe }
    }
    throw "Nenhum Java 21+ encontrado. Defina JAVA_HOME apontando para um JDK 21 ou superior."
}

$Java = Find-Java
# O Maven usa o mesmo JDK encontrado.
$env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $Java)

function Build-Projeto {
    Write-Host "Compilando o projeto..." -ForegroundColor Cyan
    mvn -q compile dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
    if ($LASTEXITCODE -ne 0) { throw "Falha na compilacao." }
}

function Get-Classpath {
    if (-not (Test-Path 'target/classpath.txt') -or -not (Test-Path 'target/classes')) {
        Build-Projeto
    }
    return "target/classes;" + (Get-Content 'target/classpath.txt' -Raw).Trim()
}

