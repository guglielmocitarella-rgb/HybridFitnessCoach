package com.hybridfitnesscoach.app.domain.v3

class V3AssetValidationException(message: String) : IllegalArgumentException(message)

data class V3LoadAudit(
    val inputRecords: Int, val parsedRecords: Int, val validatedRecords: Int, val mappedRecords: Int,
    val uniqueIds: Int, val missingIds: Int, val duplicateIds: Int, val rejected: Int,
    val silentSemanticLoss: Int, val buildTimeOnlyFields: Set<String>
)
data class V3LoadResult(val records: List<ExerciseV3>, val audit: V3LoadAudit,
    val identityEvidence: List<V3IdentityEvidence> = emptyList())

/** Isolated V3.10B boundary. No production repository, DI, selection or persistence code references this loader. */
object V3AssetLoader {
    const val AUTHORITATIVE_SCHEMA_VERSION = "3.7"
    const val AUTHORITATIVE_RECORD_COUNT = 1087
    val BUILD_TIME_ONLY_FIELDS = setOf("canonicalName","aliases","engineMetadata","legacy","unresolved","provenance")

    fun loadAuthoritative(json: String): V3LoadResult = load(json, AUTHORITATIVE_RECORD_COUNT, AUTHORITATIVE_SCHEMA_VERSION)

    fun load(json: String, expectedRecordCount: Int? = null, expectedSchemaVersion: String = AUTHORITATIVE_SCHEMA_VERSION): V3LoadResult {
        val root = try { JsonParser(json).parse() } catch (e: RuntimeException) { throw V3AssetValidationException("malformed JSON: ${e.message}") }
        val rows = root.arr("root")
        if (expectedRecordCount != null && rows.size != expectedRecordCount) fail("record count ${rows.size}, expected $expectedRecordCount")
        val dtos = rows.mapIndexed { i, v -> V3ExerciseDto(v.obj("record[$i]"), i) }
        val ids = dtos.map { it.requiredString("canonicalExerciseId") }
        val missing = ids.count { it.isBlank() }
        if (missing != 0) fail("missing canonical IDs: $missing")
        val duplicateIds = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicateIds.isNotEmpty()) fail("duplicate canonical IDs: ${duplicateIds.sorted().joinToString()}")
        val mapped = dtos.map { dto -> validateAndMap(dto, expectedSchemaVersion) }
        val losses = dtos.zip(mapped).flatMap { (dto, domain) -> lossAudit(dto, domain) }
        if (losses.isNotEmpty()) fail("silent semantic loss: ${losses.take(8).joinToString()}")
        // Ranking needs explicit native identity links. Keep only these fields, not a second
        // legacy catalog, aliases, or probabilistic runtimeModelNames matches.
        val identityEvidence = dtos.zip(mapped).filter { it.second.identityState == SemanticState.KNOWN }.map { (dto, domain) ->
            val legacy = dto.requiredObject("legacy")
            V3IdentityEvidence(domain.id,
                setOfNotNull(legacy.optionalString("id")?.takeIf(String::isNotBlank)),
                setOfNotNull(legacy.optionalString("runtimeName")?.takeIf(String::isNotBlank)))
        }
        return V3LoadResult(mapped, V3LoadAudit(rows.size, dtos.size, dtos.size, mapped.size, ids.toSet().size, 0, 0, 0, 0, BUILD_TIME_ONLY_FIELDS), identityEvidence)
    }

    private fun validateAndMap(d: V3ExerciseDto, schema: String): ExerciseV3 {
        d.requireTopLevelKeys()
        val version = d.requiredString("schemaVersion")
        if (version != schema) fail("record ${d.index}: unsupported schemaVersion '$version'")
        val id = CanonicalExerciseId(d.requiredString("canonicalExerciseId"))
        val identityState = state(d.requiredString("identityState"), "identityState")

        val cg = d.requiredObject("categoryGroup")
        val categoryState = state(cg.requiredString("state"), "categoryGroup.state")
        val category = ExerciseCategoryV3(cg.requiredString("category"))
        val group = cg.optionalString("group")?.takeIf { it.isNotBlank() }?.let(::ExerciseGroupV3)
        val groupState = state(cg.requiredString("groupState"), "categoryGroup.groupState")
        cg.requiredStringArray("secondaryGroups")

        val eq = d.requiredObject("equipmentRequirement")
        val eqState = state(eq.requiredString("state"), "equipmentRequirement.state")
        val eqMode = enumToken<EquipmentRequirementMode>(eq.requiredString("mode"), "equipmentRequirement.mode")
        val required = eq.requiredStringArray("required").map(::EquipmentId).toSet()
        val alternatives = eq.requiredStringArray("alternatives").map(::EquipmentId).toSet()
        val optional = eq.requiredStringArray("optional").map(::EquipmentId).toSet()
        val assistance = eq.requiredStringArray("assistance").map(::EquipmentId).toSet()
        val external = eq.requiredBoolean("externalLoadSupport")
        val info = if (eqState == SemanticState.KNOWN) EquipmentInformationState.KNOWN else EquipmentInformationState.UNKNOWN
        val equipment = try { EquipmentRequirement(info, eqMode, required, alternatives, optional, assistance, external, eqState) }
            catch (e: IllegalArgumentException) { fail("equipmentRequirement for ${id.value}: ${e.message}") }

        val rs = d.requiredObject("resistance")
        val rsState = state(rs.requiredString("state"), "resistance.state")
        val sourceToken = rs.requiredString("source")
        val source = if (sourceToken == "UNKNOWN") SemanticValue<ResistanceSource>(rsState) else SemanticValue.known(enumToken(sourceToken, "resistance.source"))
        val load = rs.requiredObject("loadInput")
        val loadState = state(load.requiredString("state"), "resistance.loadInput.state")
        val loadSem = load.optionalString("unitSemantics")
        val loadUnit = semanticUnit(loadState, loadSem, "resistance.loadInput.unitSemantics")
        val assist = rs.requiredObject("assistanceInput")
        val assistState = state(assist.requiredString("state"), "resistance.assistanceInput.state")
        val assistSem = assist.optionalString("unitSemantics")
        val assistUnit = semanticUnit(assistState, assistSem, "resistance.assistanceInput.unitSemantics")
        val quantification = rs.optionalString("quantification")
        val resistance = ResistanceSpec(source, loadUnit, assistUnit, sourceToken, loadSem, assistSem, quantification)

        val cap = d.requiredObject("capability")
        val capState = state(cap.requiredString("state"), "capability.state")
        val capabilities = CapabilityRequirements(
            capability(cap.requiredString("strengthRequirement"), "capability.strengthRequirement"),
            capability(cap.requiredString("technicalDifficulty"), "capability.technicalDifficulty"),
            capability(cap.requiredString("mobilityRequirement"), "capability.mobilityRequirement"),
            capability(cap.requiredString("balanceCoordinationRequirement"), "capability.balanceCoordinationRequirement"),
            capState
        )
        val legacyStatus = state(cap.requiredString("legacyLevelStatus"), "capability.legacyLevelStatus")
        val legacyRaw = cap.optionalString("legacyLevel")
        val legacyLevel = when (legacyStatus) {
            SemanticState.KNOWN -> LegacyExerciseLevel(legacyRaw ?: fail("KNOWN legacyLevelStatus requires legacyLevel"))
            else -> null
        }
        if (cap.requiredString("migrationRule") != "NO_BLIND_SCALAR_MAPPING") fail("capability.migrationRule must remain NO_BLIND_SCALAR_MAPPING")

        val mv = d.requiredObject("movementPatterns")
        val mvState = state(mv.requiredString("state"), "movementPatterns.state")
        val patterns = mv.requiredStringArray("values").map(::MovementPatternV3).toSet()
        val movement = if (mvState == SemanticState.KNOWN) SemanticValue.known(patterns) else SemanticValue<Set<MovementPatternV3>>(mvState)
        if (mvState == SemanticState.KNOWN && patterns.isEmpty()) fail("KNOWN movementPatterns must contain values")
        if (mvState != SemanticState.KNOWN && patterns.isNotEmpty()) fail("non-KNOWN movementPatterns cannot contain values")

        val pr = d.requiredObject("prescription")
        val prState = state(pr.requiredString("state"), "prescription.state")
        pr.optionalString("primaryMeasure")
        val dims = pr.requiredArray("dimensions").mapIndexed { i, j ->
            val o=j.obj("prescription.dimensions[$i]")
            val type=enumToken<PrescriptionDimension>(o.requiredString("type"), "prescription.dimensions[$i].type")
            val dimState=state(o.requiredString("state"), "prescription.dimensions[$i].state")
            if(dimState != SemanticState.KNOWN) fail("prescription dimension $type must be KNOWN in schema 3.7")
            val value=o.optionalNumber("value")
            val unit=o.optionalString("unit")?.let { enumToken<InputUnit>(it,"prescription.dimensions[$i].unit") }
            PrescribedValue(type,value,unit)
        }
        val prescription = ExercisePrescription(dims, prState, pr.requiredBoolean("openEnded"))

        val relationSeen=mutableSetOf<String>()
        val relations=d.requiredArray("skillRelations").mapIndexed { i,j ->
            val o=j.obj("skillRelations[$i]")
            val targetType=enumToken<SkillTargetType>(o.requiredString("targetType"),"skillRelations[$i].targetType")
            val targetId=SkillTargetId(o.requiredString("targetId"))
            val relationType=enumToken<SkillRelationType>(o.requiredString("relationType"),"skillRelations[$i].relationType")
            val key="${targetType.name}|${targetId.value}|${relationType.name}"
            if(!relationSeen.add(key)) fail("duplicate skill relation $key for ${id.value}")
            val provenance = o.optionalValue("provenance")
            val provenanceRef = (provenance as? JStr)?.v
            val provenancePayload = if (provenance != null && provenance != JNull && provenance !is JStr) provenance.toCanonicalJson() else null
            SkillRelation(
                targetType = targetType,
                targetId = targetId,
                relationType = relationType,
                evidenceState = EvidenceState.UNSPECIFIED,
                diagnosticProvenanceRef = provenanceRef,
                confidence = o.optionalString("confidence"),
                evidence = o.optionalString("evidence"),
                source = o.optionalString("source"),
                sourceRecordId = o.optionalString("sourceRecordId"),
                reason = o.optionalString("reason"),
                diagnosticProvenancePayload = provenancePayload
            )
        }

        d.requiredObject("engineMetadata"); d.requiredObject("legacy"); d.requiredArray("unresolved"); d.requiredObject("provenance"); d.requiredStringArray("aliases"); d.requiredString("canonicalName")
        return ExerciseV3(id,equipment,resistance,capabilities,category,group,movement,prescription,relations,legacyLevel,V3Version(version),identityState,categoryState,groupState)
    }

    private fun lossAudit(d: V3ExerciseDto, x: ExerciseV3): List<String> {
        val loss=mutableListOf<String>()
        fun check(ok:Boolean, field:String){if(!ok) loss += "${x.id.value}:$field"}
        check(d.requiredString("schemaVersion")==x.version.schemaVersion,"schemaVersion")
        check(d.requiredString("canonicalExerciseId")==x.id.value,"canonicalExerciseId")
        check(state(d.requiredString("identityState"),"identityState")==x.identityState,"identityState")
        val cg=d.requiredObject("categoryGroup")
        check(cg.requiredString("category")==x.category.value,"category")
        check(cg.optionalString("group")==x.group?.value,"group")
        check(state(cg.requiredString("groupState"),"groupState")==x.groupState,"groupState")
        val eq=d.requiredObject("equipmentRequirement")
        check(state(eq.requiredString("state"),"eq.state")==x.equipment.sourceState,"equipment.state")
        check(eq.requiredString("mode")==x.equipment.mode.name,"equipment.mode")
        check(eq.requiredStringArray("required").toSet()==x.equipment.equipment.map{it.value}.toSet(),"equipment.required")
        check(eq.requiredStringArray("alternatives").toSet()==x.equipment.alternatives.map{it.value}.toSet(),"equipment.alternatives")
        check(eq.requiredStringArray("optional").toSet()==x.equipment.optional.map{it.value}.toSet(),"equipment.optional")
        check(eq.requiredStringArray("assistance").toSet()==x.equipment.assistance.map{it.value}.toSet(),"equipment.assistance")
        check(eq.requiredBoolean("externalLoadSupport")==x.equipment.externalLoadSupport,"equipment.externalLoadSupport")
        val rs=d.requiredObject("resistance")
        check(rs.requiredString("source")==x.resistance.serializedSourceToken,"resistance.source")
        check(rs.optionalString("quantification")==x.resistance.quantification,"resistance.quantification")
        check(rs.requiredObject("loadInput").optionalString("unitSemantics")==x.resistance.loadUnitSemantics,"resistance.loadUnit")
        check(rs.requiredObject("assistanceInput").optionalString("unitSemantics")==x.resistance.assistanceUnitSemantics,"resistance.assistanceUnit")
        val cap=d.requiredObject("capability")
        check(state(cap.requiredString("state"),"cap.state")==x.capabilities.overallState,"capability.state")
        check(cap.optionalString("legacyLevel")==x.legacyLevel?.value,"legacyLevel")
        val mv=d.requiredObject("movementPatterns")
        check(state(mv.requiredString("state"),"mv.state")==x.movementPatterns.state,"movementPatterns.state")
        if(x.movementPatterns.state==SemanticState.KNOWN) check(mv.requiredStringArray("values").toSet()==x.movementPatterns.value!!.map{it.value}.toSet(),"movementPatterns.values")
        val pr=d.requiredObject("prescription")
        check(state(pr.requiredString("state"),"pr.state")==x.prescription!!.sourceState,"prescription.state")
        check(pr.requiredArray("dimensions").map{it.obj("dim").requiredString("type")}==x.prescription.prescribed.map{it.dimension.name},"prescription.dimensions")
        val relationDtos = d.requiredArray("skillRelations")
        check(relationDtos.size==x.skillRelations.size,"skillRelations")
        relationDtos.zip(x.skillRelations).forEachIndexed { i, (json, relation) ->
            val source = json.obj("skillRelations[$i]").optionalValue("provenance")
            val preserved = when (source) {
                null, JNull -> relation.diagnosticProvenanceRef == null && relation.diagnosticProvenancePayload == null
                is JStr -> relation.diagnosticProvenanceRef == source.v && relation.diagnosticProvenancePayload == null
                else -> relation.diagnosticProvenanceRef == null &&
                    relation.diagnosticProvenancePayload?.let { JsonParser(it).parse() == source } == true
            }
            check(preserved,"skillRelations[$i].provenance")
        }
        return loss
    }

    private fun semanticUnit(s:SemanticState, token:String?, path:String):SemanticValue<InputUnit> = when(s){
        SemanticState.KNOWN -> SemanticValue.known(enumToken(token ?: fail("$path missing for KNOWN state"),path))
        else -> { if(token!=null && token!="UNRESOLVED") enumToken<InputUnit>(token,path); SemanticValue(s) }
    }
    private fun capability(token:String,path:String):SemanticValue<CapabilityLevel> = when(token){
        "VERY_LOW","LOW","MODERATE","HIGH","VERY_HIGH" -> SemanticValue.known(enumToken(token,path))
        else -> SemanticValue(state(token,path))
    }
    private fun state(token:String,path:String):SemanticState = enumToken(token,path)
    private inline fun <reified T:Enum<T>> enumToken(token:String,path:String):T = enumValues<T>().firstOrNull{it.name==token} ?: fail("unknown token '$token' at $path")
    private fun fail(message:String):Nothing = throw V3AssetValidationException(message)
}

private class V3ExerciseDto(private val o:JObj,val index:Int){
    fun requireTopLevelKeys(){
        val required=setOf("schemaVersion","canonicalExerciseId","canonicalName","aliases","identityState","categoryGroup","equipmentRequirement","resistance","capability","skillRelations","prescription","movementPatterns","engineMetadata","legacy","unresolved","provenance")
        val missing=required-o.v.keys;if(missing.isNotEmpty()) throw V3AssetValidationException("record $index missing required fields: ${missing.sorted().joinToString()}")
    }
    fun requiredString(k:String)=o.requiredString(k)
    fun requiredObject(k:String)=o.requiredObject(k)
    fun requiredArray(k:String)=o.requiredArray(k)
    fun requiredStringArray(k:String)=o.requiredStringArray(k)
}

private sealed interface J
private data class JObj(val v:Map<String,J>):J {
    fun required(k:String):J=v[k]?:throw V3AssetValidationException("missing required field '$k'")
    fun requiredString(k:String)=required(k).str(k).also{if(it.isBlank()) throw V3AssetValidationException("blank required field '$k'")}
    fun optionalString(k:String)=when(val x=v[k]){null,JNull->null;else->x.str(k)}
    fun optionalValue(k:String):J?=v[k]
    fun requiredObject(k:String)=required(k).obj(k)
    fun requiredArray(k:String)=required(k).arr(k)
    fun requiredStringArray(k:String)=requiredArray(k).mapIndexed{i,x->x.str("$k[$i]")}
    fun requiredBoolean(k:String)=(required(k) as? JBool)?.v?:throw V3AssetValidationException("'$k' must be boolean")
    fun optionalNumber(k:String)=when(val x=v[k]){null,JNull->null;is JNum->x.v;else->throw V3AssetValidationException("'$k' must be number or null")}
}
private data class JArr(val v:List<J>):J
private data class JStr(val v:String):J
private data class JNum(val v:Double):J
private data class JBool(val v:Boolean):J
private data object JNull:J
private fun J.obj(path:String)=this as? JObj?:throw V3AssetValidationException("$path must be object")
private fun J.arr(path:String)=(this as? JArr)?.v?:throw V3AssetValidationException("$path must be array")
private fun J.str(path:String)=(this as? JStr)?.v?:throw V3AssetValidationException("$path must be string")
private fun J.toCanonicalJson():String = when (this) {
    is JObj -> v.entries.joinToString(prefix="{",postfix="}",separator=",") { jsonString(it.key) + ":" + it.value.toCanonicalJson() }
    is JArr -> v.joinToString(prefix="[",postfix="]",separator=",") { it.toCanonicalJson() }
    is JStr -> jsonString(v)
    is JNum -> v.toString()
    is JBool -> v.toString()
    JNull -> "null"
}
private fun jsonString(value:String):String = buildString {
    append('"')
    value.forEach { c ->
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000c' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
        }
    }
    append('"')
}

private class JsonParser(private val s:String){
    private var i=0
    fun parse():J{ws();val v=value();ws();if(i!=s.length) error("trailing content at $i");return v}
    private fun value():J{ws();if(i>=s.length) error("unexpected end");return when(s[i]){
        '{'->obj();'['->arr();'"'->JStr(string());'t'->{lit("true");JBool(true)};'f'->{lit("false");JBool(false)};'n'->{lit("null");JNull}
        '-','0','1','2','3','4','5','6','7','8','9'->number();else->error("unexpected '${s[i]}' at $i")}}
    private fun obj():JObj{expect('{');ws();val m=linkedMapOf<String,J>();if(peek('}')){i++;return JObj(m)};while(true){ws();if(!peek('"'))error("object key expected at $i");val k=string();ws();expect(':');if(m.put(k,value())!=null)error("duplicate JSON key '$k'");ws();if(peek('}')){i++;return JObj(m)};expect(',')}}
    private fun arr():JArr{expect('[');ws();val a=mutableListOf<J>();if(peek(']')){i++;return JArr(a)};while(true){a+=value();ws();if(peek(']')){i++;return JArr(a)};expect(',')}}
    private fun string():String{expect('"');val b=StringBuilder();while(i<s.length){val c=s[i++];when(c){'"'->return b.toString();'\\'->{if(i>=s.length)error("bad escape");when(val e=s[i++]){'"','\\','/'->b.append(e);'b'->b.append('\b');'f'->b.append('\u000c');'n'->b.append('\n');'r'->b.append('\r');'t'->b.append('\t');'u'->{if(i+4>s.length)error("bad unicode escape");b.append(s.substring(i,i+4).toInt(16).toChar());i+=4};else->error("bad escape $e")}};else->b.append(c)}};error("unterminated string")}
    private fun number():JNum{val st=i;if(peek('-'))i++;digits();if(peek('.')){i++;digits()};if(peek('e')||peek('E')){i++;if(peek('+')||peek('-'))i++;digits()};return JNum(s.substring(st,i).toDouble())}
    private fun digits(){val st=i;while(i<s.length&&s[i].isDigit())i++;if(st==i)error("digit expected at $i")}
    private fun lit(x:String){if(!s.startsWith(x,i))error("expected $x at $i");i+=x.length}
    private fun expect(c:Char){ws();if(i>=s.length||s[i]!=c)error("expected '$c' at $i");i++}
    private fun peek(c:Char)=i<s.length&&s[i]==c
    private fun ws(){while(i<s.length&&s[i].isWhitespace())i++}
}
