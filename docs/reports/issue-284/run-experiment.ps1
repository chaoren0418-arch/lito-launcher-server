param([switch]$PrepareOnly)
$ErrorActionPreference='Stop'
[Console]::OutputEncoding=[Text.Encoding]::UTF8
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
Set-Location -LiteralPath $root
$lease=@((Invoke-RestMethod http://localhost:8787/api/state).leases|Where-Object name -eq 'p24lab')
if($lease.Count -ne 1 -or $lease[0].holder -ne 'chaoren0418-arch/lito-launcher-server#284' -or [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() -ge $lease[0].exp-180){throw 'A current #284 p24lab lease with at least 180 seconds remaining is required'}
if(@(netstat -ano -p TCP|Select-String ':(42083|42084)\s+.*LISTENING').Count){throw 'Experiment port occupied'}
$run=Join-Path $root ('scratch\t014\run-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))
if(Test-Path -LiteralPath $run){throw 'Run already exists'}
$java='C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe'
$source='D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\server'
$jar='LiTo381-SharedResources-candidate.jar'
if((Get-FileHash -LiteralPath "$source\$jar").Hash -ne 'E1498871C7BE6B287821C57EF5D95933DB1740D9B4D4A80F6C3465F21D99D498'){throw 'Formal SHA mismatch'}
New-Item -ItemType Directory -Path $run | Out-Null
$lease|ConvertTo-Json|Set-Content -Encoding UTF8 "$run\lease.json"
foreach($d in @('config','runtime','captures','logs','lib','target','tmp')){New-Item -ItemType Directory -Path "$run\$d"|Out-Null}
Copy-Item -LiteralPath "$source\$jar" -Destination "$run\$jar"
foreach($f in Get-ChildItem -LiteralPath "$source\lib" -File){if($f.LinkType){throw 'Linked library'};Copy-Item -LiteralPath $f.FullName -Destination "$run\lib\$($f.Name)"}
$receipts=@()
foreach($f in @((Get-Item "$source\$jar")) + @(Get-ChildItem "$source\lib" -File)){
 $relative=$f.FullName.Substring($source.Length+1)
 $hash=(Get-FileHash -LiteralPath $f.FullName).Hash
 if($hash -ne (Get-FileHash -LiteralPath "$run\$relative").Hash){throw 'Copy hash mismatch'}
 $receipts+=@{File=$relative;Bytes=$f.Length;SHA256=$hash;CopyVerified=$true}
}
$receipts|ConvertTo-Json|Set-Content -Encoding UTF8 "$run\artifact-hashes.json"
& $java -cp "$run\$jar;$run\lib\*" "$PSScriptRoot\Prepare.java" $run 'D:\lineage開發\3-lito開發\scratch\wt\task-83\scratch\t020c\isolated-test.properties' "$root\lito伺服器\LiTo381-Server\config\server.properties.example"
if($LASTEXITCODE -ne 0){throw 'Configuration preparation failed'}
@{Run=$run;Utc=[DateTime]::UtcNow.ToString('o');RuntimeEntries=@(Get-ChildItem -LiteralPath "$run\runtime" -Force).Count;Java=$java}|ConvertTo-Json|Set-Content -Encoding UTF8 "$run\preparation.json"
Write-Output "Run=$run"
if($PrepareOnly){return}
# P24Lab must already have been cold-backed-up and started by its authorized owner.
# This entry point never starts/stops MySQL or changes grants/data.
& $java -cp "$run\lib\*" "$PSScriptRoot\Probe.java" "$run\config\experiment.properties" 2>&1 | Tee-Object -FilePath "$run\before.txt"
if($LASTEXITCODE -ne 0){throw 'Read-only preflight failed; do not start server'}
& "$PSScriptRoot\verify-run.ps1" -RunDirectory $run
if(@(Get-ChildItem -LiteralPath "$run\runtime" -Force).Count){throw 'Runtime must still be empty'}
@('@echo off','"'+$java+'" -Xms512m -Xmx2048m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8 "-Djava.io.tmpdir=%CD%\tmp" -jar LiTo381-SharedResources-candidate.jar config/experiment.properties 2>&1','exit /b %errorlevel%') | Set-Content -Encoding ASCII "$run\start-java.cmd"
Write-Host 'Observe startup for at most 120 seconds. On timeout use Ctrl+C in this console; do not retry.'
Write-Host 'After [LiTo] ready, complete only the authorized login check, then Ctrl+C for normal shutdown.'
Push-Location -LiteralPath $run
try {
 [DateTime]::UtcNow.ToString('o')|Set-Content -Encoding UTF8 logs/start-utc.txt
 & .\start-java.cmd | Tee-Object -FilePath logs/server-console.log
 $code=$LASTEXITCODE
 @{ExitCode=$code;EndUtc=[DateTime]::UtcNow.ToString('o')}|ConvertTo-Json|Set-Content -Encoding UTF8 logs/server-exit.json
} finally {Pop-Location}
& $java -cp "$run\lib\*" "$PSScriptRoot\Probe.java" "$run\config\experiment.properties" 2>&1 | Tee-Object -FilePath "$run\after.txt"
if($LASTEXITCODE -ne 0){throw 'Post-run read-only verification failed'}
exit $code
