param([Parameter(Mandatory=$true)][string]$ReviewPath,[Parameter(Mandatory=$true)][string]$ReviewSha256,[Parameter(Mandatory=$true)][string]$LifetimeOwnerPath,[Parameter(Mandatory=$true)][string]$LifetimeOwnerSha256)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Provider-Common.ps1')
$plan=Read-Plan
. (Join-Path $PSScriptRoot 'Provider-Lifetime.ps1')
$expectedReview=$plan.expected_prelaunch_review
if([IO.Path]::GetFullPath($ReviewPath) -ine $expectedReview){throw 'Exact supervisor prelaunch review path required'}
Assert-RegularPath $ReviewPath
if((Get-FileHash -LiteralPath $ReviewPath -Algorithm SHA256).Hash -ne $ReviewSha256){throw 'Review pin mismatch'}
$review=Get-Content -LiteralPath $ReviewPath -Raw|ConvertFrom-Json
if($review.task_id -ne $plan.task_id -or $review.reviewer -ne 'supervisor' -or $review.decision -ne 'accepted' -or $review.provider_sql_launch_authorized -ne $true -or $review.prepared_plan_sha256 -ne (Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'startup-plan.json') -Algorithm SHA256).Hash){throw 'No exact reviewed provider/SQL launch release'}
$manifestPath=Join-Path $PSScriptRoot 'launch-input-manifest.json'
if($review.launch_input_manifest_sha256 -ne (Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash){throw 'Launch code/input manifest not reviewed'}
foreach($pin in (Get-Content -LiteralPath $manifestPath -Raw|ConvertFrom-Json)){Verify-Pin $pin}
$expectedLifetime=Join-Path $plan.ascii_runtime_root 'retained-lifetime-owner.json'
if([IO.Path]::GetFullPath($LifetimeOwnerPath) -cne $expectedLifetime){throw 'Exact retained lifetime owner path required'}
Assert-RegularPath $LifetimeOwnerPath
if((Get-FileHash -LiteralPath $LifetimeOwnerPath -Algorithm SHA256).Hash -ne $LifetimeOwnerSha256){throw 'Retained lifetime owner pin moved'}
$lifetime=Get-Content -Raw -LiteralPath $LifetimeOwnerPath | ConvertFrom-Json
Assert-LifetimeBinding $lifetime $plan
if($review.kernel_job_containment_cleanup_authorized -ne $true -or $lifetime.review_sha256 -ne $ReviewSha256 -or $lifetime.root_guardian.pid -ne $review.root_guardian.pid){throw 'Root lifetime/containment release mismatch'}
Assert-ProviderStartupOpen $plan $lifetime
Assert-RegularPath $plan.ascii_runtime_root
if($plan.ascii_runtime_root -notmatch '^[\x20-\x7E]+$'){throw 'Real ASCII root required'}
foreach($name in @('start.once','native-fixture.json','ready-provider.json','owner.json','stop.request','stop-owner.request','lito-modern-mysql-fixtures')){if(Test-Path -LiteralPath (Join-Path $plan.ascii_runtime_root $name)){throw 'Used runtime; no unchanged retry'}}
$marker=[IO.File]::Open((Join-Path $plan.ascii_runtime_root 'start.once'),[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None);$marker.Dispose()
$helperIdentity=$null;$serverIdentity=$null;$mysqlIdentity=$null
$helper=$null;$server=$null
$helperAttempted=$false;$serverAttempted=$false;$startupFailure=$null
$saved=@{};$successful=$false
try{
foreach($n in @('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','JDK_JAVAC_OPTIONS','_JAVA_OPTIONS','LITO_EXTENDED_STATS_SCHEMA','LITO_NUMERIC_BREAKTHROUGH','LITO_MODERN_MYSQL_HOME')){$saved[$n]=[Environment]::GetEnvironmentVariable($n,'Process');[Environment]::SetEnvironmentVariable($n,$null,'Process')}
$owner=Current-Identity (Get-Process -Id $PID)
if($owner.parent_pid -ne $lifetime.host.pid){throw 'Owner must be exact child retained by reviewed lifetime host'}
Save-Runtime 'owner.json' $owner $plan
Save-Runtime 'provider-lifecycle.json' ([ordered]@{schema='SERVER_PROVIDER_LIFECYCLE_1';task_id=$plan.task_id;status='STARTING';at_utc=[DateTimeOffset]::UtcNow.ToString('o')}) $plan
Assert-ProviderStartupOpen $plan $lifetime
$helperAttempted=$true
$helper=Start-Java $plan @($plan.helper_argument_vector) 'fixture'
$helperIdentity=Current-Identity $helper
Assert-HelperArguments $helperIdentity $plan
Save-Runtime 'startup-ownership.json' ([ordered]@{owner=$owner;helper=$helperIdentity;server=$null;mysql=$null;plan_sha256=$review.prepared_plan_sha256;review_sha256=$ReviewSha256}) $plan
$until=[DateTime]::UtcNow.AddSeconds(180);$fixture=$null
while([DateTime]::UtcNow -lt $until){
Assert-ProviderStartupOpen $plan $lifetime
$helper.Refresh();if($helper.HasExited){throw ('Owned fixture failed exit='+$helper.ExitCode+'; inspect original fixture evidence')}
$file=Assert-Under (Join-Path $plan.ascii_runtime_root 'native-fixture.json') $plan.ascii_runtime_root
if(Test-Path -LiteralPath $file){
if((Get-Item -LiteralPath $file).Length -gt 65536){throw 'Original fixture metadata bound exceeded'}
try{$fixture=Get-Content -LiteralPath $file -Raw|ConvertFrom-Json}catch{$fixture=$null}
if($fixture){if($fixture.datadir -or $fixture.fixture_directory){$null=Assert-FixturePaths $fixture $plan};if($fixture.state -eq 'ready_for_native_client'){break}}
}
Start-Sleep -Milliseconds 250
}
if(-not $fixture -or $fixture.state -ne 'ready_for_native_client' -or [int]$fixture.helper_pid -ne $helper.Id){throw 'Fresh fixture identity/READY mismatch'}
Assert-CandidateIdentity $fixture $plan
$data=Assert-Under $fixture.datadir $plan.ascii_runtime_root
$profile=Assert-Under $fixture.profile_path $plan.ascii_runtime_root
$fixtureDirectory=Assert-Under $fixture.fixture_directory $plan.ascii_runtime_root
$mysql=Get-Process -Id ([int]$fixture.mysql_pid)
$observedMysql=Current-Identity $mysql
$null=Match-Identity $helperIdentity
Assert-MySqlAdmission $fixture $plan $helperIdentity $observedMysql ([DateTime]::UtcNow.ToFileTimeUtc())
$mysqlIdentity=$observedMysql
$port=[int]$fixture.native_port;$sqlPort=[int]$fixture.mysql_port
if($port -lt 1024 -or $port -in @(2000,3306) -or $sqlPort -lt 1024 -or $sqlPort -in @(2000,3306) -or $port -eq $sqlPort){throw 'Unsafe fresh endpoints'}
$env:LITO_EXTENDED_STATS_SCHEMA='2';$env:LITO_NUMERIC_BREAKTHROUGH='4'
$mainArgs=@('-Xmx512m','-Dfile.encoding=UTF-8','-Dlito.numeric.breakthrough=4','-jar',$plan.candidate.path,$profile)
Assert-ProviderStartupOpen $plan $lifetime
$serverAttempted=$true
$server=Start-Java $plan $mainArgs 'server'
$serverIdentity=Current-Identity $server
Save-Runtime 'startup-ownership.json' ([ordered]@{owner=$owner;helper=$helperIdentity;server=$serverIdentity;mysql=$mysqlIdentity;datadir=$data;main_argument_vector=$mainArgs;plan_sha256=$review.prepared_plan_sha256;review_sha256=$ReviewSha256}) $plan
$until=[DateTime]::UtcNow.AddSeconds(120);$ready=$false
while([DateTime]::UtcNow -lt $until){
Assert-ProviderStartupOpen $plan $lifetime
$server.Refresh();$helper.Refresh()
if($server.HasExited -or $helper.HasExited){throw 'Provider exited before READY'}
$log=Join-Path $plan.ascii_runtime_root 'server.stdout.log'
if((Get-Content -LiteralPath $log -Raw -ErrorAction SilentlyContinue) -match '\[LiTo\] ready'){$ready=$true;break}
Start-Sleep -Milliseconds 250
}
if(-not $ready){throw 'Provider READY deadline'}
$handshakes=@()
foreach($address in @('127.0.0.1','127.0.0.2')){
$client=New-Object Net.Sockets.TcpClient
try{
if(-not $client.ConnectAsync($address,$port).Wait(3000)){throw 'S_KEY endpoint connect timeout'}
$stream=$client.GetStream();$stream.ReadTimeout=3000;$frame=New-Object byte[] 7;$offset=0
while($offset -lt 7){$n=$stream.Read($frame,$offset,7-$offset);if($n -le 0){throw 'S_KEY truncated'};$offset+=$n}
if($frame[0] -ne 7 -or $frame[1] -ne 0 -or $frame[2] -ne 1){throw 'S_KEY frame mismatch'}
$handshakes += [pscustomobject]@{address=$address;port=$port;length=7;opcode=1;passed=$true}
}finally{$client.Dispose()}
}
$listeners=@(Get-NetTCPConnection -State Listen|Where-Object {$_.OwningProcess -in @($server.Id,$mysql.Id)}|Select-Object LocalAddress,LocalPort,OwningProcess)
foreach($wanted in @(@('127.0.0.1',$port,$server.Id),@('127.0.0.2',$port,$server.Id),@('127.0.0.1',$sqlPort,$mysql.Id))){
if(@($listeners|Where-Object {$_.LocalAddress -eq $wanted[0] -and $_.LocalPort -eq $wanted[1] -and $_.OwningProcess -eq $wanted[2]}).Count -ne 1){throw 'Exact fresh listener PID mismatch'}
}
$null=Match-Identity $helperIdentity;$null=Match-Identity $serverIdentity;$null=Match-Identity $mysqlIdentity
$captureLine=@(Get-Content -LiteralPath (Join-Path $plan.ascii_runtime_root 'server.stdout.log')|Where-Object {$_ -match '^\[LiTo\] capture='})|Select-Object -Last 1
if(-not $captureLine){throw 'Actual capture output path unavailable'}
$capture=Assert-Under ($captureLine.Substring('[LiTo] capture='.Length)) $plan.ascii_runtime_root
$sourceProfile=[ordered]@{schemaVersion=4;profileId='lito-current-human-test-20261008';displayName='LiTo current isolated native test';host='127.0.0.1';port=$port;originSourceId='local-development';originSourceName='LiTo 3.81C Local';classicSourceNames=@('太陽神','月亮神');staticDatabase='lito_static';dynamicDatabases=@('380_ac_1','380_ac_2');localServerConfig=$profile}
Save-Runtime 'launcher-source-profile.json' $sourceProfile $plan
$provider=[ordered]@{schema='LITO_CURRENT_PROVIDER_READY_1';status='READY';task_id=$plan.task_id;at=[DateTimeOffset]::Now.ToString('o');provider_root=$plan.ascii_runtime_root;native_jar=$plan.candidate.path;native_jar_sha256=$plan.candidate.sha256;native_client_path=$plan.client.path;native_client_sha256=$plan.client.sha256;source_profile_json=(Join-Path $plan.ascii_runtime_root 'launcher-source-profile.json');server_config=$profile;source_config=(Join-Path $fixtureDirectory 'native-sources.properties');credential_settings=(Join-Path $fixtureDirectory 'native-database.properties');credentials_file=$plan.credentials.path;credentials_printed=$false;datadir=$data;login_endpoint="127.0.0.1:$port";game_endpoint="127.0.0.2:$port";mysql_endpoint="127.0.0.1:$sqlPort";capture=$capture;ownership=@{owner=$owner;server=$serverIdentity;helper=$helperIdentity;mysql=$mysqlIdentity};listeners=$listeners;handshakes=$handshakes;stop_owner_request=(Join-Path $plan.ascii_runtime_root 'stop-owner.request');cleanup_entry=(Join-Path $PSScriptRoot 'Stop-Provider.ps1');lease_issued=$false;claim_issued=$false;native_started=$false;BIN_receive_proved=$false;GUI_accepted=$false;product_accepted=$false}
$provider.provider='server'
$provider.runtime_root=$plan.ascii_runtime_root
$provider.candidate_id=$plan.candidate_id
$provider.candidate_jar=$plan.candidate.path
$provider.candidate_sha256=$plan.candidate.sha256
$provider.candidate_manifest=$manifestPath
$provider.candidate_manifest_sha256=(Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash
$provider.source='月亮神'
$provider.character='GuiKnight'
$provider.gui_acceptance=$false
$provider.automatic_timeout=$true
$provider.provider_ready_tick_ms=[Environment]::TickCount64
$provider.provider_ready_epoch=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()/1000.0
$provider.provider_resident_end_tick_ms=[Math]::Min([long]$lifetime.lifetime_end_tick_ms,[long]$provider.provider_ready_tick_ms+[long]$plan.lifetime_budget.resident_ms)
$provider.provider_not_after_epoch=[Math]::Min([double]$lifetime.not_after_epoch,[double]$provider.provider_ready_epoch+[double]$plan.lifetime_budget.resident_ms/1000.0)
$provider.retained_lifetime_owner=$LifetimeOwnerPath
$provider.retained_lifetime_owner_sha256=$LifetimeOwnerSha256
$provider.terminal_file=(Join-Path $plan.ascii_runtime_root 'provider-terminal.json')
$provider.stop_request=(Join-Path $plan.ascii_runtime_root 'stop-owner.request')
$provider.exact_ownership_file=(Join-Path $plan.ascii_runtime_root 'startup-ownership.json')
$provider.exact_ownership=$provider.ownership
$provider.ownership=[ordered]@{datadir=$data;server_pid=$serverIdentity.pid;server_start_utc=[DateTime]::FromFileTimeUtc([long]$serverIdentity.created_filetime).ToString('o');helper_pid=$helperIdentity.pid;helper_start_utc=[DateTime]::FromFileTimeUtc([long]$helperIdentity.created_filetime).ToString('o');mysql_pid=$mysqlIdentity.pid;mysql_start_utc=[DateTime]::FromFileTimeUtc([long]$mysqlIdentity.created_filetime).ToString('o');candidate_jar=$plan.candidate.path;candidate_sha256=$plan.candidate.sha256;native_port=$port;sql_port=$sqlPort}
Assert-ProviderStartupOpen $plan $lifetime
Save-Runtime 'ready-provider.json' $provider $plan
Save-Runtime 'native-ready-contract.json' $provider $plan
Save-Runtime 'provider-lifecycle.json' ([ordered]@{schema='SERVER_PROVIDER_LIFECYCLE_1';task_id=$plan.task_id;status='READY';provider_not_after_epoch=$lifetime.not_after_epoch;at_utc=[DateTimeOffset]::UtcNow.ToString('o')}) $plan
$successful=$true
Write-Output 'Actual owned provider READY written; no lease/claim/native launch implied.'
while(-not (Test-Path -LiteralPath (Join-Path $plan.ascii_runtime_root 'stop-owner.request'))){
Assert-LifetimeAlive $lifetime
$null=Match-Identity $mysqlIdentity
$null=Match-Identity $helperIdentity
$null=Match-Identity $serverIdentity
$helper.Refresh();$server.Refresh()
if($helper.HasExited -or $server.HasExited){throw 'Resident owned provider ended'}
Start-Sleep -Milliseconds 500
}
Publish-ProviderTerminal $plan 'requested_stop' 'owner-hold'
}catch{$startupFailure=$_;try{Publish-ProviderTerminal $plan ('owner_failure:'+ $_.FullyQualifiedErrorId) 'owner'}catch{[Console]::Error.WriteLine('Primary failure preserved; terminal publication failed.')};throw}finally{
$closed=$false;$cleanupFailure=$null
try{
try{$closed=Close-Provider $plan $serverIdentity $helperIdentity $mysqlIdentity $server $helper $helperAttempted $serverAttempted}catch{$cleanupFailure=$_}
try{Save-Runtime 'provider-lifecycle.json' ([ordered]@{schema='SERVER_PROVIDER_LIFECYCLE_1';task_id=$plan.task_id;status=$(if($closed){'STOPPED'}else{'CLEANUP_INCOMPLETE'});cleanup_complete=$closed;at_utc=[DateTimeOffset]::UtcNow.ToString('o')}) $plan}catch{[Console]::Error.WriteLine('Final lifecycle publication failed; cleanup qualification unchanged.')}
if($startupFailure){
try{Save-Runtime 'startup-failure.json' ([ordered]@{original_error_id=$startupFailure.FullyQualifiedErrorId;original_exception_type=$startupFailure.Exception.GetType().FullName;original_message=$startupFailure.Exception.Message;cleanup_qualified=$closed;cleanup_error_id=$(if($cleanupFailure){$cleanupFailure.FullyQualifiedErrorId}else{$null});helper_exit_code=$(if($helper -and $helper.HasExited){$helper.ExitCode}else{$null})}) $plan}catch{[Console]::Error.WriteLine('Startup failure retained; secondary failure receipt could not be saved.')}
}
}finally{foreach($n in $saved.Keys){[Environment]::SetEnvironmentVariable($n,$saved[$n],'Process')}}
if(-not $closed -and -not $startupFailure){if($cleanupFailure){throw $cleanupFailure};throw 'Owned cleanup incomplete; exact identity review required'}
}
