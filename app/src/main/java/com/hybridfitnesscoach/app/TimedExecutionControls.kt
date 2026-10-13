package com.hybridfitnesscoach.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

@Composable
internal fun TimedExecutionControls(state:WorkoutSessionState,index:Int,now:Long,onUpdate:(WorkoutSessionState)->Unit) {
    val exercise=state.exercises[index]
    val ex=requireNotNull(exercise.exerciseSnapshot)
    val ownedClock=state.timedSet?.takeIf{it.exerciseIndex==index}
    val pending=exercise.sets.indexOfFirst{!it.completed}
    var load by rememberSaveable(state.id,index,pending){mutableStateOf(ownedClock?.loadKg?.toString().orEmpty())}
    var assistance by rememberSaveable(state.id,index,pending){mutableStateOf(ownedClock?.assistanceKg?.toString().orEmpty())}
    val paused=state.pausedAtEpochMs!=null
    val clock=state.timedSet
    if(ex.resistanceMode==ResistanceMode.EXTERNAL_LOAD)OutlinedTextField(load,{load=it.replace(',','.');onUpdate(TimedRunner.observedResistance(state,index,load.toDoubleOrNull(),assistance.toDoubleOrNull()))},label={Text("Carico usato (kg)")},enabled=!paused && (clock==null || ownedClock!=null),modifier=Modifier.fillMaxWidth())
    if(ex.resistanceMode==ResistanceMode.ASSISTED)OutlinedTextField(assistance,{assistance=it.replace(',','.');onUpdate(TimedRunner.observedResistance(state,index,load.toDoubleOrNull(),assistance.toDoubleOrNull()))},label={Text("Assistenza usata (kg)")},enabled=!paused && (clock==null || ownedClock!=null),modifier=Modifier.fillMaxWidth())
    if(clock?.exerciseIndex==index){
        val preparation=TimedRunner.preparationSeconds(state,now)
        if(preparation>0)Text("PREPARATI • $preparation",style=MaterialTheme.typography.headlineLarge)
        else if(now>=clock.startsAtEpochMs)Text("${clock.targetSeconds-TimedRunner.executedSeconds(state,now)} s • in esecuzione",style=MaterialTheme.typography.headlineMedium)
        Button(onClick={onUpdate(TimedRunner.stop(state,System.currentTimeMillis()))},modifier=Modifier.fillMaxWidth()){Text(if(preparation>0)"ANNULLA PREPARAZIONE"else "STOP E REGISTRA")}
    } else Button(onClick={onUpdate(TimedRunner.start(state,System.currentTimeMillis(),loadKg=load.toDoubleOrNull(),assistanceKg=assistance.toDoubleOrNull()))},
        enabled=!paused && clock==null && remainingSeconds(state.restEndsAtEpochMs,now)==0,modifier=Modifier.fillMaxWidth()){Text("AVVIA ESERCIZIO")}
}
