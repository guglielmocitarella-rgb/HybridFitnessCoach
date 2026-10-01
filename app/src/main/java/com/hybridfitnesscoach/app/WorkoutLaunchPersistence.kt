package com.hybridfitnesscoach.app

import org.json.JSONArray
import org.json.JSONObject

internal fun resumeWorkoutLaunch(state:WorkoutSessionState):WorkoutLaunchFlow {
    val origin=WorkoutOrigin.valueOf(state.workoutOrigin)
    val intent=WorkoutLaunchIntent(origin,
        sourceExerciseId=if(origin==WorkoutOrigin.SINGLE_EXERCISE)state.exercises.first().exerciseId?:state.exercises.first().exerciseName else null,
        plannedWorkoutId=state.plannedWorkoutId)
    return WorkoutLaunchFlow(intent,WorkoutLaunchStage.RUNNER,state.trainingLocationId,state)
}

/** Saved UI state retains the generated snapshot; restoration never calls a generator. */
internal fun WorkoutLaunchFlow.toLaunchJson():String = JSONObject()
    .put("origin",intent.origin.name).put("source",intent.sourceExerciseId).put("planned",intent.plannedWorkoutId)
    .put("skill",intent.focusSkill).put("equipment",JSONArray(intent.equipmentFilters)).put("groups",JSONArray(intent.groupFilters))
    .put("locked",intent.lockFilters).put("stage",stage.name).put("location",locationId)
    .put("workout",workout?.toJson()).put("error",errorMessage).toString()

internal fun workoutLaunchFromJson(raw:String):WorkoutLaunchFlow {
    val o=JSONObject(raw)
    fun optional(key:String)=if(o.isNull(key))null else o.getString(key)
    fun strings(key:String)=o.getJSONArray(key).let{a->(0 until a.length()).map{a.getString(it)}}
    val intent=WorkoutLaunchIntent(WorkoutOrigin.valueOf(o.getString("origin")),optional("source"),optional("planned"),
        optional("skill"),strings("equipment"),strings("groups"),o.getBoolean("locked"))
    val recovery=StoreRecovery()
    val state=if(o.isNull("workout"))null else workoutSessionFromJson(o.getJSONObject("workout"),recovery)
    require(recovery.issues.isEmpty())
    return WorkoutLaunchFlow(intent,WorkoutLaunchStage.valueOf(o.getString("stage")),optional("location"),state,optional("error"))
}
