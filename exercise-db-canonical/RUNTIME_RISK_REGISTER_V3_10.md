# Runtime Risk Register V3.10

| ID | Domain | Description | P | I | Severity | Detection | Mitigation | Rollback | Owner |
|---|---|---|---|---|---|---|---|---|---|
| R1 | identity | saved refs break | M | H | S1 | legacy fixtures | canonical resolver | old reader | C |
| R2 | equipment | vocabulary/semantics flatten | H | H | S1 | matrix | native requirements | legacy path | D |
| R3 | capability | level mapped to four dims | H | H | S1 | contract test | prohibit mapping | flag | D |
| R4 | prescription | dimensions/units lost | H | H | S1 | runner tests | typed model | old materializer | E |
| R5 | persistence | active/history unreadable | M | H | S1 | recreation | versioned readers | old schema reader | G |
| R6 | skill | duplicate graph truth | M | H | S1 | graph tests | SkillCatalog sole graph | disable bridge | F |
| R7 | unresolved | UNKNOWN silently coerced | M | H | S1 | negative tests | explicit state | fail closed | A-D |
| R8 | source truth | V2/V3 divergence | H | M | S2 | diagnostics | V3 authoritative | flag | B |
| R9 | performance | parse/startup regression | L | M | S3 | benchmark | indexes/lazy diagnostics | old loader | B/G |
| R10 | hidden legacy | name/text consumer remains | H | H | S1 | consumer map | migrate all consumers | flag | D-G |