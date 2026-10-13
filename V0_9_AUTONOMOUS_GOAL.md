# HYBRID FITNESS COACH v0.9 — AUTONOMOUS DEVELOPMENT GOAL

## 1. MISSION

Agisci come senior Android developer, software architect, QA engineer e product/UX engineer di Hybrid Fitness Coach.

Lavora ESCLUSIVAMENTE sul branch:

feature/v0.9-adaptive-progression

Porta autonomamente Hybrid Fitness Coach v0.9 fino alla Definition of Done descritta in:

V0.9_AUTONOMOUS_DOD.md

NON iniziare v1.0.

La v0.9 è un'evoluzione della v0.8 Evo, NON una riscrittura.

Ogni esecuzione di questo Goal rappresenta una nuova iterazione di un processo persistente.

Una build verde è un CHECKPOINT, non la fine del lavoro.

============================================================
2. PROCEDURA OBBLIGATORIA A OGNI ITERAZIONE
============================================================

PRIMA DI MODIFICARE IL CODICE:

1. Leggi V0.9_AUTONOMOUS_DOD.md.
2. Analizza HEAD corrente.
3. Analizza git log recente.
4. Analizza codice, test e documentazione rilevanti.
5. Determina cosa è realmente già implementato.
6. Verifica eventuali modifiche prodotte dalle iterazioni precedenti.
7. NON reimplementare funzionalità già valide.
8. Individua il gate incompleto con priorità più alta le cui dipendenze sono soddisfatte.
9. Implementa una porzione concreta e verificabile.
10. Aggiungi/aggiorna test pertinenti.
11. Esegui i test appropriati.
12. Correggi autonomamente errori introdotti.
13. Mantieni il repository compilabile.
14. Aggiorna V0.9_AUTONOMOUS_DOD.md SOLO quando un requisito è realmente implementato e verificato.

NON limitarti a produrre:
- analisi;
- TODO;
- piani;
- documentazione;
- commenti.

Quando esiste lavoro implementabile, ogni iterazione deve produrre progresso concreto nel codice.

Se un requisito è già completo, passa al successivo.

Se trovi un errore:

DIAGNOSI
→ CORREZIONE
→ TEST
→ CONTINUA.

Se trovi un blocker realmente esterno:
- documentalo;
- non inventare una soluzione;
- passa al successivo requisito implementabile.

============================================================
3. GUARDRAIL
============================================================

NON:

- lavorare su main;
- effettuare merge su main;
- modificare GitHub Actions;
- modificare secrets;
- modificare permessi repository;
- eliminare dati utente senza migrazione;
- rimuovere funzionalità v0.8 valide;
- inventare dati Health Connect;
- inventare storico atleta;
- inventare skill mastery;
- inventare carichi precedenti;
- raggiungere il target esercizi tramite duplicati artificiali;
- effettuare mega-riscritture non necessarie.

Procedi incrementalmente.

============================================================
4. REGRESSION SAFETY — v0.8 EVO
============================================================

Preserva almeno:

- profilo atleta;
- profili attrezzatura;
- libreria esercizi;
- generazione workout;
- workout personalizzati;
- REPS;
- SECONDS;
- DURATION;
- DISTANCE;
- INTERVALS;
- esercizi unilaterali;
- carico esterno;
- assistenza;
- timer;
- timer in background;
- ripristino workout;
- navigazione workout;
- skip;
- gestione serie;
- valori effettivamente eseguiti;
- RPE/fatica;
- storico;
- previsto vs eseguito;
- Recovery Engine;
- installazione come aggiornamento.

I dati esistenti devono essere preservati quando compatibili.

============================================================
5. ARCHITETTURA v0.9
============================================================

Integra quattro sistemi principali:

1. PROGRESSION ENGINE
2. WORKOUT SEQUENCING ENGINE
3. ADAPTIVE COACH
4. LOAD RECOMMENDATION ENGINE

Pipeline concettuale:

ATHLETE PROFILE
+
WORKOUT HISTORY
+
PERFORMANCE
+
RPE/RIR
+
RECOVERY
+
OPTIONAL HEALTH DATA
+
SKILL PROGRESSION
        ↓
READINESS / FATIGUE
        ↓
ADAPTIVE COACH
        ↓
WORKOUT PRESCRIPTION
        ↓
ACTUAL PERFORMANCE
        ↓
UPDATED PROGRESSION / LOAD / RECOVERY
        ↓
NEXT WORKOUT

Mantieni la complessità prevalentemente nascosta all'utente.

============================================================
6. OBIETTIVI ATLETA
============================================================

Gli obiettivi principali devono essere:

- Forza
- Ipertrofia
- Calisthenics
- Fitness generale

NON deve essere necessario selezionare un obiettivo separato
"Forza + Skill Calisthenics".

Se viene scelto CALISTHENICS mostra:

"Skill da sviluppare"

con selezione multipla.

Supportare progressivamente skill quali:

- Front Lever
- Back Lever
- Planche
- Handstand
- Muscle Up
- Human Flag
- L-Sit
- V-Sit
- Pistol Squat
- One Arm Push-Up
- One Arm Pull-Up

e altre skill coerenti introdotte nel catalogo.

Le skill selezionate DEVONO modificare realmente il workout generato.

============================================================
7. PROGRESSION ENGINE
============================================================

Rappresenta le skill mediante grafi, non semplici liste.

Supporta:

- prerequisiti;
- prerequisiti multipli;
- biforcazioni;
- percorsi convergenti;
- regressioni;
- esercizi preparatori;
- accessori;
- criteri di mastery.

Stati minimi:

LOCKED
AVAILABLE
IN_PROGRESS
MASTERED

Non considerare MASTERED un esercizio soltanto perché eseguito una volta.

Usa criteri misurabili su più sessioni.

Possibili segnali:

- set;
- reps;
- secondi;
- distanza;
- volume;
- RPE;
- RIR;
- consistenza;
- frequenza;
- performance recenti.

Esempio concettuale:

Push-Up
→ Diamond/Close Grip
→ Archer Push-Up
→ Assisted One Arm Push-Up
→ One Arm Push-Up

Le progressioni reali possono essere non lineari.

============================================================
8. SKILL TREE
============================================================

Implementa Skill Tree interattivo.

Mostra almeno:

✓ MASTERED
● IN_PROGRESS
○ AVAILABLE
🔒 LOCKED

Ogni nodo deve poter mostrare:

- esercizio;
- livello;
- stato;
- prerequisiti;
- performance corrente;
- requisito mastery/unlock;
- cosa manca;
- progressione successiva.

============================================================
9. SKILL PROGRESS
============================================================

Nella sezione Progressi mostra progresso delle skill.

Esempio:

Planche 42%
Handstand 68%
Front Lever 35%

La percentuale NON deve essere arbitraria.

Derivala da:

- nodi acquisiti;
- importanza/peso dei nodi;
- performance;
- prerequisiti;
- distanza dal milestone successivo;
- consistenza.

Mostra anche trend temporale quando i dati esistono.

Esempio:

20 → 26 → 31 → 37 → 42%

Mostra:

PROSSIMO OBIETTIVO
REQUISITO MANCANTE

============================================================
10. WORKOUT SEQUENCING ENGINE
============================================================

Non scegliere semplicemente N esercizi.

Costruisci una sessione coerente considerando:

- obiettivo;
- skill;
- livello;
- readiness;
- recovery;
- fatigue;
- storico;
- attrezzatura;
- setup cost;
- pattern motori;
- muscoli;
- volume;
- intensità;
- interferenza;
- progressioni;
- varietà recente.

Ordine tipico, da adattare al contesto:

SKILL / TECNICA
→
COMPOUND / FORZA
→
SECONDARI
→
ACCESSORI
→
CORE / CONDITIONING / MOBILITY

============================================================
11. CONTROLLED VARIETY
============================================================

Evita:

- workout sempre identici;
- workout casuali.

Usa varietà controllata basata su:

- storico recente;
- equivalenza semantica;
- skill;
- recovery;
- attrezzatura;
- pattern;
- livello.

============================================================
12. SOSTITUZIONE SEMANTICA
============================================================

Quando sostituisci un esercizio considera:

- pattern;
- muscoli;
- ruolo;
- modalità;
- difficoltà;
- attrezzatura;
- skill;
- progressione.

Non sostituire semplicemente con un esercizio casuale dello stesso muscolo.

============================================================
13. PIN / LOCK
============================================================

Permetti:

- pin esercizio;
- sostituzione singola;
- rigenerazione parziale;
- mantenimento degli esercizi desiderati.

Un esercizio pinnato NON deve cambiare durante la rigenerazione.

============================================================
14. ADAPTIVE COACH
============================================================

Usa realmente:

- storico;
- performance;
- volume;
- RPE;
- RIR;
- carichi;
- progression state;
- Recovery Engine;
- readiness;
- Health Connect quando disponibile;
- trend.

Decisioni minime:

ADVANCE
MAINTAIN
REGRESS
DELOAD

Esempi:

performance ↑ + margine adeguato
→ ADVANCE

performance stabile
→ MAINTAIN

fallimenti ripetuti
→ REGRESS

fatigue/readiness scarsa
→ DELOAD

Le decisioni devono modificare realmente la prescrizione.

============================================================
15. SPIEGABILITÀ
============================================================

Mostra spiegazioni brevi e comprensibili.

Esempi:

"Carico mantenuto: nell'ultima sessione hai raggiunto RIR 1."

"Front Lever prioritario perché è una tua skill obiettivo."

"Volume gambe ridotto per recupero incompleto."

"Advanced Tuck non ancora sbloccato: manca il requisito di tenuta."

============================================================
16. LOAD RECOMMENDATION ENGINE
============================================================

Il campo "Carico kg" vuoto NON è un suggerimento.

Per gli esercizi con resistenza esterna l'app deve poter prescrivere:

- peso suggerito;
- target reps;
- target RIR/RPE;
- motivo del suggerimento.

Se manca storico sufficiente:

CARICO DA CALIBRARE

Non inventare il peso.

L'utente inserisce il carico realmente utilizzato.

Memorizza almeno:

- peso previsto;
- peso effettivo;
- reps previste;
- reps effettive;
- RIR/RPE;
- completamento.

============================================================
17. PROGRESSIONE CARICHI
============================================================

Il Load Engine deve poter:

AUMENTARE
MANTENERE
RIDURRE

Considerando:

- storico;
- reps;
- RPE/RIR;
- completion;
- fatigue;
- trend.

Rispetta gli incrementi realmente disponibili.

Evita valori impossibili come 17.37 kg quando l'attrezzatura consente soltanto incrementi discreti.

============================================================
18. RECUPERI
============================================================

Ricalibra:

SET REST
e
EXERCISE TRANSITION REST.

Durante il collaudo sono comparsi, ad esempio:

180 / 240 s
150 / 180 s
90 / 120 s

I valori non sono completamente fissi, ma spesso risultano eccessivi.

Considera:

- obiettivo;
- intensità;
- reps;
- esercizio;
- compound/isolation;
- skill;
- livello;
- RPE/RIR;
- fatigue;
- tipo sessione.

Permetti modifica manuale.

============================================================
19. HEALTH CONNECT
============================================================

Integra realmente Health Connect quando supportato e autorizzato.

Possibili segnali:

- sonno;
- resting HR;
- HR;
- HRV quando disponibile;
- attività;
- workout;
- passi;
- altre metriche affidabili compatibili.

Mantieni separati:

RAW HEALTH DATA
→
NORMALIZED METRICS
→
READINESS MODEL

Gestisci:

- permessi negati;
- dati mancanti;
- fonti differenti;
- valori anomali;
- fallback.

L'app DEVE funzionare senza Health Connect.

============================================================
20. READINESS
============================================================

Mostra uno stato comprensibile.

Esempio:

STATO DI FORMA
82 / 100

Recupero muscolare: buono
Sonno: buono
Carico recente: medio

Evita falsa precisione.

Mostra sinteticamente quali fattori influenzano la stima.

============================================================
21. LIBRERIA ESERCIZI
============================================================

Target v0.9:

CIRCA 500 ESERCIZI REALI E UTILI.

NON creare duplicati artificiali per raggiungere il numero.

Copri:

- bilanciere;
- manubri;
- macchine;
- cavi;
- kettlebell;
- elastici;
- calisthenics;
- progressioni;
- regressioni;
- corpo libero;
- core;
- mobilità;
- conditioning;
- cardio;
- recovery/prehab.

============================================================
22. EXERCISE KNOWLEDGE MODEL
============================================================

Ogni esercizio dovrebbe poter descrivere, dove applicabile:

- stable ID;
- nome;
- alias;
- descrizione;
- istruzioni;
- muscoli primari;
- muscoli secondari;
- equipment;
- livello;
- pattern;
- categoria;
- ruolo;
- prescription mode;
- laterality;
- resistance mode;
- skill correlate;
- prerequisiti;
- progressioni;
- regressioni;
- set rest;
- transition rest;
- range prescrizione;
- media opzionali;
- schema version.

Modalità minime:

REPS
SECONDS
DURATION
DISTANCE
INTERVALS

============================================================
23. ATTREZZATURA
============================================================

Supporta e normalizza almeno:

- Corpo libero
- Sbarra
- Parallele
- Anelli
- TRX
- Bilanciere
- Manubri
- Panca
- Cavi
- Macchine
- Elastici
- Kettlebell
- Leg Press
- Foam Roller
- Ab Wheel

Foam Roller e Ab Wheel devono essere:

- selezionabili;
- filtrabili;
- collegati agli esercizi;
- utilizzabili dal generatore.

============================================================
24. FILTRI LIBRERIA
============================================================

Mantieni concettualmente separati:

ATTREZZATURA
MUSCOLI
SKILL
MISURAZIONE

MISURAZIONE:

- reps;
- secondi;
- durata;
- distanza;
- intervalli.

Evita due filtri entrambi chiamati "Tipo".

Non usare Push/Pull/Legs/Core/Cardio/Mobility come duplicazione confusa di altri filtri se già rappresentati correttamente dai metadati.

============================================================
25. INTERVALS
============================================================

Ogni esercizio INTERVALS deve descrivere una vera attività.

NON utilizzare elementi ambigui come:

"Intervalli 60/60"

senza attività.

Esempio valido:

"Corsa intervallata 60/60"

con:

60 s lavoro
60 s recupero
round
istruzioni.

============================================================
26. CATALOG VALIDATION
============================================================

Con un catalogo ampio implementa/usa validazione per:

- stable ID univoci;
- duplicati;
- riferimenti;
- equipment;
- measurement;
- progressioni;
- regressioni;
- prerequisiti;
- cicli impossibili;
- schema versioning.

Preferisci una struttura mantenibile rispetto a un enorme blocco fragile hard-coded.

============================================================
27. HOME v0.9
============================================================

La Home deve rispondere rapidamente:

COME STO?
COSA DEVO ALLENARE?
PERCHÉ?

Mostra in forma compatta:

- readiness;
- recovery;
- workout suggerito;
- obiettivo;
- skill prioritarie;
- durata;
- adattamenti;
- motivazione.

CTA primaria:

INIZIA ALLENAMENTO

Il workout suggerito deve aggiornarsi correttamente quando cambiano obiettivo/profilo.

============================================================
28. WORKOUT RUNNER
============================================================

Durante il workout rendi evidente:

- esercizio corrente;
- set corrente;
- target;
- risultato reale;
- carico suggerito;
- carico effettivo;
- RIR/RPE;
- recupero;
- progresso sessione.

Deve essere sempre chiaro:

"Cosa devo fare adesso?"

Evita CTA con funzioni ambigue.

============================================================
29. DETTAGLIO ESERCIZIO
============================================================

Gli esercizi generati devono essere apribili quando previsto dalla UX.

Il dettaglio deve poter mostrare informazioni utili quali:

- muscoli;
- equipment;
- livello;
- istruzioni;
- ruolo;
- prerequisiti;
- progressioni/regressioni;
- collegamenti skill.

============================================================
30. PROGRESSI
============================================================

Organizza progressivamente:

PANORAMICA
STORICO
FORZA / CARICHI
SKILL
RECUPERO
TREND

Possibili grafici:

- carico;
- volume;
- strength trend;
- frequenza;
- skill progress;
- recovery/readiness.

============================================================
31. RECOVERY ENGINE
============================================================

Il recupero deve dipendere dal lavoro REALMENTE ESEGUITO.

Non utilizzare semplicemente il volume programmato.

Considera quando disponibili:

- serie completate;
- reps;
- carico;
- RPE;
- muscoli;
- tempo trascorso.

============================================================
32. WORKOUT PERSISTENCE
============================================================

Preserva/verifica:

background
→ timer continua

chiusura/restart
→ workout recuperabile

riapertura
→ esercizio/set/stato corretti

Conserva:

- serie completate;
- recupero;
- esercizio corrente;
- valori inseriti.

============================================================
33. STORICO
============================================================

Distingui:

PROGRAMMATO
VS
ESEGUITO.

Conserva dove applicabile:

- esercizi;
- set;
- reps;
- secondi;
- distanza;
- carico;
- RPE/RIR;
- skip;
- modifiche;
- durata;
- timestamp;
- goal;
- skill;
- equipment profile.

============================================================
34. OFFLINE FIRST
============================================================

Devono funzionare offline almeno:

- workout;
- runner;
- storico;
- progression engine;
- Skill Tree;
- load history;
- recovery locale.

Health Connect e servizi opzionali non devono rendere inutilizzabile l'app.

============================================================
35. UX / UI
============================================================

La v0.9 deve rappresentare un miglioramento visibile.

UX e funzionalità vanno sviluppate insieme.

Obiettivi:

- moderna;
- coerente;
- leggibile;
- fluida;
- professionale;
- intuitiva.

Evita:

- schermate sovraccariche;
- pulsanti ambigui;
- filtri duplicati;
- terminologia non spiegata;
- informazioni importanti nascoste.

Mantieni coerenza Material 3.

============================================================
36. ACCESSIBILITÀ
============================================================

Verifica progressivamente:

- touch targets;
- contrasto;
- dimensioni testo;
- dark mode;
- scrolling;
- stringhe lunghe;
- dialog;
- empty state;
- error state.

============================================================
37. PERFORMANCE
============================================================

Con circa 500 esercizi:

- ricerca rapida;
- filtri fluidi;
- Skill Tree fluido;
- startup ragionevole;
- niente parsing pesante ripetuto nella UI;
- evitare recomposition inutili;
- evitare caricamenti completi inutili.

============================================================
38. TEST AUTOMATICI
============================================================

Aggiungi test deterministici progressivamente.

GENERATOR:
- goal;
- equipment;
- level;
- recovery;
- skill priority;
- controlled variety.

PROGRESSION:
- prerequisites;
- unlock;
- mastery;
- regression;
- graph integrity.

LOAD:
- calibration;
- increase;
- maintain;
- decrease;
- equipment increment rounding.

SEQUENCING:
- skill priority;
- fatigue;
- ordering;
- pinned exercises.

RECOVERY:
- partial session;
- actual sets;
- load;
- RPE.

PERSISTENCE:
- save;
- restore;
- migration.

HEALTH:
- missing data;
- denied permissions;
- normalization;
- fallback.

CATALOG:
- unique IDs;
- valid references;
- cycles;
- equipment;
- measurement;
- duplicates.

============================================================
39. REGRESSION FLOWS
============================================================

Verifica progressivamente almeno:

A)
modifica obiettivo
→ salva
→ Home aggiornata
→ restart
→ Home ancora corretta.

B)
Calisthenics
→ seleziona skill
→ genera
→ workout realmente pertinente.

C)
avvia workout
→ completa serie
→ recupero
→ background
→ riapri
→ timer corretto.

D)
chiudi/riapri
→ workout ripristinato.

E)
workout parziale
→ storico previsto/eseguito corretto.

F)
esercizio con carico
→ suggerimento/calibrazione
→ carico reale
→ storico
→ workout successivo usa lo storico.

G)
criterio skill raggiunto
→ progression state aggiornato.

H)
pin
→ rigenera
→ esercizio pinnato rimane.

I)
Health Connect assente/negato
→ app continua a funzionare.

============================================================
40. ISPIRAZIONI ARCHITETTURALI APPROVATE
============================================================

Le idee precedentemente individuate nei progetti:

- calisthenics-progression
- Motus
- CalisthenicsMemory
- workout.cool

possono essere utilizzate come ispirazione concettuale per:

- progression model;
- workout sequencing;
- progression memory/history;
- exercise knowledge model;
- catalog pipeline;
- schema versioning;
- semantic substitution.

NON copiare codice incompatibile e NON introdurre dipendenze inutili.

============================================================
41. MODULARITÀ
============================================================

Riduci progressivamente l'accoppiamento tra:

- exercise domain;
- workout domain;
- progression domain;
- adaptive domain;
- health domain;
- history;
- persistence;
- UI.

NON effettuare una mega-riscrittura soltanto per ottenere una struttura teoricamente perfetta.

============================================================
42. MIGRAZIONI
============================================================

La v0.9 deve essere installabile sopra la versione precedente quando tecnicamente compatibile.

Preserva:

- profilo;
- equipment profile;
- storico;
- sessioni;
- impostazioni;
- dati workout.

Qualsiasi modifica incompatibile al modello dati deve prevedere migrazione o fallback sicuro.

============================================================
43. VERSIONING E RELEASE
============================================================

Target:

versionName = 0.9.0

Usa versionCode coerente con la storia reale del progetto.

La release finale deve produrre l'APK previsto dall'infrastruttura CI esistente.

Non dichiarare la release pronta soltanto perché assembleDebug passa.

============================================================
44. DOCUMENTAZIONE
============================================================

Mantieni documentazione coerente con lo stato reale.

Documenta progressivamente:

- architettura;
- Progression Engine;
- Sequencing Engine;
- Adaptive Coach;
- Load Engine;
- Skill Tree;
- Health Connect;
- Recovery;
- catalogo;
- migrazioni;
- test;
- limiti conosciuti.

============================================================
45. DEFINITION OF DONE
============================================================

La fonte persistente della Definition of Done è:

V0.9_AUTONOMOUS_DOD.md

Una checkbox NON deve essere marcata completata soltanto perché:

- esiste una classe;
- esiste una funzione;
- esiste documentazione;
- una singola build è verde.

Quando applicabile devono essere verificati:

IMPLEMENTAZIONE
+
WIRING REALE
+
TEST
+
UX VISIBILE.

Prima della conclusione esegui un audit indipendente dei gate.

La v0.9 termina soltanto quando tutti i gate raggiungibili della DoD sono realmente soddisfatti, la CI finale è verde e l'APK finale previsto è prodotto.

NON iniziare v1.0.

============================================================
46. REGOLA DI CONTINUITÀ
============================================================

Se la build/test corrente passa, NON fermarti concettualmente.

Il processo persistente lancerà una nuova iterazione.

Alla successiva esecuzione:

- rileggi HEAD;
- rileggi DoD;
- riconosci il lavoro precedente;
- scegli il prossimo gate;
- continua.

Se non rimane alcun lavoro implementabile perché la DoD è realmente completa, limita le modifiche a eventuali correzioni necessarie emerse dall'audit finale.

FINE AUTONOMOUS GOAL.
