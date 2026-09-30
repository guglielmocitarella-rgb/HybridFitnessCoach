package com.hybridfitnesscoach.app
import org.junit.Assert.*
import org.junit.Test

class SkillIntegrationTest {
    @Test fun everySelectableSkillHasEvidenceGraph() {
        skillProgressionNodes.map{it.skill}.distinct().forEach { skill ->
            assertTrue("Missing graph: $skill",SkillCatalog.forSkill(skill).isNotEmpty())
            assertEquals(0,ProgressionEngine.skillProgress(SkillCatalog.progress(skill,emptyList())))
        }
    }
    @Test fun everyGraphNodeMapsToAnExecutableExercise() {
        SkillCatalog.nodes.forEach { node ->
            assertTrue("Missing exercise: ${node.exerciseName}",exercises.any{ex->(node.exerciseAliases+node.exerciseName).any{it.equals(ex.name,true)}})
        }
    }
    @Test fun newAthleteCannotGenerateLockedProgressions() {
        val gym=profiles.first{it.name=="Palestra"}
        val workout=generateWorkout(gym,AthleteProfile(goal="Calisthenics",level="Avanzato",prioritySkills=listOf("Front lever")),emptyList())
        assertFalse(workout.any{it.name=="Advanced tuck front lever"})
    }
    @Test fun catalogueHasConsistentMetadataAndNoDuplicateNames() {
        assertEquals(exercises.size,exercises.map{it.name.lowercase()}.distinct().size)
        assertTrue(exercises.all{it.movementPattern.isNotBlank()})
        assertTrue(exercises.all{it.category!=ExerciseCategory.OTHER})
        assertTrue(exercises.any{it.category==ExerciseCategory.MOBILITY && it.equipment=="Corpo libero"})
        assertEquals(ResistanceMode.EXTERNAL_LOAD,exercises.first{it.name=="Squat"}.resistanceMode)
    }
    @Test fun catalogHasBroadV09Coverage(){assertTrue(exercises.size>=200);assertEquals(exercises.size,exercises.map{it.name.lowercase()}.distinct().size);assertTrue(exercises.any{it.category==ExerciseCategory.CORE});assertTrue(exercises.any{it.category==ExerciseCategory.MOBILITY});assertTrue(exercises.any{it.category==ExerciseCategory.CARDIO});assertTrue(exercises.any{it.movementPattern=="loaded-carry"});assertTrue(exercises.any{it.role=="Conditioning"})}
    @Test fun canonicalSkillGraphsHaveUniqueIdsAndClosedPrerequisites() {
        assertEquals(SkillCatalog.nodes.size, SkillCatalog.nodes.map{it.id}.distinct().size)
        SkillCatalog.skills().forEach { skill ->
            val graph=SkillCatalog.forSkill(skill)
            val ids=graph.map{it.id}.toSet()
            assertTrue("Empty graph: $skill",graph.isNotEmpty())
            graph.forEach { node ->
                assertTrue("Foreign prerequisite in $skill / ${node.id}: ${node.prerequisites-ids}", ids.containsAll(node.prerequisites))
            }
        }
    }
    @Test fun canonicalSkillNamesDoNotDuplicateLegacyGraphsByCase() {
        assertEquals(
            SkillCatalog.skills().map{it.lowercase()}.distinct().size,
            SkillCatalog.skills().size
        )
        assertEquals(
            SkillCatalog.forSkill("Back Lever").map{it.id},
            SkillCatalog.forSkill("Back lever").map{it.id}
        )
        assertEquals(
            SkillCatalog.forSkill("Pistol Squat").map{it.id},
            SkillCatalog.forSkill("Pistol squat").map{it.id}
        )
    }

    @Test fun skillWorkoutCandidatePoolKeepsSkillPriorityAndCompatibleSupportExercises() {
        val gym=profiles.first{it.name=="Palestra"}
        val athlete=AthleteProfile(level="Avanzato")
        val pool=customWorkoutCandidatePool(gym,athlete,emptyList(),"Skill",listOf("Front lever"),emptyList())
        assertTrue(pool.isNotEmpty())
        assertTrue(pool.any{"Front lever" in it.skills})
        assertTrue(pool.all{equipmentCompatible(it,gym)})
        assertTrue(pool.all{it.name in skillEligibleExercises(emptyList())})
        assertTrue(pool.filterNot{"Front lever" in it.skills}.all{it.role in setOf("Forza","Prerequisito","Accessorio")})
    }
    @Test fun lockedSkillNodesNeverEnterCustomCandidatePoolForNewAthlete() {
        val gym=profiles.first{it.name=="Palestra"}
        val athlete=AthleteProfile(level="Avanzato")
        val pool=customWorkoutCandidatePool(gym,athlete,emptyList(),"Skill",listOf("Front lever"),emptyList())
        val locked=SkillCatalog.progress("Front lever",emptyList()).filter{it.status==ProgressionStatus.LOCKED}
            .flatMap{it.node.exerciseAliases+it.node.exerciseName}.map{it.lowercase()}.toSet()
        assertTrue(pool.none{it.name.lowercase() in locked})
    }

    @Test fun realHistoryMastersNodeAndUnlocksNextExerciseForGenerator() {
        val graph=SkillCatalog.forSkill("Muscle-up")
        val first=graph.first{it.id=="pullup_strict"}
        val next=graph.first{it.id=="pullup_chest"}
        fun qualifyingSession(ts:Long)=SessionRecord(
            timestamp=ts,profileName="Palestra",completedSets=3,totalSets=3,
            structuredExercises=listOf(
                WorkoutExerciseState(
                    exerciseName=first.exerciseName,rpe=8,
                    sets=(1..3).map{WorkoutSetState(it,targetReps=8,actualReps=8,completed=true)}
                )
            )
        )
        val none=SkillCatalog.progress("Muscle-up",emptyList())
        assertEquals(ProgressionStatus.AVAILABLE,none.first{it.node.id==first.id}.status)
        assertEquals(ProgressionStatus.LOCKED,none.first{it.node.id==next.id}.status)
        val one=SkillCatalog.progress("Muscle-up",listOf(qualifyingSession(1)))
        assertEquals(ProgressionStatus.IN_PROGRESS,one.first{it.node.id==first.id}.status)
        assertEquals(ProgressionStatus.LOCKED,one.first{it.node.id==next.id}.status)
        val history=listOf(qualifyingSession(1),qualifyingSession(2))
        val mastered=SkillCatalog.progress("Muscle-up",history)
        assertEquals(ProgressionStatus.MASTERED,mastered.first{it.node.id==first.id}.status)
        assertEquals(ProgressionStatus.AVAILABLE,mastered.first{it.node.id==next.id}.status)
        val eligible=skillEligibleExercises(history).map{it.lowercase()}.toSet()
        assertTrue((next.exerciseAliases+next.exerciseName).any{it.lowercase() in eligible})
    }

    @Test fun duplicateObservationsInsideOneSessionCannotFakeRequiredSessions() {
        val first=SkillCatalog.forSkill("Muscle-up").first{it.id=="pullup_strict"}
        val entry=WorkoutExerciseState(
            exerciseName=first.exerciseName,rpe=7,
            sets=(1..3).map{WorkoutSetState(it,actualReps=10,completed=true)}
        )
        val oneSession=SessionRecord(10,"Palestra",6,6,structuredExercises=listOf(entry,entry))
        val state=SkillCatalog.progress("Muscle-up",listOf(oneSession)).first{it.node.id==first.id}
        assertEquals(ProgressionStatus.IN_PROGRESS,state.status)
        assertEquals(0.5,state.completion,0.001)
    }

    @Test fun exerciseDetailLinksExposeRealSkillProgressAndNextUnlock() {
        val ex=exercises.first{it.skills.contains("Muscle-up")}
        val empty=exerciseSkillLinks(ex,emptyList()).first{it.skill=="Muscle-up"}
        assertEquals(0,empty.progressPercent)
        assertNotNull(empty.currentNode)
        assertTrue(empty.nextUnlock.isNotBlank())
        assertEquals(ex.skills.toSet(),exerciseSkillLinks(ex,emptyList()).map{it.skill}.toSet())
    }

    @Test fun baseExerciseWithoutSkillsHasNoSkillTreeNavigationLinks() {
        val ex=exercises.first{it.skills.isEmpty()}
        assertTrue(exerciseSkillLinks(ex,emptyList()).isEmpty())
    }


    @Test fun skillWorkoutTargetsCurrentUnlockedProgressionInsteadOfMasteredOrLockedNodes() {
        val gym=profiles.first{it.name=="Palestra"}
        val athlete=AthleteProfile(level="Avanzato")
        val first=SkillCatalog.forSkill("Muscle-up").first{it.id=="pullup_strict"}
        fun qualifyingSession(ts:Long)=SessionRecord(
            timestamp=ts,profileName="Palestra",completedSets=3,totalSets=3,
            structuredExercises=listOf(WorkoutExerciseState(
                exerciseName=first.exerciseName,rpe=7,
                sets=(1..3).map{WorkoutSetState(it,targetReps=8,actualReps=8,completed=true)}
            ))
        )
        val history=listOf(qualifyingSession(1),qualifyingSession(2))
        val states=SkillCatalog.progress("Muscle-up",history)
        val current=states.first{it.status==ProgressionStatus.AVAILABLE}
        val mastered=states.filter{it.status==ProgressionStatus.MASTERED}.flatMap{it.node.exerciseAliases+it.node.exerciseName}.map{it.lowercase()}.toSet()
        val locked=states.filter{it.status==ProgressionStatus.LOCKED}.flatMap{it.node.exerciseAliases+it.node.exerciseName}.map{it.lowercase()}.toSet()
        val pool=customWorkoutCandidatePool(gym,athlete,history,"Skill",listOf("Muscle-up"),emptyList())
        val skillWork=pool.filter{"Muscle-up" in it.skills}
        assertTrue((current.node.exerciseAliases+current.node.exerciseName).any{name->skillWork.any{it.name.equals(name,true)}})
        assertTrue(skillWork.none{it.name.lowercase() in mastered})
        assertTrue(skillWork.none{it.name.lowercase() in locked})
    }

    @Test fun skillWorkoutFocusPrefersInProgressNodeUntilItIsMastered() {
        val first=SkillCatalog.forSkill("Muscle-up").first{it.id=="pullup_strict"}
        val attempt=SessionRecord(
            timestamp=10,profileName="Palestra",completedSets=3,totalSets=3,
            structuredExercises=listOf(WorkoutExerciseState(
                exerciseName=first.exerciseName,rpe=7,
                sets=(1..3).map{WorkoutSetState(it,targetReps=8,actualReps=8,completed=true)}
            ))
        )
        val focus=skillWorkoutFocusNames(listOf("Muscle-up"),listOf(attempt)).map{it.lowercase()}.toSet()
        assertTrue((first.exerciseAliases+first.exerciseName).any{it.lowercase() in focus})
        val next=SkillCatalog.forSkill("Muscle-up").first{it.id=="pullup_chest"}
        assertFalse((next.exerciseAliases+next.exerciseName).any{it.lowercase() in focus})
    }


    @Test fun masteryThresholdsAreExplicitlyClassifiedAsAppRules() {
        SkillCatalog.nodes.forEach { node ->
            assertEquals("APP_RULE", node.criterion.evidenceClass)
            assertTrue(node.criterion.evidenceNote.isNotBlank())
        }
    }
}
