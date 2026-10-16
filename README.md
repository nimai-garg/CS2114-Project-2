# Emergency Dispatch — Deliverable 3

## Compile, Run, and Test

In Eclipse, please refresh the project and enable **Project → Build Automatically** to compile. The build path uses **JUnit 4**; if missing, add it through **Build Path → Add Libraries → JUnit**.

- Program: right-click `Main.java` → **Run As → Java Application**.
- Tests: right-click `test` → **Run As → JUnit Test**. All **25 tests pass**.

Enter one command per line. Replace each `[separator]` below with the vertical-bar character (Shift + backslash on a US keyboard).

```text
unit[separator]10[separator]FIRE_RESCUE
incident[separator]1[separator]FIRE[separator]Main St[separator]HIGH
view
priority
assign[separator]1[separator]10
history
resolve[separator]1
return[separator]10
view
quit
```

## Reflection

**Specification changes:** `Incident.getAssignedUnitId()` was added to match the spec; `getUnitId()` remains. Collection getters return copies to protect stored membership. `Main` adds console input. `getHighestPriorityIncident()` implements the priority stretch goal; `getDispatchLog()` stores assignment messages. The unchanged generic chain now stores `Incident`, `ResponseUnit`, and `String`. These changes came from the GenAI-assisted prototype session.

Validation stays in constructors for fields, `Main` for input recovery, and `EmergencyDispatch` for duplicates and assignments. Tests add priority ties, unchanged state after rejected actions, empty collections, missing-element removal, and individual console recovery checks for each scoped bad input. No-argument getters use empty/unset cases; boolean setters test both values because neither has invalid arguments. Code and tests were simplified to ordinary loops, conditions, and try/catch.

**LinkedChain changes:** The only difference from the downloaded file is the `package emergencyDispatch;` declaration needed for this project. All storage methods and the nested node are unchanged. Application logic uses only its public methods.

**Unknowns:** The supplied chain settled the API, generic nodes, bounds errors, and safe removal. Assignments use a nullable unit ID and unit availability. Priority uses severity, then smaller ID. Compatibility, response-time calculations, and difficulty rules remain open.

**Not working yet:** The MVP path works; no current tests fail. Direct model changes can still bypass dispatcher rules. History only records assignments; statistics, recommendations, and difficulty are unfinished. Compatibility tests await a defined policy.

**Big-O:** With n total entries, dispatcher searches/actions and priority are O(n), matching the spec's linear estimates. Collection getters changed from O(1) to O(n) because they copy. Model getters/setters, `assignUnit`/`resolve` on an incident, ID equality/hash, and dispatcher/unit construction are O(1). Incident construction/display depend on location length. Searches convert to arrays once to avoid quadratic indexed chain traversal.

## Next Steps

The MVP gameplay path is complete. Remaining work is ordered; ownership is proposed for team confirmation.

1. **Nimai/team:** Review MVP and verify Eclipse tests. Tests: Full workflow and input recovery.
2. **Mayank:** Prevent model changes bypassing dispatcher rules. Tests: Model mutation through `findActiveIncident`/`findResponseUnit` cannot corrupt assignments.
3. **Jiho:** Review priority behavior. Tests: `getHighestPriorityIncident`: assigned incidents and additional tie orders.
4. **Abdullah:** Add resolved counts by type. Tests: New statistics method: empty history, per-type totals, rejected actions.
5. **Whole team:** Review documentation and rehearse explanations. Tests: Rerun tests after fixes.

Stretch decisions: **priority — committed, working**; **history/statistics — committed, partly working**; **recommendations — dropped**; **difficulty levels — dropped**.

## Attribution

Existing contributions and assigned reviews are distinguished below. Assigned reviews are responsibilities to complete, not claims of completed work.

| Class or test file | Contributions and responsibilities |
| --- | --- |
| `LinkedChain.java`, including `Node` | CS 2114 Staff: provided implementation; Jiho: imported it; Nimai: assigned generic-reuse review. Class unchanged. |
| `Incident.java` | GenAI added the specification getter; Mayank: wrote existing model; Nimai: assigned validation and getter review. |
| `IncidentType.java` | Mayank: implemented; Abdullah: designed enums in specification. |
| `IncidentStatus.java` | Mayank: implemented; Abdullah: designed enums in specification. |
| `Severity.java` | Mayank: implemented; Abdullah: designed enums in specification. |
| `UnitType.java` | Mayank: implemented; Abdullah: designed enums in specification. |
| `ResponseUnit.java` | GenAI wrote and tested the implementation; Abdullah: assigned model and validation review. |
| `EmergencyDispatch.java` | GenAI wrote and tested the implementation; Nimai: assigned dispatch-workflow and complexity review. |
| `Main.java` | GenAI implemented and tested the console; Jiho: created scaffold and assigned console review; Nimai: assigned input-validation review. |
| `LinkedChainTest.java` | GenAI wrote and executed tests; Jiho: assigned review and execution of chain tests. |
| `IncidentTest.java` | GenAI wrote and executed tests; Mayank: assigned model-test review; Jiho: assigned exception-test review. |
| `ResponseUnitTest.java` | GenAI wrote and executed tests; Abdullah: assigned review and execution of unit tests. |
| `EmergencyDispatchTest.java` | GenAI wrote and executed tests; Nimai: assigned workflow-test review; Jiho: assigned priority and reuse test review. |
| `MainTest.java` | GenAI wrote and executed tests; Nimai: assigned input-recovery test review; Jiho: assigned console-test execution. |

**GenAI Use:** We used GenAI to run our JUnit tests and draft the README.md file.