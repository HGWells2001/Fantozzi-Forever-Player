#requires -Version 5.1
<#
.SYNOPSIS
  Configura automaticamente la firma permanente di Fantozzi Forever Player.
.DESCRIPTION
  Richiede Windows, GitHub CLI e JDK. Non sovrascrive keystore o segreti
  esistenti; salva una password casuale con DPAPI, e configura i quattro
  GitHub Actions Secrets senza esporre i valori nella riga di comando.
.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File .\Configura-Firma-Permanente.ps1
#>
[CmdletBinding()]
param(
    [ValidatePattern('^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$')]
    [string]$Repository = 'HGWells2001/Fantozzi-Forever-Player',
    [ValidatePattern('^[A-Za-z0-9_.-]+$')]
    [string]$Alias = 'fantozzi',
    [string]$CartellaFirma = (Join-Path $env:LOCALAPPDATA 'FantozziForever\Firma'),
    [switch]$SovrascriviSegreti,
    [switch]$MostraPassword
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'Richiesto Windows: protezione DPAPI.' }

function Trova-Programma([string]$Nome) {
    $cmd = Get-Command $Nome -ErrorAction SilentlyContinue
    if ($null -ne $cmd) { return $cmd.Source }
    return $null
}
function Nuova-Password {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) }
    finally { $rng.Dispose() }
    return ([BitConverter]::ToString($bytes)).Replace('-', '')
}
function Invia-Segreto([string]$Nome, [string]$Valore, [string]$Exe, [string]$Repo) {
    # Lo STDIN evita di passare segreti negli argomenti visibili del processo.
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $Exe
    $info.Arguments = 'secret set ' + $Nome + ' --repo ' + $Repo
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardInput = $true
    $info.RedirectStandardError = $true
    $info.RedirectStandardOutput = $true
    $processo = [System.Diagnostics.Process]::Start($info)
    if ($null -eq $processo) { throw "Impossibile avviare GitHub CLI: $Nome" }
    try {
        $processo.StandardInput.Write($Valore)
        $processo.StandardInput.Close()
        $errore = $processo.StandardError.ReadToEnd()
        $null = $processo.StandardOutput.ReadToEnd()
        $processo.WaitForExit()
        if ($processo.ExitCode -ne 0) { throw ("GitHub CLI, segreto " + $Nome + ': ' + $errore.Trim()) }
    } finally { $processo.Dispose() }
}

$gh = Trova-Programma 'gh.exe'
if (-not $gh) { $gh = Trova-Programma 'gh' }
if (-not $gh) { throw 'Manca GitHub CLI. Installa: winget install --id GitHub.cli --exact' }

$keytool = Trova-Programma 'keytool.exe'
if (-not $keytool) { $keytool = Trova-Programma 'keytool' }
if (-not $keytool -and $env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
    if (Test-Path -LiteralPath $candidate) { $keytool = $candidate }
}
if (-not $keytool) { throw 'Manca Java JDK con keytool. Installa JDK 17.' }

Write-Host '=== Firma Android permanente: Fantozzi Forever ===' -ForegroundColor Cyan
Write-Host "Repository: $Repository"
try { & $gh auth status --hostname github.com *> $null } catch { $global:LASTEXITCODE = 1 }
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Autenticazione GitHub dal browser...' -ForegroundColor Yellow
    & $gh auth login --hostname github.com --web
    if ($LASTEXITCODE -ne 0) { throw 'Accesso GitHub non completato.' }
}
$repoTrovato = & $gh repo view $Repository --json nameWithOwner --jq '.nameWithOwner'
if ($LASTEXITCODE -ne 0 -or (($repoTrovato | Out-String).Trim() -ne $Repository)) {
    throw 'Repository GitHub non accessibile.'
}
$nomi = @('FF_KEYSTORE_BASE64', 'FF_STORE_PASSWORD', 'FF_KEY_ALIAS', 'FF_KEY_PASSWORD')
$esistenti = @(& $gh secret list --repo $Repository --json name --jq '.[].name')
if ($LASTEXITCODE -ne 0) { throw 'Impossibile consultare i Secrets. Verifica i permessi GitHub.' }
$presenti = @($nomi | Where-Object { $esistenti -contains $_ })

if (-not (Test-Path -LiteralPath $CartellaFirma)) {
    New-Item -ItemType Directory -Path $CartellaFirma -Force | Out-Null
}
# La cartella e nel profilo utente e conserva le ACL standard ereditate da
# %LOCALAPPDATA%. Non usare Set-Acl: puo richiedere SeSecurityPrivilege
# e interrompere la configurazione anche quando keystore e password sono validi.
# Non spostare il keystore in cartelle condivise o sincronizzate.
Write-Host 'Uso i permessi Windows esistenti per la cartella privata di firma.'
$keystore = Join-Path $CartellaFirma 'fantozzi-permanente.jks'
$credenziali = Join-Path $CartellaFirma 'credenziali-dpapi.xml'
$esisteChiave = Test-Path -LiteralPath $keystore
$esisteCredenziale = Test-Path -LiteralPath $credenziali
if ($esisteChiave -ne $esisteCredenziale) {
    throw 'Firma locale incompleta. Ripristina keystore e credenziali originali.'
}
if (-not $esisteChiave -and $presenti.Count -gt 0) {
    throw 'Su GitHub esistono gia segreti di firma. Recupera la chiave originale: non generarne una nuova.'
}

if ($esisteChiave) {
    Write-Host 'Keystore esistente: riutilizzo senza generare nuove chiavi.'
    $credential = Import-Clixml -LiteralPath $credenziali
    if (-not ($credential -is [System.Management.Automation.PSCredential])) {
        throw 'Credenziali DPAPI non valide.'
    }
    if ($credential.UserName -ne $Alias) { throw "Alias diverso. Usa -Alias $($credential.UserName)" }
    $password = $credential.GetNetworkCredential().Password
} else {
    $password = Nuova-Password
    $env:FF_FIRMA_PASSWORD_TEMP = $password
    try {
        & $keytool -genkeypair -noprompt -keystore $keystore -storetype JKS -alias $Alias -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Fantozzi Forever Player, OU=Android, O=Progetto indipendente, C=IT' '-storepass:env' FF_FIRMA_PASSWORD_TEMP '-keypass:env' FF_FIRMA_PASSWORD_TEMP
        if ($LASTEXITCODE -ne 0) { throw 'keytool non ha creato la firma.' }
        $secure = ConvertTo-SecureString -String $password -AsPlainText -Force
        $credential = [System.Management.Automation.PSCredential]::new($Alias, $secure)
        $credential | Export-Clixml -LiteralPath $credenziali
        Write-Host 'Nuova firma RSA 3072 creata fuori dal repository.' -ForegroundColor Green
    } catch {
        if (-not (Test-Path -LiteralPath $credenziali)) {
            Remove-Item -LiteralPath $keystore -Force -ErrorAction SilentlyContinue
        }
        throw
    } finally {
        Remove-Item Env:FF_FIRMA_PASSWORD_TEMP -ErrorAction SilentlyContinue
    }
}
# Con PowerShell 5.1, i messaggi informativi di keytool su stderr possono
# essere promossi a NativeCommandError con ErrorActionPreference=Stop.
# Avviamo keytool come processo e controlliamo soltanto l'exit code.
# La password passa tramite variabile d'ambiente, mai negli argomenti.
$env:FF_FIRMA_PASSWORD_TEMP = $password
try {
    $processInfo = New-Object System.Diagnostics.ProcessStartInfo
    $processInfo.FileName = $keytool
    $processInfo.Arguments = '-list -keystore "' + $keystore + '" -storetype JKS -alias ' + $Alias + ' -storepass:env FF_FIRMA_PASSWORD_TEMP'
    $processInfo.UseShellExecute = $false
    $processInfo.CreateNoWindow = $true
    $processInfo.RedirectStandardOutput = $true
    $processInfo.RedirectStandardError = $true
    $verificaProcesso = [System.Diagnostics.Process]::Start($processInfo)
    if ($null -eq $verificaProcesso) { throw 'Impossibile avviare keytool per la verifica.' }
    try {
        $outputVerifica = $verificaProcesso.StandardOutput.ReadToEnd()
        $erroreVerifica = $verificaProcesso.StandardError.ReadToEnd()
        $verificaProcesso.WaitForExit()
        if ($verificaProcesso.ExitCode -ne 0) {
            throw ('Verifica keystore fallita (codice ' + $verificaProcesso.ExitCode + '): ' + $erroreVerifica.Trim())
        }
    } finally {
        $verificaProcesso.Dispose()
    }
    Write-Host 'Keystore e alias verificati correttamente.' -ForegroundColor Green
} finally {
    Remove-Item Env:FF_FIRMA_PASSWORD_TEMP -ErrorAction SilentlyContinue
}
if ($MostraPassword) {
    Write-Warning 'La password appare sullo schermo. Salvala nel tuo gestore di password.'
    Write-Host ('Alias: ' + $Alias)
    Write-Host ('Password keystore e chiave: ' + $password)
}
if ($presenti.Count -gt 0 -and -not $SovrascriviSegreti) {
    if ($presenti.Count -eq $nomi.Count) {
        Write-Host 'I quattro Secrets esistono gia. Non sono stati modificati.' -ForegroundColor Yellow
        Write-Host 'Per reinviare la stessa firma verificata usa -SovrascriviSegreti.'
        exit 0
    }
    throw 'Secrets parziali su GitHub. Controlla prima i valori esistenti; usa -SovrascriviSegreti solo con la chiave originale.'
}

Write-Host 'Caricamento dei quattro GitHub Actions Secrets...' -ForegroundColor Cyan
$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore))
Invia-Segreto 'FF_KEYSTORE_BASE64' $base64 $gh $Repository
Invia-Segreto 'FF_STORE_PASSWORD' $password $gh $Repository
Invia-Segreto 'FF_KEY_ALIAS' $Alias $gh $Repository
Invia-Segreto 'FF_KEY_PASSWORD' $password $gh $Repository
$verifica = @(& $gh secret list --repo $Repository --json name --jq '.[].name')
if ($LASTEXITCODE -ne 0) { throw 'Impossibile confermare i Secrets su GitHub.' }
$mancanti = @($nomi | Where-Object { $verifica -notcontains $_ })
if ($mancanti.Count -gt 0) { throw ('Secrets mancanti: ' + ($mancanti -join ', ')) }

Write-Host 'FIRMA CONFIGURATA: quattro Secrets disponibili su GitHub.' -ForegroundColor Green
Write-Host "Keystore locale: $keystore"
Write-Host "Password protetta con DPAPI: $credenziali"
Write-Warning 'Conserva un backup del keystore e della password: DPAPI non e trasferibile su un altro account Windows.'
Write-Warning 'Non pubblicare mai keystore o credenziali nel repository.'
Write-Host 'La firma si applica alla PROSSIMA nuova versione. Non ripubblico v0.4.0: il tag e gia esistente.'
