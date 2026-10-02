package com.hybridfitnesscoach.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CapabilityEditor(profile:AthleteProfile,onChange:(AthleteProfile)->Unit) {
    var open by remember{mutableStateOf<CapabilityDimension?>(CapabilityDimension.EXPERIENCE)}
    Text("Capacità indipendenti",style=MaterialTheme.typography.titleMedium)
    Text("Valutazione facoltativa: seleziona il livello che esegui con controllo. I livelli sono categorie del catalogo, non soglie fisiologiche. Ogni area non valutata resta conservativa.")
    CapabilityDimension.entries.forEach{dimension->
        val selected=profile.toCapabilityProfile().levelFor(dimension)
        Column(Modifier.fillMaxWidth().padding(vertical=8.dp)) {
            TextButton(onClick={open=if(open==dimension)null else dimension}){Text(dimension.label+" • "+(selected?.let{listOf("Principiante","Intermedio","Avanzato")[it-1]}?:"Non valutato"))}
            if(open==dimension){
            FilterChip(selected==null,{onChange(profile.withCapability(dimension,null))},{Text("Non valutato")})
            val examples=capabilityAssessmentExamples(dimension,exercises)
            (1..3).forEach{level->
                val label=listOf("Principiante","Intermedio","Avanzato")[level-1]
                val example=examples.firstOrNull{levelScore(it.level)==level}?.name
                FilterChip(selected==level,{onChange(profile.withCapability(dimension,level))},
                    {Text(label+(example?.let{" • $it"}?:""))})
            }
            }
        }
    }
}
