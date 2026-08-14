$ErrorActionPreference = "Stop"

Write-Host "== Backend =="
Push-Location (Join-Path $PSScriptRoot "..\backend")
try {
    if (Test-Path ".\mvnw.cmd") {
        .\mvnw.cmd test
    } else {
        mvn test
    }
}
finally {
    Pop-Location
}

Write-Host "== Map =="
Push-Location (Join-Path $PSScriptRoot "..\map")
try {
    if (-not (Test-Path ".\node_modules")) {
        npm ci
    }
    npm run check
}
finally {
    Pop-Location
}

Write-Host "== Git whitespace =="
git diff --check

Write-Host "All Orientation checks passed."
