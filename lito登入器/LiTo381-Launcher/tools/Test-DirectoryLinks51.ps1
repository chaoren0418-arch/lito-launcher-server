$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Remove-DirectoryLink.ps1')
$launcher=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$fixture=Join-Path $launcher ('docs\link-test-'+[Guid]::NewGuid().ToString('N'))
$scope=Join-Path $fixture 'cleanup-scope'
$shared=Join-Path $fixture 'shared-resource'
[void][IO.Directory]::CreateDirectory($scope)
[void][IO.Directory]::CreateDirectory($shared)
$sentinel=Join-Path $shared 'sentinel.txt'
[IO.File]::WriteAllText($sentinel,'must survive junction removal')
$before=(Get-FileHash -LiteralPath $sentinel).Hash
$link=Join-Path $scope 'Font'
[void](New-Item -ItemType Junction -Path $link -Target $shared)
Remove-ProjectReparseLink -LiteralPath $link -Scope $scope
if(Test-Path -LiteralPath $link){throw 'Link still exists'}
if(-not(Test-Path -LiteralPath $sentinel)){throw 'Shared target was removed'}
if((Get-FileHash -LiteralPath $sentinel).Hash -ne $before){throw 'Shared target changed'}
$rejected=$false
try {Remove-ProjectReparseLink -LiteralPath $shared -Scope $scope} catch {$rejected=$true}
if(-not $rejected){throw 'Out of scope target was not rejected'}
$regular=Join-Path $scope 'regular';[void][IO.Directory]::CreateDirectory($regular)
$rejected=$false
try {Remove-ProjectReparseLink -LiteralPath $regular -Scope $scope} catch {$rejected=$true}
if(-not $rejected){throw 'Ordinary directory was not rejected'}
[PSCustomObject]@{powershell=$PSVersionTable.PSVersion.ToString();junction_removed=$true;target_unchanged=$true;outside_scope_refused=$true;ordinary_directory_refused=$true;fixture=$fixture} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $launcher 'docs\junction-safety-test.json') -Encoding UTF8
Write-Output 'PASS: junction unlinked; external target untouched; outside-scope and ordinary directories refused.'
