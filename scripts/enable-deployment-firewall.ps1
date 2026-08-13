$ErrorActionPreference = 'Stop'

$rules = @(
    @{ Name = 'MOVEAI-HTTP-In-TCP'; DisplayName = 'MOVE-AI HTTP (80)'; Port = 80 },
    @{ Name = 'MOVEAI-HTTPS-In-TCP'; DisplayName = 'MOVE-AI HTTPS (443)'; Port = 443 }
)

foreach ($rule in $rules) {
    $existing = Get-NetFirewallRule -Name $rule.Name -ErrorAction SilentlyContinue
    if ($null -eq $existing) {
        New-NetFirewallRule `
            -Name $rule.Name `
            -DisplayName $rule.DisplayName `
            -Enabled True `
            -Direction Inbound `
            -Protocol TCP `
            -Action Allow `
            -LocalPort $rule.Port | Out-Null
    } else {
        Set-NetFirewallRule -Name $rule.Name -Enabled True -Direction Inbound -Action Allow
    }
}

Get-NetFirewallRule -Name 'MOVEAI-HTTP-In-TCP', 'MOVEAI-HTTPS-In-TCP' |
    Select-Object Name, Enabled, Direction, Action
