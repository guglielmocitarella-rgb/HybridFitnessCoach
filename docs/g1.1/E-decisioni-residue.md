# G1.1-E — Decisioni residue e gate G1.2

## Decisioni di prodotto approvate

Come da [decisione G1](../g1-v3-athlete-assessment-decision.md): cinque livelli; autovalutazione guidata con “Non so”; verifica progressiva; quattro capacità e nove competenze motorie separate; skill specifica; nessuna conversione legacy 1–3, punteggio globale o promozione automatica; eligibility prima del ranking; V2 resta produttivo. Queste decisioni non si riaprono.

## Specifica consolidata

- **A — Rubrica:** quattro capacità e nove competenze, cinque livelli, prove, criteri osservabili, limiti ed evidenze; validazione che ogni livello esiste nei dati.
- **B — Eleggibilità:** self-report guidato sufficiente per requisiti KNOWN ordinali a ogni livello se l’atleta comprende l’ancora; PD richiede envelope; skill richiede assessment target-specifico; unknown/terminal fail-closed. Nessun test universale per HIGH.
- **C — Audit:** hash byte originali, 1.087 record e ID unici, distribuzioni e campione record-level con copertura dei 20 incroci dimensione-livello.
- **D — G1.2:** repository a revisioni, provenance/protocollo/versione, bridge senza conversioni, policy esterna, reason codes esistenti e proposti distinti, test e acceptance.

## Evidenze verificate

- Allegato: SHA-256 0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a, 19.054.419 byte, 1.087 record e ID unici.
- Distribuzione: 441 strength PD; 48 campi unresolved su 19 record.
- Kotlin e loader accettano i cinque livelli nello stesso ordine; selector usa confronto ordinale minimo, N/A per una dimensione e terminal per campo; ranking è downstream.
- Schema 3.7 non documenta VERY_LOW e VERY_HIGH.
- Step 8A vieta scalarizzazione PD e richiede confronto typed; roadmap mantiene V2 e rinvia cutover a 10H.

## Proposte da implementare, non codice già presente

1. Assessment policy versionata esterna al JSON: KNOWN ordinali self-report guidato; direct/verified per envelope PD; assessment specifico per skill; mapping motorio esplicito per pattern gate.
2. Persistenza V3 a revisioni, provenance UNKNOWN esplicita, bridge e invalidazione tracciabile senza conversione legacy.
3. Correzione documentale schema 3.7 e nuovi reason codes per policy/mapping.
4. Includere assessment revision nello snapshot ranking; scartare risultati obsoleti se cambia la valutazione.

## Ipotesi non bloccanti

Comprensione delle ancore, accordo fra valutatori, usabilità UI, validità/ripetibilità dei protocolli e compatibilità fra rubric version. Sono criteri di qualità da verificare nei protocolli pilota, non decisioni prodotto mancanti. Nessuna soglia di rischio, clearance clinica o scadenza arbitraria.

## Pubblicazione

G1.1 è COMPLETE senza decisioni bloccanti. Pubblicare A–E in un solo commit documentale su feature/exercise-db-v3. HEAD remoto iniziale verificato: 9e929edcc60efbd73b3f58745727de79b4430756. Prima dell’update ref ricontrollare branch e usare SHA atteso; se è avanzato, preservare i commit concorrenti. Nessuna modifica a Kotlin, JSON, asset, Runner o workflow.

## Prossimo passo

G1.2: implementare persistenza/revisioni e UI guidata, bridge, assessment/movement policy, eligibility reasons e test locali. Cutover V3 resta ai gate successivi della roadmap.
