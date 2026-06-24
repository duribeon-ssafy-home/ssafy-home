param(
    [string]$InputPath = "backend/src/main/resources/data/legal-dong-code.txt",
    [string]$OutputPath = "backend/src/main/resources/db/legal_dongs.sql",
    [int]$BatchSize = 500
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Escape-SqlValue {
    param([AllowNull()][string]$Value)

    if ($null -eq $Value -or $Value -eq "") {
        return "NULL"
    }

    return "'" + $Value.Replace("'", "''") + "'"
}

function Resolve-LocationParts {
    param([string]$FullName)

    $parts = $FullName -split "\s+"
    $sido = $parts[0]
    $gugun = $null
    $dong = $null

    if ($parts.Count -eq 2) {
        if ($parts[1] -match "(시|군|구)$") {
            $gugun = $parts[1]
        } else {
            $dong = $parts[1]
        }
    } elseif ($parts.Count -eq 3) {
        if ($parts[1] -match "시$" -and $parts[2] -match "구$") {
            $gugun = "$($parts[1]) $($parts[2])"
        } else {
            $gugun = $parts[1]
            $dong = $parts[2]
        }
    } elseif ($parts.Count -ge 4) {
        if ($parts[1] -match "시$" -and $parts[2] -match "구$") {
            $gugun = "$($parts[1]) $($parts[2])"
            $dong = ($parts[3..($parts.Count - 1)] -join " ")
        } else {
            $gugun = $parts[1]
            $dong = ($parts[2..($parts.Count - 1)] -join " ")
        }
    }

    return [PSCustomObject]@{
        Sido  = $sido
        Gugun = $gugun
        Dong  = $dong
    }
}

$inputFullPath = (Resolve-Path -LiteralPath $InputPath).Path
$outputFullPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutputPath)
$outputDirectory = Split-Path -Parent $outputFullPath
if (-not (Test-Path -LiteralPath $outputDirectory)) {
    New-Item -ItemType Directory -Path $outputDirectory | Out-Null
}

$cp949 = [System.Text.Encoding]::GetEncoding(949)
$lines = [System.IO.File]::ReadAllLines($inputFullPath, $cp949)

$rows = New-Object System.Collections.Generic.List[string]
foreach ($line in $lines | Select-Object -Skip 1) {
    if ([string]::IsNullOrWhiteSpace($line)) {
        continue
    }

    $columns = $line -split "`t"
    if ($columns.Count -lt 3) {
        continue
    }

    $code = $columns[0].Trim()
    $fullName = $columns[1].Trim()
    $status = $columns[2].Trim()

    if ($status -ne "존재" -or $code -eq "" -or $fullName -eq "") {
        continue
    }

    $location = Resolve-LocationParts -FullName $fullName
    $rows.Add(
        "(" +
        (Escape-SqlValue $code) + ", " +
        (Escape-SqlValue $location.Sido) + ", " +
        (Escape-SqlValue $location.Gugun) + ", " +
        (Escape-SqlValue $location.Dong) + ", " +
        (Escape-SqlValue $fullName) + ", TRUE)"
    )
}

$sql = New-Object System.Collections.Generic.List[string]
$sql.Add("-- Generated from backend/src/main/resources/data/legal-dong-code.txt")
$sql.Add("-- Source encoding: CP949. Output encoding: UTF-8.")
$sql.Add("-- Rows: $($rows.Count)")
$sql.Add("")
$sql.Add("SET NAMES utf8mb4;")
$sql.Add("START TRANSACTION;")
$sql.Add("")
$sql.Add("UPDATE legal_dongs SET active = FALSE WHERE id > 0;")
$sql.Add("")

for ($i = 0; $i -lt $rows.Count; $i += $BatchSize) {
    $end = [Math]::Min($i + $BatchSize - 1, $rows.Count - 1)
    $batch = $rows[$i..$end]

    $sql.Add("INSERT INTO legal_dongs (code, sido, gugun, dong, full_name, active) VALUES")
    for ($j = 0; $j -lt $batch.Count; $j++) {
        $suffix = if ($j -eq $batch.Count - 1) { "" } else { "," }
        $sql.Add("    $($batch[$j])$suffix")
    }
    $sql.Add("ON DUPLICATE KEY UPDATE")
    $sql.Add("    sido = VALUES(sido),")
    $sql.Add("    gugun = VALUES(gugun),")
    $sql.Add("    dong = VALUES(dong),")
    $sql.Add("    full_name = VALUES(full_name),")
    $sql.Add("    active = VALUES(active);")
    $sql.Add("")
}

$sql.Add("COMMIT;")

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($outputFullPath, $sql, $utf8NoBom)

Write-Host "Generated $OutputPath with $($rows.Count) active legal dong rows."
