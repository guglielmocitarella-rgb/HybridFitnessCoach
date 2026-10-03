#!/usr/bin/env python3
"""Validate canonical-candidate structure and report evidence blockers honestly."""

from collections import Counter
import json
import pathlib
import re
import sys

PROJECT_ROOT = pathlib.Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from tools.build_exercise_db_canonical import RECOGNIZED_EQUIPMENT, _skill_nodes

EQUIPMENT_CATALOG_PATH = PROJECT_ROOT / "app/src/main/java/com/hybridfitnesscoach/app/EquipmentCatalog.kt"
_EQUIPMENT_CATALOG_TEXT = EQUIPMENT_CATALOG_PATH.read_text(encoding="utf-8")
APP_SUPPORTED_EQUIPMENT = set(re.findall(r'\b[A-Z][A-Z0-9_]*\("([a-z0-9_]+)"\s*,', _EQUIPMENT_CATALOG_TEXT))


def validate_database(records, progressions, sources):
    errors, warnings = [], []
    active_skill_ids = {node["id"] for node in _skill_nodes(PROJECT_ROOT)}
    ids = [record.get("id") for record in records]
    id_set = {value for value in ids if value}
    source_ids = {source.get("source_id") for source in sources}

    def provenance_source_ids(value):
        if isinstance(value, dict):
            if value.get("source_id"):
                yield value["source_id"]
            for nested in value.values():
                yield from provenance_source_ids(nested)
        elif isinstance(value, list):
            for nested in value:
                yield from provenance_source_ids(nested)
    duplicates = [value for value, count in Counter(ids).items() if not value or count > 1]
    errors.extend(f"duplicate id or missing id: {value!r}" for value in duplicates)

    for record in records:
        exercise_id = record.get("id") or "<missing-id>"
        if not str(record.get("canonicalNameEn") or "").strip():
            errors.append(f"{exercise_id}: missing canonicalNameEn")
        if record.get("equipmentStatus") not in {"KNOWN", "UNKNOWN"}:
            errors.append(f"{exercise_id}: invalid equipmentStatus")
        if record.get("levelStatus") not in {"KNOWN", "UNKNOWN"}:
            errors.append(f"{exercise_id}: missing or invalid levelStatus")
        for required in ("resistanceMode", "category", "role", "measure", "movementPattern", "skillIds"):
            if required not in record:
                errors.append(f"{exercise_id}: missing selection field {required}")
        for token in record.get("equipmentRequired") or []:
            if token not in RECOGNIZED_EQUIPMENT:
                errors.append(f"{exercise_id}: unrecognized equipment token {token}")
        if record.get("resistanceMode") == "EXTERNAL_LOAD":
            equipment = set(record.get("equipmentRequired") or [])
            external_equipment = {"barbell", "cable", "dumbbell", "ez_bar", "kettlebell", "leg_press", "machine", "medicine_ball", "resistance_band"}
            if record.get("equipmentStatus") != "KNOWN" or not equipment.intersection(external_equipment):
                errors.append(f"{exercise_id}: EXTERNAL_LOAD requires known external-load equipment")
        if not record.get("movementPatternStatus"):
            errors.append(f"{exercise_id}: missing movementPatternStatus provenance")
        for edge in record.get("progressionRelations") or []:
            if edge.get("from") not in id_set or edge.get("to") not in id_set:
                errors.append(f"{exercise_id}: dangling embedded progression relation")
        for ref in record.get("modelSourceRefs") or []:
            if ref.get("source_id") not in source_ids:
                errors.append(f"{exercise_id}: unregistered model provenance source")
        for ref in record.get("sources") or []:
            source_id = ref.get("source_id") if isinstance(ref, dict) else ref
            if source_id not in source_ids:
                errors.append(f"{exercise_id}: unregistered exercise source {source_id}")
        for provenance in (record.get("fieldProvenance") or {}).values():
            for source_id in provenance_source_ids(provenance.get("source")):
                if source_id not in source_ids:
                    errors.append(f"{exercise_id}: unregistered field provenance source {source_id}")
            if provenance.get("status") == "EXTERNAL_SOURCE_DIRECT":
                source = provenance.get("source") or {}
                if source.get("source_id") not in source_ids or not source.get("evidence_type"):
                    errors.append(f"{exercise_id}: external field provenance lacks a registered typed source")
                if not str(source.get("locator") or "").startswith("https://"):
                    errors.append(f"{exercise_id}: external field provenance lacks a source URL")
                if provenance.get("confidence") not in {"HIGH", "MEDIUM", "LOW"}:
                    errors.append(f"{exercise_id}: external field provenance lacks a valid confidence")
        app_metadata = record.get("applicationMetadata") or {}
        for app_entry in (app_metadata.get("sourceEntries") or []) + (app_metadata.get("ambiguousSourceEntries") or []):
            source_id = (app_entry.get("source") or {}).get("source_id")
            if source_id not in source_ids:
                errors.append(f"{exercise_id}: unregistered runtime metadata source {source_id}")
            for provenance in (app_entry.get("fieldProvenance") or {}).values():
                field_source_id = provenance.get("source_id")
                if field_source_id not in source_ids:
                    errors.append(f"{exercise_id}: unregistered runtime metadata source {field_source_id}")
        equipment_provenance = (record.get("fieldProvenance") or {}).get("equipmentRequired") or {}
        if equipment_provenance.get("status") == "NAME_EXPLICIT_EQUIPMENT_RULE":
            source_id = (equipment_provenance.get("source") or {}).get("source_id")
            if source_id not in source_ids:
                errors.append(f"{exercise_id}: unregistered explicit-name equipment rule source {source_id}")
        pattern_provenance = (record.get("fieldProvenance") or {}).get("movementPattern") or {}
        if pattern_provenance.get("status") == "V2_MOVEMENT_TYPE_RULE":
            source_id = (pattern_provenance.get("source") or {}).get("source_id")
            if source_id not in source_ids:
                errors.append(f"{exercise_id}: unregistered movement-type pattern rule source {source_id}")
        if pattern_provenance.get("status") == "V2_FAMILY_PATTERN_RULE":
            source_id = (pattern_provenance.get("source") or {}).get("source_id")
            if source_id not in source_ids:
                errors.append(f"{exercise_id}: unregistered family-pattern rule source {source_id}")
        for skill_id in record.get("skillIds") or []:
            if not str(skill_id).strip():
                errors.append(f"{exercise_id}: empty skill link")
            elif skill_id not in active_skill_ids:
                errors.append(f"{exercise_id}: unknown SkillCatalog ID {skill_id}")

    edge_keys = set()
    for edge in progressions:
        key = (edge.get("from"), edge.get("to"), edge.get("relationship"))
        if key in edge_keys:
            errors.append(f"duplicate progression edge: {key}")
        edge_keys.add(key)
        if key[0] not in id_set or key[1] not in id_set:
            errors.append(f"dangling progression edge: {key[0]} -> {key[1]}")
        for source in edge.get("sources") or []:
            source_id = source.get("source_id") if isinstance(source, dict) else source
            if source_id not in source_ids:
                errors.append(f"progression edge {key[0]}->{key[1]}: unregistered source {source_id}")

    equipment_unknown = sum(record.get("equipmentStatus") != "KNOWN" for record in records)
    equipment_unsupported_by_app = sum(
        any(token in RECOGNIZED_EQUIPMENT and token not in APP_SUPPORTED_EQUIPMENT for token in (record.get("equipmentRequired") or []))
        for record in records
    )
    pattern_missing = sum(not (record.get("movementPattern") or []) for record in records)
    instruction_incomplete = sum(
        not str(record.get("descriptionIt") or "").strip()
        or not str(record.get("setupIt") or "").strip()
        or not (record.get("executionStepsIt") or [])
        for record in records
    )
    level_unknown = sum(record.get("levelStatus") != "KNOWN" for record in records)
    resistance_unknown = sum(record.get("resistanceMode") in {None, "UNKNOWN"} for record in records)
    skill_links_missing = sum(record.get("movementType") == "SKILL" and not record.get("skillIds") for record in records)
    if equipment_unknown:
        warnings.append(f"{equipment_unknown} exercises still have unknown equipment requirements")
    if equipment_unsupported_by_app:
        warnings.append(f"{equipment_unsupported_by_app} exercises use equipment tokens not supported by the current app catalog")
    if pattern_missing:
        warnings.append(f"{pattern_missing} exercises still lack an explicit movement pattern")
    if instruction_incomplete:
        warnings.append(f"{instruction_incomplete} exercises lack supported execution instructions")
    if level_unknown:
        warnings.append(f"{level_unknown} exercises have unknown level")
    if resistance_unknown:
        warnings.append(f"{resistance_unknown} exercises have unknown resistance mode")
    if skill_links_missing:
        warnings.append(f"{skill_links_missing} skill exercises lack a stable SkillCatalog link")
    selection_unknown = equipment_unknown + equipment_unsupported_by_app + pattern_missing + level_unknown + resistance_unknown + skill_links_missing
    return {
        "ok": not errors,
        "errors": errors,
        "warnings": warnings,
        "releaseReady": not errors and not selection_unknown and not instruction_incomplete,
        "counts": {"records": len(records), "unique_ids": len(id_set), "progression_edges": len(progressions)},
        "blockers": {
            "equipment_unknown": equipment_unknown,
            "equipment_unsupported_by_app": equipment_unsupported_by_app,
            "movement_pattern_missing": pattern_missing,
            "instruction_incomplete": instruction_incomplete,
            "level_unknown": level_unknown,
            "resistance_mode_unknown": resistance_unknown,
            "skill_links_missing": skill_links_missing,
        },
    }


def main():
    root = pathlib.Path(__file__).resolve().parents[1]
    candidate = root / "exercise-db-canonical"
    records = json.loads((candidate / "exercise-db-canonical.json").read_text(encoding="utf-8"))
    progressions = json.loads((root / "app/src/main/assets/exercise-db-v2/progressions.json").read_text(encoding="utf-8"))
    sources = json.loads((candidate / "sources.json").read_text(encoding="utf-8"))
    report = validate_database(records, progressions, sources)
    (candidate / "VALIDATION_REPORT.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
