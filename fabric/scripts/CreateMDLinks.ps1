# Check if running as Administrator
if (-not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "Script needs to be run as Administrator. Restarting with elevated privileges..."

    # Relaunch PowerShell as Administrator
    Start-Process powershell -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    exit
}

# Set base paths
$basePath = "D:\Coding\Ascension\fabric\versions"
$srcPath = "D:\Coding\Ascension\fabric\src"

# Loop through each version folder
Get-ChildItem -Directory $basePath | ForEach-Object {
    $versionPath = $_.FullName
    $linkPath = Join-Path $versionPath "mappedSrc"

    # Remove existing link if it exists
    if (Test-Path $linkPath) {
        Write-Host "Removing existing link at $linkPath"
        Remove-Item $linkPath -Force
    }

    # Create new symbolic link
    Write-Host "Creating symbolic link in $versionPath"
    cmd.exe /c "mklink /D `"$linkPath`" `"$srcPath`""
}
