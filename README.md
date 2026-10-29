# Fantozzi Forever Player

Applicazione Android non ufficiale in italiano, con catalogo nativo ispirato a Successo Player v5.0 e collegamento agli episodi ufficiali su RaiPlay Sound.

## Versione 0.4.1: prima distribuzione con firma permanente

- La firma permanente e' stata configurata per GitHub Actions.
- Il workflow ora **interrompe la pubblicazione** se mancano i segreti di firma.
- Prima della Release verifica firma APK, applicationId, versionCode e versionName.
- `versionCode` incrementato a 7 per garantire un numero di versione Android crescente.
- **Attenzione:** la migrazione dagli APK di debug 0.4.0 o precedenti non puo' essere installata sopra una firma diversa. Conserva un backup dei dati prima di disinstallare la vecchia app. Dalla versione 0.4.1 in poi la firma deve rimanere la stessa.

## Versione 0.4.0: aggiornamenti dall'app

- Controllo di nuove versioni all'avvio e voce **Aggiornamenti** nel menu ⋮.
- Lettura delle GitHub Releases pubbliche, incluse le versioni di prova.
- Richiesta di conferma prima del download.
- Download dell'APK in una cartella privata temporanea, con avanzamento, annullamento e verifica **SHA-256**.
- Controllo dell'identificativo Android, del `versionCode` e della **firma digitale** prima di aprire l'installer.
- Installazione con il consenso dell'utente attraverso il sistema Android e `FileProvider`.
- Nessuna richiesta di accesso generale ai file del dispositivo.
- Workflow GitHub Actions predisposto per firma permanente attraverso quattro repository secrets.

### Limite fondamentale della firma

Le versioni fino alla **0.3.0** sono state compilate e pubblicate come APK di debug dai runner temporanei di GitHub. Di norma **non condividono lo stesso certificato** e non possono quindi aggiornarsi conservando i dati. L'app **blocca** il tentativo se la firma non coincide.

Per avere aggiornamenti installabili uno sopra l'altro, configurare un'unica chiave di firma permanente con lo [script PowerShell automatico](tools/Configura-Firma-Permanente.ps1) e le [istruzioni](docs/ISTRUZIONI_FIRMA.md). Non caricare mai la chiave privata nel repository.

La prima transizione da un vecchio APK di debug a un APK con firma permanente **non garantisce la conservazione dei dati**. Le release successive firmate con la stessa chiave potranno aggiornarsi normalmente.

### Funzioni attuali e limitazioni

Catalogo nativo parziale, ricerca, preferiti, selezione delle puntate e pannello inferiore fisso. La riproduzione **non è ancora nativa Media3**: il pulsante apre il sito ufficiale nel browser. L'app non distribuisce audio o immagini protette.

## Compilazione

Java 17, Android SDK 36, Gradle 8.11.1:

```bash
gradle :app:assembleDebug
```

Identificativo `it.fantozziforever.player`, Android 8.0+.

Progetto indipendente, non affiliato a Rai o ai produttori di *Fantozzi Forever*. Tutti i contenuti e marchi appartengono ai rispettivi titolari.
