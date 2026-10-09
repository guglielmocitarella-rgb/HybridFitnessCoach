# G1 — Decisione di prodotto: assessment atleta V3 (2026-10-09)

**Stato:** decisione approvata; rubrica operativa e protocolli **da progettare e validare** (G1.1); implementazione **non avviata** (G1.2).

**Contesto:** G1 Work ha restituito PARTIAL, senza commit/push. Il contratto attuale `V3NativeAthleteCapabilityContract` usa cinque valori ordinali (`VERY_LOW`, `LOW`, `MODERATE`, `HIGH`, `VERY_HIGH`) per le quattro capacità trasversali, ma **non definisce** una rubrica per assegnarli all'atleta. Il profilo attuale usa dieci valutazioni 1–3, senza equivalenza valida ai cinque livelli. Il bridge lascia correttamente UNKNOWN le quattro capacità; il ranking V3 è isolato e V2 genera gli allenamenti produttivi.

## Decisione vincolante

1. **Autovalutazione guidata con verifica progressiva**, non autovalutazione libera priva di descrittori e non test obbligatori per tutti all'onboarding.
2. **Cinque livelli ordinati V3** per le quattro capacità trasversali (forza, competenza tecnica, mobilità, equilibrio/coordinazione) **e** per le competenze motorie, con rubriche **distinte** e prove di riferimento pertinenti.
3. **Non convertire numericamente** le valutazioni legacy 1–3 in 1–5. Esperienza generale è contesto, non clearance automatica; le dimensioni di movimento restano indipendenti. Le skill specifiche mantengono progressioni/assessment propri.
4. **Nessun punteggio globale unico** né media delle quattro capacità per autorizzare un esercizio.
5. **Evidenza esplicita e verificabile**: autovalutazione dichiarata, assessment diretto osservato, derivazione verificata o UNKNOWN; memorizzare data, protocollo/versione rubrica ed eventuale riferimento alla prova, quando applicabile. La provenienza non deve essere falsificata. La confidenza è distinta dal livello.
6. **Le autovalutazioni guidate possono contribuire all'eleggibilità iniziale**, ma l'accesso a requisiti particolarmente impegnativi può richiedere una prova mirata secondo una **politica esplicita da definire in G1.1**. Non applicare penalizzazioni arbitrarie (es. livello dichiarato meno uno).
7. **Aggiornamento progressivo**: storico, carichi, RPE, ripetizioni e qualità esecutiva possono suggerire rivalutazione; non promuovono automaticamente capacità senza una regola validata. Conservare la tracciabilità delle revisioni e la possibilità di invalidare assessment.
8. **Nessuna assunzione favorevole per valori mancanti**. L'atleta può completare gli assessment per gradi, ma l'app non può mascherare UNKNOWN come livello basso o sufficiente. Il percorso iniziale deve essere progettato esplicitamente e rispettare i gate di eleggibilità.
9. **Distinguere requisiti di esercizio e intensità della prescrizione**. Carico, assistenza, ripetizioni e contesto possono cambiare la fattibilità; non trasformare `PRESCRIPTION_DEPENDENT` in un livello scalare arbitrario.
10. **V3-only al cutover**, conservando fino ad allora V2 produttivo e il ranking V3 isolato. La roadmap consente il reset esplicito dei dati pre-V3 di prova, ma richiede la persistenza affidabile dei nuovi dati V3.

## Rubrica concettuale, non ancora un protocollo di misura

| Livello | Descrittore generale provvisorio |
| --- | --- |
| VERY_LOW | Compiti molto semplificati, capacità ancora in sviluppo |
| LOW | Compiti semplici e prevedibili |
| MODERATE | Compiti standard autonomi e controllati |
| HIGH | Compiti impegnativi con controllo ripetibile |
| VERY_HIGH | Compiti particolarmente impegnativi/complessi con controllo ripetibile |

**Questa tabella non autorizza assegnazioni automatiche né confronti clinici.** Per ciascuna delle quattro capacità servono definizioni operative, ancore osservabili, esempi e controesempi, condizioni di prova e soglie giustificate. La forza non coincide con il solo carico assoluto; la mobilità non coincide con l'escursione massima. Per le competenze motorie servono rubriche separate per spinta/tirata orizzontale e verticale, squat, hinge, core, isometriche e skill; l'esperienza generale resta descrittiva.

## Politica di eleggibilità — principi vincolanti

- Requisito KNOWN e assessment KNOWN sufficiente: possibile eleggibilità **per quella dimensione**, subordinata a provenienza/politica di verifica, altri requisiti, attrezzatura e prescrizione.
- Capacità insufficiente: esclusione con motivazione.
- Capacità UNKNOWN: non assumere sufficienza; mostrare motivazione e percorso di assessment.
- Requisito esercizio UNKNOWN: fail closed; non risolvibile aumentando l'autovalutazione.
- NOT_APPLICABLE: nessun assessment richiesto per quella dimensione.
- PRESCRIPTION_DEPENDENT: usare il resolver contestuale e un envelope attendibile.
- EVIDENCE_UNRESOLVED: stato canonico terminale, non sanabile da assessment atleta.
- L'eleggibilità precede il ranking: nessun bonus di punteggio recupera un esercizio escluso.
- **Da decidere e testare in G1.1:** quali requisiti accettano la sola autovalutazione, quali richiedono test, quale comportamento esplicito per casi non verificati e zero-pool. Non introdurre clearance di sicurezza implicite.

## Architettura obiettivo

1. Profilo atleta V3: assessment per movimento + quattro capacità trasversali, livello, provenienza, data, protocollo, versione, confidenza/stato.
2. Validazione e persistenza V3-native; dati legacy non convertiti arbitrariamente.
3. Bridge V3 tipizzato e snapshot con revisioni, senza reload JSON.
4. Eligibility engine con spiegazioni e politica di evidenza; ranking personalizzato solo sul pool eleggibile.
5. Composer (10C) bilancia movimenti, skill, volume, durata e varietà; prescriber (10D) risolve serie, carico, assistenza e recuperi.
6. Diagnostica: distinguere requisito canonico mancante, assessment assente, capacità insufficiente, prova richiesta, prescrizione non risolta e zero-pool.

## Piano e criteri di accettazione

**G1.1 — Contratto/rubrica (prossimo incarico):**
- Leggere contratti, roadmap, V3.2B, motore e un campione rappresentativo del **candidato finale congelato**; non basarsi solo su etichette enum.
- Definire rubriche specifiche e verificabili per quattro capacità e nove dimensioni motorie; esperienza generale resta contestuale.
- Mappare prove, fonti/evidenze, soglie, protocollo e limiti di autovalutazione; separare ciò che è deciso da ciò che richiede evidenza/approvazione.
- Progettare politica di eleggibilità per autovalutazione vs verifica, incluso UNKNOWN, PD, terminali, zero-pool e rischio di falsi positivi.
- Consegnare matrice esercizi/requisiti/capacità/prove, casi limite, proposte di UI e schema dati, con riferimenti puntuali ai record esaminati.
- **Nessuna modifica Kotlin, JSON canonico o asset Android e nessun push Actions per prove iterative.** Una revisione documentale approvata precede G1.2.

**G1.2 — Implementazione successiva all'approvazione:**
- UI assessment guidato, modello/persistenza, provenienza, bridge, policy di eleggibilità, diagnostica, test Kotlin e dispositivo.
- V2 resta produttivo; nessun cutover, Runner o cambio autorità del database.
- Eseguire test localmente; un solo push consolidato se completo, preservando il budget Actions.

**Sequenza:** G1.1 -> approvazione rubrica/policy -> G1.2 -> validazione Android -> 10C (isolato, poi integrazione secondo roadmap) -> 10D–10G -> 10H switch autorità V3 -> 10I rimozione V2.

## Vincoli su asset e verifiche

Asset Android 9R-B: 1087 esercizi, requisiti trasversali UNKNOWN; zero-pool è corretto. Candidato finale congelato: stesso insieme di identità, requisiti più informativi ma anche PD e terminali. Non alterare né promuovere gli asset durante G1.1/G1.2. Per test positivi utilizzare il candidato finale **solo in isolamento** con input espliciti e non spacciare fixture simulate per capacità reali.

## Perché questa decisione

Un passaggio cosmetico da 1–3 a 1–5 non rende il motore più accurato. L'obiettivo è collegare in modo spiegabile **ciò che l'atleta dichiara e dimostra**, **ciò che l'esercizio richiede** e **ciò che la prescrizione comporta**, senza falsi livelli, senza selezioni incompatibili e senza bloccare inutilmente l'onboarding. La politica d'evidenza deve essere esplicita e sottoposta a verifica prima di modificare il codice.

**Riferimenti:** `exercise-db-canonical/step9-v3-integration-roadmap.md`, `docs/step10b2-isolated-ranking.md`, `app/src/main/java/com/hybridfitnesscoach/app/domain/v3/V3NativeAthleteCapabilityContract.kt`, `app/src/main/java/com/hybridfitnesscoach/app/CapabilityDimensions.kt`, `app/src/main/java/com/hybridfitnesscoach/app/domain/v3/V3SelectionEngine.kt`.
