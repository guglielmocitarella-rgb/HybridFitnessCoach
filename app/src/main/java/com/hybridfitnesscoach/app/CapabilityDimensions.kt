package com.hybridfitnesscoach.app

data class CapabilityProfile(
    val trainingExperience:Int? = null,
    val pushCapacity:Int? = null,
    val pullCapacity:Int? = null,
    val horizontalPullCapacity:Int? = null,
    val verticalPushCapacity:Int? = null,
    val squatCapacity:Int? = null,
    val hingeCapacity:Int? = null,
    val coreCapacity:Int? = null,
    val isometricCapacity:Int? = null,
    val skillCapacity:Int? = null
)
internal fun AthleteProfile.toCapabilityProfile()=CapabilityProfile(
    trainingExperience=assessedTrainingExperience?.takeIf{it in 1..3},
    pushCapacity=assessedPushCapacity?.takeIf{it in 1..3},
    pullCapacity=assessedPullCapacity?.takeIf{it in 1..3},
    horizontalPullCapacity=assessedHorizontalPullCapacity?.takeIf{it in 1..3},
    verticalPushCapacity=assessedVerticalPushCapacity?.takeIf{it in 1..3},
    squatCapacity=assessedSquatCapacity?.takeIf{it in 1..3},
    hingeCapacity=assessedHingeCapacity?.takeIf{it in 1..3},
    coreCapacity=assessedCoreCapacity?.takeIf{it in 1..3},
    isometricCapacity=assessedIsometricCapacity?.takeIf{it in 1..3},
    skillCapacity=assessedSkillCapacity?.takeIf{it in 1..3}
)

enum class CapabilityDimension(val label:String) {
    EXPERIENCE("Esperienza generale"),HORIZONTAL_PUSH("Spinta orizzontale"),HORIZONTAL_PULL("Tirata orizzontale"),
    VERTICAL_PUSH("Spinta verticale"),VERTICAL_PULL("Tirata verticale"),SQUAT("Gambe / squat"),
    HINGE("Catena posteriore / hip hinge"),CORE("Core"),ISOMETRIC("Isometriche"),SKILL("Skill / calisthenics")
}

internal fun CapabilityProfile.levelFor(d:CapabilityDimension):Int? = when(d){
    CapabilityDimension.EXPERIENCE->trainingExperience
    CapabilityDimension.HORIZONTAL_PUSH->pushCapacity // The existing push-up assessment measures this axis.
    CapabilityDimension.VERTICAL_PULL->pullCapacity // The existing pull-up assessment measures this axis.
    CapabilityDimension.HORIZONTAL_PULL->horizontalPullCapacity
    CapabilityDimension.VERTICAL_PUSH->verticalPushCapacity
    CapabilityDimension.SQUAT->squatCapacity
    CapabilityDimension.HINGE->hingeCapacity
    CapabilityDimension.CORE->coreCapacity
    CapabilityDimension.ISOMETRIC->isometricCapacity
    CapabilityDimension.SKILL->skillCapacity
}

/** APP_RULE: difficulty clearance is per movement; an unassessed dimension is conservative. */
internal fun capabilityLevelFor(ex:Exercise,capability:CapabilityProfile):Int {
    val pattern=ex.movementPattern.replace('_','-')
    val required=buildSet {
        when {
            pattern=="horizontal-pull"->add(CapabilityDimension.HORIZONTAL_PULL)
            pattern=="vertical-pull"->add(CapabilityDimension.VERTICAL_PULL)
            pattern=="vertical-push"->add(CapabilityDimension.VERTICAL_PUSH)
            pattern=="horizontal-push"->add(CapabilityDimension.HORIZONTAL_PUSH)
            pattern=="hinge"->add(CapabilityDimension.HINGE)
            pattern=="knee-dominant"->add(CapabilityDimension.SQUAT)
            pattern in setOf("core-control","anti-extension","anti-rotation","loaded-carry")->add(CapabilityDimension.CORE)
            ex.category==ExerciseCategory.LEGS->add(CapabilityDimension.SQUAT)
            ex.category==ExerciseCategory.CORE->add(CapabilityDimension.CORE)
            ex.category==ExerciseCategory.PUSH->add(CapabilityDimension.HORIZONTAL_PUSH)
            ex.category==ExerciseCategory.PULL->add(CapabilityDimension.VERTICAL_PULL)
        }
        if(ex.category==ExerciseCategory.SKILL)add(CapabilityDimension.SKILL)
        if(ex.measure==ExerciseMeasure.SECONDS)add(CapabilityDimension.ISOMETRIC)
        if(isEmpty())add(CapabilityDimension.EXPERIENCE)
    }
    return required.minOf{capability.levelFor(it)?.takeIf{value->value in 1..3} ?: 1}
}

internal fun AthleteProfile.withCapability(d:CapabilityDimension,level:Int?):AthleteProfile {
    require(level==null || level in 1..3)
    return when(d){
        CapabilityDimension.EXPERIENCE->copy(assessedTrainingExperience=level)
        CapabilityDimension.HORIZONTAL_PUSH->copy(assessedPushCapacity=level)
        CapabilityDimension.VERTICAL_PULL->copy(assessedPullCapacity=level)
        CapabilityDimension.HORIZONTAL_PULL->copy(assessedHorizontalPullCapacity=level)
        CapabilityDimension.VERTICAL_PUSH->copy(assessedVerticalPushCapacity=level)
        CapabilityDimension.SQUAT->copy(assessedSquatCapacity=level)
        CapabilityDimension.HINGE->copy(assessedHingeCapacity=level)
        CapabilityDimension.CORE->copy(assessedCoreCapacity=level)
        CapabilityDimension.ISOMETRIC->copy(assessedIsometricCapacity=level)
        CapabilityDimension.SKILL->copy(assessedSkillCapacity=level)
    }
}

/** Catalog difficulty, not a physiological threshold. Assessment is self-reported controlled execution. */
internal fun capabilityAssessmentExamples(d:CapabilityDimension,catalogue:List<Exercise>):List<Exercise> =
    catalogue.filter{ex->when(d){
        CapabilityDimension.HORIZONTAL_PUSH->ex.movementPattern=="horizontal-push"
        CapabilityDimension.HORIZONTAL_PULL->ex.movementPattern=="horizontal-pull"
        CapabilityDimension.VERTICAL_PUSH->ex.movementPattern=="vertical-push"
        CapabilityDimension.VERTICAL_PULL->ex.movementPattern=="vertical-pull"
        CapabilityDimension.SQUAT->ex.category==ExerciseCategory.LEGS && ex.movementPattern!="hinge"
        CapabilityDimension.HINGE->ex.movementPattern=="hinge"
        CapabilityDimension.CORE->ex.category==ExerciseCategory.CORE
        CapabilityDimension.ISOMETRIC->ex.measure==ExerciseMeasure.SECONDS
        CapabilityDimension.SKILL->ex.category==ExerciseCategory.SKILL
        else->false
    }}.groupBy{levelScore(it.level)}.toSortedMap().values.map{xs->xs.sortedBy{it.name}.first()}
