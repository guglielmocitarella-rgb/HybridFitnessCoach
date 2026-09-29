package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class ExerciseDbV2IntegrationTest {
    private fun asset(name:String):java.io.File =
        listOf(java.io.File("src/main/assets/exercise-db-v2/$name"), java.io.File("app/src/main/assets/exercise-db-v2/$name")).first { it.exists() }

    private fun records():List<ExerciseV2Record> =
        ExerciseDbV2.parseExercises(asset("exercises.json").readText())

    @Test fun packagedRuntimeAssetsLoadCanonicalExercisesAndSourceRegistry() {
        val context=RuntimeEnvironment.getApplication()
        assertEquals(records(),ExerciseDbV2.load(context))
        assertEquals(ExerciseDbV2.parseSources(asset("sources.json").readText()),ExerciseDbV2.loadSources(context))
    }

    @Test fun canonicalAssetLoadsWithStableUniqueIds() {
        val db=records()
        assertEquals(587,db.size)
        assertEquals(db.size,db.map{it.id}.distinct().size)
        assertTrue(db.all{it.id.isNotBlank() && it.nameIt.isNotBlank() && it.sources.isNotEmpty()})
    }

    @Test fun canonicalRecordsMapToExecutableAppExercises() {
        val mapped=records().map{ExerciseDbV2.toLegacyExercise(it)}
        assertEquals(587,mapped.size)
        assertTrue(mapped.all{it.name.isNotBlank() && it.progressionKey!=null})
    }

    @Test fun legacyResolutionIsFailClosedForUnknownNames() {
        val db=records()
        assertNull(ExerciseDbV2.resolveLegacy("definitely-not-a-real-exercise",db))
        val known=db.first()
        assertEquals(known.id,ExerciseDbV2.resolveLegacy(known.canonicalNameEn,db)?.id)
        assertEquals(known.id,ExerciseDbV2.resolveLegacy(known.nameIt,db)?.id)
    }
    @Test fun runtimeInstallUsesCanonicalIdsWithoutDestroyingLegacySnapshots() {
        val previous=exercises
        try {
            val db=records()
            val snapshot=Exercise("Legacy snapshot","Core","Corpo libero","Principiante",targetSets=7,targetValue=13.0)
            installExerciseDbV2(db)
            val installed=exercises
            installExerciseDbV2(db)
            assertEquals(installed,exercises)
            assertEquals(587,exercises.size)
            assertEquals(587,exercises.mapNotNull{it.progressionKey}.distinct().size)
            assertEquals(7,snapshot.targetSets)
            assertEquals(13.0,snapshot.targetValue!!,0.001)
        } finally {
            exercises=previous
        }
    }

    @Test fun emptyOrDuplicateCatalogueCannotSilentlyReplaceTheRuntimeCatalogue() {
        val previous=exercises
        try {
            val db=records()
            installExerciseDbV2(db)
            val installed=exercises
            assertTrue(runCatching { installExerciseDbV2(emptyList()) }.isFailure)
            assertEquals(installed,exercises)
            assertTrue(runCatching { installExerciseDbV2(listOf(db.first(),db.first())) }.isFailure)
            assertEquals(installed,exercises)
        } finally {
            exercises=previous
        }
    }
    @Test fun runtimeCatalogPreservesLegacySkillAndPrescriptionMetadataWhenNamesResolve() {
        val previous=exercises
        val db=records()
        try {
            installExerciseDbV2(db)
            var checked=0
            db.forEach { record ->
                val legacy=record.lookupNames().asSequence().mapNotNull { name ->
                    previous.firstOrNull { it.name.trim().lowercase()==name }
                }.firstOrNull()
                if (legacy!=null && (legacy.skills.isNotEmpty() || legacy.role!="Base" || legacy.targetValue!=null)) {
                    val mapped=exercises.single { it.progressionKey==record.id }
                    assertEquals(legacy.skills,mapped.skills)
                    assertEquals(legacy.role,mapped.role)
                    assertEquals(legacy.setRestSec,mapped.setRestSec)
                    assertEquals(legacy.exerciseRestSec,mapped.exerciseRestSec)
                    assertEquals(legacy.secondaryGroups,mapped.secondaryGroups)
                    assertEquals(legacy.movementPattern,mapped.movementPattern)
                    assertEquals(legacy.intensity,mapped.intensity)
                    assertEquals(legacy.systemicFatigue,mapped.systemicFatigue)
                    assertEquals(legacy.technicalComplexity,mapped.technicalComplexity)
                    assertEquals(legacy.laterality,mapped.laterality)
                    assertEquals(legacy.resistanceMode,mapped.resistanceMode)
                    assertEquals(legacy.targetSets,mapped.targetSets)
                    assertEquals(legacy.targetValue,mapped.targetValue)
                    assertEquals(legacy.intervalRestSec,mapped.intervalRestSec)
                    checked++
                }
            }
            assertTrue(checked>0)
        } finally {
            exercises=previous
        }
    }

    @Test fun runtimeInstallKeepsSkillTreeExecutableAgainstCanonicalCatalog() {
        val previous=exercises
        try {
            val db=records()
            installExerciseDbV2(db)
            assertEquals(587,exercises.size)
            SkillCatalog.nodes.forEach { node ->
                val record=(node.exerciseAliases+node.exerciseName)
                    .firstNotNullOfOrNull { ExerciseDbV2.resolveLegacy(it,db) }
                assertNotNull("No canonical exercise or DB alias for skill node: ${node.exerciseName}",record)
                assertTrue("Uninstalled canonical exercise for skill node: ${node.exerciseName}",
                    exercises.any { it.progressionKey==record!!.id })
            }
        } finally {
            exercises=previous
        }
    }

    @Test fun canonicalEquipmentMapsIntoProfileTaxonomyAndGeneratorModel() {
        val db=records()
        val barbell=db.first{ "barbell" in it.equipment }
        val mapped=ExerciseDbV2.toLegacyExercise(barbell)
        assertEquals("Bilanciere",mapped.equipment)
        assertTrue(equipmentCompatible(mapped,EquipmentProfile("Test","",listOf("Bilanciere"))))
        assertNotEquals(ResistanceMode.NONE,mapped.resistanceMode)
    }

    @Test fun bodyweightLegacyMatchRemainsAvailableAfterDbV2Installation() {
        val previous=exercises
        try {
            val db=records()
            installExerciseDbV2(db)
            val row=db.first { it.canonicalNameEn.equals("Push-Up",true) }
            val mapped=exercises.single { it.progressionKey==row.id }
            assertEquals("Corpo libero",mapped.equipment)
            assertTrue(equipmentCompatible(mapped,EquipmentProfile("Home","",emptyList())))
        } finally {
            exercises=previous
        }
    }

    @Test fun legacyActiveWorkoutNameResolvesFromLegacyCatalogButUnknownStableIdDoesNotFallback() {
        val previous=exercises
        try {
            val legacy=legacyExerciseSnapshotForName("Trazioni prone")!!
            installExerciseDbV2(records())
            val saved=WorkoutExerciseState(
                exerciseName=legacy.name,
                sets=listOf(WorkoutSetState(0,targetReps=6),WorkoutSetState(1,targetReps=6)),
                setRestSec=legacy.setRestSec,
                exerciseRestSec=legacy.exerciseRestSec
            )
            assertEquals(listOf(legacy.copy(targetSets=2,targetValue=6.0)),restoreExercises(WorkoutSessionState("legacy",1,"Home",listOf(saved))))
            assertTrue(restoreExercises(WorkoutSessionState("missing-id",1,"Home",listOf(saved.copy(exerciseId="removed-db-id")))).isEmpty())
        } finally {
            exercises=previous
        }
    }

    @Test fun sourceBackedProgressionGraphHasReferentialIntegrityForRuntimeIds() {
        val ids=records().map{it.id}.toSet()
        val f=listOf(java.io.File("src/main/assets/exercise-db-v2/progressions.json"),java.io.File("app/src/main/assets/exercise-db-v2/progressions.json")).first{it.exists()}
        val edges=ExerciseDbV2.parseProgressions(asset("progressions.json").readText())
        assertEquals(113,edges.size)
        assertTrue(edges.all{it.from in ids && it.to in ids && it.sources.isNotEmpty()})
        assertTrue(edges.any{it.relationship=="MAIN_PROGRESSION"})
    }

    @Test fun canonicalResistanceRecordRemainsCompatibleWithPrescriptionEngine() {
        val mapped=records().asSequence().map{ExerciseDbV2.toLegacyExercise(it)}
            .first{it.measure==ExerciseMeasure.REPS && it.resistanceMode!=ResistanceMode.NONE &&
                it.category in setOf(ExerciseCategory.PUSH,ExerciseCategory.PULL,ExerciseCategory.LEGS,ExerciseCategory.CORE)}
        val context=PrescriptionContext("Forza","Intermedio",mapped.category,5,5,5,null,80,null)
        assertNotNull(SourcePrescriptionBaselineRegistry.baselineFor(mapped,context))
    }
    @Test fun emptyEquipmentIsUnknownAndFailsClosedForConstrainedGeneration() {
        val row=records().first{it.equipment.isEmpty()}
        val mapped=ExerciseDbV2.toLegacyExercise(row)
        assertEquals("Non specificato",mapped.equipment)
        assertFalse(equipmentCompatible(mapped,EquipmentProfile("Home","",emptyList())))
    }

    @Test fun multiTokenEquipmentPreservesAllRequiredResources() {
        val row=records().first{it.equipment.size>1}
        val mapped=ExerciseDbV2.toLegacyExercise(row)
        assertTrue(mapped.equipment.contains(" / "))
        val all=EquipmentProfile("All","",mapped.equipment.split(" / "))
        assertTrue(equipmentCompatible(mapped,all))
        assertFalse(equipmentCompatible(mapped,EquipmentProfile("Partial","",listOf(mapped.equipment.substringBefore(" / ")))))
    }

    @Test fun bodyweightDoesNotHideMissingEquipmentInMultiRequirement() {
        val hybrid=Exercise("Hybrid movement","Test","Corpo libero / Bilanciere","Intermedio")
        assertFalse(equipmentCompatible(hybrid,EquipmentProfile("Partial","",emptyList())))
        assertTrue(equipmentCompatible(hybrid,EquipmentProfile("Complete","",listOf("Bilanciere"))))
    }

    @Test fun canonicalEquipmentAliasesNormalizeToTheSameProfileRequirement() {
        val row=records().first { "cable" in it.equipment || "cable_machine" in it.equipment }
        val mapped=ExerciseDbV2.toLegacyExercise(row)
        assertTrue(equipmentCompatible(mapped,EquipmentProfile("Canonical","",listOf("cable_machine"))))
        assertTrue(equipmentCompatible(mapped,EquipmentProfile("Localized","",listOf("Cavi"))))
    }

    @Test fun ropeOrTowelRequirementAcceptsEitherExplicitAlternative() {
        val row=records().first { "rope_or_towel" in it.equipment }
        val mapped=ExerciseDbV2.toLegacyExercise(row)
        assertTrue(equipmentCompatible(mapped,EquipmentProfile("Rope","",listOf("rope"))))
        assertTrue(equipmentCompatible(mapped,EquipmentProfile("Towel","",listOf("towel"))))
        assertFalse(equipmentCompatible(mapped,EquipmentProfile("Neither","",emptyList())))
    }

    @Test fun unknownCanonicalEquipmentCannotBeMadeAvailableByMatchingItsLabel() {
        val unknown=ExerciseDbV2.toLegacyExercise(records().first().copy(equipment=listOf("unregistered_equipment")))
        assertEquals("Non specificato",unknown.equipment)
        assertFalse(equipmentCompatible(unknown,EquipmentProfile("Unknown","",listOf("unregistered equipment","Non specificato"))))
    }

    @Test fun workoutGenerationIsIndependentOfCanonicalCatalogueOrdering() {
        val previous=exercises
        try {
            installExerciseDbV2(records())
            val profile=profiles.first { it.name=="Palestra" }
            val athlete=AthleteProfile(goal="Forza + Skill Calisthenics",level="Intermedio")
            val first=generateWorkout(profile,athlete,emptyList())
            exercises=exercises.reversed()
            val reversed=generateWorkout(profile,athlete,emptyList())
            assertEquals(first,reversed)
            assertEquals(6,first.size)
            assertTrue(first.all { equipmentCompatible(it,profile) && it.progressionKey!=null })
        } finally {
            exercises=previous
        }
    }
    @Test fun sourceRegistryAndProgrammingGuidanceAreConsumableWithoutInventingExerciseSpecificRules() {
        val sources=ExerciseDbV2.parseSources(asset("sources.json").readText())
        val guidance=ExerciseDbV2.parseProgrammingGuidance(asset("programming_guidance.json").readText())
        val db=records()
        ExerciseDbV2.install(db)
        ExerciseDbV2.installSources(sources)
        assertEquals(14,sources.size)
        assertEquals(17,guidance.size)
        assertTrue(guidance.all{it.sourceId in sources})
        assertTrue(guidance.any{it.scope=="STRENGTH"})
        val sample=db.first()
        assertEquals(sample.sources.toSet(),ExerciseDbV2.sourcesFor(ExerciseDbV2.toLegacyExercise(sample)).map{it.id}.toSet())
        assertTrue(runCatching { ExerciseDbV2.installSources(emptyMap()) }.isFailure)
        assertEquals(sample.sources.toSet(),ExerciseDbV2.sourcesFor(ExerciseDbV2.toLegacyExercise(sample)).map{it.id}.toSet())
    }
}
