#!/usr/bin/env python3
"""Materialize an auditable union of the v0.9.5 V2 and current-model catalogues.

This creates a review candidate only. It does not edit either source catalogue or
claim that missing exercise-science evidence has been completed.
"""

import json
import pathlib
import re
import sys
from collections import Counter, defaultdict

PROJECT_ROOT = pathlib.Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


BASELINE = "9b8a41cfe5c7ee007f37219f9987a843f3ae3911"
DB_REL = pathlib.Path("app/src/main/assets/exercise-db-v2")
APP_REL = pathlib.Path("app/src/main/java/com/hybridfitnesscoach/app")
EXTERNAL_ENRICHMENT_REL = pathlib.Path("exercise-db-canonical/external-enrichment.json")
ACE_EXERCISE_LIBRARY_SOURCE = {
    "source_id": "ACE_EXERCISE_LIBRARY",
    "title": "ACE Fitness Exercise Library",
    "primary_use": "Exercise-specific equipment, difficulty, and technique descriptions from the American Council on Exercise exercise library.",
}
# Explicitly reviewed name-only duplicate. Both entries have matching group,
# equipment, level, role, measure, and effective movement metadata. Preserve the
# skill label and both source locations when materializing their single identity.
EQUIVALENT_CURRENT_NAMES = {
    "straight arm pulldown": {"straight arm pulldown", "straight-arm pulldown"},
}
RECOGNIZED_EQUIPMENT = {
    "ab_wheel", "barbell", "battle_ropes", "bench", "bosu", "box", "cable",
    "dip_bars", "dumbbell", "ez_bar", "foam_roller", "kettlebell", "leg_press",
    "machine", "medicine_ball", "parallettes", "pull_up_bar", "resistance_band",
    "rings", "rope", "stability_ball", "towel", "suspension_trainer",
}
EQUIPMENT_ALIASES = {
    "cable_machine": "cable", "rope_or_towel": "rope",
    "ab wheel": "ab_wheel", "bilanciere": "barbell", "battle rope": "battle_ropes",
    "battle ropes": "battle_ropes", "panca": "bench", "bosu": "bosu", "box": "box",
    "cavi": "cable", "cavo": "cable", "parallele": "dip_bars", "manubri": "dumbbell",
    "bilanciere ez": "ez_bar", "foam roller": "foam_roller", "kettlebell": "kettlebell",
    "leg press": "leg_press", "macchina": "machine", "macchine": "machine",
    "palla medica": "medicine_ball", "parallettes": "parallettes", "sbarra": "pull_up_bar",
    "elastici": "resistance_band", "anelli": "rings", "corda": "rope",
    "fitball": "stability_ball", "asciugamano": "towel", "trx": "suspension_trainer",
}
CANONICAL_NAME_EQUIPMENT_SOURCE = {
    "source_id": "CANONICAL_NAME_EQUIPMENT_RULE",
    "title": "Explicit equipment terms in canonical exercise names",
    "primary_use": "Conservative equipment inference only when a name explicitly identifies a recognized implement; does not establish technique or instructions.",
}
MOVEMENT_TYPE_PATTERN_SOURCE = {
    "source_id": "V2_MOVEMENT_TYPE_PATTERN_RULE",
    "title": "Top-level pattern from explicit Exercise DB V2 movement type",
    "primary_use": "Conservative high-level classification only: flexibility/mobility to mobility, SMR to recovery, and aerobic/conditioning to conditioning.",
}
V2_FAMILY_PATTERN_SOURCE = {
    "source_id": "V2_FAMILY_PATTERN_RULE",
    "title": "Whitelisted Exercise DB V2 family-to-pattern rules",
    "primary_use": "Infer broad movement patterns from selected movement families, with name-based exceptions for mixed pull-up/push-up families and explicit vertical chest press labels.",
}
EXERCISE_METADATA_SOURCE = {
    "source_id": "APP_EXERCISE_METADATA",
    "title": "Legacy runtime metadata completion rules",
    "primary_use": "Records the exact withExerciseMetadata category, movement-pattern, resistance-mode, secondary-group, and technical-complexity rules. These are app-model metadata, not exercise-science evidence.",
}
APP_V2_ADAPTER_SOURCE = {
    "source_id": "APP_V2_ADAPTER_RULE",
    "title": "Exercise DB V2 to legacy runtime adapter",
    "primary_use": "Documents app-model category, group, role, and fallback handling in ExerciseDbV2.toLegacyExercise; does not add exercise-science claims.",
}
APP_SKILL_CATALOG_SOURCE = {
    "source_id": "APP_SKILL_CATALOG",
    "title": "Active SkillCatalog node and alias mapping",
    "primary_use": "Provenance for stable SkillCatalog IDs resolved from runtime exercise names and the active node/alias catalogue.",
}
APP_EQUIPMENT_LABEL_SOURCE = {
    "source_id": "APP_EQUIPMENT_LABEL_RULE",
    "title": "Runtime equipment display-label to canonical token mapping",
    "primary_use": "Maps explicit runtime EquipmentCatalog labels and documented aliases to stable equipment tokens; the app equipment catalog remains the source of supported capabilities.",
}
V2_EQUIPMENT_RESISTANCE_SOURCE = {
    "source_id": "V2_EQUIPMENT_RESISTANCE_RULE",
    "title": "V2 recognized external equipment to resistance mode",
    "primary_use": "Maps an explicit recognized external-load equipment token to EXTERNAL_LOAD; it does not assign resistance mode when equipment is unknown.",
}
V2_TYPE_RESISTANCE_SOURCE = {
    "source_id": "V2_TYPE_RESISTANCE_RULE",
    "title": "V2 bodyweight movement-type resistance fallback",
    "primary_use": "Maps CALISTHENICS or SKILL with known equipment requirements to BODYWEIGHT; unknown equipment does not satisfy the rule.",
}


def _rule_source(source_id, evidence_type, locator):
    return {"source_id": source_id, "evidence_type": evidence_type, "locator": locator}


def explicit_equipment_from_name(name):
    """Return only equipment named explicitly enough to identify its implement."""
    name = str(name or "")
    patterns = (
        (r"\b(?:high|low)-pulley\b", "cable"),
        (r"\brings?\b", "rings"),
        (r"\bbench\b", "bench"),
        (r"\bbar\s+pull-ups?\b|\b(?:pull-ups?|chin-ups?)\s+bar\b", "pull_up_bar"),
        (r"\bparallel bars?\b", "dip_bars"),
        (r"\bbox\b", "box"),
        (r"\bbands?\b", "resistance_band"),
    )
    return list(dict.fromkeys(token for pattern, token in patterns if re.search(pattern, name, re.IGNORECASE)))


def v2_family_pattern(family, name):
    """Map only whitelisted V2 families whose broad mechanics are coherent."""
    family, name = str(family or "").upper(), str(name or "")
    mapping = {
        "HORIZONTAL_PULL": "horizontal-pull", "VERTICAL_PULL": "vertical-pull",
        "HORIZONTAL_PUSH": "horizontal-push", "VERTICAL_PUSH": "vertical-push",
        "HINGE": "hinge", "SQUAT": "knee-dominant", "SINGLE_LEG_SQUAT": "knee-dominant",
        "STEP_UP": "knee-dominant", "LUNGE": "knee-dominant", "DIP": "vertical-push",
        "HANDSTAND_PUSH_UP": "vertical-push", "PLANCHE": "horizontal-push",
        "CHEST": "horizontal-push", "CHEST_FLY": "horizontal-push", "BICEPS": "pull-accessory",
        "CORE": "core-control",
    }
    if family == "PULL_UP":
        return "horizontal-pull" if re.search(r"\brows?\b|horizontal pull", name, re.IGNORECASE) else "vertical-pull"
    if family == "PUSH_UP":
        return "vertical-push" if re.search(r"\b(?:headstand|handstand|pike)\b", name, re.IGNORECASE) else "horizontal-push"
    if family == "HORIZONTAL_PUSH" and re.search(r"\bvertical\b", name, re.IGNORECASE):
        return "vertical-push"
    return mapping.get(family)


def norm(value):
    return " ".join(str(value or "").strip().casefold().replace("-", " ").split())


def _split_args(text):
    args, start, depth, quoted, escaped = [], 0, 0, False, False
    for i, char in enumerate(text):
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char in "([{":
            depth += 1
        elif char in ")]}":
            depth -= 1
        elif char == "," and depth == 0:
            args.append(text[start:i].strip())
            start = i + 1
    tail = text[start:].strip()
    if tail:
        args.append(tail)
    return args


def _balanced_body(text, open_paren):
    depth, quoted, escaped = 1, False, False
    for i in range(open_paren + 1, len(text)):
        char = text[i]
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[open_paren + 1:i], i + 1
    raise ValueError("unclosed Exercise(...) in catalogue source")


def _value(expr):
    expr = expr.strip()
    if expr.startswith('"'):
        try:
            return json.loads(expr)
        except json.JSONDecodeError:
            return expr[1:-1]
    if expr in {"emptyList()", "emptySet()"}:
        return []
    if expr.startswith("listOf(") or expr.startswith("setOf("):
        inside = expr[expr.find("(") + 1:-1]
        return [_value(item) for item in _split_args(inside)]
    if expr.startswith("ExerciseCategory."):
        return expr.rsplit(".", 1)[1]
    if expr.startswith("ExerciseMeasure."):
        return expr.rsplit(".", 1)[1]
    if expr.startswith("ResistanceMode."):
        return expr.rsplit(".", 1)[1]
    if expr.startswith("Laterality."):
        return expr.rsplit(".", 1)[1]
    if expr == "null":
        return None
    if expr == "true":
        return True
    if expr == "false":
        return False
    try:
        return float(expr) if "." in expr else int(expr)
    except ValueError:
        return expr


def parse_exercise_entries(source, source_path):
    entries = []
    for match in re.finditer(r"\bExercise\s*\(", source):
        body, end = _balanced_body(source, match.end() - 1)
        args = _split_args(body)
        if not args or not args[0].strip().startswith('"'):
            continue  # the Exercise data-class declaration
        positional, named = [], {}
        for arg in args:
            if "=" in arg:
                key, value = arg.split("=", 1)
                named[key.strip()] = _value(value)
            else:
                positional.append(_value(arg))
        if len(positional) < 4:
            continue
        line = source.count("\n", 0, match.start()) + 1
        entry = {
            "name": positional[0], "group": positional[1], "equipmentLabel": positional[2],
            "level": positional[3], "skills": positional[4] if len(positional) > 4 else [],
            "role": positional[5] if len(positional) > 5 else named.get("role", "Base"),
            "sourceFile": source_path, "sourceLine": line,
        }
        explicit_fields = {"name", "group", "equipmentLabel", "level"}
        if len(positional) > 4:
            explicit_fields.add("skills")
        if len(positional) > 5 or "role" in named:
            explicit_fields.add("role")
        for field in (
            "setRestSec", "exerciseRestSec", "secondaryGroups", "movementPattern", "category",
            "intensity", "systemicFatigue", "technicalComplexity", "measure", "laterality",
            "resistanceMode", "progressionKey", "targetSets", "targetValue", "intervalRestSec",
        ):
            if field in named:
                entry[field] = named[field]
                explicit_fields.add(field)
        entry["_explicit_fields"] = explicit_fields
        entries.append(entry)
    return entries


def parse_runtime_catalogue(root):
    main_path = root / APP_REL / "MainActivity.kt"
    extended_path = root / APP_REL / "ExtendedExerciseCatalog.kt"
    main = main_path.read_text(encoding="utf-8")
    extended = extended_path.read_text(encoding="utf-8")
    core = main.split("private val coreExerciseCatalog", 1)[1].split(").map(::withExerciseMetadata)", 1)[0]
    expansion = extended.split("internal val extendedExerciseCatalog", 1)[1].split(").map(::withExerciseMetadata)", 1)[0]
    entries = parse_exercise_entries(core, "app/src/main/java/com/hybridfitnesscoach/app/MainActivity.kt")
    entries += parse_exercise_entries(expansion, "app/src/main/java/com/hybridfitnesscoach/app/ExtendedExerciseCatalog.kt")
    unique, seen = [], set()
    for entry in entries:
        key = entry["name"].strip().lower()
        if key not in seen:
            seen.add(key)
            unique.append(entry)
    return unique


def equipment_tokens(label):
    label = str(label or "").strip()
    if not label or label.casefold() in {"non specificato", "unknown"}:
        return [], "UNKNOWN"
    if label.casefold() == "corpo libero":
        return [], "KNOWN"
    tokens, unknown = [], []
    for item in re.split(r"\s*/\s*|\s*\+\s*|,", label):
        item = item.strip()
        if not item or item.casefold() == "corpo libero":
            continue
        token = EQUIPMENT_ALIASES.get(item.casefold())
        if token:
            tokens.append(token)
        else:
            unknown.append(item)
    tokens = list(dict.fromkeys(tokens))
    return tokens, "KNOWN" if not unknown else "UNKNOWN"


def effective_legacy_metadata(entry):
    name, category, group, role = entry["name"].casefold(), entry.get("category", "OTHER"), entry["group"], entry["role"]
    if category == "OTHER":
        if role == "Progressione":
            category = "SKILL"
        elif group.startswith("Gambe"):
            category = "LEGS"
        elif group.startswith("Core"):
            category = "CORE"
        elif group.startswith(("Dorso", "Bicipiti")):
            category = "PULL"
        elif group.startswith("Cardio"):
            category = "CARDIO"
        else:
            category = "PUSH"
    pattern = entry.get("movementPattern") or ""
    if not pattern:
        if category == "MOBILITY": pattern = "mobility"
        elif category == "CARDIO": pattern = "conditioning"
        elif any(word in name for word in ("deadlift", "stacco", "thrust", "bridge", "swing")): pattern = "hinge"
        elif category == "LEGS": pattern = "knee-dominant"
        elif category == "CORE" or "sit" in name or "flag" in name: pattern = "core-control"
        elif any(word in name for word in ("row", "rematore", "pulley")): pattern = "horizontal-pull"
        elif any(word in name for word in ("trazioni", "pull-up", "lever", "lat machine")): pattern = "vertical-pull"
        elif any(word in name for word in ("handstand", "pike", "military", "shoulder press")): pattern = "vertical-push"
        elif category == "PULL": pattern = "pull-accessory"
        else: pattern = "horizontal-push"
        pattern_origin = "APP_RULE_NAME_OR_GROUP_HEURISTIC"
    else:
        pattern_origin = "APP_MODEL_DIRECT"
    resistance = entry.get("resistanceMode", "NONE")
    tokens, equipment_status = equipment_tokens(entry["equipmentLabel"])
    if resistance != "NONE":
        resistance_origin = "APP_MODEL_DIRECT"
    elif category == "MOBILITY":
        resistance_origin = "APP_EXERCISE_METADATA_RULE"
    elif "zavorrat" in name or any(t in tokens for t in {"barbell", "dumbbell", "cable", "machine", "kettlebell", "leg_press"}):
        resistance, resistance_origin = "EXTERNAL_LOAD", "APP_RULE_EQUIPMENT_OR_NAME"
    elif "assist" in name and "resistance_band" in tokens:
        resistance, resistance_origin = "ASSISTED", "APP_RULE_EQUIPMENT_AND_NAME"
    else:
        resistance, resistance_origin = "BODYWEIGHT", "APP_RULE_DEFAULT"
    return {
        "category": category, "movementPattern": pattern, "movementPatternOrigin": pattern_origin,
        "resistanceMode": resistance, "resistanceModeOrigin": resistance_origin,
        "equipmentRequired": tokens, "equipmentStatus": equipment_status,
    }


def runtime_application_metadata(entry):
    """Snapshot the effective legacy Exercise values without treating them as science evidence."""
    effective = effective_legacy_metadata(entry)
    explicit = entry.get("_explicit_fields", set())
    role = entry.get("role", "Base")
    category = effective["category"]
    group = entry["group"]
    secondary_groups = entry.get("secondaryGroups") or group.split(" / ")[1:]
    default_rest = {"Forza": 150, "Progressione": 120, "Prerequisito": 90}.get(role, 90)
    snapshot = {
        "name": entry["name"],
        "group": group,
        "equipmentLabel": entry["equipmentLabel"],
        "equipmentRequired": effective["equipmentRequired"],
        "level": entry.get("level"),
        "skills": entry.get("skills", []),
        "role": role,
        "setRestSec": entry.get("setRestSec", default_rest),
        "exerciseRestSec": entry.get("exerciseRestSec", default_rest),
        "secondaryGroups": secondary_groups,
        "movementPattern": effective["movementPattern"],
        "category": category,
        "intensity": entry.get("intensity", 5),
        "systemicFatigue": entry.get("systemicFatigue", 5),
        "technicalComplexity": 3 if category == "SKILL" else (2 if role == "Forza" else 1),
        "measure": entry.get("measure", "REPS"),
        "laterality": entry.get("laterality", "BILATERAL"),
        "resistanceMode": effective["resistanceMode"],
        "progressionKey": entry.get("progressionKey"),
        "targetSets": entry.get("targetSets", 3),
        "targetValue": entry.get("targetValue"),
        "intervalRestSec": entry.get("intervalRestSec"),
    }
    fields = {}
    source = {
        "source_id": "APP_MODEL_9B8A41C",
        "source_name": entry["name"],
        "evidence_type": "APP_MODEL_ENTRY",
        "locator": f'{entry["sourceFile"]}:{entry["sourceLine"]}@{BASELINE}',
    }
    for field in snapshot:
        if field == "equipmentRequired":
            status, source_id = "APP_EQUIPMENT_LABEL_RULE", "APP_EQUIPMENT_LABEL_RULE"
        elif field in {"category", "movementPattern", "resistanceMode", "secondaryGroups", "technicalComplexity"}:
            direct = (
                (field == "category" and field in explicit and entry.get("category") != "OTHER")
                or (field == "movementPattern" and field in explicit and bool(entry.get("movementPattern")))
                or (field == "resistanceMode" and field in explicit and (entry.get("resistanceMode") != "NONE" or category == "MOBILITY"))
                or (field == "secondaryGroups" and field in explicit and bool(entry.get("secondaryGroups")))
            )
            status = "APP_MODEL_ENTRY" if direct else "APP_EXERCISE_METADATA_RULE"
            source_id = "APP_MODEL_9B8A41C" if direct else "APP_EXERCISE_METADATA"
        elif field in explicit:
            status, source_id = "APP_MODEL_ENTRY", "APP_MODEL_9B8A41C"
        elif field in {"setRestSec", "exerciseRestSec"}:
            status, source_id = "APP_MODEL_DEFAULT_RULE", "APP_MODEL_9B8A41C"
        else:
            status, source_id = "APP_MODEL_DEFAULT", "APP_MODEL_9B8A41C"
        if field == "equipmentRequired":
            locator = f'app/src/main/java/com/hybridfitnesscoach/app/EquipmentCatalog.kt <- {entry["sourceFile"]}:{entry["sourceLine"]}'
        elif status == "APP_EXERCISE_METADATA_RULE":
            locator = "app/src/main/java/com/hybridfitnesscoach/app/ExerciseMetadata.kt@" + BASELINE
        elif status == "APP_MODEL_DEFAULT_RULE":
            locator = "app/src/main/java/com/hybridfitnesscoach/app/MainActivity.kt:recommendedSetRest"
        elif status == "APP_MODEL_DEFAULT":
            locator = f'app/src/main/java/com/hybridfitnesscoach/app/MainActivity.kt:Exercise.{field}'
        else:
            locator = source["locator"]
        fields[field] = {"status": status, "source_id": source_id, "locator": locator}
    return {"source": source, "snapshot": snapshot, "fieldProvenance": fields}


def _slug(name):
    base = re.sub(r"[^a-z0-9]+", "-", name.casefold()).strip("-")
    return "cm-" + (base or "unnamed")


def _runtime_category(record, legacy_metadata=None):
    if legacy_metadata:
        return legacy_metadata["category"]
    movement_type, family = record.get("movementType"), str(record.get("family") or "").upper()
    if movement_type == "CORE" or "CORE" in family or "L_SIT" in family:
        return "CORE"
    if movement_type in {"AEROBIC", "CONDITIONING"} or family == "CARDIO":
        return "CARDIO"
    if movement_type in {"MOBILITY", "FLEXIBILITY", "SMR"} or "MOBILITY" in family or family == "STRETCH":
        return "MOBILITY"
    if movement_type == "SKILL" or family in {"PLANCHE", "FRONT_LEVER", "BACK_LEVER", "HANDSTAND", "HUMAN_FLAG", "MUSCLE_UP", "SKILL"}:
        return "SKILL"
    if "PULL" in family or family in {"BACK", "BICEPS", "UPRIGHT_ROW", "SHRUG", "PULLOVER", "REAR_DELTOID"}:
        return "PULL"
    if "PUSH" in family or family in {"CHEST", "CHEST_FLY", "DIP", "TRICEPS", "SHOULDER", "HANDSTAND_PUSH_UP"}:
        return "PUSH"
    if family in {"LEG", "SQUAT", "HINGE", "LOWER_BODY", "SINGLE_LEG_SQUAT", "NORDIC_HAMSTRING", "LUNGE", "STEP_UP", "CALF", "POSTERIOR_CHAIN"}:
        return "LEGS"
    return "OTHER"


def _skill_nodes(root):
    path = root / APP_REL / "SkillCatalog.kt"
    text = path.read_text(encoding="utf-8")
    nodes = []
    for m in re.finditer(r'SkillNode\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"', text):
        body, _ = _balanced_body(text, text.find("(", m.start()))
        args = _split_args(body)
        alias_match = re.search(r'exerciseAliases\s*=\s*setOf\(([^)]*)\)', body)
        aliases = _value("setOf(" + alias_match.group(1) + ")") if alias_match else (_value(args[5]) if len(args) > 5 and args[5].startswith("setOf(") else [])
        nodes.append({"id": m.group(1), "exerciseName": m.group(2), "skill": m.group(3), "aliases": aliases})
    initial_skills = {node["skill"].casefold() for node in nodes}
    main_text = (root / APP_REL / "MainActivity.kt").read_text(encoding="utf-8")
    for m in re.finditer(r'SkillProgressionNode\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"', main_text):
        node_id, skill, label = m.groups()
        if skill.casefold() in initial_skills or skill.casefold() in {"l-sit", "v-sit"}:
            continue
        nodes.append({"id": node_id, "exerciseName": label, "skill": skill, "aliases": []})
    alias_map_match = re.search(r'\baliases\s*=\s*mapOf\s*\(', text)
    if alias_map_match:
        alias_body, _ = _balanced_body(text, text.find("(", alias_map_match.start()))
        alias_map = {}
        for item in _split_args(alias_body):
            pair = re.match(r'\s*("(?:\\.|[^"])*")\s+to\s+setOf\(([^)]*)\)\s*$', item)
            if pair:
                alias_map[json.loads(pair.group(1))] = _value("setOf(" + pair.group(2) + ")")
        for node in nodes:
            node["aliases"] = list(dict.fromkeys(node["aliases"] + alias_map.get(node["exerciseName"], [])))
    return nodes


def _skill_ids(root, names):
    keys = {norm(name) for name in names if name}
    return sorted({node["id"] for node in _skill_nodes(root) if norm(node["exerciseName"]) in keys or any(norm(alias) in keys for alias in node["aliases"])})


def build_database(root, output_dir):
    root, output_dir = pathlib.Path(root), pathlib.Path(output_dir)
    db_dir, app_dir = root / DB_REL, root / APP_REL
    exercises = json.loads((db_dir / "exercises.json").read_text(encoding="utf-8"))
    progressions = json.loads((db_dir / "progressions.json").read_text(encoding="utf-8"))
    sources = json.loads((db_dir / "sources.json").read_text(encoding="utf-8"))
    runtime = parse_runtime_catalogue(root)
    by_name = {entry["name"].strip().lower(): entry for entry in runtime}
    lookup = defaultdict(set)
    for record in exercises:
        for name in [record.get("canonicalNameEn"), record.get("nameIt"), *record.get("aliases", [])]:
            if name:
                lookup[name.strip().lower()].add(record["id"])
    unique_lookup = {name: next(iter(ids)) for name, ids in lookup.items() if len(ids) == 1}
    normalized_lookup = defaultdict(set)
    for record in exercises:
        for name in [record.get("canonicalNameEn"), record.get("nameIt"), *record.get("aliases", [])]:
            if name:
                normalized_lookup[norm(name)].add(record["id"])
    app_source = {
        "source_id": "APP_MODEL_9B8A41C",
        "title": "Hybrid Fitness Coach v0.9.5 current exercise model at baseline 9b8a41c",
        "primary_use": "Provenance for the app's runtime labels and explicitly stored programming metadata; not exercise-science or technique evidence.",
    }
    equipment_rule_source = CANONICAL_NAME_EQUIPMENT_SOURCE
    pattern_rule_source = MOVEMENT_TYPE_PATTERN_SOURCE
    family_pattern_source = V2_FAMILY_PATTERN_SOURCE
    exercise_metadata_source = EXERCISE_METADATA_SOURCE
    adapter_source = APP_V2_ADAPTER_SOURCE
    skill_catalog_source = APP_SKILL_CATALOG_SOURCE
    equipment_label_source = APP_EQUIPMENT_LABEL_SOURCE
    equipment_resistance_source = V2_EQUIPMENT_RESISTANCE_SOURCE
    type_resistance_source = V2_TYPE_RESISTANCE_SOURCE

    v2_records = []
    matched_legacy_names = set()
    exact_runtime_matches = set()
    runtime_names = {}
    v2_by_id = {}
    for record in exercises:
        hit = next((by_name[name.strip().lower()] for name in [record.get("canonicalNameEn"), record.get("nameIt"), *record.get("aliases", [])] if name and name.strip().lower() in by_name), None)
        exact_hit = hit
        ambiguous_current_link = any(
            name and len(lookup.get(name.strip().lower(), set())) > 1
            for name in [record.get("canonicalNameEn"), record.get("nameIt"), *record.get("aliases", [])]
        )
        if not hit:
            candidates = {entry["name"].strip().lower(): entry for entry in runtime}
            for name in [record.get("canonicalNameEn"), record.get("nameIt"), *record.get("aliases", [])]:
                ids = normalized_lookup.get(norm(name), set()) if name else set()
                if len(ids) == 1 and record["id"] in ids:
                    normalized_name = next((entry for entry in runtime if norm(entry["name"]) == norm(name)), None)
                    if normalized_name:
                        hit = normalized_name
                        break
        display_name = (
            record.get("canonicalNameEn") or record.get("nameIt")
            if ambiguous_current_link
            else (hit["name"] if hit else record.get("nameIt") or record["canonicalNameEn"])
        )
        runtime_names[record["id"]] = display_name
        if hit:
            matched_legacy_names.add(hit["name"].strip().lower())
            if exact_hit:
                exact_runtime_matches.add(hit["name"].strip().lower())
        # An app label that resolves to multiple V2 IDs is not safe evidence for
        # copying app metadata onto any one of those identities.
        effective = effective_legacy_metadata(hit) if hit and not ambiguous_current_link else None
        equipment = list(record.get("equipment") or [])
        if equipment:
            eq = list(dict.fromkeys(EQUIPMENT_ALIASES.get(str(x).casefold(), str(x)) for x in equipment))
            eq_status = "KNOWN" if all(token in RECOGNIZED_EQUIPMENT for token in eq) else "UNKNOWN"
        elif effective:
            eq, eq_status = effective["equipmentRequired"], effective["equipmentStatus"]
        else:
            eq, eq_status = [], "UNKNOWN"
        equipment_origin = "V2_DIRECT" if equipment else ("APP_MODEL_DIRECT" if effective and eq_status == "KNOWN" else "UNKNOWN")
        if eq_status != "KNOWN":
            named_equipment = explicit_equipment_from_name(record.get("canonicalNameEn"))
            if named_equipment:
                eq, eq_status = named_equipment, "KNOWN"
                equipment_origin = "NAME_EXPLICIT_EQUIPMENT_RULE"
        movement_pattern = list(record.get("movementPattern") or [])
        pattern_origin = "V2_DIRECT" if movement_pattern else "UNKNOWN"
        if not movement_pattern and effective:
            movement_pattern = [effective["movementPattern"]]
            pattern_origin = effective["movementPatternOrigin"]
        if not movement_pattern:
            broad_pattern = {
                "FLEXIBILITY": "mobility", "MOBILITY": "mobility", "SMR": "recovery",
                "CONDITIONING": "conditioning", "AEROBIC": "conditioning",
            }.get(record.get("movementType"))
            if broad_pattern:
                movement_pattern = [broad_pattern]
                pattern_origin = "V2_MOVEMENT_TYPE_RULE"
        if not movement_pattern:
            family_pattern = v2_family_pattern(record.get("family"), record.get("canonicalNameEn"))
            if family_pattern:
                movement_pattern = [family_pattern]
                pattern_origin = "V2_FAMILY_PATTERN_RULE"
        if effective:
            mode, mode_status = effective["resistanceMode"], effective["resistanceModeOrigin"]
        elif any(token in eq for token in {"barbell", "dumbbell", "cable", "machine", "kettlebell", "leg_press"}):
            mode, mode_status = "EXTERNAL_LOAD", "APP_RULE_EQUIPMENT_TOKEN"
        elif record.get("movementType") in {"CALISTHENICS", "SKILL"} and eq_status == "KNOWN":
            mode, mode_status = "BODYWEIGHT", "APP_RULE_MOVEMENT_TYPE"
        else:
            mode, mode_status = "UNKNOWN", "UNKNOWN"
        category = _runtime_category(record, effective)
        role = hit.get("role") if effective else "Base"
        role_status = "APP_MODEL_DIRECT" if effective else "APP_RULE_RUNTIME_DEFAULT"
        level = record.get("level") or (hit.get("level") if effective else None)
        matching_skills = [] if ambiguous_current_link else _skill_ids(root, [display_name, record.get("canonicalNameEn"), record.get("nameIt"), *record.get("aliases", [])])
        equipment_provenance_source = (
            _rule_source(equipment_rule_source["source_id"], "EXPLICIT_NAME_TOKEN", record.get("canonicalNameEn"))
            if equipment_origin == "NAME_EXPLICIT_EQUIPMENT_RULE" else
            (_rule_source(equipment_label_source["source_id"], "APP_EQUIPMENT_LABEL_RULE", hit.get("equipmentLabel")) if effective else record["sources"])
        )
        if pattern_origin == "V2_MOVEMENT_TYPE_RULE":
            pattern_provenance_source = _rule_source(pattern_rule_source["source_id"], "V2_MOVEMENT_TYPE_RULE", record.get("movementType"))
        elif pattern_origin == "V2_FAMILY_PATTERN_RULE":
            pattern_provenance_source = _rule_source(family_pattern_source["source_id"], "V2_FAMILY_PATTERN_RULE", record.get("family"))
        elif pattern_origin.startswith("APP_RULE"):
            pattern_provenance_source = _rule_source(exercise_metadata_source["source_id"], "APP_EXERCISE_METADATA_RULE", "ExerciseMetadata.kt")
        else:
            pattern_provenance_source = record["sources"]
        if mode_status == "APP_RULE_EQUIPMENT_TOKEN":
            resistance_provenance_source = _rule_source(equipment_resistance_source["source_id"], "V2_EQUIPMENT_RESISTANCE_RULE", eq)
        elif mode_status == "APP_RULE_MOVEMENT_TYPE":
            resistance_provenance_source = _rule_source(type_resistance_source["source_id"], "V2_TYPE_RESISTANCE_RULE", record.get("movementType"))
        elif mode_status.startswith("APP_RULE"):
            resistance_provenance_source = _rule_source(exercise_metadata_source["source_id"], "APP_EXERCISE_METADATA_RULE", "ExerciseMetadata.kt")
        elif effective:
            resistance_provenance_source = model_ref
        else:
            resistance_provenance_source = record["sources"]
        level_provenance_source = record["sources"] if record.get("level") else (model_ref if effective else record["sources"])
        category_provenance_source = model_ref if effective else _rule_source(adapter_source["source_id"], "APP_V2_ADAPTER_RULE", "ExerciseDbV2.kt:toLegacyExercise")
        role_provenance_source = model_ref if effective else _rule_source(adapter_source["source_id"], "APP_V2_ADAPTER_RULE", "ExerciseDbV2.kt:toLegacyExercise")
        skill_provenance_source = (
            _rule_source(skill_catalog_source["source_id"], "SKILL_CATALOG_NAME_OR_ALIAS", "SkillCatalog.kt")
            if matching_skills else (model_ref if hit else record["sources"])
        )
        copy = dict(record)
        model_ref = None
        if hit:
            model_ref = {
                "source_id": app_source["source_id"], "source_name": hit["name"],
                "evidence_type": "APP_MODEL_ENTRY", "locator": f'{hit["sourceFile"]}:{hit["sourceLine"]}@{BASELINE}',
            }
        aliases = list(record.get("aliases") or [])
        if hit and norm(hit["name"]) == norm(record["canonicalNameEn"]) and hit["name"] not in [record.get("canonicalNameEn"), record.get("nameIt"), *aliases]:
            aliases.append(hit["name"])
        copy.update({
            "aliases": aliases,
            "identitySource": "EXERCISE_DB_V2",
            "runtimeName": display_name,
            "runtimeModelNames": [hit["name"]] if hit else [],
            "modelMatchType": "AMBIGUOUS_ALIAS" if ambiguous_current_link else ("EXACT_NAME_OR_ALIAS" if exact_hit else ("NORMALIZED_NAME" if hit else "NO_MATCH")),
            "identityResolution": "AMBIGUOUS_CURRENT_MODEL_LINK" if ambiguous_current_link else ("MERGED_V2_AND_CURRENT_MODEL" if model_ref else "V2_ONLY"),
            "modelSourceRefs": [model_ref] if model_ref else [],
            "applicationMetadata": {
                "sourceEntries": [runtime_application_metadata(hit)] if hit and not ambiguous_current_link else [],
                "ambiguousSourceEntries": [runtime_application_metadata(hit)] if hit and ambiguous_current_link else [],
            },
            "equipmentRequired": eq,
            "equipmentStatus": eq_status,
            "level": level,
            "levelStatus": "KNOWN" if level else "UNKNOWN",
            "category": category,
            "role": role,
            "resistanceMode": mode,
            "movementPattern": movement_pattern,
            "movementPatternStatus": pattern_origin,
            "skillIds": sorted(set(matching_skills)),
            "progressionRelations": [edge for edge in progressions if edge.get("from") == record["id"] or edge.get("to") == record["id"]],
            "fieldProvenance": {
                "identity": {"status": "V2_STABLE_ID", "source": record["id"]},
                "equipmentRequired": {
                    "status": equipment_origin,
                    "source": equipment_provenance_source,
                },
                "movementPattern": {
                    "status": pattern_origin,
                    "source": pattern_provenance_source,
                },
                "resistanceMode": {"status": mode_status, "source": resistance_provenance_source},
                "level": {"status": "V2_DIRECT" if record.get("level") else ("APP_MODEL_DIRECT" if effective else "UNKNOWN"), "source": level_provenance_source},
                "category": {"status": "APP_MODEL_DIRECT" if effective else "APP_RULE_RUNTIME_CONVERSION", "source": category_provenance_source},
                "role": {"status": role_status, "source": role_provenance_source},
                "skillIds": {"status": "APP_MODEL_DIRECT" if matching_skills else ("AMBIGUOUS_IDENTITY" if ambiguous_current_link else "UNKNOWN"), "source": skill_provenance_source},
                "instructions": {"status": "V2_EVIDENCE_TIER", "executionEvidence": record.get("executionEvidence", "UNKNOWN"), "sources": record.get("sources", [])},
            },
        })
        v2_records.append(copy)
        v2_by_id[record["id"]] = copy

    original_current_only_source_entries = [entry for entry in runtime if entry["name"].strip().lower() not in exact_runtime_matches and entry["name"].strip().lower() not in unique_lookup]
    current_only_source_entries = [entry for entry in runtime if entry["name"].strip().lower() not in matched_legacy_names and entry["name"].strip().lower() not in unique_lookup]
    current_only, current_model_to_v2 = [], []
    for entry in current_only_source_entries:
        candidates = normalized_lookup.get(norm(entry["name"]), set())
        if len(candidates) == 1:
            target_id = next(iter(candidates))
            target = v2_by_id.get(target_id)
            if target is not None:
                target["runtimeModelNames"] = list(dict.fromkeys(target["runtimeModelNames"] + [entry["name"]]))
                target["identityResolution"] = "MERGED_V2_AND_CURRENT_MODEL"
                target["modelSourceRefs"].append({
                    "source_id": app_source["source_id"], "source_name": entry["name"],
                    "evidence_type": "APP_MODEL_ENTRY", "locator": f'{entry["sourceFile"]}:{entry["sourceLine"]}@{BASELINE}',
                })
                current_model_to_v2.append({"sourceName": entry["name"], "targetId": target_id})
                continue
        # Ambiguous V2 lookup names are intentionally retained as distinct records.
        current_only.append(entry)
    current_only_source_entries = [
        entry for entry in current_only_source_entries
        if not any(entry["name"] == link["sourceName"] for link in current_model_to_v2)
    ]
    current_only = []
    consumed = set()
    for entry in current_only_source_entries:
        name_key = norm(entry["name"])
        if name_key in consumed:
            continue
        group = next((names for names in EQUIVALENT_CURRENT_NAMES.values() if name_key in {norm(n) for n in names}), None)
        if not group:
            current_only.append(entry)
            continue
        equivalent = [candidate for candidate in current_only_source_entries if norm(candidate["name"]) in {norm(n) for n in group}]
        consumed.update(norm(candidate["name"]) for candidate in equivalent)
        def signature(candidate):
            effective_candidate = effective_legacy_metadata(candidate)
            return (
                candidate.get("group"), candidate.get("equipmentLabel"), candidate.get("level"),
                candidate.get("role"), candidate.get("measure", "REPS"), candidate.get("targetSets", 3),
                candidate.get("targetValue"), effective_candidate["category"],
                effective_candidate["movementPattern"], effective_candidate["resistanceMode"],
            )
        if len({signature(candidate) for candidate in equivalent}) > 1:
            raise ValueError(f"reviewed duplicate names disagree on source metadata: {sorted(group)}")
        merged = dict(equivalent[0])
        merged["equivalentSourceEntries"] = equivalent
        merged["skills"] = list(dict.fromkeys(skill for candidate in equivalent for skill in candidate.get("skills", [])))
        current_only.append(merged)
    current_records = []
    for entry in current_only:
        effective = effective_legacy_metadata(entry)
        name = entry["name"]
        source_entries = entry.get("equivalentSourceEntries", [entry])
        source_refs = [{
            "source_id": app_source["source_id"], "source_name": source_entry["name"],
            "evidence_type": "APP_MODEL_ENTRY", "locator": f'{source_entry["sourceFile"]}:{source_entry["sourceLine"]}@{BASELINE}',
        } for source_entry in source_entries]
        source_ref = source_refs[0]
        matching_skills = _skill_ids(root, [name, *entry.get("skills", [])])
        equipment_rule_ref = _rule_source(equipment_label_source["source_id"], "APP_EQUIPMENT_LABEL_RULE", entry.get("equipmentLabel"))
        category_direct = "category" in entry.get("_explicit_fields", set()) and entry.get("category") != "OTHER"
        role_direct = "role" in entry.get("_explicit_fields", set())
        pattern_direct = "movementPattern" in entry.get("_explicit_fields", set()) and bool(entry.get("movementPattern"))
        resistance_direct = "resistanceMode" in entry.get("_explicit_fields", set()) and (entry.get("resistanceMode") != "NONE" or effective["category"] == "MOBILITY")
        measure_direct = "measure" in entry.get("_explicit_fields", set())
        skill_source = (
            _rule_source(skill_catalog_source["source_id"], "SKILL_CATALOG_NAME_OR_ALIAS", "SkillCatalog.kt")
            if matching_skills else source_ref
        )
        category_source = source_ref if category_direct else _rule_source(exercise_metadata_source["source_id"], "APP_EXERCISE_METADATA_RULE", "ExerciseMetadata.kt")
        pattern_source = source_ref if pattern_direct else _rule_source(exercise_metadata_source["source_id"], "APP_EXERCISE_METADATA_RULE", "ExerciseMetadata.kt")
        resistance_source = source_ref if resistance_direct else _rule_source(exercise_metadata_source["source_id"], "APP_EXERCISE_METADATA_RULE", "ExerciseMetadata.kt")
        role_source = source_ref if role_direct else _rule_source(app_source["source_id"], "APP_MODEL_DEFAULT", "MainActivity.kt:Exercise.role")
        measure_source = source_ref if measure_direct else _rule_source(app_source["source_id"], "APP_MODEL_DEFAULT", "MainActivity.kt:Exercise.measure")
        category_to_type = {
            "PUSH": "STRENGTH", "PULL": "STRENGTH", "LEGS": "STRENGTH", "CORE": "CORE",
            "CARDIO": "CONDITIONING", "MOBILITY": "MOBILITY", "SKILL": "SKILL", "OTHER": "STRENGTH",
        }
        field_provenance = {
            "identity": {"status": "APP_MODEL_DIRECT", "source": source_ref},
            "name": {"status": "APP_MODEL_DIRECT", "source": source_ref},
            "equipmentRequired": {"status": "APP_EQUIPMENT_LABEL_RULE" if effective["equipmentStatus"] == "KNOWN" else "UNKNOWN", "source": equipment_rule_ref},
            "level": {"status": "APP_MODEL_DIRECT", "source": source_ref},
            "category": {"status": "APP_MODEL_DIRECT" if category_direct else "APP_EXERCISE_METADATA_RULE", "source": category_source},
            "role": {"status": "APP_MODEL_DIRECT" if role_direct else "APP_MODEL_DEFAULT", "source": role_source},
            "movementPattern": {"status": "APP_MODEL_DIRECT" if pattern_direct else effective["movementPatternOrigin"], "source": pattern_source},
            "resistanceMode": {"status": "APP_MODEL_DIRECT" if resistance_direct else effective["resistanceModeOrigin"], "source": resistance_source},
            "measure": {"status": "APP_MODEL_DIRECT" if measure_direct else "APP_MODEL_DEFAULT", "source": measure_source},
            "skillIds": {"status": "APP_MODEL_DIRECT" if matching_skills else "UNKNOWN", "source": skill_source},
            "instructions": {"status": "MISSING", "source": None},
            "muscles": {"status": "MISSING", "source": None},
        }
        current_records.append({
            "id": _slug(name), "canonicalNameEn": name, "nameIt": name,
            "aliases": list(dict.fromkeys(source_entry["name"] for source_entry in source_entries if source_entry["name"] != name)),
            "movementType": category_to_type.get(effective["category"], "STRENGTH"),
            "family": re.sub(r"[^A-Z0-9]+", "_", str(entry["group"]).upper()).strip("_"),
            "relatedFamilies": [], "variantOf": None, "movementPattern": [effective["movementPattern"]],
            "equipment": effective["equipmentRequired"], "equipmentRequired": effective["equipmentRequired"],
            "equipmentStatus": effective["equipmentStatus"], "level": entry.get("level"),
            "levelStatus": "KNOWN" if entry.get("level") else "UNKNOWN",
            "measure": entry.get("measure", "REPS"), "descriptionIt": "", "setupIt": "",
            "executionStepsIt": [], "executionEvidence": "UNKNOWN", "techniqueCuesIt": [],
            "commonErrorsIt": [], "primaryMuscles": [], "secondaryMuscles": entry.get("secondaryGroups", []),
            "stabilizers": [], "rangeOfMotion": None,
            "unilateral": entry.get("laterality") in {"UNILATERAL", "ALTERNATING"},
            "isometric": entry.get("measure") == "SECONDS", "prerequisites": [],
            "prescription": [], "restGuidance": [], "contraindicationNotes": [],
            "sources": source_refs, "confidence": "LOW", "video": None,
            "identitySource": "CURRENT_MODEL", "runtimeName": name,
            "identityResolution": "MERGED_EQUIVALENT_LABELS" if len(source_entries) > 1 else "SINGLE_SOURCE_ENTRY",
            "sourceNames": [source_entry["name"] for source_entry in source_entries],
            "category": effective["category"], "resistanceMode": effective["resistanceMode"],
            "movementPatternStatus": effective["movementPatternOrigin"],
            "role": entry["role"], "skillLabels": entry.get("skills", []),
            "skillIds": matching_skills, "progressionKey": entry.get("progressionKey"),
            "applicationPrescription": {
                "sets": entry.get("targetSets", 3), "targetValue": entry.get("targetValue"),
                "measure": entry.get("measure", "REPS"), "sourceType": "APP_RULE",
            },
            "applicationMetadata": {
                "sourceEntries": [runtime_application_metadata(source_entry) for source_entry in source_entries],
                "ambiguousSourceEntries": [],
            },
            "equipmentLabel": entry["equipmentLabel"], "instructionCompleteness": "MISSING",
            "fieldProvenance": field_provenance,
        })

    all_records = v2_records + current_records
    external_enrichment = json.loads((root / EXTERNAL_ENRICHMENT_REL).read_text(encoding="utf-8"))
    records_by_id = {record["id"]: record for record in all_records}
    external_fields_applied = Counter()
    instruction_fields = {"descriptionIt", "setupIt", "executionStepsIt", "techniqueCuesIt", "commonErrorsIt"}
    for exercise_id, field_values in external_enrichment.get("exercises", {}).items():
        if exercise_id not in records_by_id:
            raise ValueError(f"external enrichment targets unknown canonical ID: {exercise_id}")
        record = records_by_id[exercise_id]
        provenance = record.setdefault("fieldProvenance", {})
        for field, evidence in field_values.items():
            value = evidence["value"]
            source_ref = {
                "source_id": ACE_EXERCISE_LIBRARY_SOURCE["source_id"],
                "source_name": evidence["source_name"],
                "evidence_type": evidence["evidence_type"],
                "locator": evidence["url"],
            }
            if field == "equipmentRequired":
                value = list(dict.fromkeys(value))
                if any(token not in RECOGNIZED_EQUIPMENT for token in value):
                    raise ValueError(f"external enrichment has unrecognized equipment for {exercise_id}: {value}")
                record["equipmentRequired"] = value
                record["equipment"] = value
                record["equipmentStatus"] = "KNOWN"
            elif field == "level":
                if value not in {"Principiante", "Intermedio", "Avanzato"}:
                    raise ValueError(f"external enrichment has invalid level for {exercise_id}: {value}")
                record["level"] = value
                record["levelStatus"] = "KNOWN"
            elif field in instruction_fields:
                if field in {"executionStepsIt", "techniqueCuesIt", "commonErrorsIt"} and not isinstance(value, list):
                    raise ValueError(f"external enrichment expects a list for {exercise_id}.{field}")
                if field in {"descriptionIt", "setupIt"} and not isinstance(value, str):
                    raise ValueError(f"external enrichment expects text for {exercise_id}.{field}")
                record[field] = value
            else:
                raise ValueError(f"external enrichment field is not yet supported: {field}")
            provenance[field] = {
                "status": "EXTERNAL_SOURCE_DIRECT",
                "confidence": evidence["confidence"],
                "source": source_ref,
            }
            if "observed_value" in evidence:
                provenance[field]["observedValue"] = evidence["observed_value"]
            if "mapping" in evidence:
                provenance[field]["mapping"] = evidence["mapping"]
            if field in instruction_fields:
                record["fieldProvenance"]["instructions"] = {
                    "status": "EXTERNAL_SOURCE_DIRECT",
                    "confidence": evidence["confidence"],
                    "source": source_ref,
                }
                if "observed_value" in evidence:
                    record["fieldProvenance"]["instructions"]["observedValue"] = evidence["observed_value"]
                record["executionEvidence"] = "EXTERNAL_TECHNICAL_REFERENCE"
                record["instructionCompleteness"] = "SUPPORTED_EXTERNAL_REFERENCE"
            if source_ref not in record.setdefault("sources", []):
                record["sources"].append(source_ref)
            external_fields_applied[field] += 1
    # Resistance-mode tranche: after reviewed enrichment has finalized equipment,
    # promote only deterministic external-load cases. This deliberately excludes
    # bands, assistance, bodyweight apparatus and ambiguous implements.
    external_load_tokens = {"barbell", "dumbbell", "cable", "machine", "kettlebell", "leg_press"}
    for record in all_records:
        if (
            record.get("resistanceMode") in {None, "UNKNOWN"}
            and record.get("equipmentStatus") == "KNOWN"
            and any(token in external_load_tokens for token in (record.get("equipmentRequired") or []))
        ):
            record["resistanceMode"] = "EXTERNAL_LOAD"
            record.setdefault("fieldProvenance", {})["resistanceMode"] = {
                "status": "EQUIPMENT_DETERMINISTIC",
                "confidence": "HIGH",
                "source": _rule_source(
                    equipment_resistance_source["source_id"],
                    "KNOWN_EXTERNAL_EQUIPMENT_TO_RESISTANCE_MODE",
                    record.get("equipmentRequired") or [],
                ),
                "derivation": "Known canonical external-load equipment deterministically maps to EXTERNAL_LOAD.",
            }

    # BODYWEIGHT tranche: classify only reviewed self-loaded movement families.
    # Empty equipment means no apparatus; the three allowed apparatus tokens are
    # supports/anchors rather than an external resistance source for these families.
    bodyweight_families = {
        "PUSH_UP", "HANDSTAND", "HANDSTAND_PUSH_UP", "PLANCHE", "FRONT_LEVER",
        "BACK_LEVER", "L_SIT", "MUSCLE_UP", "PULL_UP", "DIP", "SQUAT",
        "SINGLE_LEG_SQUAT",
    }
    bodyweight_support_equipment = {"pull_up_bar", "dip_bars", "rings"}
    bodyweight_exclusion_terms = ("weighted", "assisted", "band")
    bodyweight_candidates = []
    for record in all_records:
        equipment = record.get("equipmentRequired") or []
        name_key = str(record.get("canonicalNameEn") or "").lower()
        exclusions_verified = {
            "equipmentKnown": record.get("equipmentStatus") == "KNOWN",
            "noExternalLoadEquipment": not any(token in external_load_tokens for token in equipment),
            "noWeightedAssistedBandName": not any(term in name_key for term in bodyweight_exclusion_terms),
            "supportEquipmentOnly": not equipment or all(token in bodyweight_support_equipment for token in equipment),
            "reviewedSelfLoadedFamily": record.get("family") in bodyweight_families,
            "unambiguousCanonicalIdentity": record.get("identityResolution") != "AMBIGUOUS_CURRENT_MODEL_LINK",
        }
        if record.get("resistanceMode") in {None, "UNKNOWN"} and all(exclusions_verified.values()):
            bodyweight_candidates.append({
                "id": record["id"],
                "canonicalNameEn": record.get("canonicalNameEn"),
                "family": record.get("family"),
                "equipmentRequired": equipment,
                "movementType": record.get("movementType"),
                "reason": "Reviewed self-loaded family with known no-load/support-only equipment.",
                "exclusionsVerified": exclusions_verified,
                "confidence": "HIGH",
            })
            record["resistanceMode"] = "BODYWEIGHT"
            record.setdefault("fieldProvenance", {})["resistanceMode"] = {
                "status": "EQUIPMENT_FAMILY_DETERMINISTIC",
                "confidence": "HIGH",
                "source": _rule_source(
                    equipment_resistance_source["source_id"],
                    "KNOWN_SUPPORT_EQUIPMENT_AND_SELF_LOADED_FAMILY_TO_BODYWEIGHT",
                    {"family": record.get("family"), "equipment": equipment},
                ),
                "derivation": "Known no-load/support-only equipment plus reviewed self-loaded family deterministically maps to BODYWEIGHT; weighted/assisted/band names are excluded.",
            }
    bodyweight_candidates.sort(key=lambda item: item["id"])

    # NONE tranche: only non-resistance mobility/flexibility/SMR work with
    # known equipment. Foam rollers are explicitly treated as SMR tools, not
    # resistance sources. All other equipment is excluded from this rule.
    none_types = {"MOBILITY", "FLEXIBILITY", "SMR"}
    none_families = {"STRETCH", "SMR", "MOBILITY", "LOWER_BODY_MOBILITY", "TRUNK_MOBILITY", "SHOULDER_MOBILITY"}
    none_allowed_equipment = {"foam_roller"}
    none_exclusion_terms = ("weighted", "assisted", "resisted", "band")
    none_candidates = []
    for record in all_records:
        equipment = record.get("equipmentRequired") or []
        name_key = str(record.get("canonicalNameEn") or "").lower()
        mobility_semantics = record.get("movementType") in none_types and (
            record.get("family") in none_families
            or "mobility" in (record.get("movementPattern") or [])
            or "recovery" in (record.get("movementPattern") or [])
        )
        exclusions_verified = {
            "equipmentKnown": record.get("equipmentStatus") == "KNOWN",
            "mobilityFlexibilityOrSmrSemantics": mobility_semantics,
            "noResistanceVariantTerms": not any(term in name_key for term in none_exclusion_terms),
            "noResistanceSourceEquipment": not equipment or all(token in none_allowed_equipment for token in equipment),
            "notBodyweightStrengthFamily": record.get("family") not in bodyweight_families,
            "unambiguousCanonicalIdentity": record.get("identityResolution") != "AMBIGUOUS_CURRENT_MODEL_LINK",
        }
        if record.get("resistanceMode") in {None, "UNKNOWN"} and all(exclusions_verified.values()):
            none_candidates.append({
                "id": record["id"],
                "canonicalNameEn": record.get("canonicalNameEn"),
                "family": record.get("family"),
                "movementType": record.get("movementType"),
                "equipmentRequired": equipment,
                "movementPattern": record.get("movementPattern") or [],
                "candidateResistanceMode": "NONE",
                "reason": "Mobility/flexibility/SMR semantics with known no-resistance equipment; foam roller is modeled as an SMR tool, not a resistance source.",
                "existingResistanceModeProvenance": (record.get("fieldProvenance") or {}).get("resistanceMode"),
                "exclusionsVerified": exclusions_verified,
                "confidence": "HIGH",
            })
            record["resistanceMode"] = "NONE"
            record.setdefault("fieldProvenance", {})["resistanceMode"] = {
                "status": "MOBILITY_SEMANTICS_DETERMINISTIC",
                "confidence": "HIGH",
                "source": _rule_source(
                    equipment_resistance_source["source_id"],
                    "KNOWN_NON_RESISTANCE_MOBILITY_OR_SMR_TO_NONE",
                    {"family": record.get("family"), "type": record.get("movementType"), "equipment": equipment},
                ),
                "derivation": "Known mobility/flexibility/SMR record with no resistance-source equipment maps to NONE; foam roller is explicitly an SMR tool, while bands, stability balls, suspension trainers, rings, bars, machines/cables and external loads are excluded.",
            }
    none_candidates.sort(key=lambda item: item["id"])

    # BODYWEIGHT-general tranche: expand only to reviewed general/core/conditioning
    # families whose canonical record has known zero equipment. No apparatus is
    # admitted here: stability balls, suspension trainers, bands, bars, rings,
    # machines/cables and all external loads remain outside this rule.
    bodyweight_general_families = {"CORE", "CONDITIONING", "GENERAL", "LOCOMOTION"}
    bodyweight_general_types = {"CORE", "CONDITIONING", "CALISTHENICS"}
    bodyweight_general_exclusion_terms = ("weighted", "assisted", "band", "resisted", "machine", "cable")
    bodyweight_general_candidates = []
    for record in all_records:
        equipment = record.get("equipmentRequired") or []
        name_key = str(record.get("canonicalNameEn") or "").lower()
        exclusions_verified = {
            "equipmentKnown": record.get("equipmentStatus") == "KNOWN",
            "zeroEquipment": not equipment,
            "reviewedSelfLoadedGeneralFamily": record.get("family") in bodyweight_general_families,
            "reviewedMovementType": record.get("movementType") in bodyweight_general_types,
            "noWeightedAssistedBandOrMachineName": not any(term in name_key for term in bodyweight_general_exclusion_terms),
            "unambiguousCanonicalIdentity": record.get("identityResolution") != "AMBIGUOUS_CURRENT_MODEL_LINK",
        }
        if record.get("resistanceMode") in {None, "UNKNOWN"} and all(exclusions_verified.values()):
            bodyweight_general_candidates.append({
                "id": record["id"],
                "canonicalNameEn": record.get("canonicalNameEn"),
                "family": record.get("family"),
                "movementType": record.get("movementType"),
                "equipmentRequired": equipment,
                "reason": "Reviewed CORE/CONDITIONING/GENERAL/LOCOMOTION self-loaded movement with known zero equipment and no alternate resistance qualifier.",
                "exclusionsVerified": exclusions_verified,
                "confidence": "HIGH",
            })
            record["resistanceMode"] = "BODYWEIGHT"
            record.setdefault("fieldProvenance", {})["resistanceMode"] = {
                "status": "GENERAL_SELF_LOADED_DETERMINISTIC",
                "confidence": "HIGH",
                "source": _rule_source(
                    equipment_resistance_source["source_id"],
                    "KNOWN_SELF_LOADED_GENERAL_OR_CORE_TO_BODYWEIGHT",
                    {"family": record.get("family"), "type": record.get("movementType"), "equipment": equipment},
                ),
                "derivation": "Known zero-equipment CORE/CONDITIONING/GENERAL/LOCOMOTION movement in a reviewed self-loaded type deterministically maps to BODYWEIGHT; weighted, assisted, band/resisted, machine/cable, apparatus and ambiguous identities are excluded.",
            }
    bodyweight_general_candidates.sort(key=lambda item: item["id"])

    # RECORD-FIRST tranche 1: resistance-band records selected from the
    # residual resistanceMode blocker. Audit the complete record, but promote
    # only fields independently supported by deterministic canonical evidence.
    record_first_band_candidates = []
    band_pattern_by_name = {
        "ex-banded-biceps-curl": "pull-accessory",
        "ex-banded-deadlift": "hinge",
        "ex-banded-deltoid-row": "horizontal-pull",
        "ex-banded-front-raise": "push-accessory",
        "ex-banded-lateral-raise": "push-accessory",
        "ex-banded-lying-leg-extension": "knee-dominant",
        "ex-banded-triceps-extension": "push-accessory",
        "ex-banded-unilateral-triceps-extension": "push-accessory",
        "ex-banded-upright-row": "vertical-pull",
        "ex-rotator-cuff-stabilization-with-theraband": "mobility",
        "ex-seated-unilateral-banded-leg-curl": "knee-dominant",
    }
    for record in all_records:
        equipment = record.get("equipmentRequired") or []
        if record.get("resistanceMode") not in {None, "UNKNOWN"} or record.get("equipmentStatus") != "KNOWN" or equipment != ["resistance_band"]:
            continue
        missing_before = []
        if not record.get("level"): missing_before.append("level")
        if not record.get("movementPattern"): missing_before.append("movementPattern")
        if record.get("resistanceMode") in {None, "UNKNOWN"}: missing_before.append("resistanceMode")
        if record.get("movementType") == "SKILL" and not record.get("skillIds"): missing_before.append("skillLink")
        for field in ("descriptionIt", "setupIt", "executionStepsIt", "techniqueCuesIt", "commonErrorsIt"):
            if not record.get(field): missing_before.append(field)
        resolved = []
        record["resistanceMode"] = "EXTERNAL_LOAD"
        record.setdefault("fieldProvenance", {})["resistanceMode"] = {
            "status": "EQUIPMENT_DETERMINISTIC",
            "confidence": "HIGH",
            "source": _rule_source(
                equipment_resistance_source["source_id"],
                "KNOWN_RESISTANCE_BAND_TO_EXTERNAL_LOAD",
                {"equipment": equipment},
            ),
            "derivation": "A verified resistance_band is itself the external resistance source; no inference is made to level, instructions, or other fields.",
        }
        resolved.append({
            "field": "resistanceMode", "value": "EXTERNAL_LOAD", "confidence": "HIGH",
            "evidence": "verified resistance_band equipment",
            "source": equipment_resistance_source["source_id"],
        })
        pattern = band_pattern_by_name.get(record["id"])
        if pattern and not record.get("movementPattern"):
            record["movementPattern"] = [pattern]
            record["movementPatternStatus"] = "KNOWN"
            record.setdefault("fieldProvenance", {})["movementPattern"] = {
                "status": "REVIEWED_CANONICAL_MECHANICS_RULE",
                "confidence": "HIGH",
                "source": _rule_source(
                    family_pattern_source["source_id"],
                    "RECORD_FIRST_BAND_MOVEMENT_MECHANICS",
                    {"id": record["id"], "family": record.get("family"), "canonicalNameEn": record.get("canonicalNameEn")},
                ),
                "derivation": "The canonical movement label and family uniquely identify the broad movement mechanic; resistance-band presence does not determine this field.",
            }
            resolved.append({
                "field": "movementPattern", "value": [pattern], "confidence": "HIGH",
                "evidence": "canonical movement identity plus reviewed family mechanics",
                "source": family_pattern_source["source_id"],
            })
        missing_after = []
        if not record.get("level"): missing_after.append("level")
        if not record.get("movementPattern"): missing_after.append("movementPattern")
        if record.get("resistanceMode") in {None, "UNKNOWN"}: missing_after.append("resistanceMode")
        if record.get("movementType") == "SKILL" and not record.get("skillIds"): missing_after.append("skillLink")
        for field in ("descriptionIt", "setupIt", "executionStepsIt", "techniqueCuesIt", "commonErrorsIt"):
            if not record.get(field): missing_after.append(field)
        record_first_band_candidates.append({
            "id": record["id"], "canonicalNameEn": record.get("canonicalNameEn"),
            "missingFieldsBefore": missing_before, "resolvedFields": resolved,
            "remainingIncompleteFields": missing_after,
            "fullyComplete": not missing_after,
            "exclusionReason": None,
        })
    record_first_band_candidates.sort(key=lambda item: item["id"])

    from tools.validate_exercise_db_canonical import APP_SUPPORTED_EQUIPMENT, validate_database
    all_sources = sources + [
        app_source, equipment_rule_source, pattern_rule_source, family_pattern_source,
        exercise_metadata_source, adapter_source, skill_catalog_source,
        equipment_resistance_source, type_resistance_source,
        equipment_label_source,
    ]
    if external_enrichment.get("exercises"):
        all_sources.append(ACE_EXERCISE_LIBRARY_SOURCE)
    structural_validation = validate_database(all_records, progressions, all_sources)
    ids = [record["id"] for record in all_records]
    runtime_duplicate_names = defaultdict(list)
    for record in all_records:
        runtime_duplicate_names[norm(record.get("runtimeName"))].append(record["id"])
    duplicate_runtime_names = {name: ids for name, ids in runtime_duplicate_names.items() if name and len(ids) > 1}
    id_counts = Counter(ids)
    dangling_edges = [edge for edge in progressions if edge.get("from") not in set(ids) or edge.get("to") not in set(ids)]
    equipment_unknown = [record["id"] for record in all_records if record.get("equipmentStatus") != "KNOWN"]
    equipment_unsupported_by_app = [
        record["id"] for record in all_records
        if any(token in RECOGNIZED_EQUIPMENT and token not in APP_SUPPORTED_EQUIPMENT for token in record.get("equipmentRequired") or [])
    ]
    pattern_unknown = [record["id"] for record in all_records if not record.get("movementPattern")]
    instruction_missing = [record["id"] for record in all_records if not record.get("descriptionIt") or not record.get("setupIt") or not record.get("executionStepsIt")]
    level_unknown = [record["id"] for record in all_records if not record.get("level")]
    resistance_unknown = [record["id"] for record in all_records if record.get("resistanceMode") in {None, "UNKNOWN"}]
    skill_link_missing = [record["id"] for record in all_records if record.get("movementType") == "SKILL" and not record.get("skillIds")]
    app_metadata_entries = [
        app_entry
        for record in all_records
        for app_entry in (record.get("applicationMetadata") or {}).get("sourceEntries", [])
    ]
    ambiguous_app_metadata_entries = [
        app_entry
        for record in all_records
        for app_entry in (record.get("applicationMetadata") or {}).get("ambiguousSourceEntries", [])
    ]
    application_metadata_fields = sorted({
        field for app_entry in app_metadata_entries
        for field in (app_entry.get("snapshot") or {})
    })
    normalized_merges = [
        {"targetId": record["id"], "canonicalName": record["canonicalNameEn"], "runtimeNames": record["runtimeModelNames"]}
        for record in v2_records if record.get("modelMatchType") == "NORMALIZED_NAME"
    ]
    report = {
        "baseline": {"repository": "guglielmocitarella-rgb/HybridFitnessCoach", "branch": "feature/v0.9.5", "commit": BASELINE},
        "status": "CANDIDATE_NOT_RELEASE_READY",
        "counts": {
            "v2_records": len(v2_records), "current_model_entries": len(runtime),
            "current_model_only_source_entries": len(original_current_only_source_entries),
            "current_model_normalized_merges": sum(record.get("modelMatchType") == "NORMALIZED_NAME" for record in v2_records),
            "current_model_only": len(current_records), "current_model_duplicates_merged": len(current_only_source_entries) - len(current_records),
            "canonical_records": len(all_records),
            "progression_edges": len(progressions), "unique_ids": len(set(ids)),
            "equipment_unknown": len(equipment_unknown), "movement_pattern_missing": len(pattern_unknown),
            "equipment_unsupported_by_app": len(equipment_unsupported_by_app),
            "instruction_incomplete": len(instruction_missing), "runtime_duplicate_labels": len(duplicate_runtime_names),
            "level_unknown": len(level_unknown), "resistance_mode_unknown": len(resistance_unknown),
            "skill_links_missing": len(skill_link_missing),
            "runtime_application_entries": len(runtime),
            "application_metadata_snapshots": len(app_metadata_entries),
            "ambiguous_application_metadata_candidates": len(ambiguous_app_metadata_entries),
        },
        "reconciliation": {
            "handoff_expected_total": 1091,
            "materialized_total": len(all_records),
            "difference": len(all_records) - 1091,
            "documented_identity_merges": [
                {"nameKey": key, "sourceNames": sorted(names), "reason": "Same exercise label apart from punctuation; matching app-model fields; aliases and both provenances retained."}
                for key, names in EQUIVALENT_CURRENT_NAMES.items()
            ],
            "unique_normalized_v2_merges": normalized_merges,
            "runtime_duplicate_labels": duplicate_runtime_names,
            "ambiguous_v2_lookup_names": {name: sorted(ids) for name, ids in lookup.items() if len(ids) > 1},
        },
        "internalApplicationMetadataAudit": {
            "runtime_entries_parsed": len(runtime),
            "canonical_records_with_unambiguous_runtime_metadata": len({
                record["id"] for record in all_records
                if (record.get("applicationMetadata") or {}).get("sourceEntries")
            }),
            "unambiguous_source_snapshots": len(app_metadata_entries),
            "ambiguous_source_candidates": len(ambiguous_app_metadata_entries),
            "snapshot_fields": application_metadata_fields,
            "source_ids": [app_source["source_id"], exercise_metadata_source["source_id"]],
            "interpretation": "Snapshots preserve current app defaults and rules separately from exercise-science evidence and canonical prescriptions.",
        },
        "externalEnrichmentAudit": {
            "source_ids": [ACE_EXERCISE_LIBRARY_SOURCE["source_id"]] if external_fields_applied else [],
            "fields_applied": dict(sorted(external_fields_applied.items())),
            "exercise_ids": sorted(external_enrichment.get("exercises", {})),
            "unresolved_conflicts": external_enrichment.get("unresolved_conflicts", []),
        },
        "acceptance": {
            "stable_unique_ids": len(id_counts) == len(all_records) and all(count == 1 for count in id_counts.values()),
            "structural_integrity": structural_validation["ok"],
            "all_equipment_known": not equipment_unknown,
            "app_equipment_compatibility": not equipment_unsupported_by_app,
            "no_missing_movement_patterns": not pattern_unknown,
            "all_instructions_supported": not instruction_missing,
            "all_selection_critical_known": not (equipment_unknown or pattern_unknown or level_unknown or resistance_unknown or skill_link_missing),
            "progressions_referentially_integral": not dangling_edges,
        },
        "unresolved_ids": {
            "equipment_unknown": equipment_unknown,
            "equipment_unsupported_by_app": equipment_unsupported_by_app,
            "movement_pattern_missing": pattern_unknown,
            "instruction_incomplete": instruction_missing,
            "level_unknown": level_unknown,
            "resistance_mode_unknown": resistance_unknown,
            "skill_links_missing": skill_link_missing,
            "dangling_progression_edges": dangling_edges,
        },
        "policy": "APP_MODEL and APP_RULE fields identify software-model provenance, not exercise-science evidence. External fields use the separate reviewed enrichment input with per-field source URL, evidence type, and confidence. UNKNOWN remains explicit; no guidance or progression edge is invented.",
        "structuralValidation": structural_validation,
    }
    output_dir.mkdir(parents=True, exist_ok=True)
    resistance_unknown_records = []
    for record in sorted(
        (record for record in all_records if record.get("resistanceMode") in {None, "UNKNOWN"}),
        key=lambda record: record["id"],
    ):
        resistance_unknown_records.append({
            "id": record["id"],
            "canonicalNameEn": record.get("canonicalNameEn"),
            "family": record.get("family"),
            "movementType": record.get("movementType"),
            "equipmentRequired": record.get("equipmentRequired", []),
            "equipmentStatus": record.get("equipmentStatus"),
            "level": record.get("level"),
            "movementPattern": record.get("movementPattern", []),
            "resistanceMode": "UNKNOWN",
            "resistanceModeProvenance": (record.get("fieldProvenance") or {}).get("resistanceMode"),
            "equipmentProvenance": (record.get("fieldProvenance") or {}).get("equipmentRequired"),
            "identitySource": record.get("identitySource"),
            "modelSourceRefs": record.get("modelSourceRefs", []),
            "sources": record.get("sources", []),
            "applicationMetadata": record.get("applicationMetadata", {}),
        })
    if len(resistance_unknown_records) != report["counts"]["resistance_mode_unknown"]:
        raise ValueError(
            "resistance UNKNOWN operational report count disagrees with canonical audit: "
            f"{len(resistance_unknown_records)} != {report['counts']['resistance_mode_unknown']}"
        )
    resistance_unknown_report = {
        "schemaVersion": 1,
        "generatedFrom": "exercise-db-canonical.json",
        "filter": "resistanceMode == UNKNOWN",
        "count": len(resistance_unknown_records),
        "records": resistance_unknown_records,
    }

    record_first_report = {
        "schemaVersion": 1,
        "strategy": "RECORD_FIRST_ENRICHMENT",
        "selectionBlocker": "resistanceMode UNKNOWN",
        "batch": "resistance-band-known-equipment",
        "screenedResistanceUnknown": 165,
        "count": len(record_first_band_candidates),
        "records": record_first_band_candidates,
        "summary": {
            "recordsAnalyzed": len(record_first_band_candidates),
            "recordsFullyComplete": sum(item["fullyComplete"] for item in record_first_band_candidates),
            "fieldsResolvedTotal": sum(len(item["resolvedFields"]) for item in record_first_band_candidates),
            "resistanceModeResolved": sum(any(field["field"] == "resistanceMode" for field in item["resolvedFields"]) for item in record_first_band_candidates),
            "movementPatternResolved": sum(any(field["field"] == "movementPattern" for field in item["resolvedFields"]) for item in record_first_band_candidates),
            "recordsStillPartial": sum(not item["fullyComplete"] for item in record_first_band_candidates),
        },
    }
    (output_dir / "RECORD_FIRST_ENRICHMENT_BATCH_01.json").write_text(json.dumps(record_first_report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    bodyweight_general_candidate_report = {
        "schemaVersion": 1,
        "screenedResistanceUnknown": 187,
        "rule": "KNOWN_SELF_LOADED_GENERAL_OR_CORE_TO_BODYWEIGHT",
        "count": len(bodyweight_general_candidates),
        "candidates": bodyweight_general_candidates,
    }
    (output_dir / "RESISTANCE_MODE_BODYWEIGHT_GENERAL_CANDIDATES.json").write_text(json.dumps(bodyweight_general_candidate_report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    none_candidate_report = {
        "schemaVersion": 1,
        "screenedResistanceUnknown": 214,
        "rule": "KNOWN_NON_RESISTANCE_MOBILITY_OR_SMR_TO_NONE",
        "count": len(none_candidates),
        "candidates": none_candidates,
    }
    (output_dir / "RESISTANCE_MODE_NONE_CANDIDATES.json").write_text(json.dumps(none_candidate_report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    bodyweight_candidate_report = {
        "schemaVersion": 1,
        "screenedResistanceUnknown": 309,
        "rule": "KNOWN_SUPPORT_EQUIPMENT_AND_SELF_LOADED_FAMILY_TO_BODYWEIGHT",
        "count": len(bodyweight_candidates),
        "candidates": bodyweight_candidates,
    }
    (output_dir / "RESISTANCE_MODE_BODYWEIGHT_CANDIDATES.json").write_text(json.dumps(bodyweight_candidate_report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (output_dir / "exercise-db-canonical.json").write_text(json.dumps(all_records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (output_dir / "exercise-db-canonical-audit.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (output_dir / "RESISTANCE_MODE_UNKNOWN.json").write_text(json.dumps(resistance_unknown_report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (output_dir / "sources.json").write_text(json.dumps(all_sources, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report


def main():
    root = pathlib.Path(__file__).resolve().parents[1]
    output_dir = root / "exercise-db-canonical"
    report = build_database(root, output_dir)
    print(json.dumps({"status": report["status"], "counts": report["counts"], "acceptance": report["acceptance"], "structuralValidation": report["structuralValidation"]}, ensure_ascii=False, indent=2))
    return 0 if report["acceptance"]["structural_integrity"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
