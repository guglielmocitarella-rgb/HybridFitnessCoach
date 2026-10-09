# G1.1-B — Politica di eleggibilità e progressione della verifica

**Stato:** proposta tecnica; non è autorizzazione medica né policy di rischio clinico.  
**Decisione vincolante:** stati ignoti restano espliciti; requisito e capacità atleta sono distinti; l’eleggibilità precede ranking; stati di prescrizione non si scalarizzano.

## Pipeline

Capacità atleta → requisito canonico → policy di provenienza/prova → ambiente e attrezzatura → prescrizione contestuale → eleggibilità spiegata → ranking del solo pool eleggibile → composer → prescrizione finale.

Il ranking riceve solo esercizi eleggibili. Score e bonus non possono recuperare un rifiutato. Pool vuoto è un risultato esplicito.

## Matrice requisito × assessment × contesto

| Requisito | Assessment atleta | Contesto | Esito |
|---|---|---|---|
| KNOWN | SELF_REPORTED guidato, livello sufficiente e policy consente self-report | Altri gate superati | Eleggibile per questa dimensione; etichetta “dichiarato”. |
| KNOWN | DIRECT_ASSESSMENT valido e sufficiente | Prova corrisponde a dimensione/variante, altri gate superati | Eleggibile; conserva protocollo, data e osservazione. |
| KNOWN | VERIFIED_DERIVATION sufficiente | Regola riproducibile e applicabile | Eleggibile; conserva regola versionata e fonti della derivazione. |
| KNOWN | Livello noto inferiore al requisito | Qualunque | Escludi CAPABILITY_INSUFFICIENT. Nessuna compensazione o riduzione del requisito. |
| KNOWN | UNKNOWN, self-report non guidato o non pertinente | Qualunque | ASSESSMENT_REQUIRED / ATHLETE_CAPABILITY_UNKNOWN; non presumere sufficienza. |
| KNOWN | Provenienza non accettata dalla policy per quel requisito | Qualunque | Richiedi prova o derivazione ammessa; il livello da solo non supera il gate. |
| Requisito UNKNOWN | Qualunque assessment, anche massimo e verificato | Qualunque | EXERCISE_REQUIREMENT_UNRESOLVED; l’atleta non può sanare un dato DB ignoto. |
| NOT_APPLICABLE | Qualunque | Altri gate superati | Nessuna richiesta per quella dimensione soltanto; non equivale a zero in altre. |
| PRESCRIPTION_DEPENDENT strength | Livello trasversale qualunque | Manca envelope specifico o non corrisponde esercizio/dimensioni | Context unresolved / ATHLETE_CAPABILITY_UNKNOWN / CAPACITY_ENVELOPE_MISMATCH. |
| PRESCRIPTION_DEPENDENT strength | Capacity envelope validato per canonical ID e dimensioni di lavoro | EXTERNAL_LOAD confronta stesso carico/unità; ASSISTED confronta assistenza minima richiesta; uguaglianza come contratto 8A | Esito contestuale secondo contract 8A; mai scrivere valore risolto nel JSON. |
| PRESCRIPTION_DEPENDENT tecnico/mobilità/equilibrio | Nessun contesto tipizzato | Il frozen contract dichiara zero casi attuali per tali dimensioni | Se futuro requisito: unresolved finché manca contratto specifico. |
| EVIDENCE_UNRESOLVED / terminale | Qualunque valore atleta | Qualunque | TERMINAL_CANONICAL_STATE; solo revisione canonica può risolvere. |
| Policy di assessment assente/ambigua | Qualunque | Qualunque | ASSESSMENT_POLICY_UNRESOLVED; non inventare una policy dalla sola etichetta del livello. |
| Attrezzatura REQUIRED indisponibile | Sufficiente | Nessuna alternativa compatibile disponibile | EQUIPMENT_UNAVAILABLE. |
| Attrezzatura OPTIONAL / ONE_OF | Sufficiente | Alternativa compatibile disponibile | Eleggibile se altri gate passano; mostrare alternativa usata. |
| ASSISTED_BY / EXTERNAL_LOAD | Sufficiente | Supporto/carico prescritto o attrezzo non disponibile | Esclusione di ambiente/prescrizione; non rimuovere il requisito. |
| Prescrizione incompatibile | Sufficiente | Unità, dimensione o valore non valido / obbligatorio assente | PRESCRIPTION_INVALID o PRESCRIPTION_UNRESOLVED; niente fallback implicito. |
| Pool positivo | — | Tutti i gate passano | VALID_POOL; ranking opera soltanto su questi record. |
| Pool vuoto per assessment mancante o insufficiente | — | Contratto dati valido | LEGITIMATE_ZERO_POOL con ragioni e prossimo assessment utile. |
| Pool vuoto per asset, intent o contratto non valido | — | Errore di sistema o input | INVALID_ZERO_POOL, diagnostica tecnica; non presentarlo come programma valido. |

## Provenienza, accettazione e verifica

Self-report, prova diretta e derivazione verificata sono prove diverse; non sono ordinabili come punteggio di confidenza. La verifica è uno stato categoriale ricavato da provenienza e protocollo: DECLARED, OBSERVED_VALID, DERIVED_VALID, INVALIDATED o UNKNOWN.

Il candidato 3.7 non contiene una policy d’assessment per ogni requisito. Proposta: un AssessmentPolicyRegistry versionato fuori dal JSON, chiave canonical ID + dimensione + stato/valore requisito. Campi minimi:

- allowedProvenance: SELF_REPORTED, DIRECT_ASSESSMENT, VERIFIED_DERIVATION;
- protocolId, se necessario; null solo quando è sufficiente rubric-guided self-report;
- motivazione e fonte della decisione;
- regola di validità/invalidazione, se documentata;
- policyVersion.

Nessuna regola di rischio numerica né penalità automatica di un livello. Policy mancante = ASSESSMENT_POLICY_UNRESOLVED, non ammissione favorevole. L’audit record-level deve selezionare requisiti per cui l’ancora è comprensibile e verificabile; skill non note, tecnica complessa e capacità di carico/assistenza contestuale richiedono prova specifica se il protocollo lo stabilisce. Questo è una **proposta da validare**, non fatto presente nel DB.

Non fissare una soglia universale “HIGH richiede test” senza evidenza. Prima di G1.2, approvare il registry e riempire i casi identificati dall’audit. L’atleta può comunque salvare stima dichiarata e accedere a progressioni/regressioni eleggibili.

## Stati, motivi e next step

Separare almeno CAPABILITY_INSUFFICIENT, ATHLETE_CAPABILITY_UNKNOWN, ASSESSMENT_REQUIRED, ASSESSMENT_POLICY_UNRESOLVED, EXERCISE_REQUIREMENT_UNRESOLVED, TERMINAL_CANONICAL_STATE, PD_STRENGTH_CONTEXT_INSUFFICIENT/INVALID, EQUIPMENT_UNAVAILABLE, PRESCRIPTION_UNRESOLVED/INVALID, INTENT_MISMATCH e pool status.

Ogni rifiuto conserva canonical ID, dimensione, stato requisito, livello/provenienza atleta, policy, gate fallito e azione successiva. Ragioni multiple non si sovrascrivono. Suggerire solo l’assessment più piccolo pertinente, non un questionario completo. Regressione/sostituzione solo da relation canonica verificata.

## Domini e motore

Lo snapshot schema 3.7 dichiara requisiti noti LOW/MODERATE/HIGH più stati. La decisione atleta usa cinque livelli. Un confronto ordinale è ammesso solo se il dominio effettivo Kotlin condivide i token/ordine e le semantiche sono entrambe soglia minima. Verificare il contratto prima di introdurlo; nessuna conversione da vecchia scala 1–3.

PD strength richiede ID esercizio, resistance source, dimensioni di lavoro, carico/assistenza tipizzati e envelope atleta corrispondente. Load e assistenza non sono confrontabili tra loro. NOT_APPLICABLE non rimuove gli altri gate.

- Eligibility: fail-closed per ogni dimensione/stato.
- Ranking 10B: soltanto pool eleggibile; punteggio di compatibilità non autorizza esercizi.
- Progressioni 10C: relazioni verificate; nessuna promozione automatica.
- Skill 10E: per skill ID/nodo e relazione; nessun livello globale.
- Composer 10C: può esplicitare compromessi di varietà/durata, mai rilassare eligibility.
- Prescriber 10D: unità/carico/assistenza e envelope PD; non modifica requisiti per evitare zero-pool.
- G8: zero-pool legittimo suggerisce assessment/filtri alternativi; zero-pool non valido apre diagnosi tecnica.
- Storico: carichi, reps, RPE o successi in esercizi diversi possono suggerire verifica, non promuovono né invalidano da soli. Contraddizione specifica mostra invito alla prova e mantiene valore precedente con stato non aggiornato.
## Policy G1.1 dopo audit: quando basta l’autovalutazione

1. Per un requisito dimensionale KNOWN fra VERY_LOW e VERY_HIGH, l’autovalutazione guidata è sufficiente a tutti i cinque livelli se l’atleta comprende gli esempi della stessa dimensione e conferma di eseguire una variante con i criteri osservabili della rubrica. Il livello resta SELF_REPORTED, non misurato. Nessuna penalità per livelli alti e nessuna regola universale per cui HIGH richieda un test.
2. “Non so”, mancata comprensione, assenza di una variante confrontabile o mancato controllo dei criteri lascia UNKNOWN o il livello inferiore che l’atleta riconosce davvero. Non completare per inferenza né modificare silenziosamente il dato esistente.
3. DIRECT_ASSESSMENT è sufficiente se protocollo e rubrica corrispondono a dimensione, variante, attrezzatura e assistenza, e gli esiti dei criteri sono registrati. VERIFIED_DERIVATION richiede regola versionata, input riproducibili e stesso costrutto.
4. Per PRESCRIPTION_DEPENDENT strength, l’autovalutazione trasversale, anche VERY_HIGH, non basta: è necessario l’envelope specifico per canonical ID, dimensioni di lavoro, carico/assistenza e unità previsto da Step 8A. Direct o derivazione verificata; mai scalarizzare.
5. La selezione intenzionale di una skill richiede assessment per il nodo/skill specifico e relazione canonica utilizzabile. Competenza generica di spinta, tirata o core non la sostituisce. In G1.2 il nuovo assessment del nodo è diretto; il calcolo SkillCatalog derivato dalla cronologia è un segnale applicativo, non verifica o promozione.
6. I nove pattern motori influenzano eleggibilità solo con un mapping esterno versionato ed evidenziato. movementPatterns, soprattutto UNKNOWN/heuristic, non prova da solo una competenza atleta.

Un’eccezione DIRECT_REQUIRED è ammessa solo se la policy specifica un target, protocollo e motivazione; mai per la sola etichetta HIGH. Policy mancante per eccezione = ASSESSMENT_POLICY_UNRESOLVED.

## Registro esterno e comportamento sugli stati

G1.2 aggiunge un AssessmentEligibilityPolicy versionato senza cambiare il JSON. Identificatore minimo: schema/canonical ID quando specifico, dimensione o target skill, valore/stato requisito, provenienza consentita, protocollo/versione, fonte/motivazione e regole di invalidazione. Default: requisito KNOWN ordinale = self-report guidato ammesso; PD strength = envelope tipizzato; EVIDENCE_UNRESOLVED o UNKNOWN = fail-closed; NOT_APPLICABLE = nessuna verifica per quella dimensione. Le eccezioni richiedono evidenza.

Nel candidato non esistono valori UNKNOWN o NOT_APPLICABLE per le quattro dimensioni, ma il selector deve gestire entrambi; equipment UNKNOWN compare in 90 record e resta un gate autonomo. Nessuna dimensione motoria atleta né soglia dei nove pattern è codificata nel JSON. Se una richiesta di pattern/skill non ha mapping attendibile, G1.2 produce il nuovo reason code MOVEMENT_REQUIREMENT_UNRESOLVED.

## Codici reali e proposti

V3SelectionRejection attuale include CAPABILITY_UNRESOLVED, CAPABILITY_INSUFFICIENT, ATHLETE_CAPABILITY_UNKNOWN, TERMINAL_CANONICAL_STATE, CAPACITY_ENVELOPE_MISMATCH, PD_STRENGTH_CONTEXT_INSUFFICIENT, PD_STRENGTH_CONTEXT_INVALID, EQUIPMENT_UNRESOLVED, EQUIPMENT_UNAVAILABLE, PRESCRIPTION_UNRESOLVED, SKILL_RELATION_UNRESOLVED e SKILL_MISMATCH. Il bridge non li rinomina.

ASSESSMENT_REQUIRED, ASSESSMENT_POLICY_UNRESOLVED e MOVEMENT_REQUIREMENT_UNRESOLVED sono nuovi codici proposti, non enum già implementati. Il selector attuale non valuta provenance policy; introdurla è delta G1.2.

Riferimento vincolante: [decisione di prodotto G1](../g1-v3-athlete-assessment-decision.md).
