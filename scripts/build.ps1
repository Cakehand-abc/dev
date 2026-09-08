param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
$careRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $careRoot
try {
    Push-Location (Join-Path $careRoot 'frontend')
    try { & npm.cmd ci --no-audit --no-fund; if($LASTEXITCODE -ne 0){throw 'npm ci 失败'}; & npm.cmd run build; if($LASTEXITCODE -ne 0){throw '前端构建失败'} }
    finally { Pop-Location }
    $careStatic = [IO.Path]::GetFullPath((Join-Path $careRoot 'src\main\resources\static'))
    if(-not $careStatic.StartsWith($careRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw '静态产物路径超出项目'}
    # This directory contains only generated build output and is excluded from Git.
    if(Test-Path -LiteralPath $careStatic){Remove-Item -LiteralPath $careStatic -Recurse -Force}
    New-Item -ItemType Directory -Path $careStatic | Out-Null
    Copy-Item -Path (Join-Path $careRoot 'frontend\dist\*') -Destination $careStatic -Recurse
    $careMavenArgs=@('-B','-ntp','package'); if($SkipTests){$careMavenArgs+= '-DskipTests'}
    & (Join-Path $careRoot 'mvnw.cmd') @careMavenArgs
    if($LASTEXITCODE -ne 0){throw 'Maven 打包失败'}
    Write-Output ('构建完成：'+(Join-Path $careRoot 'target\dev-0.0.1-SNAPSHOT.jar'))
} finally { Pop-Location }
