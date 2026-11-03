$ErrorActionPreference='Stop'
function Assert-LifetimeBinding($binding,$plan) {
    if($binding.schema -ne 'SERVER_RETAINED_PROVIDER_LIFETIME_1' -or $binding.task_id -ne $plan.task_id -or
       $binding.runtime_root -cne $plan.ascii_runtime_root -or $binding.reviewed_plan_sha256 -ne (Get-FileHash -LiteralPath (Join-Path $plan.artifact_root 'startup-plan.json') -Algorithm SHA256).Hash -or
       $binding.job_policy.atomic_birth -ne 'PROC_THREAD_ATTRIBUTE_JOB_LIST' -or $binding.job_policy.kill_on_close -ne $true -or
       $binding.job_policy.job_handle_inheritable -ne $false -or $binding.job_policy.breakaway_enabled -ne $false){throw 'Exact retained lifetime binding required'}
    if($binding.lifetime_end_tick_ms -le $binding.started_tick_ms -or
       $binding.lifetime_end_tick_ms-$binding.started_tick_ms -gt $plan.lifetime_budget.startup_ms+$plan.lifetime_budget.resident_ms){throw 'Finite original lifetime bound required'}
    if(-not $binding.host.pid -or -not $binding.root_guardian.pid){throw 'Lifetime identities required'}
}
function Assert-LifetimeAlive($binding) {
    if([Environment]::TickCount64 -ge [long]$binding.lifetime_end_tick_ms -or
       [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()/1000.0 -ge [double]$binding.not_after_epoch){throw 'PROVIDER_LIFETIME_EXPIRED'}
    $null=Match-Identity $binding.host
    $null=Match-Identity $binding.root_guardian
}
function Assert-ProviderStartupOpen($plan,$binding) {
    if(Test-Path -LiteralPath (Join-Path $plan.ascii_runtime_root 'stop-owner.request')){throw 'PROVIDER_STARTUP_STOP_REQUESTED'}
    Assert-LifetimeAlive $binding
}
function Publish-ProviderTerminal($plan,[string]$reason,[string]$phase) {
    # Publish BEFORE cleanup; a waiting coordinator can refuse immediately.
    $path=Join-Path $plan.ascii_runtime_root 'provider-terminal.json'
    if(-not (Test-Path -LiteralPath $path)) {
        Save-Runtime 'provider-terminal.json' ([ordered]@{schema='SERVER_PROVIDER_TERMINAL_1';task_id=$plan.task_id;status='TERMINAL';
            reason=$reason;phase=$phase;at_utc=[DateTimeOffset]::UtcNow.ToString('o');cleanup_complete=$null;native_started=$false;product_accepted=$false}) $plan
    }
    Save-Runtime 'provider-lifecycle.json' ([ordered]@{schema='SERVER_PROVIDER_LIFECYCLE_1';task_id=$plan.task_id;status='TERMINATING';reason=$reason;at_utc=[DateTimeOffset]::UtcNow.ToString('o')}) $plan
}
function Assert-ProviderLive($provider,$binding,$plan,[scriptblock]$IdentityCheck={param($expected) $null=Match-Identity $expected},[scriptblock]$ListenerCheck={param($rows)
    $actual=@(Get-NetTCPConnection -State Listen)
    foreach($row in $rows){if(@($actual|Where-Object {$_.LocalAddress -eq $row.LocalAddress -and $_.LocalPort -eq $row.LocalPort -and $_.OwningProcess -eq $row.OwningProcess}).Count -ne 1){throw 'PROVIDER_LIVE_LISTENER_MISSING'}}
}) {
    if(Test-Path -LiteralPath (Join-Path $plan.ascii_runtime_root 'provider-terminal.json')){throw 'PROVIDER_TERMINAL_REJECTS_READY'}
    if($provider.status -ne 'READY' -or $provider.task_id -ne $plan.task_id -or $provider.provider_root -cne $plan.ascii_runtime_root -or
       $provider.retained_lifetime_owner -ne (Join-Path $plan.ascii_runtime_root 'retained-lifetime-owner.json') -or
       $provider.retained_lifetime_owner_sha256 -ne (Get-FileHash -LiteralPath (Join-Path $plan.ascii_runtime_root 'retained-lifetime-owner.json') -Algorithm SHA256).Hash -or
       $provider.native_jar_sha256 -ne $plan.candidate.sha256 -or
       $provider.provider_resident_end_tick_ms -ne [Math]::Min([long]$binding.lifetime_end_tick_ms,[long]$provider.provider_ready_tick_ms+[long]$plan.lifetime_budget.resident_ms) -or
       $provider.provider_not_after_epoch -ne [Math]::Min([double]$binding.not_after_epoch,[double]$provider.provider_ready_epoch+[double]$plan.lifetime_budget.resident_ms/1000.0)){throw 'PROVIDER_STALE_OR_FOREIGN_READY'}
    if([Environment]::TickCount64 -ge [long]$binding.lifetime_end_tick_ms -or [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()/1000.0 -ge [double]$binding.not_after_epoch){throw 'PROVIDER_LIFETIME_EXPIRED'}
    if([long]$provider.provider_ready_tick_ms -lt [long]$binding.started_tick_ms -or [long]$provider.provider_ready_tick_ms -gt [Environment]::TickCount64 -or
       [double]$provider.provider_ready_epoch -gt [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()/1000.0){throw 'PROVIDER_READY_TIME_INVALID'}
    if([Environment]::TickCount64 -ge [long]$provider.provider_resident_end_tick_ms -or [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()/1000.0 -ge [double]$provider.provider_not_after_epoch){throw 'PROVIDER_RESIDENCY_EXPIRED'}
    & $IdentityCheck $binding.host
    & $IdentityCheck $binding.root_guardian
    foreach($role in @('owner','server','helper','mysql')) {
        if(-not $provider.exact_ownership.$role){throw 'PROVIDER_ROLE_MISSING'}
        & $IdentityCheck $provider.exact_ownership.$role
    }
    if($provider.exact_ownership.owner.parent_pid -ne $binding.host.pid){throw 'PROVIDER_OWNER_NOT_RETAINED_CHILD'}
    if(@($provider.listeners).Count -lt 3){throw 'PROVIDER_LIVE_LISTENER_SET_MISSING'}
    foreach($wanted in @(@('127.0.0.1',$provider.ownership.native_port,$provider.exact_ownership.server.pid),@('127.0.0.2',$provider.ownership.native_port,$provider.exact_ownership.server.pid),@('127.0.0.1',$provider.ownership.sql_port,$provider.exact_ownership.mysql.pid))) {
        if(@($provider.listeners|Where-Object {$_.LocalAddress -eq $wanted[0] -and $_.LocalPort -eq $wanted[1] -and $_.OwningProcess -eq $wanted[2]}).Count -ne 1){throw 'PROVIDER_EXACT_ROLE_LISTENERS_REQUIRED'}
    }
    & $ListenerCheck @($provider.listeners)
}
