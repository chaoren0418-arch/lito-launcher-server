param([switch]$Apply)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$allowed=@('src\bin','src\obj','app\__pycache__','app\server\__pycache__','tools\__pycache__')
foreach($relative in $allowed){
    $target=[IO.Path]::GetFullPath((Join-Path $projectRoot $relative))
    if(-not $target.StartsWith($projectRoot+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Target outside project'}
    if(-not(Test-Path -LiteralPath $target)){continue}
    $item=Get-Item -LiteralPath $target -Force
    if($item.Attributes -band [IO.FileAttributes]::ReparsePoint){throw "Reparse target: $target"}
    $nested=Get-ChildItem -LiteralPath $target -Recurse -Force | Where-Object {$_.Attributes -band [IO.FileAttributes]::ReparsePoint}
    if($nested){throw "Nested reparse point: $target"}
    if($Apply){Remove-Item -LiteralPath $target -Recurse -Force}else{Write-Output $target}
}
# Session records are never removed by this build-cache cleanup.
