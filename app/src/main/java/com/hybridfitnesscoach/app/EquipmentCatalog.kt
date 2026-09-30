package com.hybridfitnesscoach.app

/** Canonical equipment capability IDs. Localized labels are presentation only. */
internal enum class EquipmentId(val token:String,val labelIt:String) {
    BODYWEIGHT("bodyweight","Corpo libero"),
    AB_WHEEL("ab_wheel","Ab Wheel"),
    BARBELL("barbell","Bilanciere"),
    BATTLE_ROPES("battle_ropes","Battle rope"),
    BENCH("bench","Panca"),
    BOSU("bosu","Bosu"),
    BOX("box","Box"),
    CABLE("cable","Cavi"),
    DIP_BARS("dip_bars","Parallele"),
    DUMBBELL("dumbbell","Manubri"),
    EZ_BAR("ez_bar","Bilanciere EZ"),
    FOAM_ROLLER("foam_roller","Foam Roller"),
    KETTLEBELL("kettlebell","Kettlebell"),
    LEG_PRESS("leg_press","Leg press"),
    MACHINE("machine","Macchina"),
    MEDICINE_BALL("medicine_ball","Palla medica"),
    PARALLETTES("parallettes","Parallettes"),
    PULL_UP_BAR("pull_up_bar","Sbarra"),
    RESISTANCE_BAND("resistance_band","Elastici"),
    RINGS("rings","Anelli"),
    ROPE("rope","Corda"),
    STABILITY_BALL("stability_ball","Fitball"),
    TOWEL("towel","Asciugamano")
}

internal object EquipmentCatalog {
    private val tokenAliases:Map<String,EquipmentId> = mapOf(
        "cable_machine" to EquipmentId.CABLE,
        "rope_or_towel" to EquipmentId.ROPE
    )
    private val displayAliases:Map<String,EquipmentId> = mapOf(
        "macchine" to EquipmentId.MACHINE,
        "macchina" to EquipmentId.MACHINE,
        "cavo" to EquipmentId.CABLE,
        "cavi" to EquipmentId.CABLE,
        "corda" to EquipmentId.ROPE,
        "battle ropes" to EquipmentId.BATTLE_ROPES
    )
    private fun norm(value:String)=value.trim().lowercase()

    fun fromToken(token:String):EquipmentId? =
        EquipmentId.entries.firstOrNull { it.token==norm(token) } ?: tokenAliases[norm(token)]

    fun fromDisplayName(value:String):EquipmentId? {
        val n=norm(value)
        return EquipmentId.entries.firstOrNull { norm(it.labelIt)==n } ?: displayAliases[n]
    }

    fun displayName(id:EquipmentId):String=id.labelIt
    fun displayNameForToken(token:String):String =
        fromToken(token)?.labelIt ?: token.replace('_',' ').lowercase().replaceFirstChar{it.uppercase()}

    fun requirementsFor(exercise:Exercise):Set<EquipmentId> {
        val canonical=exercise.progressionKey?.let { id ->
            ExerciseDbV2.installedRecords().firstOrNull { it.id==id }
        }
        if(canonical!=null) {
            val ids=canonical.equipment.mapNotNull(::fromToken).toSet()
            return if(canonical.equipment.isEmpty()) setOf(EquipmentId.BODYWEIGHT) else ids
        }
        val legacy=exercise.equipment.split("/").mapNotNull(::fromDisplayName).toSet()
        return if(legacy.isEmpty() && exercise.equipment.equals("Corpo libero",true)) setOf(EquipmentId.BODYWEIGHT) else legacy
    }
}
