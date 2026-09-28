# Hybrid Fitness Coach

Android fitness coach ibrido per allenamento con pesi, corpo libero e calisthenics. Il progetto combina profilo atleta, attrezzatura disponibile, skill, generazione workout, esecuzione guidata, storico e recupero in un flusso unico.

## Stato del progetto — V0.9 release candidate

La base V0.8 Evoluta è preservata. Il branch `feature/v0.9-adaptive-progression` integra Progression Engine, Skill Tree, Load Recommendation, Adaptive Coach, sequencing, Health Connect opzionale con fallback locale e rigenerazione pinned/locked. Dopo l'audit finale il gate catalogo è stato riaperto per rispettare il requisito originale di circa 500 esercizi reali e utili; la release resta candidata finché il nuovo catalogo e l'intera regression suite non risultano verdi.\n\nBuild stabile storica di riferimento prima dell'evoluzione: **Build #69**.

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

### V0.8 Evoluta — implementata

La V0.8 Evoluta include:

- modello esercizio universale: reps, secondi, durata, distanza e intervalli;
- supporto strutturale per corpo libero, sovraccarico e assistenza;
- esercizi bilaterali, unilaterali e alternati;
- metadati per pattern motorio, categoria, intensità, fatica sistemica e complessità tecnica;
- Workout Runner multimodale data-driven per reps, secondi/isometrie, durata, distanza e intervalli, con lateralità, carico e assistenza;
- sessione attiva persistente e recuperabile sia per workout automatici sia personalizzati;
- timer robusti basati su timestamp reali;
- storico strutturato e interrogabile;
- architettura offline-first;
- predisposizione Health Connect;
- esercizi pinned/locked per futura rigenerazione parziale;
- test JVM per state machine, prescrizioni multimodali, timer/deadline, sessioni parziali, storico eseguito, Recovery, generatore, Front Lever e boundary Health readiness.

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


## Implementato nella V0.8 Evoluta

- Modello esercizio universale con modalità reps, secondi, durata, distanza e intervalli.
- Metadati per lateralità, resistenza, recuperi, intensità e futura progressione.
- Stato sessione strutturato con previsto vs eseguito, RPE, skip, ordine e pinned.
- Persistenza locale della sessione attiva e ripristino del workout automatico.
- Recuperi basati su deadline assolute, ricostruibili dopo background/process recreation.
- State transitions pure per selezione, skip/ripresa, aggiunta serie e riordino.
- Storico retrocompatibile esteso con dati strutturati.
- Recovery deterministico basato sulle serie realmente completate e usato dal generatore.
- Filtri Libreria per attrezzatura, muscoli, skill e tipologia universale.
- Boundary architetturali Health Connect: raw data → normalizzazione → readiness input.
- Test JVM dell'infrastruttura e delle transizioni principali.

## Previsto per la V0.9

La V0.9 utilizzerà queste fondamenta per Progression Engine, grafi di progressione/regressione, deload, Workout Sequencing Engine, rigenerazione parziale con esercizi pinned e Adaptive Coach. Queste funzioni **non sono dichiarate implementate nella V0.8**.

## Affidabilità V0.8 Evoluta

Le funzioni fondamentali sono locali/offline-first: profilo, attrezzatura, libreria, generazione, Runner, sessione attiva, storico e Recovery non dipendono dalla rete. Lo storage SharedPreferences mantiene lettura retrocompatibile dei record V0.8 precedenti tramite campi opzionali/default, senza destructive migration.

Il timer di recupero usa deadline assolute e viene ricostruito al restore. Le sessioni attive conservano runner di origine, esercizio corrente, set eseguiti, valori effettivi, RPE, skip e contesto del recupero.

Health Connect resta volutamente un boundary architetturale nella V0.8: dati raw → normalizzazione → readiness input. La V0.8 non simula letture sanitarie e non implementa ancora decisioni adattive basate su Health Connect.

## Confine V0.9

Progression Engine, grafi skill, advance/maintain/regress, deload, Sequencing Engine, rigenerazione intelligente e Adaptive Coach restano funzionalità V0.9. La V0.8 fornisce modello universale, stato strutturato, pinned/locked readiness, storico eseguito e boundary readiness su cui costruirle.


## V0.9 — checklist persistente DoD

Baseline CI verificata: Android Build #166 verde (`7b108819`).

| Gate | Implementato | Testato | E2E | UX | CI |
|---|---|---|---|---|---|
| Progression Engine + skill graph/prerequisiti | sì | sì | sì | sì | verde |
| Skill Tree / progress / next unlock | sì | sì | sì | sì | verde |
| Load Recommendation + calibrazione | sì | sì | sì | Runner | verde |
| Adaptive Coach + recovery/readiness/RPE | sì | sì | sì | sì | verde |
| Sequencing + pinned/locked regeneration | sì | sì | sì | sì | verde |
| Health Connect opzionale + fallback | sì | sì | sì | sì | verde |
| Catalogo ~500 reale/deduplicato/metadati | in verifica | sì | sì | sì | in verifica |
| Ab Wheel + Foam Roller generator-ready | sì | sì | sì | sì | verde |
| Salto UX complessivo v0.9 | sì | n/a | sì | sì | verde |
| Persistenza / retrocompatibilità / offline-first | sì | sì | sì | sì | verde |
| Audit finale + APK 0.9 firmato + firma verificata | riaperto sul catalogo | sì | sì | sì | in verifica |

I gate marcati `parziale` impediscono esplicitamente di dichiarare V0.9 COMPLETATA. La checklist va aggiornata solo sulla base di codice, wiring, test e CI verificati.
