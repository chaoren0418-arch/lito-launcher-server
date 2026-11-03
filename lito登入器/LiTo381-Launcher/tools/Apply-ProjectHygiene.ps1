param([switch]$VerifyOnly,[switch]$ExtrasOnly)
$ErrorActionPreference='Stop'
$taskLauncher=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskParent=Split-Path (Split-Path $taskLauncher)
$taskRoots=@((Split-Path $taskLauncher),(Join-Path $taskParent 'lito伺服器'))
$taskServer=Join-Path $taskRoots[1] 'LiTo381-Server'
if($ExtrasOnly){
    & python -B -X utf8 (Join-Path $PSScriptRoot 'maintenance_backup.py') verify
    $taskPlanName='baseline-0.6.66-hygiene-extra-log-plan.json'
    $taskResultName='baseline-0.6.66-hygiene-extra-log-result.json'
}else{
    & python -B -X utf8 (Join-Path $PSScriptRoot 'project_hygiene.py') verify
    $taskPlanName='baseline-0.6.66-hygiene-plan.json'
    $taskResultName='baseline-0.6.66-hygiene-result.json'
}
if($LASTEXITCODE -ne 0){throw 'Archive validation failed'}
$taskPlan=Get-Content -LiteralPath (Join-Path $taskLauncher ('docs\'+$taskPlanName)) -Raw -Encoding UTF8 | ConvertFrom-Json
if($ExtrasOnly){foreach($entry in $taskPlan){$scope=$taskRoots | Where-Object { $entry.path.StartsWith($_+'\',[StringComparison]::OrdinalIgnoreCase) } | Select-Object -First 1; $entry | Add-Member -NotePropertyName scope -NotePropertyValue $scope}}
$taskProtectedBackups=@((Join-Path $taskLauncher 'backups'),(Join-Path $taskServer 'backups'))
# The original audit includes one cache restored after final validation. Backups always win over cleanup.
$taskPlan=@($taskPlan | Where-Object { $value=[IO.Path]::GetFullPath($_.path); -not ($value.StartsWith($taskProtectedBackups[0]+'\',[StringComparison]::OrdinalIgnoreCase) -or $value.StartsWith($taskProtectedBackups[1]+'\',[StringComparison]::OrdinalIgnoreCase)) })
$taskParents=[Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
foreach($entry in $taskPlan){
    $target=[IO.Path]::GetFullPath($entry.path)
    if($entry.scope -notin $taskRoots -or -not $target.StartsWith($entry.scope+'\',[StringComparison]::OrdinalIgnoreCase)){throw "Outside scope: $target"}
    if($target.StartsWith((Join-Path $taskServer 'runtime\battle-settlements')+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Protected settlement journal'}
    $parent=[IO.Path]::GetDirectoryName($target)
    while($parent.Length -ge $entry.scope.Length){
        if($taskParents.Add($parent) -and ([IO.Directory]::Exists($parent))){
            if([IO.File]::GetAttributes($parent) -band [IO.FileAttributes]::ReparsePoint){throw "Linked ancestor refused: $parent"}
        }
        $parent=[IO.Path]::GetDirectoryName($parent)
    }
    if([IO.File]::Exists($target)){
        $attributes=[IO.File]::GetAttributes($target)
        if(($attributes -band [IO.FileAttributes]::Directory) -or ($attributes -band [IO.FileAttributes]::ReparsePoint)){throw "Not an ordinary file: $target"}
    }elseif([IO.Directory]::Exists($target)){throw "File replaced by directory: $target"}
}
Write-Output "Validated $($taskPlan.Count) scoped paths and all existing ancestors; checking content hashes next"
$taskBusy=Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^(java|LiTo381Launcher|LiTo)\.(exe|bin)$' -and ($_.ExecutablePath -like ($taskLauncher+'\*') -or $_.CommandLine -like ('*'+$taskLauncher+'*') -or $_.CommandLine -like ('*'+$taskServer+'*')) }
if($taskBusy){throw 'A native project process is active; cleanup refused'}
$taskRemoved=0
$taskBytes=[long]0
$taskHasher=[Security.Cryptography.SHA256]::Create()
foreach($entry in $taskPlan){
    $target=[IO.Path]::GetFullPath($entry.path)
    if(-not [IO.File]::Exists($target)){continue}
    $parent=[IO.Path]::GetDirectoryName($target)
    while($parent.Length -ge $entry.scope.Length){
        if([IO.File]::GetAttributes($parent) -band [IO.FileAttributes]::ReparsePoint){throw "Directory became a link: $parent"}
        $parent=[IO.Path]::GetDirectoryName($parent)
    }
    if([IO.File]::GetAttributes($target) -band [IO.FileAttributes]::ReparsePoint){throw "File became a link: $target"}
    $stream=[IO.File]::OpenRead($target)
    try{$hash=[BitConverter]::ToString($taskHasher.ComputeHash($stream)).Replace('-','')}finally{$stream.Dispose()}
    if($hash -ne $entry.sha256){throw "Changed during cleanup: $target"}
    if($VerifyOnly){continue}
    Remove-Item -LiteralPath $target -Force
    $taskRemoved++;$taskBytes+=[long]$entry.bytes
    if($taskRemoved % 2000 -eq 0){Write-Output "Removed $taskRemoved archived files"}
}
$taskHasher.Dispose()
if($VerifyOnly){Write-Output 'PASS: scoped paths and content hashes verified; no deletion';return}
foreach($dir in ($taskParents | Sort-Object Length -Descending)){
    if($dir -in $taskRoots){continue}
    if([IO.Directory]::Exists($dir) -and [IO.Directory]::GetFileSystemEntries($dir).Length -eq 0){
        if([IO.File]::GetAttributes($dir) -band [IO.FileAttributes]::ReparsePoint){throw "Linked directory refused: $dir"}
        Remove-Item -LiteralPath $dir -Force
    }
}
$taskResult=[ordered]@{removed_this_run=$taskRemoved;removed_bytes_this_run=$taskBytes;total_archived_targets=$taskPlan.Count;completed=(Get-Date).ToString('o')}
$taskResult | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $taskLauncher ('docs\'+$taskResultName)) -Encoding UTF8
Write-Output "Cleanup completed: $taskRemoved files, $taskBytes bytes."
