# Fantozzi Forever Player

App Android indipendente in italiano, con interfaccia compatta ispirata a Successo Player v5.0 e catalogo ufficiale di Fantozzi Forever.

## Versione 0.5.1

- Rimosso il filtro delle puntate preferite e il relativo pulsante.
- Eliminata la voce "Mostra tutte" collegata al filtro.
- Tutte le puntate sono visibili; la ricerca continua a funzionare.
- Le stelle continuano a salvare i preferiti, senza nascondere gli episodi.
- Controllo automatico in CI contro il ripristino involontario del filtro.

## Versione 0.5.0

- Catalogo **completo di 10 puntate** con titoli e durate verificati sulla pagina ufficiale RaiPlay Sound.
- Lettura asincrona del JSON pubblico del programma, cercando i collegamenti audio nei campi audio e downloadable_audio.
- Lettore nativo Media3 / ExoPlayer con MediaSessionService, controlli Android e supporto alla riproduzione in background.
- Mini-player fisso con ascolto e pausa, episodi precedente e successivo, ricerca, preferiti, tempo e punto di ripresa.
- Aggiornamenti dall'app mantenuti con firma permanente e download verificato.
- Senza WebView e senza copie degli episodi distribuite nel repository.

**Limitazioni:** l'endpoint JSON non e' documentato e può cambiare. La compatibilità dei flussi e la riproduzione audio necessitano di una prova su telefono Android reale. In caso di indisponibilità dell'audio, si può aprire il programma su RaiPlay Sound. Non si aggirano DRM o protezioni. Download offline, shuffle e timer non sono ancora implementati.

## Compilazione

Java 17, Android SDK 36, Gradle 8.11.1. Il workflow GitHub Actions pubblica l'APK firmato con il keystore permanente già configurato. Versione 0.5.1, versionCode 9, applicationId it.fantozziforever.player.

Documentazione: [Configurazione firma](docs/ISTRUZIONI_FIRMA.md).

Progetto indipendente non affiliato a Rai o ai produttori del programma. Diritti audio e marchi dei rispettivi titolari.
