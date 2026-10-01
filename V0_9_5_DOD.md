# Hybrid Fitness Coach — v0.9.5 Roadmap / Definition of Done

## Missione della release

La v0.9.5 parte dalla baseline v0.9.4 congelata. La v0.9.4 resta immutata come riferimento repository-level verificato; la v0.9.5 recupera le clausole DEVICE_PENDING e porta l'app da motore funzionale a prodotto comprensibile, coerente e verificato su dispositivo.

Baseline runtime ereditata: `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07` (`checkpoint/v0.9.4-f01-f11-green`).
CI ereditata: Exercise DB v2 #292 SUCCESS; Android APK #845 SUCCESS.

## Principi vincolanti

1. Non riaprire F01-F11 già repository-verified salvo regressione dimostrata o requisito v0.9.5 esplicito.
2. Nessuna retrocompatibilità con schemi, modelli, UI o API legacy è richiesta. Non introdurre adapter, doppie API o percorsi paralleli per preservare comportamenti obsoleti.
3. Codice/fallback legacy ancora usato dal modello corrente va rimosso quando la sostituzione corrente è completa e verificata, non mantenuto per compatibilità.
4. Adaptive Coach deve essere il motore dell'esperienza, non un concetto che l'utente deve comprendere per usare l'app.
5. L'interfaccia privilegia: stato attuale -> prossima azione -> motivazione -> dettaglio opzionale.
6. Qualunque origine di workout converge su un'unica pipeline di avvio, esecuzione, persistenza e storico.
7. Ogni nuovo comportamento deterministico deve avere test automatici; interazioni/visuali restano DEVICE_PENDING fino a test fisico.
8. Limitare i push: audit e verifiche locali/statiche prima di consumare CI.

## P00 — Multi-plan foundation

La v0.9.5 supporta **due o più piani contemporaneamente attivi**. Un piano può essere disattivato senza essere eliminato.

Requisiti:
- ogni piano ha identità e ownership proprie;
- enable/disable è persistente;
- un piano disattivato non influenza Adaptive Coach, scheduling attivo o suggerimenti;
- Adaptive Coach aggrega tutti e soli i piani attivi;
- due piani attivi possono programmare sessioni nello stesso giorno senza perdita o sovrascrittura implicita;
- collisioni nello stesso giorno devono restare esplicite e gestibili dalla UI;
- storico/esecuzioni mantengono l'origine del piano;
- eliminare un piano non deve eliminare o riscrivere evidenza storica già eseguita;
- modifica/rigenerazione di un piano non deve mutare le sessioni appartenenti a un altro piano;
- test deterministici devono coprire enable/disable, persistenza, ownership, aggregazione Adaptive Coach, collisioni same-day e storico.

**Stato corrente:** **VERIFIED a livello repository** sul checkpoint `7dc3de6da7931173550618d7f63da34f5bfa4861` (PR #14): Android APK #857 SUCCESS sull'esatto SHA, con unit test, lint e debug APK; test deterministici coprono enable/disable persistente, ownership, aggregazione dei piani attivi, collisioni same-day e conservazione dello storico. La verifica fisica/visuale resta nei gate DEVICE_PENDING di P12/P13.

## P01 — UX end-to-end e architettura informativa

### P01.1 — Navigazione top-level

Target:
- Oggi;
- Piano;
- Progressi;
- Profilo.

Decisioni:
- Attrezzatura non è una destinazione primaria: confluisce in Profilo -> Luoghi di allenamento.
- Profilo atleta non resta nascosto dentro Progressi.
- Creazione/configurazione del piano non appartiene alla Home.
- Libreria esercizi resta disponibile come funzione secondaria/contestuale da Profilo, ricerca, Skill ed Exercise Detail.
- ridurre l'uso di AlertDialog per workflow complessi: Runner e generazione diventano flussi/schermate dedicate; dialog solo per vere azioni modali o conferme.
- verificare accessibilità, scroll, back navigation, empty/error state e azioni distruttive.

### P01.2 — Pipeline universale workout

Ogni origine:
- sessione del piano;
- allenamento extra adattivo;
- workout personalizzato;
- workout generato da skill/libreria;
- singolo esercizio;

deve convergere su:

`origine -> scegli luogo -> materializza/genera -> anteprima -> Runner universale -> salva -> storico/progressi -> Adaptive Coach`.

Non creare Runner o persistenze separate per origine.

## P02 — Home / Oggi

La Home deve rispondere immediatamente a: come sto, cosa faccio oggi, perché, cosa viene dopo.

### Se oggi esiste una sessione del piano
- mostrare esplicitamente "Allenamento del piano";
- CTA primaria "Inizia";
- mostrare focus/durata/sintesi senza duplicare il calendario;
- l'esercizio effettivo viene materializzato dopo la scelta del luogo.

### Se oggi non esiste una sessione del piano
- mostrare "Nessuna sessione programmata" o stato di recupero appropriato;
- mostrare la prossima sessione;
- offrire "Allenamento extra" come azione consapevole, non mascherata da sessione del piano.

### Allenamento extra adattivo
Non è un workout personalizzato e non è una sessione del piano. È generato automaticamente dall'Adaptive Coach considerando almeno:
- Capability Profile;
- readiness/recovery;
- storico e fatica recente;
- obiettivi;
- piano futuro, per evitare interferenze non necessarie con la prossima sessione.

CTA secondaria distinta: "Crea workout personalizzato".

## P03 — Piano multisettimanale e calendario

- il piano è indipendente dal luogo di allenamento fino all'avvio della singola sessione;
- viste Settimana e Calendario mensile;
- stati pianificato/completato/parziale/saltato/riprogrammato distinguibili;
- dettaglio sessione;
- azioni secondarie raccolte senza molte CTA concorrenti;
- riprogrammazione coerente, cascata dove prevista e ripristino data originale;
- modifica obiettivo/frequenza/giorni/skill con review e conferma prima della rigenerazione futura;
- storico già eseguito immutabile.

## P04 — Gate universale di avvio workout

Vale per qualsiasi workout.

### 1. Scegli luogo
- scelta disponibile prima di ogni esecuzione;
- ultimo luogo effettivamente utilizzato preselezionato;
- utente libero di cambiarlo;
- il piano multisettimanale non richiede un luogo in fase di creazione.

### 2. Materializza / genera
Usare:
- attrezzatura realmente disponibile nel luogo scelto;
- Capability Profile;
- stato/readiness atleta;
- storico;
- intent/origine del workout.

### 3. Anteprima
Mostrare almeno:
- esercizi nell'ordine corretto a partire dal primo;
- serie;
- reps/tempo/distanza;
- recuperi;
- carichi/assistenza suggeriti quando applicabili.

### 4. Avvia
Tutte le origini entrano nello stesso Runner.

### Stato implementazione v0.9.5
- **PARTIAL / IMPLEMENTED_NOT_CI_VERIFIED:** singolo esercizio, sessione del piano e allenamento extra adattivo convergono su `WorkoutLaunchFlow` e sul Runner esistente.
- La scelta del luogo preseleziona l'ultimo luogo effettivamente utilizzato e filtra quelli incompatibili con attrezzatura/sessione.
- Il materializzatore conserva prescrizioni effettive, readiness e storico; il Runner persiste il luogo scelto e mantiene `plannedWorkoutId` per l'ownership multi-plan.
- Custom e skill restano da migrare sulla stessa astrazione; non va aggiunto un Runner separato.
- La verifica fisica del nuovo gate è **DEVICE_PENDING**.

## P05 — Workout Runner universale

Il Runner deve essere unico e coerente per sessione del piano, extra adattivo, personalizzato, skill/libreria e singolo esercizio.

### P05.1 — Esercizio corrente
- esercizio corrente dominante;
- serie corrente/totale corretta fin dalla prima serie;
- target reps/tempo/distanza;
- carico/assistenza;
- RPE esercizio default 5, valido se non modificato;
- recupero;
- precedente/successivo;
- salta/riprendi esercizio;
- annulla ultima serie.

### P05.2 — "Come eseguirlo" obbligatorio

Durante QUALSIASI workout ogni esercizio deve offrire accesso evidente a "Come eseguirlo" senza perdere lo stato del Runner.

Mostrare, quando disponibili nel DB/modello:
- setup/posizione iniziale;
- tecnica/passaggi di esecuzione;
- indicazioni tecniche;
- attrezzatura;
- target/prescrizione;
- regressione/esercizio più facile;
- progressione/esercizio più difficile.

Aprire/chiudere il dettaglio non deve perdere o alterare:
- serie;
- timer esercizio;
- timer recupero;
- pausa;
- valori inseriti;
- posizione nel workout.

### P05.3 — Recupero
- timer automatico coerente con prescrizione;
- pausa/ripresa;
- salta recupero;
- persistenza/recovery dello stato.

### P05.4 — Countdown preparazione esercizi a tempo
Per ogni esercizio prescritto a tempo:
- gli ultimi 5 secondi del recupero diventano countdown di preparazione 5-4-3-2-1;
- il countdown NON aggiunge 5 secondi al recupero;
- a zero parte automaticamente il timer a rovescio dell'esercizio;
- feedback visivo evidente; sonoro se previsto/abilitato;
- se il recupero residuo è <5 s, partire dal tempo realmente disponibile;
- regola generale del Runner, non duplicata nei singoli esercizi;
- test deterministici su recovery -> preparation -> timed exercise, pause/restore/background e boundary temporali.

### P05.5 — Fine workout
- conferma prima di terminare/salvare;
- riepilogo post-workout;
- persistenza completa dei risultati;
- storico immediatamente coerente.

## P06 — Workout personalizzato

L'utente specifica intento, non prescrizione manuale.

- passa sempre dal gate universale P04;
- ultimo luogo usato preselezionato ma modificabile;
- durata;
- focus/parti del corpo/skill;
- serie, reps, recuperi, carichi e timing generati automaticamente;
- anteprima sempre dall'esercizio 1;
- locked/pinned comprensibili;
- rigenerazione dei soli esercizi non bloccati;
- avvio nel Runner universale P05.

## P07 — Capability Profile multidimensionale 2.0

Il Capability Profile non collassa in un singolo livello globale.

Dimensioni minime:
- esperienza generale;
- spinta orizzontale;
- tirata orizzontale;
- spinta verticale;
- tirata verticale;
- gambe/squat;
- catena posteriore/hip hinge;
- core;
- capacità isometriche;
- skill/calisthenics.

Requisiti:
- ampliare gli esercizi di assessment per coprire le dimensioni;
- scegliere test con regressioni accessibili e progressioni più difficili;
- documentare provenienza/ragione soglie: SOURCE_BASED, APP_RULE, FALLBACK o UNVERIFIED;
- non inventare soglie arbitrarie;
- generator/prescription usa la dimensione pertinente all'esercizio;
- capacità elevata in un distretto non innalza automaticamente gli altri;
- stato unassessed conservativo;
- assessment iniziale breve e completabile progressivamente;
- UI comprensibile;
- regression test su classificazione, soglie, persistenza ed eligibility.

## P08 — Exercise Detail / database / progressioni

- descrizione/setup/esecuzione DB-owned;
- tecnica disponibile sia dalla Libreria sia dal Runner;
- attrezzatura e prescrizione leggibili;
- progressione/regressione esplicite;
- mostrare chiaramente esercizio più facile -> corrente -> più difficile quando il grafo lo consente;
- per skill complesse valutare rappresentazione visuale;
- dati mancanti necessari a "Come eseguirlo" devono essere identificati e completati/validati prima del release gate.

## P09 — Progressi e storico

Progressi deve rispondere a "sto migliorando?".

- allenamenti/frequenza/costanza;
- volume effettivo per esercizi caricati e trend;
- forza/carichi/reps;
- progressioni calisthenics/skill;
- dettaglio sessione programmato vs eseguito;
- serie, reps, tempi, distanze, carichi, assistenza, recuperi, RPE, durata e volume effettivo;
- stati parziale/saltato/completato coerenti con Recovery/Coach.

## P10 — Profilo e configurazione

- dati atleta;
- obiettivi;
- Capability Profile;
- Health Connect;
- accesso Libreria esercizi/ricerca;
- Luoghi di allenamento:
  - crea;
  - modifica;
  - elimina;
  - attrezzatura disponibile;
  - persistenza ultimo luogo effettivamente utilizzato.

## P11 — Onboarding

Percorso guidato minimo:
1. dati essenziali atleta;
2. obiettivo;
3. assessment iniziale Capability Profile;
4. primo luogo di allenamento;
5. Health Connect opzionale;
6. frequenza/giorni e proposta piano.

Non obbligare a completare ogni dimensione del Capability Profile prima di usare l'app; le valutazioni mancanti possono essere proposte progressivamente.

## P12 — Cleanup tecnico e device acceptance ereditata

### Cleanup
- eliminare UI obsoleta dopo la sostituzione;
- eliminare API non utilizzate;
- eliminare compatibility layer inutili;
- eliminare fallback legacy quando non servono al modello corrente;
- nessuna retrocompatibilità come motivazione per mantenere codice morto.

### DEVICE_PENDING v0.9.4
Eseguire su dispositivo fisico tutte le clausole ancora pendenti F01-F11, incluse:
- Exercise Detail;
- timer/recuperi;
- TrainingLocation;
- materializzazione piano in luoghi diversi;
- modifica/rigenerazione piano;
- calendario;
- Home;
- preview/reset/scroll;
- persistenza/recovery dopo background/process recreation.

Ogni problema trovato diventa bug v0.9.5 con riproduzione e regression test quando deterministico.

## P13 — Release gate v0.9.5

La v0.9.5 non è completa finché:
- P01-P12 richiesti sono accettati;
- suite JVM/regression/lint è verde;
- Exercise DB deterministic validation è verde;
- test della pipeline universale coprono tutte le origini workout;
- test Runner coprono tecnica esercizio, timer, recuperi, RPE e recovery;
- APK release firmato è prodotto dall'esatto SHA candidato;
- firma APK è verificata;
- device acceptance mirata è completata;
- documentazione e PROJECT_STATUS riflettono lo SHA effettivo;
- nessun risultato CI sostituisce la verifica fisica.

## Ordine di lavoro

1. P00 — multi-plan foundation: repository VERIFIED sul checkpoint `7dc3de6da7931173550618d7f63da34f5bfa4861`; device acceptance resta nei gate P12/P13.
2. P01 — navigation shell + separazione responsabilità.
3. P02/P03 — Home e Piano con origini workout inequivocabili.
4. P04 — gate universale e persistenza ultimo luogo usato.
5. P05 — Runner universale, tecnica esercizio e timer.
6. P06 — custom workout sulla pipeline comune.
7. P07 — Capability Profile multidimensionale.
8. P08 — Exercise Detail/database/progressioni.
9. P09/P10 — Progressi, Storico e Profilo.
10. P11 — Onboarding.
11. P12 — cleanup + DEVICE_PENDING.
12. P13 — audit finale, signed APK, device acceptance e freeze.

## Fuori scope salvo necessità emersa dall'audit

- riscrittura gratuita dei motori F01-F11 già verificati;
- compatibilità legacy;
- nuove integrazioni di rete non necessarie;
- espansioni che non contribuiscono a UX, correttezza, assessment, device acceptance o release readiness.
