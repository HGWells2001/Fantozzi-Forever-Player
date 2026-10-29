# Fantozzi Forever Player

Applicazione Android indipendente in italiano, con **interfaccia nativa ispirata a Successo Player v5.0**.

## v0.3.0

- Eliminata completamente la WebView.
- Schermata con intestazione verde, catalogo nativo, righe puntate, filtro preferiti, ricerca e menu compatto ⋮.
- Sei puntate con titoli e durata verificati sulla [pagina ufficiale](https://www.raiplaysound.it/programmi/fantozziforever), catalogo ancora parziale.
- Selezione e preferiti memorizzati sul telefono.
- Pannello inferiore fisso ispirato al mini-player di Successo Player.
- La riproduzione **non è ancora nativa**. Il pulsante «Apri puntata» apre attualmente la pagina del programma nel browser; non è un controllo Play.
- Nessun file audio viene fornito nel repository. Per Media3 servono URL audio affidabili e leciti.

## Compilazione

Java 17, SDK Android 36, Gradle 8.11.1:

```bash
gradle :app:assembleDebug
```

La pipeline GitHub Actions pubblica una Release APK di prova per ogni versione.

## Indipendenza

`applicationId`: `it.fantozziforever.player`. Il progetto originale [Successo Player](https://github.com/HGWells2001/Successo-Player-Android) non viene modificato. App non ufficiale e non affiliata a Rai.
