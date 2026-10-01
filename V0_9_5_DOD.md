# Hybrid Fitness Coach — v0.9.5 Roadmap / Definition of Done

## Missione della release

La v0.9.5 parte dalla baseline v0.9.4 congelata. La v0.9.4 resta immutata come riferimento repository-level verificato; la v0.9.5 porta avanti le clausole DEVICE_PENDING e concentra lo sviluppo sul passaggio da motore funzionale a prodotto comprensibile, coerente e verificato su dispositivo.

Baseline runtime ereditata: `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07` (`checkpoint/v0.9.4-f01-f11-green`).
CI ereditata: Exercise DB v2 #292 SUCCESS; Android APK #845 SUCCESS.

## Principi vincolanti

1. Non riaprire F01-F11 già repository-verified salvo regressione dimostrata o requisito v0.9.5 esplicito.
2. Nessuna retrocompatibilità con schemi/modelli legacy è richiesta.
3. Adaptive Coach deve essere il motore dell'esperienza, non un concetto che l'utente deve comprendere per usare l'app.
4. L'interfaccia deve privilegiare: stato attuale -> prossima azione -> motivazione -> dettaglio opzionale.
5. Ogni nuovo comportamento deterministico deve avere test automatici; interazioni/visuali restano DEVICE_PENDING fino a test fisico.
6. Evitare nuove grandi feature non necessarie prima di chiudere UX, device acceptance e release gate.

## P01 — Audit UX end-to-end e navigazione

Obiettivo: audit completo Home -> Piano/Calendario -> generazione -> Runner -> salvataggio -> Progressi -> Profilo.

- ridurre concetti tecnici esposti;
- definire gerarchia coerente delle azioni;
- eliminare/accorpare controlli ridondanti;
- iconografia coerente;
- navigazione principale semplice e prevedibile;
- verificare accessibilità, scroll, back navigation, conferme distruttive e stato vuoto/errori.

## P02 — Home come centro operativo

La Home deve rispondere immediatamente a: come sto, cosa faccio oggi, perché, cosa viene dopo.

- readiness/recovery leggibile;
- una CTA primaria per l'allenamento consigliato/odierno;
- durata, focus e sintesi sessione;
- motivazione Adaptive Coach espandibile, non dominante;
- prossime sessioni in forma compatta;
- piano-at-a-glance senza duplicare il calendario completo.

## P03 — Piano multisettimanale e calendario

- viste Settimana e Calendario mensile;
- stati pianificato/completato/parziale/saltato/riprogrammato immediatamente distinguibili;
- azioni secondarie raccolte in menu contestuale anziché molte CTA concorrenti;
- riprogrammazione coerente, cascata dove prevista e ripristino data originale;
- modifica obiettivo/frequenza/giorni/skill con review e conferma prima della rigenerazione futura;
- storico già eseguito immutabile.

## P04 — Generazione workout semplificata

L'utente specifica intento, non prescrizione manuale.

- luogo di allenamento;
- durata;
- focus/parti del corpo/skill;
- serie, reps, recuperi, carichi e timing generati automaticamente;
- preview sempre dall'esercizio 1;
- conferma chiara prima dell'avvio quando appropriato;
- locked/pinned comprensibili e coerenti.

## P05 — Workout Runner UX

- esercizio corrente dominante;
- indicatore serie corretto fin dalla prima serie;
- carico/reps/tempo/RPE immediatamente leggibili;
- RPE/fatica default 5 e valido se non modificato;
- navigazione precedente/successivo e salto esercizio;
- conferma per termine/salvataggio quando necessaria;
- recuperi e timer persistenti coerenti.

### P05.1 — Countdown preparazione esercizi a tempo

Per ogni esercizio prescritto a tempo:
- gli ultimi 5 secondi del recupero diventano countdown di preparazione 5-4-3-2-1;
- il countdown NON aggiunge 5 secondi al recupero;
- a zero parte automaticamente il timer a rovescio dell'esercizio;
- feedback visivo evidente; feedback sonoro se audio/feedback sonoro è abilitato;
- se il recupero residuo è <5 s, il countdown parte dal tempo effettivamente disponibile;
- comportamento implementato nel Runner come regola generale, non duplicato nei singoli esercizi;
- test deterministici su transizione recovery -> preparation -> timed exercise, pause/restore/background e boundary temporali.

## P06 — Capability Profile multidimensionale esteso

Il Capability Profile non deve limitarsi principalmente a Push/Pull e non deve collassare in un singolo livello globale.

Dimensioni minime da progettare/validare:
- spinta parte superiore;
- tirata parte superiore;
- spinta verticale;
- tirata verticale;
- gambe/squat;
- catena posteriore/hip hinge;
- core;
- capacità isometriche dove rilevanti;
- skill/calisthenics.

Requisiti:
- ampliare gli esercizi di assessment per coprire le diverse parti del corpo/capacità;
- scegliere test con regressioni accessibili e progressioni più difficili;
- documentare provenienza/ragione delle soglie: SOURCE_BASED, APP_RULE, FALLBACK o UNVERIFIED;
- non inventare soglie arbitrarie;
- generator/prescription usa la dimensione pertinente all'esercizio;
- capacità elevata in un distretto non deve innalzare automaticamente gli altri;
- stato unassessed conservativo;
- assessment iniziale può essere breve e completato progressivamente per non appesantire onboarding;
- UI comprensibile, evitando di esporre soltanto numeri interni;
- regression test su classificazione, soglie, persistenza ed eligibility.

## P07 — Progressi e storico

Progressi deve rispondere alla domanda “sto migliorando?”.

- allenamenti/frequenza/costanza;
- volume effettivo per esercizi caricati e trend;
- forza/carichi e reps;
- progressioni calisthenics/skill;
- dettaglio sessione con programmato vs eseguito;
- serie, reps, carichi, RPE, durata e volume effettivo;
- stati parziale/saltato/completato coerenti con Recovery/Coach.

## P08 — Exercise Detail e progressioni

- descrizione/setup/esecuzione DB-owned;
- attrezzatura e prescrizione leggibili;
- progressione/regressione esplicite;
- mostrare chiaramente esercizio più facile -> corrente -> più difficile quando il grafo lo consente;
- per skill complesse valutare rappresentazione visuale della progressione.

## P09 — Onboarding

Percorso guidato minimo:
1. dati essenziali atleta;
2. luoghi di allenamento;
3. obiettivi;
4. assessment iniziale Capability Profile;
5. frequenza/giorni;
6. proposta piano.

L'onboarding non deve obbligare a completare ogni dimensione del Capability Profile prima di poter usare l'app; le valutazioni mancanti possono essere proposte progressivamente.

## P10 — Device acceptance ereditata da v0.9.4

Eseguire su dispositivo fisico tutte le clausole DEVICE_PENDING F01-F11, incluse:
- Exercise Detail;
- timer/recuperi;
- TrainingLocation in Custom Workout;
- materializzazione piano in luoghi diversi;
- modifica/rigenerazione piano;
- calendario;
- Home;
- preview/reset/scroll;
- persistenza/recovery dopo background/process recreation dove rilevante.

Ogni problema trovato diventa bug v0.9.5 con riproduzione e regression test quando deterministico.

## P11 — Release gate

La v0.9.5 non è completa finché:
- P01-P10 richiesti sono accettati;
- suite JVM/regression/lint è verde;
- Exercise DB deterministic validation è verde;
- APK release firmato è prodotto dall'esatto SHA candidato;
- firma APK è verificata;
- device acceptance mirata è completata;
- documentazione e PROJECT_STATUS riflettono lo SHA effettivo;
- nessun risultato CI viene usato come sostituto della verifica fisica.

## Ordine di lavoro proposto

1. Audit UX end-to-end.
2. Capability Profile: ricerca/test/soglie e modello multidimensionale.
3. Runner, incluso countdown preparazione timed exercises.
4. Home e navigazione.
5. Piano/Calendario.
6. Generazione/preview.
7. Progressi/Storico.
8. Profilo/Capability UI ed Exercise Detail.
9. Onboarding.
10. Device acceptance completa.
11. Signed release, documentazione e freeze candidato.

## Fuori scope salvo necessità emersa dall'audit

- riscrittura dei motori F01-F11 già verificati;
- compatibilità legacy;
- nuove integrazioni di rete non necessarie;
- espansioni funzionali che non contribuiscono a UX, correttezza, assessment, device acceptance o release readiness.
