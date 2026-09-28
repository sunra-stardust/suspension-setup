# Full verify: unit tests + debug build.
# Uses Android Studio's bundled JBR 21 (the machine's default Java is a JRE-only 8).
$ErrorActionPreference = "Stop"
$jbr = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path (Join-Path $jbr "bin\java.exe")) {
    $env:JAVA_HOME = $jbr
    $env:Path = (Join-Path $jbr "bin") + ";" + $env:Path
} else {
    Write-Warning "Android Studio JBR not found at $jbr - falling back to JAVA_HOME / PATH."
}
$repo = Split-Path -Parent $PSScriptRoot
& (Join-Path $repo "gradlew.bat") -p $repo verify --console=plain
exit $LASTEXITCODE
