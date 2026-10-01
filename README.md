# Hybrid Fitness Coach

> **Compatibility policy (v0.9.3 onward):** backward compatibility with previous app builds, persistence schemas, data models, or legacy behavior is intentionally out of scope and is not planned for future releases. Development targets the clean current model. Legacy migration/fallback code must not be retained solely for compatibility unless this policy is explicitly changed by the project owner.

Android fitness coach ibrido per allenamento con pesi, corpo libero e calisthenics. Il progetto combina profilo atleta, attrezzatura disponibile, skill, generazione workout, esecuzione guidata, storico e recupero in un flusso unico.

## Stato del progetto — v0.9.4 attiva

La v0.9.3 resta congelata su `release/v0.9.3`. Lo sviluppo corrente è esclusivamente su `feature/v0.9.4` e PR #11; `V0_9_4_DOD.md` è la DoD vincolante.

### Baseline verde e candidato corrente

Il rollback sicuro corrente è `checkpoint/v0.9.4-f10-green` a `1ec072b36e2cde04bf16a7d8a190a28c6c276f44`, verificato da Exercise DB v2 #291 e Android APK #844.

Il branch di sviluppo ha come ultimo baseline CI verificato `1ec072b36e2cde04bf16a7d8a190a28c6c276f44`, congelato come `checkpoint/v0.9.4-f10-green`. Sullo SHA exact-CI Exercise DB v2 #291 e Android APK #844 sono entrambi verdi. F09 e F10 sono repository-level verificati; F11 è implementato ed è in chiusura con regression evidence rafforzata prima del pre-CI globale.

Sul candidato corrente il piano multisettimanale persiste intenti di sessione indipendenti dal luogo (schema piano v5); gli esercizi concreti vengono materializzati all'avvio in base a luogo, capability, storico e readiness. Gli snapshot concreti restano nel runner/storico come evidenza di ciò che è stato realmente prescritto/eseguito. Le migrazioni legacy del piano sono fuori scope per policy.

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
- Build Android tramite GitHub Actions; artefatto firmato sui gate release configurati
- Piano multi-settimana intent-only, modificabile e materializzato rispetto al luogo al momento dell'esecuzione
- Exercise DB v2 con provenienza/evidence tier e validazione deterministica

### Architettura consolidata

Le fondamenta delle release precedenti restano attive: runner multimodale, stato sessione strutturato, timer persistenti, storico reale, Recovery/Readiness, Health Connect opzionale, skill/progressioni e generazione adattiva. La v0.9.4 non riapre queste aree salvo regressioni dimostrate.

Il flusso corrente è:

```
Capability Profile + TrainingLocation + Recovery/Readiness + History
                              ↓
                    Plan intent / Adaptive Coach
                              ↓
             Execution-time workout materialization
                              ↓
                         Workout Runner
                              ↓
                  Structured execution evidence
                              ↓
                    History / Recovery / Coach
```

Il piano futuro non è un archivio di esercizi precongelati: conserva l'intento. Gli esercizi concreti appartengono alla materializzazione della sessione e all'evidenza di esecuzione.

## Roadmap attiva

**v0.9.3** — congelata; baseline storica.

**v0.9.4** — sviluppo e acceptance attivi. F01–F10 sono repository-level verificati nei rispettivi scope (restano le acceptance fisiche esplicitamente indicate). F11 ha già il reset di preview implementato ed è in chiusura con regression test rafforzati; seguiranno audit globale F01–F11, CI finale, APK firmato e device acceptance.

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
