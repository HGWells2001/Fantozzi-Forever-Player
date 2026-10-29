# Fantozzi Forever Player

Applicazione Android non ufficiale in italiano per consultare e ascoltare [Fantozzi Forever su RaiPlay Sound](https://www.raiplaysound.it/programmi/fantozziforever).

## Versione 0.2.2

- Accesso al sito ufficiale nella WebView dell'app.
- Navigazione tra le pagine, aggiornamento, stato di caricamento e messaggi di errore in italiano.
- Pulsante **Browser** per aprire la pagina corrente nel browser esterno, utile quando la riproduzione non è supportata dalla WebView.
- Gestione delle interruzioni del processo WebView.
- APK di prova pubblicato nelle GitHub Releases con versione letta dal progetto, evitando link temporanei agli artifact.
- Il sistema di distribuzione produce APK di **debug**. Non esiste ancora un certificato permanente di produzione.

**Limitazioni:** non è ancora un lettore Media3 nativo; non sono implementati mini-player, download offline, preferiti, avanzamento nativo o aggiornamento interno. La riproduzione dipende dal sito e deve essere provata su telefono.

## Compilazione

Java 17, SDK Android 36, Gradle 8.11.1:

```sh
gradle :app:assembleDebug
```

Identificativo: `it.fantozziforever.player`, Android 8+.

## Diritti

Progetto indipendente, non affiliato a Rai né ai produttori. Il repository non contiene copie degli episodi.
