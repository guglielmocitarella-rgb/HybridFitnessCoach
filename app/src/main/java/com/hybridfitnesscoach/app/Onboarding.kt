package com.hybridfitnesscoach.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import org.json.JSONObject
import org.json.JSONArray

internal data class OnboardingDraft(val step:Int=0,val trainingDays:Set<Int> = setOf(1,3,5),val planId:String="",val weeks:Int=8) {
    fun validated():OnboardingDraft {
        require(step in 0..6 && weeks in 1..52 && trainingDays.all{it in 1..7})
        return this
    }
}

@Composable
internal fun OnboardingScreen(store:AppStore,onExit:()->Unit,onComplete:()->Unit) {
    var draft by remember{mutableStateOf(store.loadOnboardingDraft())}
    var profile by rememberSaveable(stateSaver=athleteDraftSaver()){mutableStateOf(store.loadAthlete())}
    var error by remember{mutableStateOf(false)}
    var ageInput by rememberSaveable{mutableStateOf(profile.age.takeIf{it>0}?.toString().orEmpty())}
    var weightInput by rememberSaveable{mutableStateOf(profile.weightKg.takeIf{it>0}?.toString().orEmpty())}
    var heightInput by rememberSaveable{mutableStateOf(profile.heightCm.takeIf{it>0}?.toString().orEmpty())}
    fun syncEssentials(){profile=profile.copy(age=(ageInput.toIntOrNull()?:0).coerceAtLeast(0),
        weightKg=weightInput.toFloatOrNull()?.takeIf{it.isFinite() && it>=0} ?: 0f,
        heightCm=heightInput.toFloatOrNull()?.takeIf{it.isFinite() && it>=0} ?: 0f)}
    var locationEditor by remember{mutableStateOf<TrainingLocation?>(null)}
    val titles=listOf("Atleta","Obiettivo","Capacità facoltative","Primo luogo","Health Connect facoltativo","Primo programma")
    fun advance(next:Int) {
        syncEssentials()
        val candidate=draft.copy(step=next)
        if(store.saveAthlete(profile) && store.saveOnboardingDraft(candidate)){draft=candidate;error=false}else error=true
    }
    fun finish(createPlan:Boolean) {
        if(completeOnboarding(store,profile,draft,createPlan))onComplete()else {draft=store.loadOnboardingDraft();error=true}
    }
    fun exit() {syncEssentials();if(store.saveAthlete(profile) && store.saveOnboardingDraft(draft))onExit()else error=true}
    FlowScreen(onDismissRequest=::exit,title={Text("${draft.step+1}/6 • ${titles[draft.step.coerceAtMost(5)]}")},text={LazyColumn(Modifier.fillMaxSize()){item{
        if(error)Text("Salvataggio non riuscito. I dati già salvati sono conservati: riprova.",color=MaterialTheme.colorScheme.error)
        when(draft.step) {
            0->{
                Text("Configura il profilo. Puoi completare i dati facoltativi in seguito.")
                OutlinedTextField(profile.name,{profile=profile.copy(name=it)},label={Text("Nome")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(ageInput,{ageInput=it.filter(Char::isDigit)},label={Text("Età (facoltativa)")})
                OutlinedTextField(weightInput,{weightInput=it.replace(",",".").filter{ch->ch.isDigit() || ch=='.'}},label={Text("Peso kg (facoltativo)")})
                OutlinedTextField(heightInput,{heightInput=it.replace(",",".").filter{ch->ch.isDigit() || ch=='.'}},label={Text("Altezza cm (facoltativa)")})
            }
            1->{
                Text("Obiettivo del profilo e degli Extra. Ogni piano avrà la propria configurazione.")
                listOf("Forza","Ipertrofia","Calisthenics","Fitness generale").forEach{goal->FilterChip(profile.goal==goal,{profile=profile.copy(goal=goal,prioritySkills=emptyList())},{Text(goal)})}
                if(profile.goal=="Calisthenics")calisthenicsSkills.forEach{skill->FilterChip(skill.name in profile.prioritySkills,{profile=profile.copy(prioritySkills=if(skill.name in profile.prioritySkills)profile.prioritySkills-skill.name else profile.prioritySkills+skill.name)},{Text(skill.name)})}
            }
            2->CapabilityEditor(profile,{profile=it})
            3->{
                Text("Scegli un luogo e verifica gli attrezzi disponibili. Potrai cambiarlo all'avvio di ogni workout.")
                store.loadTrainingLocations().forEach{place->OutlinedButton(onClick={locationEditor=place},modifier=Modifier.fillMaxWidth()){Text(place.icon+" "+place.name+" • CONFIGURA")}}
                Text("Attivo: "+store.loadTrainingLocations().firstOrNull{it.id==store.loadActiveTrainingLocationId()}?.name.orEmpty())
            }
            4->{Text("Il coach funziona anche senza Health Connect. Puoi collegarlo ora o dal Profilo.");HealthConnectCard()}
            5->{
                Text("Frequenza: ${draft.trainingDays.size} giorni a settimana")
                WeekdayChoices(draft.trainingDays,{days->draft=draft.copy(trainingDays=days);profile=profile.copy(daysPerWeek=days.size.coerceAtLeast(1));error=!store.saveOnboardingDraft(draft)})
                Text("Durata: ${draft.weeks} settimane")
                Slider(draft.weeks.toFloat(),{draft=draft.copy(weeks=it.toInt());store.saveOnboardingDraft(draft)},valueRange=1f..52f,steps=50)
                Text("Rivedi: ${profile.goal} • ${draft.trainingDays.size} giorni • ${draft.weeks} settimane")
                if(profile.prioritySkills.isNotEmpty())Text("Skill: "+profile.prioritySkills.joinToString())
                Text("Il programma contiene sessioni da materializzare nel luogo scelto all'avvio. Nessuna prestazione viene registrata durante la configurazione.")
            }
        }
    }}},confirmButton={Button(onClick={if(draft.step==5)finish(true)else advance(draft.step+1)},enabled=draft.step!=5 || draft.trainingDays.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text(if(draft.step==5)"CREA E ATTIVA PIANO"else "SALVA E CONTINUA")}},dismissButton={
        if(draft.step>0)TextButton(onClick={advance(draft.step-1)}){Text("PASSO PRECEDENTE")}
        if(draft.step==5)TextButton(onClick={finish(false)}){Text("TERMINA SENZA PIANO")}
        TextButton(onClick=::exit){Text("RIPRENDI IN SEGUITO")}
    })
    locationEditor?.let{place->TrainingLocationScreen(place,{locationEditor=null}){updated->
        if(store.saveTrainingLocations(store.loadTrainingLocations().map{if(it.id==place.id)updated else it}) && store.saveActiveTrainingLocationId(updated.id))locationEditor=null else error=true
    }}
}

@Composable
internal fun WeekdayChoices(selected:Set<Int>,onChange:(Set<Int>)->Unit) {
    listOf(listOf(1,2,3,4),listOf(5,6,7)).forEach{row->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            row.forEach{day->FilterChip(day in selected,{onChange(if(day in selected)selected-day else selected+day)},
                {Text(listOf("Lun","Mar","Mer","Gio","Ven","Sab","Dom")[day-1])},modifier=Modifier.weight(1f))}
        }
    }
}

internal fun athleteDraftSaver()=Saver<AthleteProfile,String>(save={profile->
    JSONObject().put("name",profile.name).put("age",profile.age).put("height",profile.heightCm).put("weight",profile.weightKg).put("sex",profile.sex)
        .put("goal",profile.goal).put("days",profile.daysPerWeek).put("skills",JSONArray(profile.prioritySkills)).apply{
            CapabilityDimension.entries.forEach{dimension->profile.toCapabilityProfile().levelFor(dimension)?.let{put(dimension.name,it)}}
        }.toString()
},restore={raw->val o=JSONObject(raw);val skills=o.getJSONArray("skills")
    var profile=AthleteProfile(name=o.getString("name"),age=o.getInt("age"),heightCm=o.getDouble("height").toFloat(),weightKg=o.getDouble("weight").toFloat(),sex=o.getString("sex"),goal=o.getString("goal"),daysPerWeek=o.getInt("days"),prioritySkills=(0 until skills.length()).map{skills.getString(it)})
    CapabilityDimension.entries.forEach{dimension->profile=profile.withCapability(dimension,o.optInt(dimension.name).takeIf{it in 1..3})};profile
})

/** Persist identity before plan writes so interrupted completion is safe to retry. */
internal fun completeOnboarding(store:AppStore,profile:AthleteProfile,draft:OnboardingDraft,createPlan:Boolean):Boolean {
    var identified=draft
    if(createPlan) {
        if(draft.trainingDays.isEmpty())return false
        val id=draft.planId.ifBlank{store.loadOnboardingDraft().planId.ifBlank{"plan-onboarding-"+java.util.UUID.randomUUID()}}
        identified=draft.copy(planId=id)
        if(!store.saveOnboardingDraft(identified))return false
        val existing=store.loadTrainingPlans().firstOrNull{it.id==id}
        val config=PlanConfiguration(profile.goal,draft.weeks,draft.trainingDays.size,draft.trainingDays,if(profile.goal=="Calisthenics")profile.prioritySkills else emptyList()).validated()
        val plan=existing ?: generateTrainingPlan(id,java.time.LocalDate.now().toEpochDay(),config)
        if(!store.saveTrainingPlan(plan) || !store.setTrainingPlanEnabled(id,true))return false
    }
    return store.saveAthlete(profile) && store.saveOnboardingDraft(identified.copy(step=6))
}
