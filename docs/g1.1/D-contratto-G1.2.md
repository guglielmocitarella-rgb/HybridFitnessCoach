# G1.1-D — Contratto di implementazione G1.2

**Stato:** specifica tecnica G1.2 consolidata dopo audit del candidato congelato. G1.1 non modifica l’app.

## Contratti esistenti e delta minimo

Letti V3NativeAthleteCapabilityContract, V3NativeAthleteContext, V3NativeAthleteProfileAdapter, V3SelectionEngine, V3PersonalizedRanking e schema 3.7.

Già presenti: enum CapabilityLevel con cinque valori ordinati; quattro requisiti; V3AssessedValue transient con DIRECT_ASSESSMENT e VERIFIED_DERIVATION; assessment movimento legacy Int 1..3 separato; adapter senza conversione 1..3→5; eligibility fail-closed prima del ranking. Non esistono persistenza assessment V3, SELF_REPORTED o policy di provenienza.

Delta proposto:
1. Aggiungere repository persistito V3-native degli assessment.
2. Aggiungere provenienza SELF_REPORTED; UNKNOWN resta assenza di valore.
3. Nuove competenze motorie usano V3MovementLevel a cinque valori, senza leggere o convertire i vecchi Int 1..3.
4. Adapter costruisce snapshot transient dal repository, senza duplicare l’assessment in Context/Ranking o nel JSON esercizi.
5. Eligibility riceve policy di evidenza e produce motivi; ranking continua a operare soltanto sul pool eleggibile.

## Modello dati Kotlin proposto

| Campo | Tipo concettuale | Regola |
|---|---|---|
| assessmentId | ID stabile | Ogni revisione è tracciabile. |
| dimension | V3AssessmentDimension | Forza, tecnica, mobilità, equilibrio/coordinazione, nove pattern; skill solo con target specifico. |
| targetId | Canonical skill/node ID nullable | Obbligatorio per skill; nessuna skill globale che autorizzi esercizi. |
| level | V3CapabilityLevel nullable | Cinque livelli oppure nullo per unknown/invalidation. |
| provenance | SELF_REPORTED / DIRECT_ASSESSMENT / VERIFIED_DERIVATION / UNKNOWN | Provenienza effettiva, mai riclassificata. |
| assessedAt | Instant nullable | Required per ogni assessment attivo; unknown non ha data. |
| protocolId | ID/versione protocollo nullable | Richiesto per diretto e per prova auto-guidata versionata. |
| rubricVersion | String | Richiesto per ogni assessment con livello. |
| verificationState | DECLARED / OBSERVED_VALID / DERIVED_VALID / INVALIDATED / UNKNOWN | Stato categoriale, non punteggio di confidenza. |
| observation | record strutturato nullable | canonical exercise ID, variante, attrezzatura/assistenza, outcome e annotazione facoltativa. |
| supersedesAssessmentId | ID nullable | Traccia aggiornamenti; il record vecchio resta storico. |

Regole:
- UNKNOWN non è livello basso e non entra in SemanticValue KNOWN.
- SELF_REPORTED richiede risposta guidata, livello, data e rubric version; non dichiara osservazione.
- DIRECT_ASSESSMENT richiede protocollo/versione, data, outcome e prova osservata. L’osservatore nominativo è opzionale e soggetto alla policy privacy.
- VERIFIED_DERIVATION richiede regola versionata, input conservati e risultato riproducibile; non dedurre da esperienza, RPE o esercizi diversi.
- Nessun valore numerico di confidenza.
- Nota libera non sostituisce proprietà strutturate e non alimenta automaticamente ranking.

## Persistenza, aggiornamento e invalidazione

Un solo repository/AppStore per i record assessment V3, con revisioni append-only o record attivo più eventi di audit. Lo snapshot transiente già esistente è ricostruito; non si salva una seconda copia nel ranking. Nessuna migration della scala legacy 1..3.

Nuova prova o modifica manuale crea revisione e supersedes. Modifica manuale successiva a osservazione crea nuova SELF_REPORTED e non riscrive il DIRECT_ASSESSMENT. Protocollo incompatibile può invalidare solo secondo regola versionata. Cambio rubric version non invalida automaticamente: richiede decisione di compatibilità. Data senza una validità temporale approvata non scade arbitrariamente. Contraddizione dello storico mostra invito motivato alla rivalutazione, ma non cambia né cancella automaticamente il livello.

## Bridge ed eligibility

Il bridge legge assessment attivi, materializza quattro valori trasversali e competenze motorie separate, conserva provenienza e crea snapshot immutabile con revisions profile/assessment. Non ricarica il JSON e non usa vecchi punteggi come mapping.

Eligibility confronta requisiti noti soltanto quando il dominio e l’ordine sono verificati; policy d’evidenza determina se la provenienza basta. UNKNOWN requirement, terminal, PD, mismatch attrezzatura e prescrizione mantengono reason codes distinti. PD strength continua con V3PdStrengthContext ed envelope specifico, non CapabilityLevel.

Verificato sul branch: enum Kotlin e loader accettano i cinque token nello stesso ordine; il selector confronta ordinali come requisiti minimi e Step 8A conferma la semantica. Lo schema 3.7 è incompleto; non ordinare stringhe né convertire assessment legacy.

## UI proposta

- Onboarding raccoglie poche dimensioni pertinenti all’obiettivo/luogo; una domanda alla volta e “Non so” sempre disponibile.
- Ogni livello ha descrittore e variante concreta. Separare “dichiarato”, “osservato”, “derivato”.
- Prova facoltativa mostra set-up, variante, cue e criteri del protocollo; non inventa numeri.
- Skill richiede famiglia/nodo e assistenza specifici.
- Nessun punteggio globale, percentuale, badge di clearance o completion obbligatoria.
- Conferma: “Stima dichiarata in base agli esempi; non è una misurazione oggettiva”.
- L’utente può aggiornare o lasciare unknown. Mostrare sempre ragione di esclusione e assessment minimo utile.

## Test Kotlin richiesti

**Modello/persistenza:** round-trip per tutte le provenienze; unknown senza default; revision/supersedes; invalidazione protocollo; nessuna conversione 1..3→1..5; separazione snapshot e storage.

**Bridge:** quattro assi indipendenti; provenance lossless; dimensione movimento specifica; target ID per skill; unknown resta unknown; revision stale respinta.

**Eligibility:** ogni combinazione requirement known, insufficiente, sconosciuto, N/A, PD, terminal, policy permessa/non permessa, assessment diretto valido/mancante/invalidato, derivazione valida/invalidata, attrezzatura assente, prescrizione non valida e skill relation non verificata; reason codes conservati anche multipli.

**Motore:** score non recupera rifiuto; composer non rilassa eleggibilità; envelope/unità corrette; zero pool legitimo vs invalido; stessa policy per Plan, Custom, Skill, Library e Adaptive extra.

**Storico/UI:** contraddizione solo suggerisce prova; nessuna promozione; skip/Non so; livello alto resta dichiarato; round-trip/recreation del nuovo store nella fase prevista.

## Criteri di accettazione G1.2

1. Audit C chiuso su frozen SHA e rubric version approvata.
2. Modello persistito V3 nuovo, senza conversione di legacy 1..3.
3. Provenienza e protocollo preservati end-to-end.
4. UI progressiva senza autovalutazione presentata come misura oggettiva.
5. Policy definita per ogni requisito in uso; mapping mancante diagnostico e fail-closed.
6. Eligibilità precede ranking e non dipende dall’origine.
7. Nessuna mutazione JSON, switch di produzione o modifica Runner.
8. Test localmente sufficienti; pubblicazione consolidata in una sola iterazione per preservare Actions.
## Contratto esistente verificato e delta preciso

CapabilityLevel è VERY_LOW, LOW, MODERATE, HIGH, VERY_HIGH. CapabilityRequirements ha le quattro dimensioni come SemanticValue<CapabilityLevel> e uno stato aggregato. V3AssetLoader converte i cinque token noti in KNOWN; gli altri sono SemanticState. V3SelectionEngine ammette EVIDENCE_UNRESOLVED aggregato e valuta i campi separatamente: NOT_APPLICABLE salta una dimensione, EVIDENCE_UNRESOLVED dimensionale dà TERMINAL_CANONICAL_STATE, KNOWN richiede assessment atleta noto e confronto ordinale. V3SelectionRejection è elencato in B.

V3NativeAthleteCapabilityContract e V3AssessedValue sono transienti; valori noti attuali ammettono solo DIRECT_ASSESSMENT e VERIFIED_DERIVATION. V3MovementAssessments conserva Int 1..3; l’adapter lo legge da AthleteProfile. Le quattro capacità trasversali restano UNKNOWN senza input. SkillCatalog derivato dalla cronologia è regola app, non prova. Una relazione skill con confidence CERTAIN può risultare UNSPECIFIED nel campo evidenceState Kotlin: confidence non significa VERIFIED.

### Modello persistito proposto

Un unico repository di revisioni assessment V3. Riusare SemanticValue e bridge transienti; non duplicare il valore in profile, context, ranking e JSON esercizi.

~~~kotlin
enum class V3AssessmentProvenance { SELF_REPORTED, DIRECT_ASSESSMENT, VERIFIED_DERIVATION, UNKNOWN }
enum class V3AssessmentVerification { DECLARED, OBSERVED_VALID, DERIVED_VALID, INVALIDATED, UNKNOWN }
data class V3AssessmentRevision(
  val id: String, val target: V3AssessmentTarget, val level: V3Level?,
  val provenance: V3AssessmentProvenance, val verification: V3AssessmentVerification,
  val assessedAt: Instant?, val protocolId: String?, val protocolVersion: String?,
  val rubricVersion: String?, val observation: V3ObservedOutcome?,
  val supersedesId: String?, val invalidationReason: String?
)
~~~

V3AssessmentTarget è una dimensione trasversale, uno dei nove pattern o una skill specifica; per skill serve node ID canonico, family soltanto per intento esplicito. V3Level usa i cinque valori separati dal legacy 1–3. UNKNOWN non ha livello. SELF_REPORTED richiede rubric version; DIRECT_ASSESSMENT protocollo ed esito; VERIFIED_DERIVATION regola versionata e input riproducibili.

V3ObservedOutcome registra canonical exercise/skill ID, variante, attrezzatura/assistenza e criteri osservati. Nessun punteggio numerico di confidenza: verification è categoriale e derivata da origine e protocollo. Capacity envelope PD è separato e tipizzato con esercizio, variante/dimensioni, carico o assistenza con unità, limite osservato, provenienza e protocollo.

Il nuovo archivio resta distinto da V3MovementAssessments<Int>; i campi legacy non si migrano o reinterpretano. Il bridge produce lo snapshot transiente. Il formato archivio resta scelta d’implementazione se revisioni, round-trip e restore sono testati.

## Reason codes: implementati e proposti

| Codici | Stato |
|---|---|
| CAPABILITY_UNRESOLVED / INSUFFICIENT, ATHLETE_CAPABILITY_UNKNOWN | Esistenti |
| TERMINAL_CANONICAL_STATE, CAPACITY_ENVELOPE_MISMATCH, PD_STRENGTH_CONTEXT_INSUFFICIENT / INVALID | Esistenti |
| EQUIPMENT_UNRESOLVED / UNAVAILABLE, PRESCRIPTION_UNRESOLVED, SKILL_RELATION_UNRESOLVED / MISMATCH | Esistenti |
| ASSESSMENT_REQUIRED, ASSESSMENT_POLICY_UNRESOLVED, MOVEMENT_REQUIREMENT_UNRESOLVED | Nuovi codici proposti |
| PRESCRIPTION_INCOMPATIBLE / INVALID | Diagnostica proposta; non rinominare PRESCRIPTION_UNRESOLVED senza delta esplicito |

## Aggiornamento, invalidazione e motore

Nuova prova crea una revisione collegata alla precedente. Il bridge legge l’ultima revisione attiva e propaga provenienza/protocollo. Rubric version nuova non invalida automaticamente; serve una regola di compatibilità versionata. Nessuna scadenza arbitraria. Lo storico suggerisce rivalutazione ma non promuove, retrocede o invalida. Il ranking include la assessmentRevision nello snapshot e scarta risultati obsoleti.

Eligibility resta prima del ranking. Composer 10C riceve solo esercizi eleggibili e non rilassa il gate; 10D materializza prescrizione/PD con unità ed envelope corretti. Diagnostica distingue assessment ignoto, requisito unresolved, stato terminale, ambiente, policy, prescrizione e zero-pool.

## Accettazione G1.2 consolidata

Round-trip di provenienze, UNKNOWN, revisioni e invalidazioni; cinque token e confronto con uguaglianza; quattro assi indipendenti; nessuna media o conversione legacy; SELF_REPORTED distinto da DIRECT; PD con unità/dimensioni corrispondenti; assessment skill per ID specifico; reason codes multipli preservati; ranking/composer non recupera esclusi; zero-pool esplicito; storico solo suggerimento; snapshot versionato; test locali per persistence, bridge, eligibility, 10C/10D e UI.

Riferimenti: [decisione G1](../g1-v3-athlete-assessment-decision.md), [roadmap Step 9](../../exercise-db-canonical/step9-v3-integration-roadmap.md), [10B.2 isolato](../step10b2-isolated-ranking.md), [audit C](C-audit-database.md).
