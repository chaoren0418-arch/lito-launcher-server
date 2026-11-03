# Build entry repair: version-only check or isolated candidate preparation.
# Package/test execution is deliberately unavailable while its original gates are stopped.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Show-Usage {
    Write-Output 'Build-Server.cmd --check | --dry-run [--output <candidate-directory>]'
    Write-Output 'Build-Server.cmd --candidate --output <candidate-directory>'
    Write-Output 'Optional tool selectors: --java-home <JDK17-home> --maven <mvn.cmd>'
    Write-Output 'Candidates must be fresh direct children of target\build-entry-repair-20261007\candidates.'
    Write-Output '--check/--dry-run are read-only; --candidate prepares pom.xml + src only.'
    Write-Output 'No Maven lifecycle, application, clean, test skipping, or publishing is available.'
}

function Assert-NoReparse([string] $Path) {
    $cursor = [IO.Path]::GetFullPath($Path)
    while ($cursor) {
        if (Test-Path -LiteralPath $cursor) {
            $item = Get-Item -LiteralPath $cursor -Force
            if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) {
                throw "reparse_point: $cursor"
            }
        }
        $cursor = [IO.Path]::GetDirectoryName($cursor)
    }
}

function Get-Sha256([string] $Path) {
    $hasher = [Security.Cryptography.SHA256]::Create()
    try {
        $stream = [IO.File]::OpenRead($Path)
        try {
            return [BitConverter]::ToString($hasher.ComputeHash($stream)).Replace('-', '')
        } finally {
            $stream.Dispose()
        }
    } finally {
        $hasher.Dispose()
    }
}

function Read-Version([string] $Tool, [string] $Switch) {
    # Windows PowerShell 5.1 represents redirected native stderr as ErrorRecord.
    $savedPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $lines = @(& $Tool $Switch 2>&1 | ForEach-Object { $_.ToString() })
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $savedPreference
    }
    Write-Output $lines | Out-Host
    if ($code -ne 0) { throw "tool_version_exit: $Tool returned $code" }
    return (($lines -join "`n") -replace '\x1b\[[0-9;]*m', '')
}

$mode = $null
$output = $null
$javaHome = 'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot'
$maven = 'D:\lineage開發\tools\apache-maven-3.9.16\bin\mvn.cmd'
$seen = @{}
$savedEnvironment = @{}
$locationPushed = $false
try {
    if ($args.Count -eq 0) { Show-Usage; exit 2 }
    for ($i = 0; $i -lt $args.Count; $i++) {
        $option = [string] $args[$i]
        if ($seen.ContainsKey($option)) { throw "duplicate_option: $option" }
        $seen[$option] = $true
        switch ($option) {
            '--help' { if ($args.Count -ne 1) { throw 'help_must_be_alone' }; Show-Usage; exit 0 }
            { $_ -in '--check', '--dry-run', '--candidate' } {
                if ($mode) { throw 'exactly_one_mode_required' }
                $mode = $option
            }
            { $_ -in '--output', '--java-home', '--maven' } {
                $i++
                if ($i -ge $args.Count -or ([string] $args[$i]).StartsWith('--')) {
                    throw "missing_option_value: $option"
                }
                switch ($option) {
                    '--output' { $output = [string] $args[$i] }
                    '--java-home' { $javaHome = [string] $args[$i] }
                    '--maven' { $maven = [string] $args[$i] }
                }
            }
            default { throw "unknown_option: $option" }
        }
    }
    if (-not $mode) { throw 'explicit_mode_required' }
    if ($mode -eq '--candidate' -and -not $output) { throw 'candidate_requires_explicit_output' }

    $project = [IO.Path]::GetFullPath($PSScriptRoot)
    Push-Location -LiteralPath $project
    $locationPushed = $true
    if (Test-Path -LiteralPath (Join-Path $project '.mvn')) {
        throw 'maven_project_config: review .mvn before version-only probing'
    }
    $candidateRoot = Join-Path $project 'target\build-entry-repair-20261007\candidates'
    if (-not $output) { $output = Join-Path $candidateRoot 'review-candidate' }
    if ($output -match '(^|[\\/])\.\.?([\\/]|$)') { throw 'destination_traversal' }
    if (-not [IO.Path]::IsPathRooted($output)) { $output = Join-Path $project $output }
    $candidate = [IO.Path]::GetFullPath($output)
    $leaf = [IO.Path]::GetFileName($candidate)
    if (-not [string]::Equals([IO.Path]::GetDirectoryName($candidate), $candidateRoot,
            [StringComparison]::OrdinalIgnoreCase) -or
        $leaf -notmatch '^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$' -or
        $leaf -match '^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$') {
        throw "destination_confined: expected fresh direct child of $candidateRoot"
    }
    Assert-NoReparse $candidate
    if (Test-Path -LiteralPath $candidate) { throw "output_collision: $candidate" }
    $pom = Join-Path $project 'pom.xml'
    Assert-NoReparse $pom
    if ((Get-Sha256 $pom) -ne
        '0AA0927CAE3C3698203B4AC47AF63652DDA23638E717CF904DB44507EFDAE25B') {
        throw 'pom_pin_mismatch: review the new build model before using this entry'
    }
    $source = Join-Path $project 'src'
    Assert-NoReparse $source
    if (-not (Test-Path -LiteralPath $source -PathType Container)) { throw 'source_missing' }
    $sourceItems = @(Get-ChildItem -LiteralPath $source -Recurse -Force)
    if (@($sourceItems | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }).Count) {
        throw 'source_reparse_point: candidate inputs must be regular files/directories'
    }

    $javaHome = [IO.Path]::GetFullPath($javaHome)
    $maven = [IO.Path]::GetFullPath($maven)
    $java = Join-Path $javaHome 'bin\java.exe'
    $javac = Join-Path $javaHome 'bin\javac.exe'
    foreach ($tool in @($java, $javac, $maven)) {
        Assert-NoReparse $tool
        if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) { throw "tool_missing: $tool" }
    }
    if ([IO.Path]::GetFileName($maven) -ine 'mvn.cmd') { throw 'maven_requires_mvn_cmd' }
    $variables = @('JAVA_HOME', 'PATH', 'MAVEN_SKIP_RC', 'MAVEN_ARGS', 'MAVEN_OPTS',
        'MAVEN_BASEDIR', 'MAVEN_PROJECTBASEDIR', 'JVM_CONFIG_MAVEN_PROPS', 'MAVEN_DEBUG_OPTS',
        'MAVEN_BATCH_PAUSE', 'MAVEN_BATCH_ECHO', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS')
    foreach ($name in $variables) {
        $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    }
    foreach ($name in $variables | Where-Object { $_ -notin 'JAVA_HOME', 'PATH', 'MAVEN_SKIP_RC' }) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
    $env:JAVA_HOME = $javaHome
    $env:PATH = (Join-Path $javaHome 'bin') + ';' + $savedEnvironment['PATH']
    $env:MAVEN_SKIP_RC = '1'
    # Pin the launcher base so it cannot discover a caller/ancestor .mvn directory.
    $env:MAVEN_BASEDIR = $project
    Write-Output "JAVA_HOME=$javaHome"
    Write-Output "JAVA=$java"
    $javaVersion = Read-Version $java '-version'
    if ($javaVersion -notmatch '(?:openjdk|java) version "17(?:\.|"|-)') { throw 'jdk_version: requires Java 17' }
    Write-Output "JAVAC=$javac"
    $compilerVersion = Read-Version $javac '-version'
    if ($compilerVersion -notmatch '^javac 17(?:\.|$)') { throw 'javac_version: requires JDK 17 compiler' }
    Write-Output "MAVEN=$maven"
    $mavenVersion = Read-Version $maven '--version'
    if ($mavenVersion -notmatch 'Apache Maven 3\.9\.16(?:\s|\()') { throw 'maven_version: requires Maven 3.9.16' }
    if ($mavenVersion -notmatch 'Java version: 17(?:\.|,)') { throw 'maven_jdk_version: requires Java 17' }
    if ($mavenVersion.IndexOf("runtime: $javaHome", [StringComparison]::OrdinalIgnoreCase) -lt 0) {
        throw 'maven_java_home: runtime must match selected JDK'
    }

    $candidatePom = Join-Path $candidate 'pom.xml'
    Write-Output "MODE=$mode"
    Write-Output "CANDIDATE=$candidate"
    Write-Output "MAVEN_WORKDIR=$candidate"
    Write-Output "OUTPUT=$(Join-Path $candidate 'target')"
    Write-Output "FUTURE_GATED_COMMAND=call `"$maven`" --batch-mode --no-transfer-progress -f `"$candidatePom`" package"
    Write-Output 'LIFECYCLE_EXECUTED=false; APPLICATION_EXECUTED=false; PUBLISH_AVAILABLE=false'
    if ($mode -ne '--candidate') {
        Write-Output 'CHECK_OK: version probes only; no candidate/files created.'
        exit 0
    }

    # Never merge, overwrite, clean, or delete. A failed preparation is retained.
    Assert-NoReparse $candidate
    if (Test-Path -LiteralPath $candidate) { throw "output_collision: $candidate" }
    New-Item -ItemType Directory -Path $candidateRoot -Force | Out-Null
    New-Item -ItemType Directory -Path $candidate -ErrorAction Stop | Out-Null
    Copy-Item -LiteralPath $pom -Destination $candidatePom -ErrorAction Stop
    Copy-Item -LiteralPath $source -Destination (Join-Path $candidate 'src') -Recurse -Force -ErrorAction Stop
    if ((Get-Sha256 $candidatePom) -ne (Get-Sha256 $pom)) {
        throw 'staged_pom_mismatch: preserve candidate and stop'
    }
    Write-Output 'CANDIDATE_PREPARED=true; PACKAGE_BLOCKED=true'
    Write-Output 'missing_input=qualified lifecycle/test scope and original constructor/domain gates; supplier=controller + original authority; resume_when=separate input-complete task without test skipping; consumer=candidate package'
    exit 30
} catch {
    [Console]::Error.WriteLine('ERROR: ' + $_.Exception.Message)
    exit 1
} finally {
    foreach ($name in $savedEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')
    }
    if ($locationPushed) { Pop-Location }
}
