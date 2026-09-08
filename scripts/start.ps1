param([string]$Profile='local',[int]$Port=8080,[string]$EnvironmentFile)
$ErrorActionPreference='Stop'
$careRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if($EnvironmentFile){
    $careConfig=Get-Content -LiteralPath $EnvironmentFile -Raw | ConvertFrom-Json
    foreach($careProperty in $careConfig.PSObject.Properties){
        if($careProperty.Name -in @('DB_URL','DB_USERNAME','DB_PASSWORD','APP_ADMIN_PASSWORD')){[Environment]::SetEnvironmentVariable($careProperty.Name,[string]$careProperty.Value,'Process')}
    }
}
if(-not $env:DB_PASSWORD){throw '请配置 DB_PASSWORD，或提供本地环境配置文件'}
$env:SPRING_PROFILES_ACTIVE=$Profile;$env:SERVER_PORT=[string]$Port
if($Profile -eq 'demo'){$env:APP_SEED_DEMO='true'}else{$env:APP_SEED_DEMO='false'}
$careJar=Join-Path $careRoot 'target\dev-0.0.1-SNAPSHOT.jar'
if(-not(Test-Path -LiteralPath $careJar)){throw '请先运行 scripts/build.ps1'}
Write-Output "启动配置 $Profile，访问 http://127.0.0.1:$Port。按 Ctrl+C 停止。"
& java '-Dfile.encoding=UTF-8' '-jar' $careJar
exit $LASTEXITCODE
