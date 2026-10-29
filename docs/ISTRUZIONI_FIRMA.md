# Configurazione automatica della firma Android

Per aggiornare Fantozzi Forever Player senza disinstallarlo serve una sola chiave di firma Android permanente.

## Prerequisiti

Windows 10/11, Windows PowerShell 5.1 o PowerShell 7, Java JDK 17 con keytool, GitHub CLI con permessi di gestione dei Secrets.

Per installare GitHub CLI (se manca):

~~~powershell
winget install --id GitHub.cli --exact
~~~

## Come eseguire lo script

Da PowerShell, scarica lo script ufficiale del tuo repository e avvialo:

~~~powershell
Invoke-WebRequest "https://raw.githubusercontent.com/HGWells2001/Fantozzi-Forever-Player/main/tools/Configura-Firma-Permanente.ps1" -OutFile ".\Configura-Firma-Permanente.ps1"
powershell -NoProfile -ExecutionPolicy Bypass -File ".\Configura-Firma-Permanente.ps1"
~~~

Non servono i privilegi di amministratore. La prima volta GitHub CLI può richiedere l'accesso tramite browser.

Lo script genera una chiave RSA a 3072 bit, un keystore e una password casuale forte, poi invia automaticamente quattro Secrets a GitHub: FF_KEYSTORE_BASE64, FF_STORE_PASSWORD, FF_KEY_ALIAS, FF_KEY_PASSWORD. Le password passano via STDIN, non negli argomenti di un processo.

**Sicurezza:** il keystore non viene creato nel repository ma in %LOCALAPPDATA%\FantozziForever\Firma. La password viene salvata in credenziali-dpapi.xml, protetta dall'account Windows corrente. Per vedere la password e conservarla nel tuo gestore di password:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ".\Configura-Firma-Permanente.ps1" -MostraPassword
~~~

Fai un backup sicuro sia del keystore sia della password. Il file DPAPI non è normalmente utilizzabile da un altro account Windows.

## Riutilizzo, protezioni e compilazione

La firma viene generata solo quando non esiste né sul PC né come Secrets su GitHub. Lo script non sostituisce chiavi esistenti; in caso di Secrets parziali interrompe la configurazione. Usa **-SovrascriviSegreti** soltanto per riconfigurare GitHub con il keystore originale già verificato.

Dopo la configurazione, verifica i Secrets in GitHub, Settings > Secrets and variables > Actions. La prossima versione con versionCode e versionName incrementati in app/build.gradle sarà firmata automaticamente dal workflow e produrrà un file con suffisso _release.apk.

**Importante:** non lanciare la vecchia versione 0.4.0 già pubblicata: per la prossima Release bisogna aumentare la versione. I vecchi APK di debug potrebbero non essere aggiornabili mantenendo i dati perché il certificato è differente. Non disinstallarli senza valutare la perdita dei preferiti.

Lo script non avvia automaticamente una Release per evitare di sovrascrivere un tag esistente.
