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
        for field in (
            "setRestSec", "exerciseRestSec", "secondaryGroups", "movementPattern", "category",
            "intensity", "systemicFatigue", "technicalComplexity", "measure", "laterality",
            "resistanceMode", "progressionKey", "targetSets", "targetValue", "intervalRestSec",
        ):
            if field in named:
                entry[field] = named[field]
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
    if resistance != "NONE" or category == "MOBILITY":
        resistance_origin = "APP_MODEL_DIRECT"
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
        alias_match = re.search(r'exerciseAliases\s*=\s*setOf\(([^)]*)\)', body)
        aliases = _value("setOf(" + alias_match.group(1) + ")") if alias_match else []
        nodes.append({"id": m.group(1), "exerciseName": m.group(2), "skill": m.group(3), "aliases": aliases})
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
        display_name = hit["name"] if hit else record.get("nameIt") or record["canonicalNameEn"]
        runtime_names[record["id"]] = display_name
        if hit:
            matched_legacy_names.add(hit["name"].strip().lower())
            if exact_hit:
                exact_runtime_matches.add(hit["name"].strip().lower())
        effective = effective_legacy_metadata(hit) if hit else None
        equipment = list(record.get("equipment") or [])
        if equipment:
            eq = list(dict.fromkeys(EQUIPMENT_ALIASES.get(str(x).casefold(), str(x)) for x in equipment))
            eq_status = "KNOWN" if all(token in RECOGNIZED_EQUIPMENT for token in eq) else "UNKNOWN"
        elif hit:
            eq, eq_status = effective["equipmentRequired"], effective["equipmentStatus"]
        else:
            eq, eq_status = [], "UNKNOWN"
        equipment_origin = "V2_DIRECT" if equipment else ("APP_MODEL_DIRECT" if hit and eq_status == "KNOWN" else "UNKNOWN")
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
        role = hit.get("role") if hit else "Base"
        role_status = "APP_MODEL_DIRECT" if hit else "APP_RULE_RUNTIME_DEFAULT"
        level = record.get("level") or (hit.get("level") if hit else None)
        skill_labels = hit.get("skills", []) if hit else []
        matching_skills = [] if ambiguous_current_link else _skill_ids(root, [display_name, record.get("canonicalNameEn"), record.get("nameIt"), *record.get("aliases", [])])
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
                    "source": ({"source_id": equipment_rule_source["source_id"], "evidence_type": "EXPLICIT_NAME_TOKEN", "locator": record.get("canonicalNameEn")} if equipment_origin == "NAME_EXPLICIT_EQUIPMENT_RULE" else record["sources"]),
                },
                "movementPattern": {
                    "status": pattern_origin,
                    "source": (
                        {"source_id": pattern_rule_source["source_id"], "evidence_type": "V2_MOVEMENT_TYPE_RULE", "locator": record.get("movementType")} if pattern_origin == "V2_MOVEMENT_TYPE_RULE" else
                        {"source_id": family_pattern_source["source_id"], "evidence_type": "V2_FAMILY_PATTERN_RULE", "locator": record.get("family")} if pattern_origin == "V2_FAMILY_PATTERN_RULE" else
                        ("ExerciseMetadata.kt" if pattern_origin.startswith("APP_RULE") else record["sources"])
                    ),
                },
                "resistanceMode": {"status": mode_status, "source": "ExerciseMetadata.kt" if mode_status.startswith("APP_RULE") else record["sources"]},
                "level": {"status": "V2_DIRECT" if record.get("level") else ("APP_MODEL_DIRECT" if hit else "UNKNOWN"), "source": record["sources"]},
                "category": {"status": "APP_MODEL_DIRECT" if effective else "APP_RULE_RUNTIME_CONVERSION", "source": hit["sourceFile"] + ":" + str(hit["sourceLine"]) if hit else "ExerciseDbV2.toLegacyExercise"},
                "role": {"status": role_status, "source": hit["sourceFile"] + ":" + str(hit["sourceLine"]) if hit else "ExerciseDbV2.toLegacyExercise"},
                "skillIds": {"status": "APP_MODEL_DIRECT" if matching_skills else ("AMBIGUOUS_IDENTITY" if ambiguous_current_link else "UNKNOWN"), "source": hit["sourceFile"] + ":" + str(hit["sourceLine"]) if hit else None},
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
        category_to_type = {
            "PUSH": "STRENGTH", "PULL": "STRENGTH", "LEGS": "STRENGTH", "CORE": "CORE",
            "CARDIO": "CONDITIONING", "MOBILITY": "MOBILITY", "SKILL": "SKILL", "OTHER": "STRENGTH",
        }
        field_provenance = {
            "identity": {"status": "APP_MODEL_DIRECT", "source": source_ref["locator"]},
            "name": {"status": "APP_MODEL_DIRECT", "source": source_ref["locator"]},
            "equipmentRequired": {"status": effective["equipmentStatus"], "source": source_ref["locator"]},
            "level": {"status": "APP_MODEL_DIRECT", "source": source_ref["locator"]},
            "category": {"status": "APP_MODEL_DIRECT" if entry.get("category") not in {None, "OTHER"} else "APP_RULE_GROUP_OR_ROLE", "source": source_ref["locator"]},
            "role": {"status": "APP_MODEL_DIRECT", "source": source_ref["locator"]},
            "movementPattern": {"status": effective["movementPatternOrigin"], "source": "ExerciseMetadata.kt" if effective["movementPatternOrigin"].startswith("APP_RULE") else source_ref["locator"]},
            "resistanceMode": {"status": effective["resistanceModeOrigin"], "source": "ExerciseMetadata.kt" if effective["resistanceModeOrigin"].startswith("APP_RULE") else source_ref["locator"]},
            "measure": {"status": "APP_MODEL_DIRECT", "source": source_ref["locator"]},
            "skillIds": {"status": "APP_MODEL_DIRECT" if matching_skills else "UNKNOWN", "source": source_ref["locator"]},
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
            "equipmentLabel": entry["equipmentLabel"], "instructionCompleteness": "MISSING",
            "fieldProvenance": field_provenance,
        })

    all_records = v2_records + current_records
    from tools.validate_exercise_db_canonical import validate_database
    structural_validation = validate_database(all_records, progressions, sources + [app_source, equipment_rule_source, pattern_rule_source, family_pattern_source])
    ids = [record["id"] for record in all_records]
    runtime_duplicate_names = defaultdict(list)
    for record in all_records:
        runtime_duplicate_names[norm(record.get("runtimeName"))].append(record["id"])
    duplicate_runtime_names = {name: ids for name, ids in runtime_duplicate_names.items() if name and len(ids) > 1}
    id_counts = Counter(ids)
    dangling_edges = [edge for edge in progressions if edge.get("from") not in set(ids) or edge.get("to") not in set(ids)]
    equipment_unknown = [record["id"] for record in all_records if record.get("equipmentStatus") != "KNOWN"]
    pattern_unknown = [record["id"] for record in all_records if not record.get("movementPattern")]
    instruction_missing = [record["id"] for record in all_records if not record.get("descriptionIt") or not record.get("setupIt") or not record.get("executionStepsIt")]
    level_unknown = [record["id"] for record in all_records if not record.get("level")]
    resistance_unknown = [record["id"] for record in all_records if record.get("resistanceMode") in {None, "UNKNOWN"}]
    skill_link_missing = [record["id"] for record in all_records if record.get("movementType") == "SKILL" and not record.get("skillIds")]
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
            "instruction_incomplete": len(instruction_missing), "runtime_duplicate_labels": len(duplicate_runtime_names),
            "level_unknown": len(level_unknown), "resistance_mode_unknown": len(resistance_unknown),
            "skill_links_missing": len(skill_link_missing),
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
        "acceptance": {
            "stable_unique_ids": len(id_counts) == len(all_records) and all(count == 1 for count in id_counts.values()),
            "structural_integrity": structural_validation["ok"],
            "all_equipment_known": not equipment_unknown,
            "no_missing_movement_patterns": not pattern_unknown,
            "all_instructions_supported": not instruction_missing,
            "all_selection_critical_known": not (equipment_unknown or pattern_unknown or level_unknown or resistance_unknown or skill_link_missing),
            "progressions_referentially_integral": not dangling_edges,
        },
        "unresolved_ids": {
            "equipment_unknown": equipment_unknown,
            "movement_pattern_missing": pattern_unknown,
            "instruction_incomplete": instruction_missing,
            "level_unknown": level_unknown,
            "resistance_mode_unknown": resistance_unknown,
            "skill_links_missing": skill_link_missing,
            "dangling_progression_edges": dangling_edges,
        },
        "policy": "APP_MODEL and APP_RULE fields identify software-model provenance, not exercise-science evidence. UNKNOWN remains explicit; no guidance or progression edge is invented.",
        "structuralValidation": structural_validation,
    }
    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "exercise-db-canonical.json").write_text(json.dumps(all_records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (output_dir / "exercise-db-canonical-audit.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (output_dir / "sources.json").write_text(json.dumps(sources + [app_source, equipment_rule_source, pattern_rule_source, family_pattern_source], ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report


def main():
    root = pathlib.Path(__file__).resolve().parents[1]
    output_dir = root / "exercise-db-canonical"
    report = build_database(root, output_dir)
    print(json.dumps({"status": report["status"], "counts": report["counts"], "acceptance": report["acceptance"], "structuralValidation": report["structuralValidation"]}, ensure_ascii=False, indent=2))
    return 0 if report["acceptance"]["structural_integrity"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
