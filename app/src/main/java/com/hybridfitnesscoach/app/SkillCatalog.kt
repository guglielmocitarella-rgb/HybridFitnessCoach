package com.hybridfitnesscoach.app

/** Canonical v0.9 calisthenics progression graphs beyond the initial Front Lever / Planche coverage. */
object SkillCatalog {
    private val initialNodes: List<SkillNode> = listOf(
        // Pull-up -> Muscle-up
        SkillNode("pullup_strict", "Pull-up", "Muscle-up", criterion = MasteryCriterion(minSets = 3, minReps = 8), exerciseAliases = setOf("Trazioni prone")),
        SkillNode("pullup_chest", "Chest-to-bar Pull-up", "Muscle-up", setOf("pullup_strict"), MasteryCriterion(minSets = 3, minReps = 6)),
        SkillNode("muscleup_negative", "Muscle-up Negative", "Muscle-up", setOf("pullup_chest"), MasteryCriterion(minSets = 3, minReps = 3), setOf("Muscle-up negativo")),
        SkillNode("muscleup_band", "Band-assisted Muscle-up", "Muscle-up", setOf("muscleup_negative"), MasteryCriterion(minSets = 3, minReps = 5)),
        SkillNode("muscleup_strict", "Muscle-up", "Muscle-up", setOf("muscleup_band"), MasteryCriterion(minSets = 3, minReps = 3)),

        // Handstand
        SkillNode("handstand_wall", "Wall Handstand", "Handstand", criterion = MasteryCriterion(minSets = 3, minSeconds = 30)),
        SkillNode("handstand_wall_shoulder_tap", "Wall Handstand Shoulder Tap", "Handstand", setOf("handstand_wall"), MasteryCriterion(minSets = 3, minReps = 8), setOf("Handstand shoulder taps")),
        SkillNode("handstand_freestanding", "Freestanding Handstand", "Handstand", setOf("handstand_wall_shoulder_tap"), MasteryCriterion(minSets = 3, minSeconds = 20)),

        // Back Lever
        SkillNode("backlever_tuck", "Tuck Back Lever", "Back Lever", criterion = MasteryCriterion(minSets = 3, minSeconds = 15)),
        SkillNode("backlever_advanced_tuck", "Advanced Tuck Back Lever", "Back Lever", setOf("backlever_tuck"), MasteryCriterion(minSets = 3, minSeconds = 15)),
        SkillNode("backlever_straddle", "Straddle Back Lever", "Back Lever", setOf("backlever_advanced_tuck"), MasteryCriterion(minSets = 3, minSeconds = 10)),
        SkillNode("backlever_full", "Back Lever", "Back Lever", setOf("backlever_straddle"), MasteryCriterion(minSets = 3, minSeconds = 8)),

        // Human Flag
        SkillNode("flag_vertical", "Vertical Flag Hold", "Human Flag", criterion = MasteryCriterion(minSets = 3, minSeconds = 15)),
        SkillNode("flag_tuck", "Tuck Human Flag", "Human Flag", setOf("flag_vertical"), MasteryCriterion(minSets = 3, minSeconds = 10)),
        SkillNode("flag_straddle", "Straddle Human Flag", "Human Flag", setOf("flag_tuck"), MasteryCriterion(minSets = 3, minSeconds = 8)),
        SkillNode("flag_full", "Human Flag", "Human Flag", setOf("flag_straddle"), MasteryCriterion(minSets = 3, minSeconds = 6)),

        // L-sit -> V-sit
        SkillNode("lsit_tuck", "Tuck L-sit", "L-sit / V-sit", criterion = MasteryCriterion(minSets = 3, minSeconds = 20), exerciseAliases = setOf("L-sit tuck")),
        SkillNode("lsit_full", "L-sit", "L-sit / V-sit", setOf("lsit_tuck"), MasteryCriterion(minSets = 3, minSeconds = 20)),
        SkillNode("vsit_compression", "V-sit Compression", "L-sit / V-sit", setOf("lsit_full"), MasteryCriterion(minSets = 3, minReps = 8)),
        SkillNode("vsit_hold", "V-sit", "L-sit / V-sit", setOf("vsit_compression"), MasteryCriterion(minSets = 3, minSeconds = 8)),

        // Pistol squat
        SkillNode("pistol_box", "Box Pistol Squat", "Pistol Squat", criterion = MasteryCriterion(minSets = 3, minReps = 8)),
        SkillNode("pistol_assisted", "Assisted Pistol Squat", "Pistol Squat", setOf("pistol_box"), MasteryCriterion(minSets = 3, minReps = 8), setOf("Pistol squat assistito")),
        SkillNode("pistol_full", "Pistol Squat", "Pistol Squat", setOf("pistol_assisted"), MasteryCriterion(minSets = 3, minReps = 6))
    )

    private val aliases=mapOf(
        "Chest-to-bar Pull-up" to setOf("Chest to bar pull-up"),
        "Band-assisted Muscle-up" to setOf("Band assisted muscle-up"),
        "Freestanding Handstand" to setOf("Freestanding handstand hold"),
        "Tuck Back Lever" to setOf("Back lever tuck"),
        "Advanced Tuck Back Lever" to setOf("Back lever advanced tuck"),
        "Tuck Human Flag" to setOf("Human flag tuck"),
        "V-sit Compression" to setOf("Compression lift"),
        "Dip parallele" to setOf("Dip alle parallele"),
        "Trazioni chest-to-bar" to setOf("Chest to bar pull-up"),
        "Wall HSPU" to setOf("Wall handstand push-up"),
        "Dragon flag negative" to setOf("Dragon flag negativa"),
        "Assisted shrimp squat" to setOf("Shrimp squat assistito")
    )
    val nodes:List<SkillNode> = (initialNodes + skillProgressionNodes.filter { legacy ->
        initialNodes.none{it.skill.equals(legacy.skill,true)} && legacy.skill !in listOf("L-sit","V-sit")
    }.map { legacy ->
        val timed=legacy.skill in setOf("Front lever","Planche")
        SkillNode(legacy.id,legacy.label,legacy.skill,legacy.prerequisites.toSet(),
            MasteryCriterion(minReps=if(timed)null else 6,minSeconds=if(timed)15 else null))
    }).map{it.copy(exerciseAliases=it.exerciseAliases+aliases[it.exerciseName].orEmpty())}

    fun forSkill(skill: String): List<SkillNode> = nodes.filter { it.skill.equals(skill, ignoreCase = true) || (skill in listOf("L-sit","V-sit") && it.skill=="L-sit / V-sit") }
    fun skills(): List<String> = nodes.map { it.skill }.distinct()

    /** Evaluate prerequisites from completed sessions, never from a manually advanced level. */
    fun progress(skill: String, sessions: List<SessionRecord>): List<SkillNodeProgress> {
        val mastered = mutableSetOf<String>()
        return forSkill(skill).map { node ->
            ProgressionEngine.evaluate(node, mastered, sessions).also {
                if (it.status == ProgressionStatus.MASTERED) mastered += node.id
            }
        }
    }
}


/** Locked nodes are unavailable regardless of the self-reported athlete level. */
internal fun skillEligibleExercises(sessions:List<SessionRecord>):Set<String> {
    val states=SkillCatalog.skills().flatMap{SkillCatalog.progress(it,sessions)}
    val unlocked=states.filter{it.status!=ProgressionStatus.LOCKED}.flatMap{it.node.exerciseAliases+it.node.exerciseName}.map{it.lowercase()}.toSet()
    val locked=states.filter{it.status==ProgressionStatus.LOCKED}.flatMap{it.node.exerciseAliases+it.node.exerciseName}.map{it.lowercase()}.toSet()
    return exercises.filter{it.name.lowercase() !in locked || it.name.lowercase() in unlocked}.map{it.name}.toSet()
}
