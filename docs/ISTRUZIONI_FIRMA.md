# Firma permanente degli APK

Per poter aggiornare Fantozzi Forever direttamente dall'app occorre utilizzare **sempre lo stesso keystore Android** per tutte le Release.

## Prima configurazione (Windows)

Creare il keystore una sola volta su un computer sicuro (Java `keytool`):

```powershell
keytool -genkeypair -v -keystore fantozzi-permanente.jks -alias fantozzi -keyalg RSA -keysize 3072 -validity 10000
```

Conservare il file `.jks`, l'alias e le password in un luogo sicuro, **fuori dal repository**. Per convertire il file in Base64 da PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\\percorso\\fantozzi-permanente.jks")) | Set-Clipboard
```

In GitHub aprire **Repository > Settings > Secrets and variables > Actions > New repository secret** e creare:

- `FF_KEYSTORE_BASE64`: contenuto Base64 del file `.jks`;
- `FF_STORE_PASSWORD`: password del keystore;
- `FF_KEY_ALIAS`: alias della chiave (es. `fantozzi`);
- `FF_KEY_PASSWORD`: password della chiave.

Dopo la configurazione, ogni nuova build pubblicata dal workflow `Compila e pubblica APK Android` sarà firmata con quel certificato. Gli APK saranno pubblicati come `*_release.apk` e saranno compatibili con i successivi aggiornamenti, purché `applicationId`, firma e `versionCode` restino coerenti.

## Attenzione agli APK precedenti

Le versioni 0.2 e 0.3 sono APK di debug firmati con chiavi temporanee: non è garantita la compatibilità con la nuova firma. **Non disinstallare l'app precedente senza aver valutato la perdita dei preferiti e dei dati locali.** L'app controlla la firma prima di proporre l'installazione.

Se i segreti mancano completamente, il workflow pubblica per compatibilità un APK di prova con suffisso `_debug.apk`; questo **non rende possibili aggiornamenti affidabili**. Se è configurata solo una parte dei segreti, la compilazione fallisce intenzionalmente invece di pubblicare un APK dalla firma inattesa.
