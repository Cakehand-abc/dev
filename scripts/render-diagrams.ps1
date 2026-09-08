param(
    [string]$PlantUmlJar = "D:\maven\repository\net\sourceforge\plantuml\plantuml\1.2024.8\plantuml-1.2024.8.jar"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root "docs\diagrams"
$output = Join-Path $source "rendered"

if (-not (Test-Path -LiteralPath $PlantUmlJar)) {
    throw "PlantUML JAR 不存在：$PlantUmlJar。先执行 mvn dependency:get -Dartifact=net.sourceforge.plantuml:plantuml:1.2024.8"
}

New-Item -ItemType Directory -Force -Path $output | Out-Null
& java "-Dfile.encoding=UTF-8" -jar $PlantUmlJar -charset UTF-8 -tpng -o $output (Join-Path $source "*.puml")
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Get-ChildItem -LiteralPath $output -Filter "*.png" | Sort-Object Name | Select-Object Name, Length
