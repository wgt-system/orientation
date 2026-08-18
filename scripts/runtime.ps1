param(
    [Parameter(Position = 0)]
    [ValidateSet("setup", "start", "stop", "status", "rebuild")]
    [string]$Action = "status",

    [switch]$OpenBrowser
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$RuntimeRoot = Join-Path $RepoRoot ".runtime"
$DownloadRoot = Join-Path $RuntimeRoot "downloads"
$MotisRoot = Join-Path $RuntimeRoot "motis"
$MotisInstallRoot = Join-Path $MotisRoot "v2.11.0"
$MotisWorkRoot = Join-Path $MotisRoot "hamburg"
$LogsRoot = Join-Path $RuntimeRoot "logs"
$StatePath = Join-Path $RuntimeRoot "processes.json"
$DatasetRoot = Join-Path $RuntimeRoot "datasets\hamburg"
$MotisZip = Join-Path $DownloadRoot "motis-windows-v2.11.0.zip"
$OsmPath = Join-Path $DatasetRoot "hamburg-260801.osm.pbf"
$OsmMd5Path = "$OsmPath.md5"
$GtfsPath = Join-Path $DatasetRoot "hvv_Rohdaten_GTFS_Fpl_20260408.zip"
$MotisConfigPath = Join-Path $MotisWorkRoot "config.yml"
$SetupMarker = Join-Path $RuntimeRoot "hamburg-setup.json"

$MotisUrl = "https://github.com/motis-project/motis/releases/download/v2.11.0/motis-windows.zip"
$OsmUrl = "https://download.geofabrik.de/europe/germany/hamburg-260801.osm.pbf"
$OsmMd5Url = "$OsmUrl.md5"
$GtfsUrl = "https://daten.transparenz.hamburg.de/Dataport.HmbTG.ZS.Webservice.GetRessource100/GetRessource100.svc/f0073555-962c-4d55-870e-b94bb676ad9d/Upload__hvv_Rohdaten_GTFS_Fpl_20260408.ZIP"
$GtfsSourcePage = "https://suche.transparenz.hamburg.de/dataset/hvv-fahrplandaten-gtfs-april-2026-bis-dezember-2026"
$OsmSourcePage = "https://download.geofabrik.de/europe/germany/hamburg.html"
$AppUrl = "http://127.0.0.1:5173/app.html"

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Assert-Windows {
    if ($env:OS -ne "Windows_NT") {
        throw "The local Hamburg bootstrap currently targets Windows because it uses the official MOTIS Windows release."
    }
}

function Ensure-Directory([string]$Path) {
    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Download-File([string]$Url, [string]$Destination) {
    Ensure-Directory (Split-Path -Parent $Destination)
    $temp = "$Destination.download"
    Remove-Item $temp -Force -ErrorAction SilentlyContinue
    Write-Host "Downloading $Url"
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($null -ne $curl) {
        & $curl.Source --fail --location --retry 3 --retry-all-errors --output $temp $Url
        if ($LASTEXITCODE -ne 0) {
            throw "Download failed: $Url"
        }
    } else {
        Invoke-WebRequest -Uri $Url -OutFile $temp
    }
    Move-Item $temp $Destination -Force
}

function Get-MotisExecutable {
    $candidate = Get-ChildItem -Path $MotisInstallRoot -Filter "motis.exe" -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $candidate) {
        throw "MOTIS executable not found. Run '.\scripts\runtime.ps1 setup'."
    }
    return $candidate.FullName
}

function To-YamlPath([string]$Path) {
    return $Path.Replace("\", "/").Replace("'", "''")
}

function Write-MotisConfig {
    Ensure-Directory $MotisWorkRoot
    $osm = To-YamlPath $OsmPath
    $gtfs = To-YamlPath $GtfsPath
    $config = @"
server:
  host: 127.0.0.1
  port: 8081
  data_attribution_link: https://github.com/wgt-system/orientation/blob/main/deployment/local-hamburg/ATTRIBUTION.md
osm: '$osm'
timetable:
  first_day: TODAY
  num_days: 365
  railviz: false
  with_shapes: true
  datasets:
    hvv:
      path: '$gtfs'
street_routing: true
osr_footpath: true
geocoding: true
reverse_geocoding: true
logging:
  log_level: info
"@
    Set-Content -Path $MotisConfigPath -Value $config -Encoding utf8NoBOM
}

function Assert-OsmChecksum {
    if (-not (Test-Path $OsmMd5Path)) {
        throw "Geofabrik checksum file is missing."
    }
    $expected = ((Get-Content $OsmMd5Path -Raw).Trim() -split "\s+")[0].ToLowerInvariant()
    $actual = (Get-FileHash -Algorithm MD5 -Path $OsmPath).Hash.ToLowerInvariant()
    if ($actual -ne $expected) {
        throw "Hamburg OSM checksum mismatch. Expected $expected, got $actual."
    }
}

function Invoke-Setup([switch]$ForceRebuild) {
    Assert-Windows
    Ensure-Directory $RuntimeRoot
    Ensure-Directory $DownloadRoot
    Ensure-Directory $DatasetRoot
    Ensure-Directory $LogsRoot

    Write-Step "Check prerequisites"
    foreach ($command in @("docker", "node", "npm")) {
        if ($null -eq (Get-Command $command -ErrorAction SilentlyContinue)) {
            throw "Missing prerequisite: $command"
        }
    }
    & docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker is installed but the Docker engine is not running."
    }

    Write-Step "Prepare pinned MOTIS v2.11.0"
    if (-not (Test-Path $MotisZip)) {
        Download-File $MotisUrl $MotisZip
    }
    if ($ForceRebuild -or -not (Test-Path $MotisInstallRoot)) {
        Remove-Item $MotisInstallRoot -Recurse -Force -ErrorAction SilentlyContinue
        Ensure-Directory $MotisInstallRoot
        Expand-Archive -Path $MotisZip -DestinationPath $MotisInstallRoot -Force
    }
    $motisExe = Get-MotisExecutable
    Write-Host "MOTIS: $motisExe"

    Write-Step "Prepare pinned Hamburg OSM + official hvv GTFS"
    if ($ForceRebuild -or -not (Test-Path $OsmPath)) {
        Download-File $OsmUrl $OsmPath
    }
    if ($ForceRebuild -or -not (Test-Path $OsmMd5Path)) {
        Download-File $OsmMd5Url $OsmMd5Path
    }
    Assert-OsmChecksum
    if ($ForceRebuild -or -not (Test-Path $GtfsPath)) {
        Download-File $GtfsUrl $GtfsPath
    }
    Write-Host ("OSM:  {0:N1} MB" -f ((Get-Item $OsmPath).Length / 1MB))
    Write-Host ("GTFS: {0:N1} MB" -f ((Get-Item $GtfsPath).Length / 1MB))

    Write-Step "Import local MOTIS Hamburg runtime"
    if ($ForceRebuild) {
        Remove-Item (Join-Path $MotisWorkRoot "data") -Recurse -Force -ErrorAction SilentlyContinue
    }
    Write-MotisConfig
    Push-Location $MotisWorkRoot
    try {
        & $motisExe import
        if ($LASTEXITCODE -ne 0) {
            throw "MOTIS import failed."
        }
    } finally {
        Pop-Location
    }

    Write-Step "Prepare pinned Valhalla Hamburg graph"
    & docker compose -f (Join-Path $RepoRoot "deployment\valhalla\docker-compose.yml") up -d
    if ($LASTEXITCODE -ne 0) {
        throw "Valhalla Docker startup failed."
    }
    Wait-Endpoint "http://127.0.0.1:8002/status" "Valhalla" 600
    & docker compose -f (Join-Path $RepoRoot "deployment\valhalla\docker-compose.yml") down

    Write-Step "Prepare backend and browser dependencies"
    Push-Location (Join-Path $RepoRoot "backend")
    try {
        & .\mvnw.cmd --batch-mode -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw "Backend bootstrap build failed." }
    } finally { Pop-Location }
    Push-Location (Join-Path $RepoRoot "map")
    try {
        & npm ci
        if ($LASTEXITCODE -ne 0) { throw "Map dependency install failed." }
    } finally { Pop-Location }

    $marker = [ordered]@{
        preparedAt = (Get-Date).ToString("o")
        motisVersion = "v2.11.0"
        osm = "hamburg-260801.osm.pbf"
        osmMd5 = (Get-FileHash -Algorithm MD5 -Path $OsmPath).Hash.ToLowerInvariant()
        gtfs = "hvv_Rohdaten_GTFS_Fpl_20260408.zip"
        gtfsSha256 = (Get-FileHash -Algorithm SHA256 -Path $GtfsPath).Hash.ToLowerInvariant()
        gtfsValidThrough = "2026-12-12"
        osmSource = $OsmSourcePage
        gtfsSource = $GtfsSourcePage
    }
    $marker | ConvertTo-Json | Set-Content -Path $SetupMarker -Encoding utf8NoBOM

    Write-Host "`nLocal Hamburg runtime prepared. Normal runtime queries no longer need hosted semantic providers." -ForegroundColor Green
}

function Wait-Endpoint([string]$Url, [string]$Name, [int]$TimeoutSeconds = 120) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            Invoke-WebRequest -Uri $Url -TimeoutSec 2 | Out-Null
            Write-Host "$Name ready: $Url" -ForegroundColor Green
            return
        } catch {
            Start-Sleep -Seconds 1
        }
    } while ((Get-Date) -lt $deadline)
    throw "$Name did not become ready at $Url within $TimeoutSeconds seconds."
}

function Start-LoggedProcess([string]$Name, [string]$FilePath, [string[]]$Arguments, [string]$WorkingDirectory) {
    Ensure-Directory $LogsRoot
    $stdout = Join-Path $LogsRoot "$Name.out.log"
    $stderr = Join-Path $LogsRoot "$Name.err.log"
    Remove-Item $stdout, $stderr -Force -ErrorAction SilentlyContinue
    return Start-Process -FilePath $FilePath -ArgumentList $Arguments -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
}

function Invoke-Start {
    Assert-Windows
    if (-not (Test-Path $SetupMarker)) {
        throw "Local runtime is not prepared. Run '.\scripts\runtime.ps1 setup' once first."
    }
    if (Test-Path $StatePath) {
        Write-Host "A runtime state file already exists. Running status first."
        Invoke-Status
        throw "Stop the existing runtime before starting another one."
    }

    Write-Step "Start Valhalla"
    & docker compose -f (Join-Path $RepoRoot "deployment\valhalla\docker-compose.yml") up -d
    if ($LASTEXITCODE -ne 0) { throw "Valhalla startup failed." }
    Wait-Endpoint "http://127.0.0.1:8002/status" "Valhalla" 180

    Write-Step "Start MOTIS"
    $motisExe = Get-MotisExecutable
    $motis = Start-LoggedProcess "motis" $motisExe @("server") $MotisWorkRoot
    try {
        Wait-Endpoint "http://127.0.0.1:8081/api/v1/health" "MOTIS" 120

        Write-Step "Start Orientation backend"
        $backend = Start-LoggedProcess "backend" "cmd.exe" @("/c", ".\mvnw.cmd spring-boot:run") (Join-Path $RepoRoot "backend")
        Wait-Endpoint "http://127.0.0.1:8080/actuator/health" "Orientation backend" 120

        Write-Step "Start Orientation browser"
        $map = Start-LoggedProcess "map" "cmd.exe" @("/c", "npm run dev -- --host 127.0.0.1") (Join-Path $RepoRoot "map")
        Wait-Endpoint $AppUrl "Orientation browser" 60

        [ordered]@{
            motis = $motis.Id
            backend = $backend.Id
            map = $map.Id
            startedAt = (Get-Date).ToString("o")
        } | ConvertTo-Json | Set-Content -Path $StatePath -Encoding utf8NoBOM

        Write-Host "`nOrientation is ready: $AppUrl" -ForegroundColor Green
        Write-Host "Runtime logs: $LogsRoot"
        if ($OpenBrowser) {
            Start-Process $AppUrl
        }
    } catch {
        foreach ($process in @($motis, $backend, $map)) {
            if ($null -ne $process) { Stop-ProcessTree $process.Id }
        }
        throw
    }
}

function Stop-ProcessTree([int]$ProcessId) {
    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($null -eq $process) { return }
    & taskkill.exe /PID $ProcessId /T /F *> $null
}

function Invoke-Stop {
    Write-Step "Stop Orientation local runtime"
    if (Test-Path $StatePath) {
        $state = Get-Content $StatePath -Raw | ConvertFrom-Json
        foreach ($name in @("map", "backend", "motis")) {
            $pidValue = $state.$name
            if ($null -ne $pidValue) { Stop-ProcessTree ([int]$pidValue) }
        }
        Remove-Item $StatePath -Force
    }
    if ($null -ne (Get-Command docker -ErrorAction SilentlyContinue)) {
        & docker compose -f (Join-Path $RepoRoot "deployment\valhalla\docker-compose.yml") down *> $null
    }
    Write-Host "Stopped. Cached datasets/imports remain for the next start." -ForegroundColor Green
}

function Test-Endpoint([string]$Url) {
    try {
        Invoke-WebRequest -Uri $Url -TimeoutSec 1 | Out-Null
        return $true
    } catch {
        return $false
    }
}

function Invoke-Status {
    $rows = @(
        [pscustomobject]@{ Component = "MOTIS"; Endpoint = "127.0.0.1:8081"; Ready = (Test-Endpoint "http://127.0.0.1:8081/api/v1/health") },
        [pscustomobject]@{ Component = "Valhalla"; Endpoint = "127.0.0.1:8002"; Ready = (Test-Endpoint "http://127.0.0.1:8002/status") },
        [pscustomobject]@{ Component = "Orientation"; Endpoint = "127.0.0.1:8080"; Ready = (Test-Endpoint "http://127.0.0.1:8080/actuator/health") },
        [pscustomobject]@{ Component = "Browser"; Endpoint = "127.0.0.1:5173"; Ready = (Test-Endpoint $AppUrl) }
    )
    $rows | Format-Table -AutoSize
    if (Test-Path $SetupMarker) {
        Write-Host "Prepared dataset:"
        Get-Content $SetupMarker -Raw | ConvertFrom-Json | Format-List motisVersion, osm, gtfs, gtfsValidThrough, preparedAt
    } else {
        Write-Host "Local Hamburg runtime has not been set up yet."
    }
}

switch ($Action) {
    "setup" { Invoke-Setup }
    "rebuild" { Invoke-Stop; Invoke-Setup -ForceRebuild }
    "start" { Invoke-Start }
    "stop" { Invoke-Stop }
    "status" { Invoke-Status }
}
