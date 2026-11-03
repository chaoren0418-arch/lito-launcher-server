param([Parameter(Mandatory=$true)][string]$PlanPath)
$ErrorActionPreference='Stop'
try {
    $expected='D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\runtime\server-provider-lifetime-recovery-20261008\startup-plan.json'
    if([IO.Path]::GetFullPath($PlanPath) -cne $expected){throw 'Exact current lifetime plan required'}
    . (Join-Path ([IO.Path]::GetDirectoryName($PlanPath)) 'Provider-Common.ps1')
    . (Join-Path ([IO.Path]::GetDirectoryName($PlanPath)) 'Provider-Lifetime.ps1')
    $plan=Get-Content -Raw -LiteralPath $PlanPath | ConvertFrom-Json
    foreach($pin in (Get-Content -Raw -LiteralPath (Join-Path $plan.artifact_root 'launch-input-manifest.json') | ConvertFrom-Json)){Verify-Pin $pin}
    if(Test-Path -LiteralPath (Join-Path $plan.ascii_runtime_root 'provider-terminal.json')){throw 'PROVIDER_TERMINAL_REJECTS_READY'}
    $binding=Get-Content -Raw -LiteralPath (Join-Path $plan.ascii_runtime_root 'retained-lifetime-owner.json') | ConvertFrom-Json
    Assert-LifetimeBinding $binding $plan
    $provider=Get-Content -Raw -LiteralPath (Join-Path $plan.ascii_runtime_root 'ready-provider.json') | ConvertFrom-Json
    Assert-ProviderLive $provider $binding $plan
    [ordered]@{ok=$true;status='LIVE_IDENTITIES_AND_LISTENERS_CURRENT';provider_root=$plan.ascii_runtime_root;native_started=$false;product_accepted=$false} | ConvertTo-Json -Compress
    exit 0
} catch {
    [ordered]@{ok=$false;status='TERMINAL_OR_NOT_CURRENT';reason=$_.Exception.Message;native_started=$false;product_accepted=$false} | ConvertTo-Json -Compress
    exit 2
}
