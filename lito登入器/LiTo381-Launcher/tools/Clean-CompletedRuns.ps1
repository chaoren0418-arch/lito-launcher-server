param([switch]$Apply,[ValidateRange(3,50)][int]$Keep=5)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$appRoot=Join-Path $projectRoot 'app'
$active=Get-CimInstance Win32_Process | Where-Object {
    ($_.Name -eq 'LiTo381Launcher.exe' -and $_.ExecutablePath -and $_.ExecutablePath.StartsWith($projectRoot,[StringComparison]::OrdinalIgnoreCase)) -or
    ($_.Name -in @('python.exe','java.exe') -and $_.CommandLine -like "*$projectRoot*")
}
if($active){throw 'Close this launcher and its owned helper/server before pruning completed runs.'}
$runs=Join-Path $appRoot 'runs'
if(-not(Test-Path -LiteralPath $runs)){return}
$completed=Get-ChildItem -LiteralPath $runs -Directory | Where-Object {
    $receipt=Join-Path $_.FullName 'terminal.json'
    if(Test-Path -LiteralPath $receipt){try{(Get-Content -LiteralPath $receipt -Raw -Encoding UTF8 | ConvertFrom-Json).cleanup_verified -eq $true}catch{$false}}
} | Sort-Object LastWriteTime -Descending | Select-Object -Skip $Keep
foreach($run in $completed){
    foreach($base in @('runs','gui-runs','server\runs')){
        $target=[IO.Path]::GetFullPath((Join-Path (Join-Path $appRoot $base) $run.Name))
        if(-not $target.StartsWith($appRoot+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Outside app runtime'}
        if(-not(Test-Path -LiteralPath $target)){continue}
        if((Get-Item -LiteralPath $target).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Reparse point refused'}
        if(Get-ChildItem -LiteralPath $target -Recurse -Force | Where-Object {$_.Attributes -band [IO.FileAttributes]::ReparsePoint}){throw 'Nested reparse point refused'}
        if($Apply){Remove-Item -LiteralPath $target -Recurse -Force}else{Write-Output $target}
    }
}
