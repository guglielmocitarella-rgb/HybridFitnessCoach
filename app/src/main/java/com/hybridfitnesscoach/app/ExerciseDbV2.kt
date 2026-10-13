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
    val sourceRefs:List<ExerciseV2EvidenceRef> = emptyList(),
    val descriptionIt:String="",
    val setupIt:String="",
    val executionStepsIt:List<String> = emptyList(),
    val executionEvidence:String="UNKNOWN"
) {
    fun lookupNames():Set<String> =
        (listOf(canonicalNameEn,nameIt)+aliases)
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
}

internal data class ExerciseV2EvidenceRef(val sourceId:String,val evidenceType:String?,val locator:String?)
internal data class ExerciseV2Edge(val from:String,val to:String,val relationship:String,val sources:List<String>)
internal data class ExerciseV2Source(val id:String,val title:String,val evidenceType:String?,val locator:String?)
internal data class ExerciseV2Guidance(val id:String,val sourceId:String,val scope:String,val guidance:Map<String,IntRange>)
internal data class ExerciseV2ProgressionRelations(val easier:List<ExerciseV2Record>,val harder:List<ExerciseV2Record>)

internal object ExerciseDbV2 {
    private var installed:List<ExerciseV2Record> = emptyList()
    private var sourceRegistry:Map<String,ExerciseV2Source> = emptyMap()
    private var progressionRegistry:List<ExerciseV2Edge> = emptyList()
    private var programmingGuidance:List<ExerciseV2Guidance> = emptyList()
    fun install(records:List<ExerciseV2Record>) { installed=records }
    fun installedRecords():List<ExerciseV2Record> = installed
    fun installSources(sources:Map<String,ExerciseV2Source>) { sourceRegistry=sources }
    fun installProgressions(edges:List<ExerciseV2Edge>) { progressionRegistry=edges }
    fun installProgrammingGuidance(rules:List<ExerciseV2Guidance>) { programmingGuidance=rules }
    fun installedProgrammingGuidance():List<ExerciseV2Guidance> = programmingGuidance
    fun sourceById(id:String):ExerciseV2Source? = sourceRegistry[id]
    fun progressionRelations(exercise:Exercise):ExerciseV2ProgressionRelations {
        val record=recordFor(exercise) ?: return ExerciseV2ProgressionRelations(emptyList(),emptyList())
        val byId=installed.associateBy{it.id}
        val main=progressionRegistry.filter{it.relationship=="MAIN_PROGRESSION"}
        val easier=main.filter{it.to==record.id}.mapNotNull{byId[it.from]}.distinctBy{it.id}
        val harder=main.filter{it.from==record.id}.mapNotNull{byId[it.to]}.distinctBy{it.id}
        return ExerciseV2ProgressionRelations(easier,harder)
    }
    fun sourcesFor(exercise:Exercise):List<ExerciseV2Source> {
        val record=recordFor(exercise) ?: return emptyList()
        val refs=record.sourceRefs.associateBy { it.sourceId }
        return record.sources.mapNotNull { id ->
            sourceRegistry[id]?.let { source ->
                refs[id]?.let { ref -> source.copy(evidenceType=ref.evidenceType?:source.evidenceType, locator=ref.locator?:source.locator) } ?: source
            }
        }
    }
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
                executionEvidence=o.optString("executionEvidence","UNKNOWN"),
                sources=o.optJSONArray("sources").objects().mapNotNull{it.optString("source_id").takeIf(String::isNotBlank)},
                sourceRefs=o.optJSONArray("sources").objects().mapNotNull { source ->
                    source.optString("source_id").takeIf(String::isNotBlank)?.let { id ->
                        ExerciseV2EvidenceRef(id,source.optString("evidence_type").takeIf(String::isNotBlank),source.optString("locator").takeIf(String::isNotBlank))
                    }
                },
                confidence=o.optString("confidence")
            )
        }
    }

    fun load(context:Context):List<ExerciseV2Record> =
        context.assets.open("exercise-db-v2/exercises.json").bufferedReader().use{parseExercises(it.readText())}

    fun parseSources(json:String):Map<String,ExerciseV2Source> {
        val a=JSONArray(json)
        return (0 until a.length()).map{i->a.getJSONObject(i)}.associate { o ->
            val id=o.getString("source_id")
            id to ExerciseV2Source(id,o.optString("title").ifBlank{o.optString("name").ifBlank{id}},
                o.optString("evidence_type").takeIf{it.isNotBlank()},o.optString("locator").takeIf{it.isNotBlank()})
        }
    }

    fun loadSources(context:Context):Map<String,ExerciseV2Source> =
        context.assets.open("exercise-db-v2/sources.json").bufferedReader().use { parseSources(it.readText()) }

    fun parseProgrammingGuidance(json:String):List<ExerciseV2Guidance> {
        val root=JSONObject(json); val a=root.optJSONArray("rules")?:JSONArray()
        return (0 until a.length()).map{i->a.getJSONObject(i)}.map { o ->
            val g=o.getJSONObject("guidance"); val values=mutableMapOf<String,IntRange>()
            g.keys().forEach { key ->
                val v=g.get(key)
                when(v){ is JSONArray -> if(v.length()>=2) values[key]=v.getInt(0)..v.getInt(1); is Number -> values[key]=v.toInt()..v.toInt() }
            }
            ExerciseV2Guidance(o.getString("id"),o.getString("source_id"),o.getString("scope"),values)
        }
    }

    fun loadProgrammingGuidance(context:Context):List<ExerciseV2Guidance> =
        context.assets.open("exercise-db-v2/programming_guidance.json").bufferedReader().use { parseProgrammingGuidance(it.readText()) }

    fun parseProgressions(json:String):List<ExerciseV2Edge> {
        val a=JSONArray(json)
        return (0 until a.length()).map { i -> a.getJSONObject(i) }.map { o ->
            ExerciseV2Edge(o.getString("from"),o.getString("to"),o.getString("relationship"),
                o.optJSONArray("sources").objects().mapNotNull{it.optString("source_id").takeIf(String::isNotBlank)})
        }
    }

    fun loadProgressions(context:Context):List<ExerciseV2Edge> =
        context.assets.open("exercise-db-v2/progressions.json").bufferedReader().use { parseProgressions(it.readText()) }

    fun uniqueLookup(records:List<ExerciseV2Record>):Map<String,ExerciseV2Record> {
        val candidates=linkedMapOf<String,MutableList<ExerciseV2Record>>()
        records.forEach { r -> r.lookupNames().forEach { candidates.getOrPut(it){mutableListOf()}.add(r) } }
        return candidates.mapNotNull { (k,v) -> v.distinctBy{it.id}.singleOrNull()?.let{k to it} }.toMap()
    }

    fun resolveLegacy(name:String,records:List<ExerciseV2Record>):ExerciseV2Record? =
        uniqueLookup(records)[name.trim().lowercase()]

    fun toLegacyExercise(record:ExerciseV2Record,legacy:Exercise?=null):Exercise {
        // DB v2 owns identity, execution evidence and equipment. Sets/reps/rest below
        // are operational APP_RULE baselines adapted later by the coaching layer.
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
            name=legacy?.name?:record.nameIt,
            group=group,
            equipment=if(record.equipment.isNotEmpty()) record.equipment.joinToString(" / "){EquipmentCatalog.displayNameForToken(it)} else legacy?.equipment?:"Non specificato",
            level=record.level?:legacy?.level?:"Non valutato",
            skills=legacy?.skills?:emptyList(),
            role=legacy?.role?:"Base",
            setRestSec=legacy?.setRestSec?:recommendedSetRest(legacy?.role?:"Base",record.nameIt,record.equipment.joinToString("/")),
            exerciseRestSec=legacy?.exerciseRestSec?:recommendedExerciseRest(legacy?.role?:"Base",record.nameIt,record.equipment.joinToString("/")),
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

}

private fun JSONArray?.strings():List<String> =
    if(this==null) emptyList() else (0 until length()).mapNotNull{optString(it).takeIf(String::isNotBlank)}
private fun JSONArray?.objects():List<JSONObject> =
    if(this==null) emptyList() else (0 until length()).mapNotNull{optJSONObject(it)}
