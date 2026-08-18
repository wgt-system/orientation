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
$DatasetRoot = Join-Path $RuntimeRoot "datasets\hamburg"
$MotisInstallRoot = Join-Path $RuntimeRoot "motis\v2.11.0"
$MotisWorkRoot = Join-Path $RuntimeRoot "motis\hamburg"
$LogsRoot = Join-Path $RuntimeRoot "logs"
$StatePath = Join-Path $RuntimeRoot "processes.json"
$SetupMarker = Join-Path $RuntimeRoot "hamburg-setup.json"
$MotisZip = Join-Path $DownloadRoot "motis-windows-v2.11.0.zip"
$OsmPath = Join-Path $DatasetRoot "hamburg-260801.osm.pbf"
$OsmMd5Path = "$OsmPath.md5"
$GtfsPath = Join-Path $DatasetRoot "hvv_Rohdaten_GTFS_Fpl_20260408.zip"
$MotisConfigPath = Join-Path $MotisWorkRoot "config.yml"
$BackendJar = Join-Path $RepoRoot "backend\target\orientation-backend-0.1.0-SNAPSHOT.jar"
$ViteCli = Join-Path $RepoRoot "map\node_modules\vite\bin\vite.js"
$ComposePath = Join-Path $RepoRoot "deployment\valhalla\docker-compose.yml"
$AppUrl = "http://127.0.0.1:5173/app.html"

$MotisUrl = "https://github.com/motis-project/motis/releases/download/v2.11.0/motis-windows.zip"
$OsmUrl = "https://download.geofabrik.de/europe/germany/hamburg-260801.osm.pbf"
$OsmMd5Url = "$OsmUrl.md5"
$GtfsUrl = "https://daten.transparenz.hamburg.de/Dataport.HmbTG.ZS.Webservice.GetRessource100/GetRessource100.svc/f0073555-962c-4d55-870e-b94bb676ad9d/Upload__hvv_Rohdaten_GTFS_Fpl_20260408.ZIP"
$GtfsSourcePage = "https://suche.transparenz.hamburg.de/dataset/hvv-fahrplandaten-gtfs-april-2026-bis-dezember-2026"
$OsmSourcePage = "https://download.geofabrik.de/europe/germany/hamburg.html"
$GtfsValidThrough = [datetime]::ParseExact("2026-12-12", "yyyy-MM-dd", [Globalization.CultureInfo]::InvariantCulture)

function Write-Step([string]$Message) { Write-Host "`n==> $Message" -ForegroundColor Cyan }
function Ensure-Directory([string]$Path) { if (-not (Test-Path $Path)) { New-Item -ItemType Directory -Path $Path -Force | Out-Null } }
function Write-Utf8File([string]$Path, [string]$Content) {
    [System.IO.File]::WriteAllText($Path, $Content, (New-Object System.Text.UTF8Encoding($false)))
}

function Assert-Windows {
    if ($env:OS -ne "Windows_NT") { throw "This local runtime bootstrap currently targets Windows." }
}

function Assert-GtfsFreshEnough {
    if ((Get-Date).Date -gt $GtfsValidThrough.Date) {
        throw "The pinned official hvv GTFS expired on 2026-12-12. Review a newer official dataset before rebuilding."
    }
}

function Download-File([string]$Url, [string]$Destination) {
    Ensure-Directory (Split-Path -Parent $Destination)
    $temporary = "$Destination.download"
    Remove-Item $temporary -Force -ErrorAction SilentlyContinue
    Write-Host "Downloading $Url"
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($null -ne $curl) {
        & $curl.Source --fail --location --retry 3 --output $temporary $Url
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $Url" }
    } else {
        Invoke-WebRequest -Uri $Url -OutFile $temporary
    }
    Move-Item $temporary $Destination -Force
}

function Test-Endpoint([string]$Url) {
    try { Invoke-WebRequest -Uri $Url -TimeoutSec 1 | Out-Null; return $true } catch { return $false }
}

function Wait-Endpoint([string]$Url, [string]$Name, [int]$TimeoutSeconds = 120) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Endpoint $Url) { Write-Host "$Name ready: $Url" -ForegroundColor Green; return }
        Start-Sleep -Seconds 1
    }
    throw "$Name did not become ready at $Url within $TimeoutSeconds seconds."
}

function Get-MotisExecutable {
    $candidate = Get-ChildItem -Path $MotisInstallRoot -Filter "motis.exe" -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $candidate) { throw "MOTIS executable not found. Run '.\scripts\local-runtime.ps1 setup'." }
    return $candidate.FullName
}

function Write-MotisConfig {
    Ensure-Directory $MotisWorkRoot
    $osm = $OsmPath.Replace("\", "/").Replace("'", "''")
    $gtfs = $GtfsPath.Replace("\", "/").Replace("'", "''")
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
    Write-Utf8File $MotisConfigPath $config
}

function Assert-OsmChecksum {
    $expected = ((Get-Content $OsmMd5Path -Raw).Trim() -split "\s+")[0].ToLowerInvariant()
    $actual = (Get-FileHash -Algorithm MD5 -Path $OsmPath).Hash.ToLowerInvariant()
    if ($actual -ne $expected) { throw "Hamburg OSM checksum mismatch. Expected $expected, got $actual." }
}

function Invoke-Setup([switch]$ForceRebuild) {
    Assert-Windows
    Assert-GtfsFreshEnough
    if (Test-Path $StatePath) { throw "Stop the local runtime before setup/rebuild." }
    foreach ($path in @($RuntimeRoot, $DownloadRoot, $DatasetRoot, $LogsRoot)) { Ensure-Directory $path }

    Write-Step "Check prerequisites"
    foreach ($command in @("docker", "java", "node", "npm")) {
        if ($null -eq (Get-Command $command -ErrorAction SilentlyContinue)) { throw "Missing prerequisite: $command" }
    }
    & docker info *> $null
    if ($LASTEXITCODE -ne 0) { throw "Docker is installed but its engine is not running." }
    & docker compose version *> $null
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose v2 is required." }

    Write-Step "Prepare pinned MOTIS v2.11.0"
    if ($ForceRebuild -or -not (Test-Path $MotisZip)) { Download-File $MotisUrl $MotisZip }
    if ($ForceRebuild -or -not (Test-Path $MotisInstallRoot)) {
        Remove-Item $MotisInstallRoot -Recurse -Force -ErrorAction SilentlyContinue
        Ensure-Directory $MotisInstallRoot
        Expand-Archive -Path $MotisZip -DestinationPath $MotisInstallRoot -Force
    }
    $motisExe = Get-MotisExecutable

    Write-Step "Prepare Hamburg OSM + official hvv GTFS"
    if ($ForceRebuild -or -not (Test-Path $OsmPath)) { Download-File $OsmUrl $OsmPath }
    if ($ForceRebuild -or -not (Test-Path $OsmMd5Path)) { Download-File $OsmMd5Url $OsmMd5Path }
    Assert-OsmChecksum
    if ($ForceRebuild -or -not (Test-Path $GtfsPath)) { Download-File $GtfsUrl $GtfsPath }
    Write-Host ("OSM:  {0:N1} MB" -f ((Get-Item $OsmPath).Length / 1MB))
    Write-Host ("GTFS: {0:N1} MB" -f ((Get-Item $GtfsPath).Length / 1MB))

    Write-Step "Import local MOTIS Hamburg runtime"
    if ($ForceRebuild) { Remove-Item (Join-Path $MotisWorkRoot "data") -Recurse -Force -ErrorAction SilentlyContinue }
    Write-MotisConfig
    Push-Location $MotisWorkRoot
    try {
        & $motisExe import
        if ($LASTEXITCODE -ne 0) { throw "MOTIS import failed." }
    } finally { Pop-Location }

    Write-Step "Prepare pinned Valhalla Hamburg graph"
    & docker compose -f $ComposePath up -d
    if ($LASTEXITCODE -ne 0) { throw "Valhalla startup failed." }
    try { Wait-Endpoint "http://127.0.0.1:8002/status" "Valhalla" 600 }
    finally { & docker compose -f $ComposePath down *> $null }

    Write-Step "Build backend and browser once"
    Push-Location (Join-Path $RepoRoot "backend")
    try {
        & .\mvnw.cmd --batch-mode -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw "Backend bootstrap build failed." }
    } finally { Pop-Location }
    Push-Location (Join-Path $RepoRoot "map")
    try {
        & npm ci
        if ($LASTEXITCODE -ne 0) { throw "Map dependency install failed." }
        & npm run build
        if ($LASTEXITCODE -ne 0) { throw "Map bootstrap build failed." }
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
    Write-Utf8File $SetupMarker ($marker | ConvertTo-Json)
    Write-Host "`nLocal Hamburg runtime prepared." -ForegroundColor Green
}

function Start-LoggedProcess([string]$Name, [string]$FilePath, [string[]]$Arguments, [string]$WorkingDirectory) {
    Ensure-Directory $LogsRoot
    $stdout = Join-Path $LogsRoot "$Name.out.log"
    $stderr = Join-Path $LogsRoot "$Name.err.log"
    Remove-Item $stdout, $stderr -Force -ErrorAction SilentlyContinue
    return Start-Process -FilePath $FilePath -ArgumentList $Arguments -WorkingDirectory $WorkingDirectory -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
}

function Stop-TrackedProcess($Process) {
    if ($null -eq $Process) { return }
    Stop-Process -Id ([int]$Process.Id) -ErrorAction SilentlyContinue
}

function Assert-RuntimeEndpointsFree {
    $occupied = @()
    foreach ($entry in @(
        @{ Name = "MOTIS"; Url = "http://127.0.0.1:8081/api/v1/health" },
        @{ Name = "Valhalla"; Url = "http://127.0.0.1:8002/status" },
        @{ Name = "Orientation"; Url = "http://127.0.0.1:8080/actuator/health" },
        @{ Name = "Browser"; Url = $AppUrl }
    )) { if (Test-Endpoint $entry.Url) { $occupied += $entry.Name } }
    if ($occupied.Count -gt 0) { throw "Runtime endpoint(s) already active: $($occupied -join ', '). Stop them before starting Orientation." }
}

function Invoke-Start {
    Assert-Windows
    Assert-GtfsFreshEnough
    if (-not (Test-Path $SetupMarker)) { throw "Run '.\scripts\local-runtime.ps1 setup' once first." }
    if (Test-Path $StatePath) { Invoke-Status; throw "A tracked runtime is already started." }
    Assert-RuntimeEndpointsFree
    if (-not (Test-Path $BackendJar)) { throw "Backend JAR missing. Run setup again." }
    if (-not (Test-Path $ViteCli)) { throw "Vite runtime missing. Run setup again." }

    $motis = $null
    $backend = $null
    $map = $null
    try {
        Write-Step "Start Valhalla"
        & docker compose -f $ComposePath up -d
        if ($LASTEXITCODE -ne 0) { throw "Valhalla startup failed." }
        Wait-Endpoint "http://127.0.0.1:8002/status" "Valhalla" 180

        Write-Step "Start MOTIS"
        $motis = Start-LoggedProcess "motis" (Get-MotisExecutable) @("server") $MotisWorkRoot
        Wait-Endpoint "http://127.0.0.1:8081/api/v1/health" "MOTIS" 120

        Write-Step "Start Orientation backend"
        $java = (Get-Command java).Source
        $backend = Start-LoggedProcess "backend" $java @("-jar", $BackendJar) $RepoRoot
        Wait-Endpoint "http://127.0.0.1:8080/actuator/health" "Orientation backend" 120

        Write-Step "Start Orientation browser"
        $node = (Get-Command node).Source
        $map = Start-LoggedProcess "map" $node @($ViteCli, "preview", "--host", "127.0.0.1", "--port", "5173", "--strictPort") (Join-Path $RepoRoot "map")
        Wait-Endpoint $AppUrl "Orientation browser" 60

        $state = [ordered]@{ motis = $motis.Id; backend = $backend.Id; map = $map.Id; startedAt = (Get-Date).ToString("o") }
        Write-Utf8File $StatePath ($state | ConvertTo-Json)
        Write-Host "`nOrientation is ready: $AppUrl" -ForegroundColor Green
        Write-Host "Logs: $LogsRoot"
        if ($OpenBrowser) { Start-Process $AppUrl }
    } catch {
        Stop-TrackedProcess $map
        Stop-TrackedProcess $backend
        Stop-TrackedProcess $motis
        & docker compose -f $ComposePath down *> $null
        throw
    }
}

function Invoke-Stop {
    Write-Step "Stop Orientation local runtime"
    if (Test-Path $StatePath) {
        $state = Get-Content $StatePath -Raw | ConvertFrom-Json
        foreach ($name in @("map", "backend", "motis")) {
            $processId = $state.$name
            if ($null -ne $processId) { Stop-Process -Id ([int]$processId) -ErrorAction SilentlyContinue }
        }
        Remove-Item $StatePath -Force
    }
    if ($null -ne (Get-Command docker -ErrorAction SilentlyContinue)) { & docker compose -f $ComposePath down *> $null }
    Write-Host "Stopped. Cached datasets/imports remain for the next start." -ForegroundColor Green
}

function Invoke-Status {
    @(
        [pscustomobject]@{ Component = "MOTIS"; Endpoint = "127.0.0.1:8081"; Ready = (Test-Endpoint "http://127.0.0.1:8081/api/v1/health") },
        [pscustomobject]@{ Component = "Valhalla"; Endpoint = "127.0.0.1:8002"; Ready = (Test-Endpoint "http://127.0.0.1:8002/status") },
        [pscustomobject]@{ Component = "Orientation"; Endpoint = "127.0.0.1:8080"; Ready = (Test-Endpoint "http://127.0.0.1:8080/actuator/health") },
        [pscustomobject]@{ Component = "Browser"; Endpoint = "127.0.0.1:5173"; Ready = (Test-Endpoint $AppUrl) }
    ) | Format-Table -AutoSize
    if (Test-Path $SetupMarker) {
        Write-Host "Prepared dataset:"
        Get-Content $SetupMarker -Raw | ConvertFrom-Json | Format-List motisVersion, osm, gtfs, gtfsValidThrough, preparedAt
    } else { Write-Host "Local Hamburg runtime has not been set up yet." }
}

switch ($Action) {
    "setup" { Invoke-Setup }
    "rebuild" { Invoke-Stop; Invoke-Setup -ForceRebuild }
    "start" { Invoke-Start }
    "stop" { Invoke-Stop }
    "status" { Invoke-Status }
}
