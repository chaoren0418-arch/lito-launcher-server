param([Parameter(Mandatory=$true)][string]$RunDirectory)
$ErrorActionPreference='Stop'
$run=(Resolve-Path -LiteralPath $RunDirectory).Path
$lease=@((Invoke-RestMethod 'http://localhost:8787/api/state').leases | Where-Object name -eq 'p24lab')
if($lease.Count -ne 1 -or $lease[0].holder -ne 'chaoren0418-arch/lito-launcher-server#284' -or $lease[0].exp -le ([DateTimeOffset]::UtcNow.ToUnixTimeSeconds()+120)){throw 'Issue 284 requires a valid p24lab lease covering the next 120 seconds'}
if((Get-FileHash -LiteralPath "$run/LiTo381-Server.jar" -Algorithm SHA256).Hash -ne '0F3762CE8C8E94F3B91D5404CC03B2BF815B4E329D0CBDCBC20F24F6B8686205'){throw 'Formal JAR hash mismatch'}
foreach($item in @('config/experiment.properties','config/combat.properties','combat-source-hashes.json','lib/mysql-connector-j-8.0.33.jar','runtime','tmp','logs')){
  if(-not (Test-Path -LiteralPath (Join-Path $run $item))){throw "Missing required path: $item"}
}
foreach($inputFile in (Get-Content -LiteralPath "$run/combat-source-hashes.json" -Raw -Encoding UTF8|ConvertFrom-Json)){
 if((Get-FileHash -LiteralPath $inputFile.File).Hash -ne $inputFile.SHA256){throw 'Combat source changed after preparation'}
}
if(@(Get-ChildItem -LiteralPath "$run/runtime" -Force).Count){throw 'Runtime must be newly created and empty'}
if(Test-Path -LiteralPath "$run/logs/server-console.log"){throw 'Existing attempt; prepare a fresh run directory'}
$settings=@{}
foreach($line in Get-Content -LiteralPath "$run/config/experiment.properties" -Encoding UTF8){
  if($line -match '^\s*(#|!|$)'){continue}
  $parts=$line.Split([char[]]'=',2,[StringSplitOptions]::None)
  if($parts.Count -ne 2 -or $settings.ContainsKey($parts[0])){throw 'Invalid or duplicate setting'}
  $settings[$parts[0]]=$parts[1].Replace('\:',':').Replace('\=','=')
}
$keys=@('bind','game.bind','port','server.name','database.jdbc.base.url','database.jdbc.options','database.user','database.password','database.static.database','database.lito.database','weight.rate','capture.directory','battle.ai.enabled','battle.physical.hit.enabled','battle.natural.mp.regen.enabled','native.siege.roster.enforced','native.tournament.allowance','memory.initial','memory.maximum','cpu.limit','logs.directory','startup.timeout.seconds')
if(@(Compare-Object ($keys | Sort-Object) (@($settings.Keys) | Sort-Object)).Count){throw 'Expected all 22 UnifiedConfiguration keys'}
$expectedSettings=@{
  'bind'='127.0.0.1';'game.bind'='127.0.0.2';'port'='42083';
  'database.jdbc.base.url'='jdbc:mysql://127.0.0.1:43324/';
  'database.static.database'='p24_380db_lito_exp';'database.lito.database'='p24_lito_lito_exp';
  'capture.directory'='../captures';'logs.directory'='../logs';
  'memory.initial'='512m';'memory.maximum'='2048m';'cpu.limit'='4';'startup.timeout.seconds'='120'
}
foreach($key in $expectedSettings.Keys){if($settings[$key] -cne $expectedSettings[$key]){throw "Unexpected experimental setting: $key"}}
$before=Get-Content -LiteralPath "$run/before.txt" -Raw
foreach($expected in @('identity=PASS port=43324 uuid=e3443e40-d090-11f1-8040-4cedfb679789 bind=127.0.0.1','route=1,p24_accounts_lito_exp,1','lito_battle_settlement=0')){
  if(-not $before.Contains($expected)){throw 'Missing verified database preflight; do not start'}
}
if((Get-Item -LiteralPath "$run/before.txt").LastWriteTimeUtc -lt [DateTime]::UtcNow.AddMinutes(-5)){throw 'Database preflight is stale; repeat the read-only probe'}
Write-Output 'Preflight evidence verified. Observe console output; stop normally if not ready within 120 seconds.'
