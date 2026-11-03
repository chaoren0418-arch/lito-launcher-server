param([switch]$VerifyOnly)
$ErrorActionPreference='Stop'
$taskLauncher=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskServer=[IO.Path]::GetFullPath((Join-Path $taskLauncher '..\..\lito伺服器\LiTo381-Server'))
$taskRoots=@($taskLauncher,$taskServer)
& python (Join-Path $PSScriptRoot 'baseline_066.py') verify
if($LASTEXITCODE -ne 0){throw 'Baseline verification failed'}
$taskPlan=Get-Content -LiteralPath (Join-Path $taskLauncher 'docs\baseline-0.6.66-cleanup-plan.json') -Raw -Encoding UTF8 | ConvertFrom-Json
function Assert-ScopedFile([string]$value,[string]$scope){
    $target=[IO.Path]::GetFullPath($value)
    if($scope -notin $taskRoots -or -not $target.StartsWith($scope+'\',[StringComparison]::OrdinalIgnoreCase)){throw "Outside scope: $target"}
    if($target.StartsWith((Join-Path $taskServer 'runtime\battle-settlements')+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Protected settlement journal'}
    $parent=Split-Path -LiteralPath $target
    while($parent.Length -ge $scope.Length){
        if(Test-Path -LiteralPath $parent){if((Get-Item -LiteralPath $parent -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw "Reparse ancestor: $parent"}}
        $parent=Split-Path -LiteralPath $parent
    }
    if(Test-Path -LiteralPath $target){
        $item=Get-Item -LiteralPath $target -Force
        if($item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint)){throw "Not an ordinary file: $target"}
    }
    return $target
}
# Validate the entire plan before removing anything. No recursive deletion, links or cross-shell commands.
foreach($entry in $taskPlan){
    $target=Assert-ScopedFile $entry.path $entry.scope
    if((Test-Path -LiteralPath $target) -and (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ne $entry.sha256){throw "Changed after backup: $target"}
}
if($VerifyOnly){Write-Output "PASS: $($taskPlan.Count) exact scoped files verified; no deletion.";return}
$taskBusy=Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^(java|LiTo381Launcher|LiTo)\.(exe|bin)$' -and ($_.ExecutablePath -like ($taskLauncher+'\*') -or $_.CommandLine -like ('*'+$taskLauncher+'*') -or $_.CommandLine -like ('*'+$taskServer+'*')) }
if($taskBusy){throw 'Project process active; cleanup refused until it exits normally'}
$taskRemoved=[Collections.Generic.List[object]]::new()
$taskParents=[Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
foreach($entry in $taskPlan){
    $target=Assert-ScopedFile $entry.path $entry.scope
    if(-not(Test-Path -LiteralPath $target)){continue}
    if((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ne $entry.sha256){throw "Changed during cleanup: $target"}
    Remove-Item -LiteralPath $target -Force
    $taskRemoved.Add([pscustomobject]@{path=$target;bytes=$entry.bytes})
    $parent=Split-Path -LiteralPath $target
    while($parent.Length -gt $entry.scope.Length){[void]$taskParents.Add($parent);$parent=Split-Path -LiteralPath $parent}
}
foreach($dir in ($taskParents | Sort-Object Length -Descending)){
    if((Test-Path -LiteralPath $dir) -and -not (Get-ChildItem -LiteralPath $dir -Force | Select-Object -First 1)){
        $taskScope=$taskRoots | Where-Object { $dir.StartsWith($_+'\',[StringComparison]::OrdinalIgnoreCase) } | Select-Object -First 1
        if(-not $taskScope){throw 'Outside-scope empty directory'}
        if((Get-Item -LiteralPath $dir -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Directory became a link'}
        Remove-Item -LiteralPath $dir -Force
    }
}
$taskResult=@{version='0.6.66';removed_files=$taskRemoved.Count;removed_bytes=($taskRemoved|Measure-Object bytes -Sum).Sum;removed=$taskRemoved;completed=(Get-Date).ToString('o')}
$taskResult | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $taskLauncher 'docs\baseline-0.6.66-cleanup-result.json') -Encoding UTF8
Write-Output "Removed $($taskResult.removed_files) verified files; $($taskResult.removed_bytes) bytes."
