param(
    [ValidateSet("backend", "map")]
    [string]$Target = "map"
)

$ErrorActionPreference = "Stop"

if ($Target -eq "backend") {
    Push-Location (Join-Path $PSScriptRoot "..\backend")
    try {
        if (Test-Path ".\mvnw.cmd") {
            .\mvnw.cmd spring-boot:run
        } else {
            mvn spring-boot:run
        }
    }
    finally {
        Pop-Location
    }
    exit
}

Push-Location (Join-Path $PSScriptRoot "..\map")
try {
    npm run dev
}
finally {
    Pop-Location
}
