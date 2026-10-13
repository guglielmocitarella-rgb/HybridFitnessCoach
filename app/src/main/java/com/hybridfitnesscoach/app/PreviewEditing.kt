package com.hybridfitnesscoach.app

internal fun previewAlternatives(
    source:Exercise,catalogue:List<Exercise>,place:TrainingLocation,athlete:AthleteProfile,
    sessions:List<SessionRecord>,intent:WorkoutLaunchIntent
):List<Exercise> {
    val focus=intent.focusSkills
    val allowed=customWorkoutCandidatePool(place,athlete,sessions,
        if(focus.isNotEmpty())"Skill"else "Parti del corpo",focus,intent.groupFilters,
        catalogue=catalogue,equipmentFilters=intent.equipmentFilters).map{it.name}.toSet()
    return catalogue.filter{ex->
        ex.name!=source.name && locationCompatible(ex,place) &&
        levelScore(ex.level)<=capabilityLevelFor(ex,athlete.toCapabilityProfile()) &&
        ex.category==source.category && ex.role==source.role &&
        (source.movementPattern.isBlank() || ex.movementPattern==source.movementPattern) &&
        (ex.group==source.group || ex.secondaryGroups.any{it==source.group} || source.secondaryGroups.any{it==ex.group}) &&
        ex.name in allowed
    }.sortedBy{it.name}
}

internal fun replacePreviewExercise(flow:WorkoutLaunchFlow,index:Int,replacement:Exercise):WorkoutLaunchFlow {
    require(flow.stage==WorkoutLaunchStage.PREVIEW)
    val state=requireNotNull(flow.workout)
    val old=state.exercises.getOrNull(index) ?: return flow
    if(old.pinned || flow.intent.origin==WorkoutOrigin.SINGLE_EXERCISE)return flow
    val updated=plannedExerciseState(index,replacement).copy(originalOrder=old.originalOrder,rpe=5)
    return flow.copy(workout=state.copy(exercises=state.exercises.mapIndexed{i,e->if(i==index)updated else e}))
}

internal fun removePreviewExercise(flow:WorkoutLaunchFlow,index:Int):WorkoutLaunchFlow {
    require(flow.stage==WorkoutLaunchStage.PREVIEW)
    val state=requireNotNull(flow.workout)
    if(state.exercises.getOrNull(index)?.pinned!=false || flow.intent.origin==WorkoutOrigin.SINGLE_EXERCISE)return flow
    return flow.copy(workout=state.copy(exercises=state.exercises.filterIndexed{i,_->i!=index}.mapIndexed{i,e->e.copy(currentOrder=i)},currentExerciseIndex=0))
}

internal fun pinPreviewExercise(flow:WorkoutLaunchFlow,index:Int):WorkoutLaunchFlow {
    require(flow.stage==WorkoutLaunchStage.PREVIEW)
    val state=requireNotNull(flow.workout)
    return flow.copy(workout=state.copy(exercises=state.exercises.mapIndexed{i,e->if(i==index)e.copy(pinned=!e.pinned)else e}))
}
