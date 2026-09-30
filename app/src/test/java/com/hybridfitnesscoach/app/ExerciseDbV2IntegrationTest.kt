package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class ExerciseDbV2IntegrationTest {
    private fun asset(name:String):java.io.File =
        listOf(java.io.File("src/main/assets/exercise-db-v2/$name"), java.io.File("app/src/main/assets/exercise-db-v2/$name")).first { it.exists() }

    private fun records():List<ExerciseV2Record> =
        ExerciseDbV2.parseExercises(asset("exercises.json").readText())

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
        val snapshot=Exercise("Legacy snapshot","Core","Corpo libero","Principiante",targetSets=7,targetValue=13.0)
        installExerciseDbV2(records())
        assertTrue(exercises.size>=587)
        assertEquals(587,exercises.mapNotNull{it.progressionKey}.distinct().size)
        assertTrue(exercises.any{it.progressionKey==null})
        assertEquals(7,snapshot.targetSets)
        assertEquals(13.0,snapshot.targetValue!!,0.001)
    }
    @Test fun runtimeCatalogPreservesLegacySkillAndPrescriptionMetadataWhenNamesResolve() {
        val db=records()
        val legacy=exercises.first { candidate ->
            (candidate.skills.isNotEmpty() || candidate.role!="Base" || candidate.targetValue!=null) &&
                ExerciseDbV2.resolveLegacy(candidate.name,db)!=null
        }
        val resolved=ExerciseDbV2.resolveLegacy(legacy.name,db)
        assertNotNull(resolved)
        val mapped=ExerciseDbV2.toLegacyExercise(resolved!!,legacy)
        assertEquals(legacy.skills,mapped.skills)
        assertEquals(legacy.role,mapped.role)
        assertEquals(legacy.setRestSec,mapped.setRestSec)
        assertEquals(legacy.exerciseRestSec,mapped.exerciseRestSec)
        assertEquals(legacy.targetSets,mapped.targetSets)
        assertEquals(legacy.targetValue,mapped.targetValue)
        assertEquals(resolved.id,mapped.progressionKey)
    }

    @Test fun runtimeInstallKeepsSkillTreeExecutableAgainstCanonicalCatalog() {
        val previous=exercises
        try {
            installExerciseDbV2(records())
            assertTrue(exercises.size>=587)
            SkillCatalog.nodes.forEach { node ->
                assertTrue(
                    "Missing runtime skill exercise: ${node.exerciseName}",
                    exercises.any { ex -> (node.exerciseAliases+node.exerciseName).any { it.equals(ex.name,true) } }
                )
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
    @Test fun exerciseSpecificEvidenceLocatorSurvivesParsing() {
        val row=records().first{it.sourceRefs.any{ref->!ref.locator.isNullOrBlank()}}
        val ref=row.sourceRefs.first{!it.locator.isNullOrBlank()}
        assertTrue(ref.sourceId in row.sources)
        assertFalse(ref.evidenceType.isNullOrBlank())
        assertFalse(ref.locator.isNullOrBlank())
    }

    @Test fun exerciseProvenanceRemainsAvailableToDomainWhenPresentationHidesIt() {
        val db=records()
        val sources=ExerciseDbV2.parseSources(asset("sources.json").readText())
        val row=db.first{it.sourceRefs.any{ref->!ref.locator.isNullOrBlank() && ref.sourceId in sources}}
        val previous=ExerciseDbV2.installedRecords()
        try {
            ExerciseDbV2.install(db)
            ExerciseDbV2.installSources(sources)
            val mapped=ExerciseDbV2.toLegacyExercise(row)
            val runtimeSources=ExerciseDbV2.sourcesFor(mapped)
            assertTrue(runtimeSources.isNotEmpty())
            assertTrue(runtimeSources.any{!it.locator.isNullOrBlank()})
            assertTrue(row.sources.isNotEmpty())
        } finally {
            ExerciseDbV2.install(previous)
        }
    }

    @Test fun sourceRegistryAndProgrammingGuidanceAreConsumableWithoutInventingExerciseSpecificRules() {
        val sources=ExerciseDbV2.parseSources(asset("sources.json").readText())
        val guidance=ExerciseDbV2.parseProgrammingGuidance(asset("programming_guidance.json").readText())
        assertEquals(14,sources.size)
        assertEquals(17,guidance.size)
        assertTrue(guidance.all{it.sourceId in sources})
        assertTrue(guidance.any{it.scope=="STRENGTH"})
    }
    @Test fun equipmentFilterOptionsMatchCanonicalCatalogTaxonomyAndAreAlphabetical() {
        val canonical=records().map { ExerciseDbV2.toLegacyExercise(it) }
        val options=exerciseEquipmentOptions(canonical)
        assertEquals(options.sortedBy{it.lowercase()},options)
        assertTrue("Fitball" in options)
        assertTrue("Palla medica" in options)
        assertTrue("Bilanciere EZ" in options)
        assertFalse(options.any{"/" in it})
        assertFalse("Non specificato" in options)
    }

    @Test fun profileEquipmentOptionsCoverEveryKnownCanonicalEquipmentAndAreAlphabetical() {
        val canonical=records().map { ExerciseDbV2.toLegacyExercise(it) }
        val catalog=exerciseEquipmentOptions(canonical)
        val profile=profileEquipmentOptions(canonical)
        assertEquals(profile.sortedBy{it.lowercase()},profile)
        assertTrue(profile.containsAll(catalog))
        assertEquals(profile.distinct(),profile)
    }

    @Test fun installedRuntimeCatalogExposesTheSameCanonicalEquipmentTaxonomy() {
        val previous=exercises
        try {
            installExerciseDbV2(records())
            val runtime=exerciseEquipmentOptions(exercises)
            val canonical=exerciseEquipmentOptions(records().map { ExerciseDbV2.toLegacyExercise(it) })
            assertTrue(runtime.containsAll(canonical))
        } finally {
            exercises=previous
        }
    }

    @Test fun everyCanonicalEquipmentTokenRoundTripsThroughSelectableProfileAndCompatibility() {
        val canonical=records().map { ExerciseDbV2.toLegacyExercise(it) }
        val options=profileEquipmentOptions(canonical)
        canonical.filter{!it.equipment.equals("Non specificato",true)}.forEach { ex ->
            val required=ex.equipment.split("/").map(String::trim).filter(String::isNotBlank)
            required.forEach { token ->
                assertTrue("Missing selectable equipment token '$token' for ${ex.name}",token in options)
            }
            assertTrue(
                "Selecting every displayed requirement must make ${ex.name} compatible: ${ex.equipment}",
                equipmentCompatible(ex,EquipmentProfile("Contract","",required))
            )
        }
    }

}
