# Hybrid Fitness Coach

> **Compatibility policy (v0.9.3 onward):** backward compatibility with previous app builds, persistence schemas, data models, or legacy behavior is intentionally out of scope and is not planned for future releases. Development targets the clean current model. Legacy migration/fallback code must not be retained solely for compatibility unless this policy is explicitly changed by the project owner.

Android fitness coach ibrido per allenamento con pesi, corpo libero e calisthenics. Il progetto combina profilo atleta, attrezzatura disponibile, skill, generazione workout, esecuzione guidata, storico e recupero in un flusso unico.

## Stato del progetto — v0.9.3 congelata / v0.9.4 test e sviluppo

La v0.9.3 è la baseline congelata dopo il completamento dei gate repository-level e delle workflow Exercise DB v2 #250 e Android APK #802 sul checkpoint `365c07f643065df9dd68310362edd3ad3ed3236f`. Il riferimento immutabile di linea è `release/v0.9.3`; la PR #10 resta audit trail della fase v0.9.3.

Lo sviluppo e il device acceptance successivi appartengono a `feature/v0.9.4`. Qualsiasi problema trovato sul device viene registrato e corretto in v0.9.4, senza modificare la baseline v0.9.3. `V0_9_3_DOD.md` è storico; `V0_9_4_DOD.md` è la DoD attiva.

### Checkpoint v0.9.4 verificato

Il checkpoint `0735f62152221299cbdc894eb023456f685c05fa` su `feature/v0.9.4` ha superato Exercise DB v2 #273 e Android APK #826. In questo checkpoint il dominio attrezzatura/luogo usa il contratto canonico `TrainingLocation`; il vecchio livello atleta globale non guida più generazione o prescrizione, sostituito dalle dimensioni Capability valutate; i recuperi effettivi/source-backed vengono propagati e persistiti nel runner.

Il vecchio nome di branch `feature/v0.9.1-training-plan` è solo un riferimento storico e non rappresenta la release attiva.

### Funzioni disponibili

- Profilo atleta con dati fisici e Capability Profile multidimensionale; obiettivo/frequenza/skill prioritarie appartengono alla configurazione del piano
- Luoghi di allenamento (`TrainingLocation`) con attrezzatura/capacità canoniche
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
- Storico strutturato per lo schema corrente.
- Recovery deterministico basato sulle serie realmente completate e usato dal generatore.
- Filtri Libreria per attrezzatura, muscoli, skill e tipologia universale.
- Boundary architetturali Health Connect: raw data → normalizzazione → readiness input.
- Test JVM dell'infrastruttura e delle transizioni principali.

## Previsto per la V0.9

La V0.9 utilizzerà queste fondamenta per Progression Engine, grafi di progressione/regressione, deload, Workout Sequencing Engine, rigenerazione parziale con esercizi pinned e Adaptive Coach. Queste funzioni **non sono dichiarate implementate nella V0.8**.

## Affidabilità V0.8 Evoluta

Le funzioni fondamentali sono locali/offline-first: profilo, attrezzatura, libreria, generazione, Runner, sessione attiva, storico e Recovery non dipendono dalla rete. Lo storage SharedPreferences è validato sullo schema corrente; la lettura di record di versioni precedenti non costituisce un requisito.

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
| Persistenza current-schema / offline-first | sì | sì | sì | sì | verde |
| Audit finale + APK 0.9 firmato + firma verificata | riaperto sul catalogo | sì | sì | sì | in verifica |

I gate marcati `parziale` impediscono esplicitamente di dichiarare V0.9 COMPLETATA. La checklist va aggiornata solo sulla base di codice, wiring, test e CI verificati.
