# G1.1-C — Audit record-level del candidato canonico V3

**Stato: COMPLETED — audit sui byte originali allegati.**  
Branch iniziale: feature/exercise-db-v3; HEAD: 9e929edcc60efbd73b3f58745727de79b4430756.  
Fonte: exercise-db-canonical/exercise-db-v3-final-candidate.json. Decisione vincolante: [G1 — assessment V3](../g1-v3-athlete-assessment-decision.md).

## Integrità

- SHA-256 byte originali: 0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a (atteso: corrisponde).
- Dimensione: 19.054.419 byte.
- JSON valido: array di 1.087 record; ID canonici non vuoti, 1.087 unici; schemaVersion 3.7; identityState KNOWN.
- La validation artifact Step 6 riporta 3.787/3.787 materializzazioni, 441 PD preservati, 48 terminali contabilizzati, nessuna integrazione in produzione. JSON invariato.

## Distribuzioni dei requisiti

| Dimensione | VERY_LOW | LOW | MODERATE | HIGH | VERY_HIGH | PD | EVIDENCE_UNRESOLVED |
|---|---:|---:|---:|---:|---:|---:|---:|
| Forza | 91 | 97 | 160 | 182 | 106 | 441 | 10 |
| Tecnica | 11 | 370 | 335 | 267 | 93 | 0 | 11 |
| Mobilità | 10 | 336 | 510 | 203 | 11 | 0 | 17 |
| Equilibrio/coordinazione | 205 | 302 | 258 | 251 | 61 | 0 | 10 |

I valori EVIDENCE_UNRESOLVED sono 48 campi requisito su 19 record, non 48 record. Forza PD = 441 (436 EXTERNAL_LOAD, 5 ASSISTED). Equipment è KNOWN 997 / EVIDENCE_UNRESOLVED 90; mode REQUIRED 710 / NONE 287 / UNKNOWN 90. Resistenza è KNOWN 1.054 / EVIDENCE_UNRESOLVED 33, con source BODYWEIGHT 550, EXTERNAL_LOAD 436, NONE 63, UNKNOWN 33, ASSISTED 5. prescriptions KNOWN 959 / EVIDENCE_UNRESOLVED 128; movementPatterns KNOWN 911 / UNKNOWN 176. loadInput è SCHEMA_UNRESOLVED in 436 record e NOT_APPLICABLE in 651.

## Contratto dati verificato

- Tutti i cinque livelli sono presenti in ciascuna dimensione. Lo schema 3.7 documenta LOW/MODERATE/HIGH ma omette VERY_LOW/VERY_HIGH e mescola UNKNOWN/NOT_APPLICABLE nella scala. Correzione documentale candidata: separare knownCapabilityLevels (cinque token) dal dominio SemanticState; non modificare JSON.
- V3AssetLoader accetta esplicitamente i cinque token come KNOWN; CapabilityLevel Kotlin è nello stesso ordine. V3SelectionEngine confronta ordinali come soglie minime, con uguaglianza sufficiente.
- capability.state=EVIDENCE_UNRESOLVED compare in tutti i record. Il selettore ammette lo stato aggregato e valuta ogni dimensione; i valori terminali di singola dimensione causano TERMINAL_CANONICAL_STATE. Non bloccare tutto il record solo per lo stato aggregato.
- Le dimensioni di requisito non contengono UNKNOWN o NOT_APPLICABLE. Questi stati sono presenti altrove: per esempio ex-4-point-burpee ha equipment mode UNKNOWN; cm-45-degree-leg-press ha loadInput NOT_APPLICABLE quando non applicabile, mentre cm-arm-circling ha resistance source UNKNOWN e movementPatterns UNKNOWN.
- Step 8A richiede per ogni PD strength confronto contestuale, typed e matched per esercizio/dimensioni. Il JSON non contiene carico/assistenza quantitativi tipizzati né capacità atleta; reps da sole non risolvono PD. Vietati scalarizzazione e write-back.

## Campione record-level

Campione di 23 record: copre i 20 incroci dimensione × livello presenti e, dove disponibili, corpo libero, carico esterno, macchine, assistenza, isometriche, skill, PD, terminali, equipment UNKNOWN e pattern UNKNOWN. Requisiti in ordine: forza / tecnica / mobilità / equilibrio-coordinazione. Non si deduce “elementare/intermedio/avanzato” dai nomi o da legacyLevel.

| Canonical ID | Nome | Categoria / pattern | Requisiti | Equipment / resistenza | Prescrizione | Verifica |
|---|---|---|---|---|---|---|
| cm-90-90-anche | 90/90 anche | MOBILITY / hip-mobility | VERY_LOW / LOW / HIGH / LOW | NONE / BODYWEIGHT | KNOWN:DURATION | valori KNOWN: requisiti minimi ordinali; compatibile con rubrica |
| cm-bird-dog | Bird dog | CORE / core-control | LOW / MODERATE / LOW / MODERATE | NONE / BODYWEIGHT | KNOWN:REPS | valori KNOWN: requisiti minimi ordinali; compatibile con rubrica |
| cm-handstand-kick-up | Handstand kick-up | SKILL / vertical-push | MODERATE / HIGH / MODERATE / HIGH | NONE / BODYWEIGHT | KNOWN:REPS | valori KNOWN: requisiti minimi ordinali; compatibile con rubrica |
| cm-freestanding-handstand-hold | Freestanding handstand hold | SKILL / vertical-push | HIGH / VERY_HIGH / HIGH / VERY_HIGH | NONE / BODYWEIGHT | EVIDENCE_UNRESOLVED:SECONDS | isometrica; prescrizione EVIDENCE_UNRESOLVED; skill specifica handstand_freestanding |
| ex-full-back-lever | Full Back Lever | SKILL / UNKNOWN | VERY_HIGH / HIGH / VERY_HIGH / VERY_LOW | pull_up_bar / BODYWEIGHT | EVIDENCE_UNRESOLVED:SECONDS | pattern UNKNOWN |
| cm-dumbbell-shrug | Dumbbell shrug | PULL / pull-accessory | PRESCRIPTION_DEPENDENT / VERY_LOW / VERY_LOW / VERY_LOW | dumbbell / EXTERNAL_LOAD | KNOWN:REPS | PD; richiede envelope contestuale |
| cm-45-degree-leg-press | 45-degree leg press | LEGS / knee-dominant | PRESCRIPTION_DEPENDENT / LOW / LOW / VERY_LOW | machine / EXTERNAL_LOAD | KNOWN:REPS | PD; richiede envelope contestuale |
| cm-pistol-squat-assistito | Pistol squat assistito | SKILL / horizontal-push | PRESCRIPTION_DEPENDENT / MODERATE / HIGH / MODERATE | NONE / ASSISTED | KNOWN:REPS | PD; richiede envelope contestuale; assistenza non quantificata; skill specifica pistol_assisted |
| cm-back-lever-advanced-tuck | Back lever advanced tuck | SKILL / vertical-pull | HIGH / HIGH / HIGH / MODERATE | pull_up_bar / BODYWEIGHT | EVIDENCE_UNRESOLVED:SECONDS | isometrica; prescrizione EVIDENCE_UNRESOLVED; skill specifica backlever_advanced_tuck |
| cm-assisted-one-arm-pull-up | Assisted one arm pull-up | SKILL / vertical-pull | PRESCRIPTION_DEPENDENT / VERY_HIGH / MODERATE / HIGH | pull_up_bar/resistance_band / ASSISTED | KNOWN:REPS | PD; richiede envelope contestuale; assistenza non quantificata |
| cm-alternating-dumbbell-curl | Alternating dumbbell curl | PULL / pull-accessory | PRESCRIPTION_DEPENDENT / LOW / VERY_LOW / VERY_LOW | dumbbell / EXTERNAL_LOAD | KNOWN:REPS | PD; richiede envelope contestuale |
| cm-v-sit | V-sit | SKILL / core-control | VERY_HIGH / VERY_HIGH / VERY_HIGH / VERY_HIGH | dip_bars / BODYWEIGHT | EVIDENCE_UNRESOLVED:SECONDS | isometrica; prescrizione EVIDENCE_UNRESOLVED; skill specifica vsit_hold |
| cm-rematore-t-bar | Rematore T-bar | PULL / horizontal-pull | PRESCRIPTION_DEPENDENT / MODERATE / MODERATE / LOW | machine / EXTERNAL_LOAD | KNOWN:REPS | PD; richiede envelope contestuale |
| cm-chest-press | Chest press | PUSH / horizontal-push | PRESCRIPTION_DEPENDENT / LOW / LOW / VERY_LOW | machine / EXTERNAL_LOAD | KNOWN:REPS | PD; richiede envelope contestuale |
| cm-band-assisted-muscle-up | Band assisted muscle-up | SKILL / horizontal-push | PRESCRIPTION_DEPENDENT / HIGH / MODERATE / MODERATE | pull_up_bar/resistance_band / ASSISTED | KNOWN:REPS | PD; richiede envelope contestuale; assistenza non quantificata; skill specifica muscleup_band |
| cm-back-lever-tuck | Back lever tuck | SKILL / vertical-pull | HIGH / HIGH / HIGH / MODERATE | pull_up_bar / BODYWEIGHT | EVIDENCE_UNRESOLVED:SECONDS | isometrica; prescrizione EVIDENCE_UNRESOLVED; skill specifica backlever_tuck |
| cm-human-flag-tuck | Human flag tuck | SKILL / core-control | HIGH / HIGH / MODERATE / HIGH | pull_up_bar / BODYWEIGHT | EVIDENCE_UNRESOLVED:SECONDS | isometrica; prescrizione EVIDENCE_UNRESOLVED; skill specifica flag_tuck |
| cm-glute-kickback-machine | Glute kickback machine | LEGS / knee-dominant | PRESCRIPTION_DEPENDENT / LOW / LOW / VERY_LOW | machine / EXTERNAL_LOAD | KNOWN:REPS | PD; richiede envelope contestuale |
| cm-pushdown-cavo | Pushdown cavo | PUSH / horizontal-push | PRESCRIPTION_DEPENDENT / EVIDENCE_UNRESOLVED / EVIDENCE_UNRESOLVED / EVIDENCE_UNRESOLVED | cable / EXTERNAL_LOAD | KNOWN:REPS | PD; richiede envelope contestuale; dimensione terminale: fail-closed |
| cm-l-sit-tuck | L-sit tuck | SKILL / core-control | HIGH / HIGH / MODERATE / MODERATE | dip_bars / BODYWEIGHT | EVIDENCE_UNRESOLVED:SECONDS | isometrica; prescrizione EVIDENCE_UNRESOLVED; skill specifica lsit_tuck |
| cm-high-row-macchina | High row macchina | PULL / horizontal-pull | PRESCRIPTION_DEPENDENT / LOW / LOW / VERY_LOW | machine / EXTERNAL_LOAD | KNOWN:REPS | PD; richiede envelope contestuale |
| ex-4-point-burpee | 4-Point Burpee | CARDIO / conditioning | MODERATE / MODERATE / MODERATE / MODERATE | UNKNOWN / BODYWEIGHT | KNOWN:REPS | attrezzatura UNKNOWN |
| ex-advanced-tuck-back-lever | Advanced Tuck Back Lever | SKILL / UNKNOWN | HIGH / HIGH / MODERATE / MODERATE | pull_up_bar / BODYWEIGHT | EVIDENCE_UNRESOLVED:SECONDS | skill specifica backlever_advanced_tuck; pattern UNKNOWN |

## Correzioni candidate, senza mutare il JSON

1. Correggere la documentazione schema 3.7 per i cinque livelli e separare i token di stato.
2. Definire la semantica informativa di capability.state aggregato, che oggi è EVIDENCE_UNRESOLVED per tutti i record pur con requisiti individuali KNOWN/PD/terminal.
3. Mantenere i PD fail-closed finché prescrizione tipizzata e capacity envelope corrispondente non sono presenti.
4. Isometriche/skill hold con prescrizione EVIDENCE_UNRESOLVED restano non prescrivibili. Una misura primaria SECONDS non è evidenza del target di durata.
5. Il JSON contiene movementPatterns ma non i livelli/threshold dei nove pattern atleta. Le heuristics e i pattern UNKNOWN non permettono derivazioni. Per usarli come gate serve mapping esterno versionato con evidenza.
6. Revisione tassonomica manuale: cm-pistol-squat-assistito ha pattern horizontal-push e unresolved; ex-advanced-tuck-planche-pushups ha mobilità EVIDENCE_UNRESOLVED; ex-45-degree-forward-lean-dips ha due requisiti EVIDENCE_UNRESOLVED. Il campione segnala, non autorizza correzioni per nome.
7. Il candidato non dichiara quale provenienza atleta basta per ogni requisito: policy di assessment deve stare in un registro tecnico esterno, versionato.

## Riferimenti

Decisione G1; engine-schema-candidate-v3-7.json; step8a-pd-semantic-contract.json; final-canonical-candidate-step6-validation.json; roadmap Step 9; step10b2-isolated-ranking.md; ExerciseV3Domain.kt; V3AssetLoader.kt; V3SelectionEngine.kt; V3NativeAthleteCapabilityContract.kt; V3NativeAthleteProfileAdapter.kt; V3NativeAthleteContext.kt; V3PersonalizedRanking.kt.
