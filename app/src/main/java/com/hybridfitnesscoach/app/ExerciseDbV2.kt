package com.hybridfitnesscoach.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class ExerciseV2Record(
    val id:String,
    val canonicalNameEn:String,
    val nameIt:String,
    val aliases:List<String>,
    val movementType:String,
    val family:String,
    val equipment:List<String>,
    val level:String?,
    val measure:String,
    val sources:List<String>,
    val confidence:String
) {
    fun lookupNames():Set<String>=(listOf(canonicalNameEn,nameIt)+aliases).map{it.trim().lowercase()}.filter{it.isNotBlank()}.toSet()
}

internal object ExerciseDbV2 {
    fun parseExercises(json:String):List<ExerciseV2Record> {
        val a=JSONArray(json)
        return (0 until a.length()).map { i ->
            val o=a.getJSONObject(i)
            ExerciseV2Record(
                id=o.getString("id"),
                canonicalNameEn=o.getString("canonicalNameEn"),
                nameIt=o.optString("nameIt").ifBlank{o.getString("canonicalNameEn")},
                aliases=o.optJSONArray("aliases").strings(),
                movementType=o.optString("movementType"),
                family=o.optString("family"),
                equipment=o.optJSONArray("equipment").strings(),
                level=o.optString("level").takeIf{it.isNotBlank()&&it!="null"},
                measure=o.optString("measure","REPS"),
                sources=o.optJSONArray("sources").objects().mapNotNull{it.optString("source_id").takeIf(String::isNotBlank)},
                confidence=o.optString("confidence")
            )
        }
    }

    fun load(context:Context):List<ExerciseV2Record> =
        context.assets.open("exercise-db-v2/exercises.json").bufferedReader().use{parseExercises(it.readText())}

    fun uniqueLookup(records:List<ExerciseV2Record>):Map<String,ExerciseV2Record> {
        val candidates=linkedMapOf<String,MutableList<ExerciseV2Record>>()
        records.forEach { r -> r.lookupNames().forEach { candidates.getOrPut(it){mutableListOf()}.add(r) } }
        return candidates.mapNotNull { (k,v) -> v.distinctBy{it.id}.singleOrNull()?.let{k to it} }.toMap()
    }

    fun resolveLegacy(name:String,records:List<ExerciseV2Record>):ExerciseV2Record? =
        uniqueLookup(records)[name.trim().lowercase()]

    fun toLegacyExercise(record:ExerciseV2Record,legacy:Exercise?=null):Exercise {
        val measure=runCatching{ExerciseMeasure.valueOf(record.measure)}.getOrDefault(legacy?.measure?:ExerciseMeasure.REPS)
        val category=when(record.movementType){
            "CORE"->ExerciseCategory.CORE
            "AEROBIC","CONDITIONING"->ExerciseCategory.CARDIO
            "MOBILITY","FLEXIBILITY","SMR"->ExerciseCategory.MOBILITY
            "SKILL"->ExerciseCategory.SKILL
            else->legacy?.category?:ExerciseCategory.OTHER
        }
        return Exercise(
            name=record.nameIt,
            group=legacy?.group?:record.family.replace('_',' ').lowercase().replaceFirstChar{it.uppercase()},
            equipment=record.equipment.firstOrNull()?.let(::equipmentDisplay)?:legacy?.equipment?:"Corpo libero",
            level=record.level?:legacy?.level?:"Intermedio",
            skills=legacy?.skills?:emptyList(),
            role=legacy?.role?:"Base",
            setRestSec=legacy?.setRestSec?:recommendedSetRest("Base",record.nameIt,record.equipment.firstOrNull()?:"Corpo libero"),
            exerciseRestSec=legacy?.exerciseRestSec?:recommendedExerciseRest("Base",record.nameIt,record.equipment.firstOrNull()?:"Corpo libero"),
            secondaryGroups=legacy?.secondaryGroups?:emptyList(),
            movementPattern=legacy?.movementPattern?:"",
            category=category,
            intensity=legacy?.intensity?:5,
            systemicFatigue=legacy?.systemicFatigue?:5,
            technicalComplexity=legacy?.technicalComplexity?:5,
            measure=measure,
            laterality=legacy?.laterality?:Laterality.BILATERAL,
            resistanceMode=legacy?.resistanceMode?:ResistanceMode.NONE,
            progressionKey=record.id,
            targetSets=legacy?.targetSets?:3,
            targetValue=legacy?.targetValue,
            intervalRestSec=legacy?.intervalRestSec
        )
    }

    private fun equipmentDisplay(token:String)=token.replace('_',' ').lowercase().replaceFirstChar{it.uppercase()}
}

private fun JSONArray?.strings():List<String> =
    if(this==null) emptyList() else (0 until length()).mapNotNull{optString(it).takeIf(String::isNotBlank)}
private fun JSONArray?.objects():List<JSONObject> =
    if(this==null) emptyList() else (0 until length()).mapNotNull{optJSONObject(it)}
