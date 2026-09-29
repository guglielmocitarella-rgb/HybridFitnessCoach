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
    val confidence:String,
    val descriptionIt:String="",
    val setupIt:String="",
    val executionStepsIt:List<String> = emptyList()
) {
    fun lookupNames():Set<String> =
        (listOf(canonicalNameEn,nameIt)+aliases)
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
}

internal data class ExerciseV2Edge(val from:String,val to:String,val relationship:String,val sources:List<String>)
internal data class ExerciseV2Source(val id:String,val title:String,val evidenceType:String?,val locator:String?)
internal data class ExerciseV2Guidance(val id:String,val sourceId:String,val scope:String,val guidance:Map<String,IntRange>)

internal object ExerciseDbV2 {
    private var installed:List<ExerciseV2Record> = emptyList()
    fun install(records:List<ExerciseV2Record>) { installed=records }
    fun recordFor(exercise:Exercise):ExerciseV2Record? =
        exercise.progressionKey?.let { id -> installed.firstOrNull { it.id==id } }
            ?: resolveLegacy(exercise.name,installed)
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
                level=if(o.isNull("level")) null else o.optString("level").takeIf{it.isNotBlank()},
                measure=o.optString("measure","REPS"),
                descriptionIt=if(o.isNull("descriptionIt")) "" else o.optString("descriptionIt"),
                setupIt=if(o.isNull("setupIt")) "" else o.optString("setupIt"),
                executionStepsIt=o.optJSONArray("executionStepsIt").strings(),
                sources=o.optJSONArray("sources").objects().mapNotNull{it.optString("source_id").takeIf(String::isNotBlank)},
                confidence=o.optString("confidence")
            )
        }
    }

    fun load(context:Context):List<ExerciseV2Record> =
        context.assets.open("exercise-db-v2/exercises.json").bufferedReader().use{parseExercises(it.readText())}

    fun loadSources(context:Context):Map<String,ExerciseV2Source> =
        context.assets.open("exercise-db-v2/sources.json").bufferedReader().use { reader ->
            val a=JSONArray(reader.readText())
            (0 until a.length()).map{i->a.getJSONObject(i)}.associate { o ->
                val id=o.getString("source_id")
                id to ExerciseV2Source(id,o.optString("title").ifBlank{o.optString("name").ifBlank{id}},
                    o.optString("evidence_type").takeIf{it.isNotBlank()},o.optString("locator").takeIf{it.isNotBlank()})
            }
        }

    fun loadProgrammingGuidance(context:Context):List<ExerciseV2Guidance> =
        context.assets.open("exercise-db-v2/programming_guidance.json").bufferedReader().use { reader ->
            val root=JSONObject(reader.readText()); val a=root.optJSONArray("rules")?:JSONArray()
            (0 until a.length()).map{i->a.getJSONObject(i)}.map { o ->
                val g=o.getJSONObject("guidance"); val values=mutableMapOf<String,IntRange>()
                g.keys().forEach { key ->
                    val v=g.get(key)
                    when(v){ is JSONArray -> if(v.length()>=2) values[key]=v.getInt(0)..v.getInt(1); is Number -> values[key]=v.toInt()..v.toInt() }
                }
                ExerciseV2Guidance(o.getString("id"),o.getString("source_id"),o.getString("scope"),values)
            }
        }

    fun loadProgressions(context:Context):List<ExerciseV2Edge> =
        context.assets.open("exercise-db-v2/progressions.json").bufferedReader().use { reader ->
            val a=JSONArray(reader.readText())
            (0 until a.length()).map { i -> a.getJSONObject(i) }.map { o ->
                ExerciseV2Edge(o.getString("from"),o.getString("to"),o.getString("relationship"),
                    o.optJSONArray("sources").objects().mapNotNull{it.optString("source_id").takeIf(String::isNotBlank)})
            }
        }

    fun uniqueLookup(records:List<ExerciseV2Record>):Map<String,ExerciseV2Record> {
        val candidates=linkedMapOf<String,MutableList<ExerciseV2Record>>()
        records.forEach { r -> r.lookupNames().forEach { candidates.getOrPut(it){mutableListOf()}.add(r) } }
        return candidates.mapNotNull { (k,v) -> v.distinctBy{it.id}.singleOrNull()?.let{k to it} }.toMap()
    }

    fun resolveLegacy(name:String,records:List<ExerciseV2Record>):ExerciseV2Record? =
        uniqueLookup(records)[name.trim().lowercase()]

    fun toLegacyExercise(record:ExerciseV2Record,legacy:Exercise?=null):Exercise {
        val measure=runCatching{ExerciseMeasure.valueOf(record.measure)}.getOrDefault(legacy?.measure?:ExerciseMeasure.REPS)
        val family=record.family.uppercase()
        val category=when {
            record.movementType=="CORE" || "CORE" in family || "L_SIT" in family -> ExerciseCategory.CORE
            record.movementType in setOf("AEROBIC","CONDITIONING") || family=="CARDIO" -> ExerciseCategory.CARDIO
            record.movementType in setOf("MOBILITY","FLEXIBILITY","SMR") || "MOBILITY" in family || family=="STRETCH" -> ExerciseCategory.MOBILITY
            record.movementType=="SKILL" || family in setOf("PLANCHE","FRONT_LEVER","BACK_LEVER","HANDSTAND","HUMAN_FLAG","MUSCLE_UP","SKILL") -> ExerciseCategory.SKILL
            family.contains("PULL") || family in setOf("BACK","BICEPS","UPRIGHT_ROW","SHRUG","PULLOVER","REAR_DELTOID") -> ExerciseCategory.PULL
            family.contains("PUSH") || family in setOf("CHEST","CHEST_FLY","DIP","TRICEPS","SHOULDER","HANDSTAND_PUSH_UP") -> ExerciseCategory.PUSH
            family in setOf("LEG","SQUAT","HINGE","LOWER_BODY","SINGLE_LEG_SQUAT","NORDIC_HAMSTRING","LUNGE","STEP_UP","CALF","POSTERIOR_CHAIN") -> ExerciseCategory.LEGS
            else -> legacy?.category?:ExerciseCategory.OTHER
        }
        val group=legacy?.group?:when(category){
            ExerciseCategory.PULL->"Dorso"
            ExerciseCategory.PUSH->"Petto"
            ExerciseCategory.LEGS->"Gambe"
            ExerciseCategory.CORE->"Core"
            ExerciseCategory.CARDIO->"Cardio"
            ExerciseCategory.MOBILITY->"Mobilità"
            ExerciseCategory.SKILL->"Skill"
            else->record.family.replace('_',' ').lowercase().replaceFirstChar{it.uppercase()}
        }
        return withExerciseMetadata(Exercise(
            name=record.nameIt,
            group=group,
            equipment=if(record.equipment.isNotEmpty()) record.equipment.joinToString(" / "){equipmentDisplay(it)} else legacy?.equipment?:"Non specificato",
            level=record.level?:legacy?.level?:"Intermedio",
            skills=legacy?.skills?:emptyList(),
            role=legacy?.role?:"Base",
            setRestSec=legacy?.setRestSec?:90,
            exerciseRestSec=legacy?.exerciseRestSec?:120,
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
        ))
    }

    private fun equipmentDisplay(token:String)=when(token.lowercase()) {
        "barbell" -> "Bilanciere"
        "dumbbell" -> "Manubri"
        "kettlebell" -> "Kettlebell"
        "cable","cable_machine" -> "Cavi"
        "machine" -> "Macchine"
        "pull_up_bar" -> "Sbarra"
        "dip_bars","parallettes" -> "Parallele"
        "rings" -> "Anelli"
        "resistance_band" -> "Elastici"
        "ab_wheel" -> "Ab Wheel"
        "stability_ball" -> "Fitball"
        "medicine_ball" -> "Palla medica"
        "battle_ropes","rope","rope_or_towel" -> "Corda"
        "box" -> "Box"
        "bosu" -> "Bosu"
        "ez_bar" -> "Bilanciere EZ"
        "towel" -> "Asciugamano"
        else -> token.replace('_',' ').lowercase().replaceFirstChar{it.uppercase()}
    }
}

private fun JSONArray?.strings():List<String> =
    if(this==null) emptyList() else (0 until length()).mapNotNull{optString(it).takeIf(String::isNotBlank)}
private fun JSONArray?.objects():List<JSONObject> =
    if(this==null) emptyList() else (0 until length()).mapNotNull{optJSONObject(it)}
