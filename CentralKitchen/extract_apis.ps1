$ErrorActionPreference = "Stop"
$branches = @(
  "origin/be/fixRouteAllocation", "be/fixRouteAllocation",
  "origin/be/confirm-receipt-final", "be/confirm-receipt-final",
  "origin/be/BOM/Wastage_spoilage", "be/BOM/Wastage_spoilage",
  "origin/be/settings-change-password", "be/settings-change-password",
  "origin/be/store-status-toggle", "be/store-status-toggle",
  "origin/be/store-profile-settings", "be/store-profile-settings",
  "origin/be/fixManageRecipes", "be/fixManageRecipes",
  "origin/be/GetProductList", "be/GetProductList"
)

$out = @()
$processed = @{}

foreach ($b in $branches) {
  if (git rev-parse --verify --quiet $b 2>$null) {
    $commit = git rev-parse $b
    if (-not $processed.ContainsKey($commit)) {
      $processed[$commit] = $true
      $out += "============================="
      $out += "BRANCH: $b"
      $out += "============================="
      
      $logs = git log origin/main..$b -p -- "src/main/java/com/groupSWP/centralkitchenplatform/controllers/"
      foreach ($line in $logs) {
        if ($line -match "^\+.*@(RequestMapping|GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping|PreAuthorize)") {
          $out += $line.Trim()
        }
      }
    }
  }
}
$out | Out-File -FilePath "extract_apis.txt" -Encoding utf8
