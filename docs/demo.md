# Repository demonstration

Run from the repository root:

```sh
sh scripts/test.sh
sh scripts/run.sh
```

Use these commands live, pausing to explain the results:

```text
unit|10|FIRE_RESCUE
unit|20|EMT
incident|1|FIRE|Main Street|HIGH
incident|2|MEDICAL|Campus Clinic|CRITICAL
view
priority
incident|3|FIRE| |HIGH
assign|999|10
assign|1|10
return|10
assign|2|20
view
resolve|1
return|10
resolve|2
return|20
history
stats
view
quit
```

`view` retrieves `Incident` and `ResponseUnit` objects through the same `LinkedChain<T>` API. Point to the four typed chain fields in `EmergencyDispatch` and the private generic node in `LinkedChain`. `history` also retrieves `String` entries from that same generic class.

`priority` selects incident 2 (CRITICAL). The blank location produces `Invalid input`; the missing incident and premature unit return produce `Rejected`, and the program continues. After resolution/return, both units are available. History retains both assigned unit IDs. Statistics show 0 active incidents, 2 resolved incidents, 2 successful dispatches, 1 resolved FIRE and 1 resolved MEDICAL incident. Rejected operations did not increase counts.

For a repeatable smoke test:

```sh
sh scripts/run.sh < docs/demo-input.txt
```

For the presentation, each teammate should explain a portion they understand. Useful review points: why the chain permits duplicates while the dispatcher rejects duplicate IDs; why a snapshot does not update after a later assignment; why resolving an incident does not immediately free its unit; and why priority belongs in the dispatcher. The code is a simulation, and compatibility is the user's decision.

This guide is preparation for the live repository demo. The slide deck and team ethics reflection are separate work, as requested.
