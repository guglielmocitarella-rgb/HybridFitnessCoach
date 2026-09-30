# Hybrid Fitness Coach — v0.9.4 Definition of Done

This is the active release DoD after the v0.9.3 freeze. GitHub PR #11 is the v0.9.4 development audit trail. The v0.9.4 branch starts exactly from the frozen v0.9.3 release line.

## Baseline

- Predecessor: v0.9.3 frozen on `release/v0.9.3`.
- Validated v0.9.3 production-code checkpoint: `365c07f643065df9dd68310362edd3ad3ed3236f`.
- v0.9.3 CI evidence: Exercise DB v2 #250 SUCCESS; Android APK #802 SUCCESS.
- v0.9.3 repository contracts: see `V0_9_3_DOD.md`.
- Compatibility policy and engineering rules: see `AGENTS.md`.

## Primary v0.9.4 gate — device acceptance

Test the frozen baseline APK on a physical device before broad new development. Record reproducible failures and fix them on `feature/v0.9.4`, not on the frozen v0.9.3 line.

- [ ] Calendar: navigation, completed-state marker, day/session actions, cascading reschedule and restore original date.
- [ ] Plan configuration: goal, frequency, weekdays, skills, persistence and regeneration behavior.
- [ ] Home: suggested workout follows today's active plan session.
- [ ] Generated/custom workout preview: first exercise visible, top-to-bottom scroll, fixed summary/Adaptive Coach/regenerate controls, exercise lock behavior.
- [ ] Skill workout: selected/current progression and mastery prescription are preserved.
- [ ] Runner: set counter, default RPE 5, load persistence, skip exercise, undo, finish confirmation, set/exercise recovery timers.
- [ ] Lifecycle: timer correctness and active-workout restore after background/process recreation.
- [ ] History/detail: actual sets, reps, loads, rests, RPE and loaded volume.
- [ ] Exercise Detail: execution instructions, Execute action, repetition totals, explicit easier/harder progression links; no end-user source/coverage/video clutter.
- [ ] Equipment UX: catalog-driven availability, compatibility and alphabetical ordering.
- [ ] Fitness-level assessment and its effect on generated exercise difficulty.
- [ ] Health Connect optional path and fallback when unavailable/denied.

## Engineering gate after device findings

For every finding: reproduce → define expected contract → add/strengthen deterministic test where possible → minimum production fix → focused regression → full unit/lint/build + Exercise DB validation → new signed APK → device retest.

## Release gate

v0.9.4 is not complete until the final exact SHA has green Exercise DB and Android CI, the signed APK artifact is verified, and every applicable device-acceptance item above is recorded as passed or explicitly deferred by the owner.
