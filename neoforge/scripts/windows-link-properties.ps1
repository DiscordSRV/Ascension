# Links every ../../modded/versions/VERSION/gradle.properties to ../versions/VERSION/gradle.properties

# Check if running as Administrator
if (-not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "Script needs to be run as Administrator. Restarting with elevated privileges..."
    Start-Process powershell -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    exit
}

# Set base paths
$srcBase = Join-Path $PSScriptRoot "..\versions"
$moddedBase = Join-Path $PSScriptRoot "..\..\modded\versions"

# Loop through each version folder in ../versions
Get-ChildItem -Directory $srcBase | ForEach-Object {
    $versionName = $_.Name
    $versionPath = $_.FullName
    $target = Join-Path (Join-Path $moddedBase $versionName) "gradle.properties"
    $link = Join-Path $versionPath "gradle.properties"

    # Remove existing link/file if it exists
    if (Test-Path $link) {
        Write-Host "Removing existing file/link at $link"
        Remove-Item $link -Force
    }

    # Skip if target missing
    if (-not (Test-Path $target)) {
        Write-Host "Target not found: $target. Skipping $versionName"
        return
    }

    # Create new symbolic link (file)
    Write-Host "Creating symbolic link: $link -> $target"
    try {
        New-Item -ItemType SymbolicLink -Path $link -Target $target | Out-Null
    } catch {
        Write-Host "New-Item failed, falling back to mklink via cmd: $_"
        cmd.exe /c "mklink `"$link`" `"$target`""
    }
}
