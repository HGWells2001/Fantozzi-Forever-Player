# Fantozzi Forever Player per Android

Applicazione **non ufficiale e interamente in italiano**, dedicata a [Fantozzi Forever su RaiPlay Sound](https://www.raiplaysound.it/programmi/fantozziforever).

## Versione 0.1.0: prima base Android

Questa prima versione comprende un'app Android autonoma che apre il programma nel sito ufficiale tramite una WebView, con navigazione Puntate, Indietro, Avanti e Aggiorna, barra di caricamento, supporto JavaScript e apertura esterna dei collegamenti non Rai. Il catalogo e l'eventuale riproduzione restano gestiti dal sito RaiPlay Sound: **la riproduzione audio in WebView non è ancora verificata su dispositivo fisico**.

**Non sono ancora implementati**: Media3 nativo, mini-lettore, download offline, preferiti, progressi per puntata, ricerca nativa, coda, shuffle e aggiornamenti automatici. Sono previsti nelle versioni successive soltanto dopo aver verificato un accesso affidabile e consentito alle risorse audio.

## Compilazione

Richiede Java 17, Android SDK 36 e Gradle 8.11.1. In locale:

```sh
gradle :app:assembleDebug
```

Il workflow GitHub Actions `Compila APK Android` esegue la compilazione e salva l'APK di debug fra gli artifact del workflow, se la compilazione va a buon fine. Nessuna firma di distribuzione o release stabile è ancora configurata.

## Dati tecnici

- Identificativo app: `it.fantozziforever.player`
- Android 8.0 e successivi (minSdk 26)
- targetSdk 35, compileSdk 36
- Java 17

## Nota sui diritti

Progetto indipendente, non affiliato a Rai o ai produttori di *Fantozzi Forever*. Contenuti, immagini e marchi appartengono ai rispettivi titolari. Il codice non include copie degli episodi.
