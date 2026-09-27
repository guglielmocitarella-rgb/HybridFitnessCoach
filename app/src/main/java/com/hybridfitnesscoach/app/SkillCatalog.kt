package com.hybridfitnesscoach.app

/** Canonical v0.9 calisthenics progression graphs beyond the initial Front Lever / Planche coverage. */
object SkillCatalog {
    val nodes: List<SkillNode> = listOf(
        // Pull-up -> Muscle-up
        SkillNode("pullup_strict", "Pull-up", "Muscle-up", criterion = MasteryCriterion(minSets = 3, minReps = 8)),
        SkillNode("pullup_chest", "Chest-to-bar Pull-up", "Muscle-up", setOf("pullup_strict"), MasteryCriterion(minSets = 3, minReps = 6)),
        SkillNode("muscleup_negative", "Muscle-up Negative", "Muscle-up", setOf("pullup_chest"), MasteryCriterion(minSets = 3, minReps = 3)),
        SkillNode("muscleup_band", "Band-assisted Muscle-up", "Muscle-up", setOf("muscleup_negative"), MasteryCriterion(minSets = 3, minReps = 5)),
        SkillNode("muscleup_strict", "Muscle-up", "Muscle-up", setOf("muscleup_band"), MasteryCriterion(minSets = 3, minReps = 3)),

        // Handstand
        SkillNode("handstand_wall", "Wall Handstand", "Handstand", criterion = MasteryCriterion(minSets = 3, minSeconds = 30)),
        SkillNode("handstand_wall_shoulder_tap", "Wall Handstand Shoulder Tap", "Handstand", setOf("handstand_wall"), MasteryCriterion(minSets = 3, minReps = 8)),
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
        SkillNode("lsit_tuck", "Tuck L-sit", "L-sit / V-sit", criterion = MasteryCriterion(minSets = 3, minSeconds = 20)),
        SkillNode("lsit_full", "L-sit", "L-sit / V-sit", setOf("lsit_tuck"), MasteryCriterion(minSets = 3, minSeconds = 20)),
        SkillNode("vsit_compression", "V-sit Compression", "L-sit / V-sit", setOf("lsit_full"), MasteryCriterion(minSets = 3, minReps = 8)),
        SkillNode("vsit_hold", "V-sit", "L-sit / V-sit", setOf("vsit_compression"), MasteryCriterion(minSets = 3, minSeconds = 8)),

        // Pistol squat
        SkillNode("pistol_box", "Box Pistol Squat", "Pistol Squat", criterion = MasteryCriterion(minSets = 3, minReps = 8)),
        SkillNode("pistol_assisted", "Assisted Pistol Squat", "Pistol Squat", setOf("pistol_box"), MasteryCriterion(minSets = 3, minReps = 8)),
        SkillNode("pistol_full", "Pistol Squat", "Pistol Squat", setOf("pistol_assisted"), MasteryCriterion(minSets = 3, minReps = 6))
    )

    fun forSkill(skill: String): List<SkillNode> = nodes.filter { it.skill.equals(skill, ignoreCase = true) }
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
