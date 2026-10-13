package com.hybridfitnesscoach.app

/** Fill legacy catalogue omissions without changing persisted exercise names. */
internal fun withExerciseMetadata(ex:Exercise):Exercise {
    val n=ex.name.lowercase()
    val category=if(ex.category!=ExerciseCategory.OTHER)ex.category else when {
        ex.role=="Progressione"->ExerciseCategory.SKILL
        ex.group.startsWith("Gambe")->ExerciseCategory.LEGS
        ex.group.startsWith("Core")->ExerciseCategory.CORE
        ex.group.startsWith("Dorso")||ex.group.startsWith("Bicipiti")->ExerciseCategory.PULL
        ex.group.startsWith("Cardio")->ExerciseCategory.CARDIO
        else->ExerciseCategory.PUSH
    }
    val pattern=ex.movementPattern.ifBlank{when {
        category==ExerciseCategory.MOBILITY->"mobility"
        category==ExerciseCategory.CARDIO->"conditioning"
        "deadlift" in n||"stacco" in n||"thrust" in n||"bridge" in n||"swing" in n->"hinge"
        category==ExerciseCategory.LEGS->"knee-dominant"
        category==ExerciseCategory.CORE||"sit" in n||"flag" in n->"core-control"
        "row" in n||"rematore" in n||"pulley" in n->"horizontal-pull"
        "trazioni" in n||"pull-up" in n||"lever" in n||"lat machine" in n->"vertical-pull"
        "handstand" in n||"pike" in n||"military" in n||"shoulder press" in n->"vertical-push"
        category==ExerciseCategory.PULL->"pull-accessory"
        else->"horizontal-push"
    }}
    val resistance=if(ex.resistanceMode!=ResistanceMode.NONE || category==ExerciseCategory.MOBILITY)ex.resistanceMode else when {
        "zavorrat" in n || ex.equipment.split(" / ").any{it in setOf("Bilanciere","Manubri","Cavi","Macchina","Macchine","Kettlebell","Leg press")}->ResistanceMode.EXTERNAL_LOAD
        "assist" in n && "Elastici" in ex.equipment->ResistanceMode.ASSISTED
        else->ResistanceMode.BODYWEIGHT
    }
    return ex.copy(category=category,movementPattern=pattern,resistanceMode=resistance,
        secondaryGroups=if(ex.secondaryGroups.isEmpty())ex.group.split(" / ").drop(1) else ex.secondaryGroups,
        technicalComplexity=when{category==ExerciseCategory.SKILL->3;ex.role=="Forza"->2;else->1})
}
