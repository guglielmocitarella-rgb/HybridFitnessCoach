# Hybrid Fitness Coach

Android fitness coach ibrido per allenamento con pesi, corpo libero e calisthenics. Il progetto combina profilo atleta, attrezzatura disponibile, skill, generazione workout, esecuzione guidata, storico e recupero in un flusso unico.

## Stato del progetto — V0.8 Evoluta

La **V0.8 stabile** ha consolidato il flusso completo di allenamento. È ora iniziata la **V0.8 Evoluta**, dedicata alla robustezza dell'infrastruttura che diventerà la base dell'Adaptive Coach della V0.9.

Build stabile di riferimento prima dell'evoluzione: **Build #69**.

### Funzioni disponibili

- Profilo atleta con obiettivo, livello, frequenza, dati fisici e skill prioritarie
- Profili attrezzatura completamente personalizzabili
- Workout automatico dalla Home
- Creazione workout personalizzato per parti del corpo o skill calisthenics multiple
- Libreria esercizi con filtri e generazione workout
- Workout player con navigazione libera
- Timer allenamento e recuperi tra serie/esercizi
- Pausa, ripresa e salto recupero
- RPE per esercizio e fatica complessiva
- Gestione esercizi completati, parziali e non eseguiti
- Storico sessioni con confronto programmato/eseguito
- Recovery Engine basato sul lavoro realmente svolto
- Persistenza locale di profilo, attrezzatura, storico e recovery
- Build Android firmata tramite GitHub Actions

### V0.8 Evoluta — in sviluppo

L'evoluzione corrente introduce progressivamente:

- modello esercizio universale: reps, secondi, durata, distanza e intervalli;
- supporto strutturale per corpo libero, sovraccarico e assistenza;
- esercizi bilaterali, unilaterali e alternati;
- metadati per pattern motorio, categoria, intensità, fatica sistemica e complessità tecnica;
- Workout Runner generalizzato e indipendente dal tipo di esercizio;
- sessione attiva persistente e recuperabile;
- timer robusti basati su timestamp reali;
- storico strutturato e interrogabile;
- architettura offline-first;
- predisposizione Health Connect;
- esercizi pinned/locked per futura rigenerazione parziale;
- test automatici per state machine, timer, persistenza e migrazioni.

Il modello dati è già predisposto con un collegamento opaco alle future progressioni, evitando una nuova migrazione strutturale quando verrà introdotta la V0.9.

## Roadmap

**V0.8 Evoluta**  
Workout infrastructure, runner multimodale, affidabilità, persistenza e Health Connect readiness.

**V0.9**  
Progression Engine + Workout Sequencing Engine + Adaptive Coach. I dati di recovery, storico, RPE e salute potranno essere utilizzati per adattare progressivamente la programmazione.

## Stack

- Kotlin 2.4.10
- Jetpack Compose + Material 3
- Compose BOM 2026.09.00
- Android Gradle Plugin 9.4.0
- compileSdk / targetSdk 37
- minSdk 26

## Build automatica

GitHub Actions compila il progetto a ogni push sui branch configurati. La pipeline produce l'APK release firmato quando sono disponibili i secret di firma e pubblica l'artefatto della build per il collaudo Android.

Le modifiche della V0.8 Evoluta vengono introdotte incrementalmente: ogni macro-fase deve compilare correttamente prima di procedere alla successiva.

## Principio architetturale

```
Profilo atleta + Attrezzatura + Recovery
                 ↓
          Workout Generation
                 ↓
           Workout Runner
                 ↓
       Sessione strutturata
                 ↓
        Storico / Recovery
                 ↓
     Health Data Normalization
                 ↓
        Adaptive Coach V0.9
```

L'obiettivo della V0.8 Evoluta non è anticipare l'Adaptive Coach, ma assicurare che Workout Runner, dati e storico siano abbastanza solidi da non dover essere riscritti nella V0.9.
