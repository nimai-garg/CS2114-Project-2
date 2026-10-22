# Contracts, complexity, and test coverage

The final JUnit suite contains 39 `@Test` methods across five files. Each test may make several assertions or iterate through several invalid inputs. Run `sh scripts/test.sh` to execute the entire suite. No test is disabled or skipped.

## Generic storage contract

`LinkedChain<T>` has an unbounded type parameter, a private `Node<E>` class, `firstNode`, and `numberOfEntries`. A node holds one nonnull entry and a next reference. Empty means `firstNode == null` and size zero; otherwise size equals the number of reachable nodes and the final link is null. Neither application classes nor tests access nodes. The class is the course-provided implementation, imported with the application package declaration.

The chain permits duplicate entries, compares with `equals`, and has no domain-specific sorting. `add` inserts at the front; `addToEnd` preserves insertion order. It stores/retrieves its own entry references; copying domain objects is the dispatcher's responsibility.

| Public operation | Behavior and edge case | Time | JUnit evidence in `LinkedChainTest` |
| --- | --- | --- | --- |
| `LinkedChain()` | Empty chain. | O(1) | `emptyAndMissingOperations` |
| `add(T)` | Add at front; null throws `IllegalArgumentException`. | O(1) | `addRetrieveDuplicatesAndRemoveAllPositions`; `invalidArgumentsDoNotModifyChain` |
| `addToEnd(T)` | Append; null throws `IllegalArgumentException`. | O(n) | Same tests; traverses to final node. |
| `remove()` | Remove first; empty returns null. | O(1) | `addRetrieveDuplicatesAndRemoveAllPositions`; `emptyAndMissingOperations` |
| `remove(T)` | Remove first equal entry; missing/null/empty returns false. | O(n) | Same tests; includes head, middle, tail, duplicate and missing removal. |
| `clear()` | Reset head and size; already empty is safe. | O(1) | Same tests. |
| `get(int)` | Zero-based retrieval; negative or index >= size throws `IndexOutOfBoundsException`. | O(n) | All three tests, including empty and boundary indexes. |
| `contains(T)` | True if equal entry exists; null/missing returns false. | O(n) | Normal and empty tests. |
| `count(T)` | Count equal entries; null/missing returns zero. | O(n) | Normal and empty tests; duplicates included. |
| `size()` | Stored entry count, including zero. | O(1) | All three tests verify counts after actions/rejections. |
| `isEmpty()` | True exactly when size is zero. | O(1) | Normal and empty tests. |
| `toArray(T[])` | Fill caller's array; null/undersized throws `IllegalArgumentException`. | O(n) | Normal, empty and invalid tests. |
| `toString()` | First-to-last entries in brackets; empty is `[]`. | O(n) | Normal and empty tests. |

## Application ownership and validation

`EmergencyDispatch` owns copies of registered models and changes only its own stored objects. Finders, priority, and collection queries return detached copies. Changes to the chain or models in a query result do not affect the system. Results are snapshots, so repeat a query after an action to obtain updated state.

Incident IDs are unique across active and resolved chains. Unit IDs are unique within the unit chain; incident and unit IDs may coincide. Every stored incident is in exactly one incident chain, and its status matches that chain. A unit assigned to an active incident is unavailable and cannot be assigned again. Resolution preserves the assignment ID and keeps the unit busy until an explicit return. Assignment messages record successful assignments only, in chronological order. Resolved statistics are derived from actual resolved records, avoiding a second counter that can drift.

| Bad input or state | Validation owner | Result |
| --- | --- | --- |
| Negative model ID; null type/severity; null/blank location | `Incident` / `ResponseUnit` constructor | `IllegalArgumentException`; no object is registered. |
| Blank, nonnumeric, negative or overflowing console ID | `Main.parseId` | Error and another command allowed. |
| Invalid enum name; wrong number of fields; unknown/blank command | `Main` parser and `requireLength` | Error and another command allowed. |
| Null/duplicate registration; preassigned/resolved incident | `EmergencyDispatch.reportIncident` / `addResponseUnit` | False; stored state unchanged. |
| Missing/resolved incident, missing/busy unit, already assigned incident | `EmergencyDispatch.assignUnit` | False; no assignment/log entry. |
| Missing/already resolved incident | `EmergencyDispatch.resolveIncident` | False; no duplicate history/statistics. |
| Missing unit or unit still serving an active incident | `EmergencyDispatch.returnUnitToService` | False; availability unchanged. |
| Negative standalone assignment ID | `Incident.assignUnit` | `IllegalArgumentException`. |
| Standalone incident resolved/already assigned | `Incident.assignUnit` | `IllegalStateException`; original assignment unchanged. |
| Repeated standalone resolution / repeated availability setting | Model method | Safe and idempotent. |
| Null statistics type | `EmergencyDispatch.getResolvedCount` | `IllegalArgumentException`. |
| Empty/missing chain operations | `LinkedChain` | Contract above; console never performs invalid indexed retrieval. |

Field validation lives in models so standalone construction is valid. Cross-object rules live in the dispatcher because a single model cannot know whether a unit/incident is registered or busy elsewhere. Input recovery lives in `Main`; the generic chain has no emergency-specific logic.

## Public application methods and tests

Let `a`, `r`, `u`, and `h` be active incidents, resolved incidents, registered units, and successful assignments. Model copying is O(1) assuming bounded location length. Array snapshots cost linear temporary space. No indexed `get(i)` loop is used to search a chain.

### `EmergencyDispatch`

| Public method | Normal / bad-input or boundary behavior | Time | Test methods in `EmergencyDispatchTest` |
| --- | --- | --- | --- |
| Constructor | Four empty chains; queries/actions safe on empty system. | O(1) | `emptySystemAllQueriesAndActionsAreSafe` |
| `reportIncident(Incident)` | Registers an active, unassigned copy; null, duplicate, preassigned, or resolved returns false. | O(a+r) | `reportsRejectDuplicateResolvedAndAlreadyAssignedIncidents`; `inputObjectsCannotChangeStoredStateAfterRegistration` |
| `addResponseUnit(ResponseUnit)` | Stores a copy; null/duplicate returns false. Availability supplied by standalone unit is preserved. | O(u) | `unitRegistrationRejectsDuplicatesAndNull`; input ownership test |
| `assignUnit(int,int)` | Assign and mark busy; invalid state/IDs return false unchanged. Type choice remains user's responsibility. | O(a+u+h) | `fullMvpAndThirdTypeStorage`; `rejectedAssignmentsDoNotChangeStateOrLog`; finder ownership test |
| `resolveIncident(int)` | Move active record to resolved; missing/repeated returns false. Unassigned resolution allowed. | O(a) | `reportsRejectDuplicateResolvedAndAlreadyAssignedIncidents`; full workflow and statistics tests |
| `returnUnitToService(int)` | Free unit if no active assignment; missing/premature returns false. Already available returns true. | O(u+a) | `fullMvpAndThirdTypeStorage`; `unitCanServeSeveralIncidentsWithoutLosingResolvedAssignments` |
| `findActiveIncident(int)` | Detached snapshot; missing/negative/resolved returns null. | O(a) | Empty, registration, finder ownership and ID-boundary tests |
| `findResponseUnit(int)` | Detached snapshot; missing/negative returns null. | O(u) | Empty, registration, finder ownership and ID-boundary tests |
| `getHighestPriorityIncident()` | Severity descending then ID ascending; includes assigned active incidents; empty returns null; detached. | O(a) | `priorityUsesSeverityThenIdAndExcludesResolvedIncidents`; `priorityCoversEverySeverityAndEventuallyBecomesEmpty`; `priorityIncludesAssignedIncidentsAndBreaksTiesInEitherInsertionOrder`; snapshot tests |
| `getActiveIncidents()` | Detached chain and models; empty chain when none. Newest report first. | O(a) | `everyCollectionGetterProtectsMembershipAndPreservesOrder`; `collectionAndPrioritySnapshotsProtectModelState`; `snapshotsRemainDetachedAfterLaterDispatcherActions` |
| `getResponseUnits()` | Detached chain and models, including busy units; empty when none. Newest registration first. | O(u) | Same ownership tests; empty and workflow tests |
| `getResolvedIncidents()` | Detached chain and resolved records, retaining assigned-unit IDs; empty when none. Newest resolution first. | O(r) | Same ownership tests; full workflow and repeated-unit tests |
| `getDispatchLog()` | Separate chain of immutable Strings, oldest assignment first; empty when no successful dispatch. | O(h) | `thirdTypeLogPreservesOrderAcrossMultipleDispatches`; empty/rejection/membership tests |
| `getDispatchCount()` | Number of successful assignments, including still-active incidents; zero initially/rejections do not count. | O(1) | `statisticsCountOnlySuccessfulActionsAndCorrectTypes`; repeated-unit and empty-statistics tests |
| `getResolvedCount(IncidentType)` | Count resolved records of given type; zero for no matches; null throws. | O(r) | `statisticsCountOnlySuccessfulActionsAndCorrectTypes`; `statisticsRejectNullIncidentType`; `statisticsCoverEveryIncidentTypeAndAccumulateSameType` |

`assignUnit` includes O(h) because the unchanged chain's `addToEnd` traverses the history to append. This still matches the specification's O(n) estimate when n counts all relevant records. Snapshot getters were estimated O(1) in the spec; their implemented O(n) copies protect ownership. Returning the newest item first for most views reflects `add`, not priority sorting. Log ordering uses `addToEnd` deliberately.

### `Incident`

All identity/dispatch-state methods are O(1). Constructor, copy, and string formatting also depend on location length. `equals`/`hashCode` use only ID, which never changes.

| Public method(s) | Normal / invalid or boundary case | Evidence in `IncidentTest` |
| --- | --- | --- |
| `Incident(int,IncidentType,String,Severity)` | Store fields, trim location, start ACTIVE/unassigned; reject every invalid field. | `constructorAndEveryGetter`; `constructorRejectsEveryInvalidField` |
| `getId()` | Return stored ID; zero is a valid boundary. | `constructorAndEveryGetter` |
| `getType()` | Return constructor-validated type; null prevented at construction. | Constructor/getter and invalid-field tests |
| `getLocation()` | Return trimmed location; null/blank prevented at construction. | Same tests |
| `getSeverity()` | Return constructor-validated severity; null prevented at construction. | Same tests |
| `getStatus()` | ACTIVE initially, RESOLVED after resolution, repeated resolution safe. | Constructor/getter test; `repeatedResolutionIsSafe` |
| `getAssignedUnitId()` / `getUnitId()` | Same ID after assignment; null in unset state. | Constructor/getter and assignment tests |
| `assignUnit(int)` | Set ID including zero; negative, resolved, or already assigned rejects unchanged. | `constructorAndEveryGetter`; `assignmentRejectsInvalidStateWithoutChangingIt` |
| `resolve()` | Mark resolved; repeated call safe. | `repeatedResolutionIsSafe`; constructor/getter test |
| `equals(Object)` | Same ID equal; different ID/type/null false. | `identityHashAndDisplayStayValidAcrossStateChanges` |
| `hashCode()` | Equal IDs hash equally; state changes preserve hash. | Same identity test |
| `toString()` | Includes identity/details/status; unset unit represented as null. | Same identity test |

### `ResponseUnit`

All methods are O(1). Equality and hash use immutable ID.

| Public method(s) | Normal / invalid or boundary case | Evidence in `ResponseUnitTest` |
| --- | --- | --- |
| `ResponseUnit(int,UnitType)` | Valid ID/type, initially available; negative/null rejects. | `validConstructionGettersAndBothAvailabilityValues`; `invalidConstruction` |
| `getId()` | Return ID; zero boundary covered. | Valid-construction test |
| `getType()` | Return type; null prevented at construction. | Valid/invalid construction tests |
| `isAvailable()` | Initially true; false after assignment/setting. | Valid-construction test |
| `setAvailable(boolean)` | Accept both values; repeated setting safe. | Valid-construction test |
| `equals(Object)` | Equal ID true; different ID/type/null false. | `identityAndHashIgnoreAvailability` |
| `hashCode()` | Equal IDs same hash; availability does not change hash. | Same identity test |
| `toString()` | Includes identity, type and both availability values. | Valid-construction test |

No-argument getters cannot receive invalid arguments. Tests cover valid boundaries and empty/unset states; invalid model fields are rejected at construction. Both possible boolean values are valid. These are the applicable bad-state counterparts for such methods.

### `Main` and enums

`Main.main(String[])` is the only public console method. `MainTest` redirects input/output and restores both after each invocation. Tests cover the MVP path, every scoped bad input followed by valid input, each invalid assignment scenario, empty input/EOF/quit, statistics and history, case/whitespace, overflow/extra fields, and a Turkish default locale. `quit` with extra fields reports an error and continues; a valid `quit` stops processing later input. Console cost is the sum of the selected operations plus input/output length.

The four enums have no custom public methods; Java supplies `values`/`valueOf`. Invalid enum names are tested through `Main`, and all incident types are exercised by statistics tests. They require no separate duplicate tests of Java's generated implementation.
