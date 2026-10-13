package com.hybridfitnesscoach.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun WorkoutLaunchScreen(
    flow:WorkoutLaunchFlow,locations:List<TrainingLocation>,preferredLocationId:String,readinessScore:Int?,
    athlete:AthleteProfile,sessions:List<SessionRecord>,onFlowChanged:(WorkoutLaunchFlow)->Unit,
    onChooseLocation:(TrainingLocation)->Unit,onStart:()->Unit,onCancel:()->Unit
) {
    var locationId by rememberSaveable(flow.intent){mutableStateOf(preferredWorkoutLocation(locations,preferredLocationId)?.id)}
    var detail by rememberSaveable(flow.workout?.id){mutableStateOf<Int?>(null)}
    var replacing by rememberSaveable(flow.workout?.id){mutableStateOf<Int?>(null)}
    val single=flow.intent.origin==WorkoutOrigin.SINGLE_EXERCISE
    val location=locations.firstOrNull{it.id==flow.locationId}
    fun replace(current:WorkoutLaunchFlow,index:Int,ex:Exercise):WorkoutLaunchFlow {
        val edited=replacePreviewExercise(current,index,ex)
        if(edited==current)return current
        val state=requireNotNull(edited.workout)
        val frozen=freezeWorkoutTargets(state.copy(exercises=listOf(state.exercises[index])),sessions,readinessScore).exercises.single()
        return edited.copy(workout=state.copy(exercises=state.exercises.mapIndexed{i,e->if(i==index)frozen else e}))
    }
    Box(Modifier.fillMaxSize()) {
        FlowScreen(onDismissRequest=onCancel,title={Text(if(flow.stage==WorkoutLaunchStage.CHOOSE_LOCATION)"Scegli il luogo"else "Anteprima allenamento")},text={
            LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(vertical=8.dp)) {
                flow.errorMessage?.let{message->item{Text(message,color=MaterialTheme.colorScheme.error)}}
                if(flow.stage==WorkoutLaunchStage.CHOOSE_LOCATION){
                    if(locations.isEmpty())item{Text("Aggiungi un luogo dal Profilo.")}
                    items(locations,key={it.id}){place->
                        Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{locationId=place.id}){
                            Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                                RadioButton(locationId==place.id,{locationId=place.id})
                                Text(place.icon+" "+place.name,modifier=Modifier.weight(1f))
                                if(place.id==preferredLocationId)Text("Ultimo usato")
                            }
                        }
                    }
                } else {
                    item {
                        Text(when(flow.intent.origin){WorkoutOrigin.PLAN->"Allenamento del piano";WorkoutOrigin.ADAPTIVE_EXTRA->"Allenamento extra adattivo";WorkoutOrigin.SINGLE_EXERCISE->"Singolo esercizio • serie aggiuntive libere nel Runner";WorkoutOrigin.SKILL->"Allenamento skill";WorkoutOrigin.LIBRARY->"Workout dalla libreria";else->"Workout personalizzato"})
                        location?.let{Text("Luogo: "+it.name)}
                        Text("Tocca un esercizio per vedere come eseguirlo.")
                        if(!single && location!=null)TextButton(onClick={
                            var edited=flow
                            flow.workout?.exercises.orEmpty().forEachIndexed{index,saved->
                                if(!saved.pinned){
                                    val source=requireNotNull(saved.exerciseSnapshot)
                                    val pool=previewAlternatives(source,exercises,location,athlete,sessions,flow.intent)
                                    val candidate=regenerateCoachedWorkout(listOf(source),pool,emptySet(),coachingContext(sessions,readinessScore)).firstOrNull()
                                    if(candidate!=null){
                                        var fitted=candidate.copy(targetSets=saved.sets.size)
                                        while(fitted.targetSets>1 && estimatedExerciseSeconds(fitted)>estimatedExerciseSeconds(source))fitted=fitted.copy(targetSets=fitted.targetSets-1)
                                        if(estimatedExerciseSeconds(fitted)<=estimatedExerciseSeconds(source))edited=replace(edited,index,fitted)
                                    }
                                }
                            }
                            onFlowChanged(edited)
                        }){Text("RIGENERA NON BLOCCATI")}
                    }
                    if(flow.workout?.exercises.isNullOrEmpty())item{Text("Nessun esercizio disponibile. Modifica i filtri o il luogo.",color=MaterialTheme.colorScheme.error)}
                    itemsIndexed(flow.workout?.exercises.orEmpty(),key={_,e->e.originalOrder}){index,saved->
                        Card(Modifier.fillMaxWidth().padding(vertical=5.dp).clickable{detail=index}){
                            Column(Modifier.padding(14.dp)){
                                Text("${index+1}. ${saved.exerciseName}",style=MaterialTheme.typography.titleMedium)
                                saved.sets.forEach{set->
                                    Text("Serie ${set.index+1} • "+listOfNotNull(set.targetReps?.let{"$it rip."},set.targetSeconds?.let{"$it s"},set.targetDistanceMeters?.let{"$it m"},set.targetLoadKg?.let{"carico suggerito $it kg"},set.targetAssistanceKg?.let{"assistenza suggerita $it kg"}).joinToString(" • "))
                                }
                                Text("Recupero: ${saved.setRestSec} s tra serie • ${saved.exerciseRestSec} s tra esercizi")
                                if(!single)Row {
                                    TextButton(onClick={onFlowChanged(pinPreviewExercise(flow,index))}){Text(if(saved.pinned)"SBLOCCA"else "BLOCCA")}
                                    TextButton(onClick={replacing=index},enabled=!saved.pinned&&location!=null){Text("SOSTITUISCI")}
                                    TextButton(onClick={onFlowChanged(removePreviewExercise(flow,index))},enabled=!saved.pinned){Text("ELIMINA")}
                                }
                            }
                        }
                    }
                }
            }
        },confirmButton={Button(onClick={if(flow.stage==WorkoutLaunchStage.CHOOSE_LOCATION)locations.firstOrNull{it.id==locationId}?.let(onChooseLocation)else onStart()},
            enabled=if(flow.stage==WorkoutLaunchStage.CHOOSE_LOCATION)locations.any{it.id==locationId}else flow.workout?.exercises?.isNotEmpty()==true,
            modifier=Modifier.fillMaxWidth()){Text(if(flow.stage==WorkoutLaunchStage.CHOOSE_LOCATION)"CONTINUA"else "AVVIA")}},
            dismissButton={TextButton(onClick=onCancel){Text(if(flow.stage==WorkoutLaunchStage.PREVIEW&&!single)"CAMBIA LUOGO"else "ANNULLA")}})
        detail?.let{index->flow.workout?.exercises?.getOrNull(index)?.exerciseSnapshot?.let{ex->
            ExerciseDetailScreen(ex,sessions,{detail=null},{},{detail=null},actionLabel="TORNA ALL'ANTEPRIMA")
        }}
        replacing?.let{index->
            val source=flow.workout?.exercises?.getOrNull(index)?.exerciseSnapshot
            val alternatives=if(source!=null&&location!=null)previewAlternatives(source,exercises,location,athlete,sessions,flow.intent)else emptyList()
            FlowScreen(onDismissRequest={replacing=null},title={Text("Sostituisci esercizio")},text={LazyColumn(Modifier.fillMaxSize()){
                if(alternatives.isEmpty())item{Text("Nessuna alternativa compatibile. Gli altri esercizi restano invariati.")}
                items(alternatives,key={it.name}){ex->TextButton(onClick={onFlowChanged(replace(flow,index,ex));replacing=null},modifier=Modifier.fillMaxWidth()){Text(ex.name)}}
            }},confirmButton={TextButton(onClick={replacing=null}){Text("CHIUDI")}})
        }
    }
}
