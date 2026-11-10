param([string]$Log, [Parameter(ValueFromRemainingArguments = $true)][string[]]$MvnArgs)
$w = $PSScriptRoot
$wt = (Resolve-Path (Join-Path $w "..\..")).Path
$server = Get-ChildItem $wt -Directory | Where-Object { Test-Path (Join-Path $_.FullName "LiTo381-Server\pom.xml") } | Select-Object -First 1
New-Item -ItemType Directory -Force (Join-Path $w "tmp") | Out-Null
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$w\tools\apache-maven-3.9.9\bin;$env:PATH"
$env:TEMP = "$w\tmp"; $env:TMP = "$w\tmp"
$env:MAVEN_OPTS = "-Djava.io.tmpdir=$w\tmp -Dfile.encoding=UTF-8"
Push-Location (Join-Path $server.FullName "LiTo381-Server")
& "$w\tools\apache-maven-3.9.9\bin\mvn.cmd" -B @MvnArgs *> (Join-Path $w $Log)
$code = $LASTEXITCODE
Pop-Location
"exit $code"
