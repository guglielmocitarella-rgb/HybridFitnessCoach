# Runtime Gap Matrix V3.10

| Domain | V3 | Current runtime | Lossless now? | Strategy | Risk |
|---|---|---|---|---|---|
| identity | canonical ID | progressionKey + name fallback | partial | canonical resolver + aliases | high |
| equipment/location | structured state/mode | Set<EquipmentId> + text | no | native requirements + distinct suspension_trainer | high |
| resistance/load/assistance | separated + units | ResistanceMode + kg fields | no | typed resistance/input/unit | high |
| four capability | 4 exercise requirements | movement capacities + levelScore | no | native V3 requirements; athlete bridge | critical |
| legacy level | metadata | eligibility input | no | UI/backcompat only; ignore native selection | high |
| category | functional | ExerciseCategory | mostly | explicit validated mapping | medium |
| group | anatomical/legacy | String | partial | retain metadata | low |
| movementPattern | 0..n + state | String | no | typed set; enhancing only | medium |
| measure/prescription | multidimensional | single measure/target | no | typed prescription | critical |
| skill bridge | typed FAMILY/NODE | skills/name aliases | no | canonical relation index | high |
| unresolved | explicit states | fallback/null/text | no | sealed explicit state | critical |
| provenance | record/field/relation | limited | no | build-time + diagnostics | low |
| persistence | versioned canonical refs | legacy JSON/name refs | partial | compatibility readers | critical |

All required domains have a migration strategy. Semantic-loss plan count: 0.