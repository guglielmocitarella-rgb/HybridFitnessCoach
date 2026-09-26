# HybridFitnessCoach

Prototype Android di un fitness coach ibrido che unisce allenamento con pesi e progressioni calisthenics.

## V0.1

La prima base include:
- Home con allenamento suggerito
- profili luogo/attrezzatura (Palestra, Casa, Parco)
- libreria esercizi ricercabile
- collegamenti verso skill e progressioni
- schermata progressi

## Stack
- Kotlin 2.4.10
- Jetpack Compose + Material 3
- Compose BOM 2026.09.00
- Android Gradle Plugin 9.4.0
- compileSdk / targetSdk 37
- minSdk 26

## Build automatica

A ogni push su `main`, GitHub Actions esegue `.github/workflows/android-debug.yml`, compila `:app:assembleDebug` e pubblica `HybridFitnessCoach-debug` come artifact scaricabile dalla pagina della workflow.

## V0.2 in sviluppo\n- sessione di allenamento interattiva\n- schede esercizio apribili\n- creazione di profili attrezzatura personalizzati\n\n> V0.1 è un prototipo funzionale dell'interfaccia e del modello iniziale. Database, workout engine adattivo e catalogo esercizi esteso arriveranno nelle milestone successive.
