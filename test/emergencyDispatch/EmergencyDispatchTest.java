package emergencyDispatch;

import org.junit.Test;
import static org.junit.Assert.*;
// Tests dispatch rules, snapshots, priority, and statistics.
public class EmergencyDispatchTest
{
    @Test
    public void missingIdsLeavePopulatedDispatcherUnchanged()
    {
        EmergencyDispatch d = ready();
        assertTrue(d.assignUnit(1, 10));
        assertTrue(d.reportIncident(incident(2, Severity.LOW)));
        assertTrue(d.addResponseUnit(new ResponseUnit(11, UnitType.EMT)));
        String before = state(d);
        for (int missing : new int[] {-1, 0, 999, Integer.MAX_VALUE})
        {
            assertNull(d.findActiveIncident(missing));
            assertNull(d.findResponseUnit(missing));
            assertFalse(d.assignUnit(missing, 11));
            assertFalse(d.assignUnit(2, missing));
            assertFalse(d.resolveIncident(missing));
            assertFalse(d.returnUnitToService(missing));
            assertEquals(before, state(d));
        }
    }

    @Test
    public void duplicateIdsCannotOverwriteAssignedIncidentOrBusyUnit()
    {
        EmergencyDispatch d = ready();
        assertTrue(d.assignUnit(1, 10));
        String before = state(d);
        assertFalse(d.reportIncident(new Incident(1, IncidentType.MEDICAL,
            "Different location", Severity.CRITICAL)));
        assertFalse(d.addResponseUnit(new ResponseUnit(10, UnitType.EMT)));
        assertEquals(before, state(d));
        assertFalse(d.returnUnitToService(10));
        assertEquals(before, state(d));
    }

    @Test
    public void resolvedDuplicateIdCannotReplaceHistoryAfterUnitReuse()
    {
        EmergencyDispatch d = ready();
        assertTrue(d.assignUnit(1, 10));
        assertTrue(d.resolveIncident(1));
        assertTrue(d.returnUnitToService(10));
        assertTrue(d.reportIncident(incident(2, Severity.LOW)));
        assertTrue(d.assignUnit(2, 10));
        String before = state(d);
        assertFalse(d.reportIncident(new Incident(1, IncidentType.MEDICAL,
            "Different location", Severity.CRITICAL)));
        assertEquals(before, state(d));
    }

    @Test
    public void unitRegisteredBusyMustReturnToServiceBeforeAssignment()
    {
        EmergencyDispatch d = new EmergencyDispatch();
        ResponseUnit busy = new ResponseUnit(1000, UnitType.FIRE_RESCUE);
        busy.setAvailable(false);
        assertTrue(d.addResponseUnit(busy));
        assertTrue(d.reportIncident(incident(1, Severity.HIGH)));
        String before = state(d);
        assertFalse(d.assignUnit(1, 1000));
        assertEquals(before, state(d));
        assertTrue(d.returnUnitToService(1000));
        assertTrue(d.assignUnit(1, 1000));
        assertFalse(d.returnUnitToService(1000));
        assertFalse(d.findResponseUnit(1000).isAvailable());
        assertEquals(Integer.valueOf(1000), d.findActiveIncident(1).getUnitId());
    }

    @Test
    public void repeatedResolutionCannotReleaseUnitServingAnotherIncident()
    {
        EmergencyDispatch d = ready();
        assertTrue(d.assignUnit(1, 10));
        assertTrue(d.resolveIncident(1));
        assertTrue(d.returnUnitToService(10));
        assertTrue(d.reportIncident(incident(2, Severity.LOW)));
        assertTrue(d.assignUnit(2, 10));
        String before = state(d);
        for (int attempt = 0; attempt < 3; attempt++)
        {
            assertFalse(d.resolveIncident(1));
            assertFalse(d.assignUnit(1, 10));
            assertFalse(d.returnUnitToService(10));
            assertEquals(before, state(d));
        }
        assertTrue(d.resolveIncident(2));
        assertTrue(d.returnUnitToService(10));
        assertEquals(2, d.getResolvedCount(IncidentType.FIRE));
    }

    @Test
    public void resolutionRemovesOnlyRequestedIncidentAtEveryChainPosition()
    {
        for (int target = 1; target <= 3; target++)
        {
            EmergencyDispatch d = new EmergencyDispatch();
            for (int id = 1; id <= 3; id++)
            {
                assertTrue(d.reportIncident(incident(id, Severity.HIGH)));
            }
            assertTrue(d.resolveIncident(target));
            assertFalse(d.resolveIncident(target));
            assertEquals(2, d.getActiveIncidents().size());
            assertEquals(1, d.getResolvedIncidents().size());
            assertEquals(target, d.getResolvedIncidents().get(0).getId());
            assertNull(d.findActiveIncident(target));
            for (int id = 1; id <= 3; id++)
            {
                if (id != target)
                {
                    assertEquals(IncidentStatus.ACTIVE, d.findActiveIncident(id).getStatus());
                }
            }
        }
    }

    @Test
    public void assignmentWithEitherChainEmptyPreservesExistingEntries()
    {
        EmergencyDispatch onlyIncident = new EmergencyDispatch();
        assertTrue(onlyIncident.reportIncident(incident(1, Severity.HIGH)));
        String before = state(onlyIncident);
        assertFalse(onlyIncident.assignUnit(1, 10));
        assertEquals(before, state(onlyIncident));

        EmergencyDispatch onlyUnit = new EmergencyDispatch();
        assertTrue(onlyUnit.addResponseUnit(new ResponseUnit(10, UnitType.EMT)));
        before = state(onlyUnit);
        assertFalse(onlyUnit.assignUnit(1, 10));
        assertEquals(before, state(onlyUnit));
    }

    @Test
    public void dispatcherCanStartAnotherWorkflowAfterActiveChainBecomesEmpty()
    {
        EmergencyDispatch d = ready();
        assertTrue(d.assignUnit(1, 10));
        assertTrue(d.resolveIncident(1));
        assertTrue(d.getActiveIncidents().isEmpty());
        assertNull(d.getHighestPriorityIncident());
        assertTrue(d.returnUnitToService(10));
        assertTrue(d.reportIncident(incident(2, Severity.CRITICAL)));
        assertEquals(2, d.getHighestPriorityIncident().getId());
        assertTrue(d.assignUnit(2, 10));
        assertTrue(d.resolveIncident(2));
        assertTrue(d.getActiveIncidents().isEmpty());
        assertNull(d.getHighestPriorityIncident());
        assertEquals(2, d.getResolvedIncidents().size());
        assertEquals(2, d.getDispatchCount());
    }

    // Include fields omitted by model equals(), which compares only IDs.
    private String state(EmergencyDispatch d)
    {
        StringBuilder result = new StringBuilder();
        result.append(d.getActiveIncidents()).append('\n');
        result.append(d.getResolvedIncidents()).append('\n');
        result.append(d.getResponseUnits()).append('\n');
        result.append(d.getDispatchLog()).append('\n');
        result.append(d.getDispatchCount()).append('\n');
        result.append(d.getHighestPriorityIncident()).append('\n');
        for (IncidentType type : IncidentType.values())
        {
            result.append(type).append(':').append(d.getResolvedCount(type)).append('\n');
        }
        return result.toString();
    }

    @Test
    public void inputObjectsCannotChangeStoredStateAfterRegistration()
    {
        EmergencyDispatch d = new EmergencyDispatch();
        Incident input = incident(1, Severity.HIGH);
        ResponseUnit unit = new ResponseUnit(10, UnitType.FIRE_RESCUE);
        assertTrue(d.reportIncident(input));
        assertTrue(d.addResponseUnit(unit));
        input.assignUnit(999);
        input.resolve();
        unit.setAvailable(false);
        assertEquals(IncidentStatus.ACTIVE, d.findActiveIncident(1).getStatus());
        assertNull(d.findActiveIncident(1).getAssignedUnitId());
        assertTrue(d.findResponseUnit(10).isAvailable());
        assertTrue(d.assignUnit(1, 10));
    }

    @Test
    public void finderSnapshotsCannotBypassAssignmentAndResolutionRules()
    {
        EmergencyDispatch d = ready();
        assertTrue(d.assignUnit(1, 10));
        d.findActiveIncident(1).resolve();
        d.findResponseUnit(10).setAvailable(true);
        assertEquals(IncidentStatus.ACTIVE, d.findActiveIncident(1).getStatus());
        assertFalse(d.findResponseUnit(10).isAvailable());
        d.reportIncident(incident(2, Severity.CRITICAL));
        assertFalse(d.assignUnit(2, 10));
        assertFalse(d.returnUnitToService(10));
        assertEquals(1, d.getDispatchCount());
    }

    @Test
    public void collectionAndPrioritySnapshotsProtectModelState()
    {
        EmergencyDispatch d = ready();
        d.getActiveIncidents().get(0).assignUnit(999);
        d.getHighestPriorityIncident().resolve();
        d.getResponseUnits().get(0).setAvailable(false);
        assertNull(d.findActiveIncident(1).getUnitId());
        assertEquals(IncidentStatus.ACTIVE, d.findActiveIncident(1).getStatus());
        assertTrue(d.assignUnit(1, 10));
        assertTrue(d.resolveIncident(1));
        d.getResolvedIncidents().clear();
        assertEquals(IncidentStatus.RESOLVED, d.getResolvedIncidents().get(0).getStatus());
        assertEquals(Integer.valueOf(10), d.getResolvedIncidents().get(0).getAssignedUnitId());
    }

    @Test
    public void snapshotsRemainDetachedAfterLaterDispatcherActions()
    {
        EmergencyDispatch d = ready();
        Incident before = d.findActiveIncident(1);
        ResponseUnit unitBefore = d.findResponseUnit(10);
        LinkedChain<Incident> activeBefore = d.getActiveIncidents();
        assertTrue(d.assignUnit(1, 10));
        assertTrue(d.resolveIncident(1));
        assertNull(before.getAssignedUnitId());
        assertEquals(IncidentStatus.ACTIVE, before.getStatus());
        assertTrue(unitBefore.isAvailable());
        assertEquals(1, activeBefore.size());
        assertEquals(IncidentStatus.ACTIVE, activeBefore.get(0).getStatus());
        assertEquals(IncidentStatus.RESOLVED, d.getResolvedIncidents().get(0).getStatus());
    }

    @Test
    public void statisticsCountOnlySuccessfulActionsAndCorrectTypes()
    {
        EmergencyDispatch d = ready();
        assertEquals(0, d.getDispatchCount());
        for (IncidentType type : IncidentType.values())
        {
            assertEquals(0, d.getResolvedCount(type));
        }
        assertFalse(d.assignUnit(1, 999));
        assertEquals(0, d.getDispatchCount());
        assertTrue(d.assignUnit(1, 10));
        assertEquals(0, d.getResolvedCount(IncidentType.FIRE));
        assertTrue(d.resolveIncident(1));
        assertFalse(d.resolveIncident(1));
        assertEquals(1, d.getResolvedCount(IncidentType.FIRE));
        assertTrue(d.reportIncident(new Incident(2, IncidentType.MEDICAL, "Clinic", Severity.LOW)));
        assertTrue(d.resolveIncident(2));
        assertEquals(1, d.getResolvedCount(IncidentType.MEDICAL));
        assertEquals(0, d.getResolvedCount(IncidentType.THEFT));
        assertEquals(1, d.getDispatchCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void statisticsRejectNullIncidentType()
    {
        new EmergencyDispatch().getResolvedCount(null);
    }

    @Test
    public void statisticsCoverEveryIncidentTypeAndAccumulateSameType()
    {
        EmergencyDispatch d = new EmergencyDispatch();
        int id = 0;
        for (IncidentType type : IncidentType.values())
        {
            assertTrue(d.reportIncident(new Incident(id, type, "Site", Severity.LOW)));
            assertTrue(d.resolveIncident(id));
            assertEquals(1, d.getResolvedCount(type));
            id++;
        }
        d.reportIncident(incident(id, Severity.LOW));
        d.resolveIncident(id);
        assertEquals(2, d.getResolvedCount(IncidentType.FIRE));
        assertEquals(0, d.getDispatchCount());
    }

    @Test
    public void priorityIncludesAssignedIncidentsAndBreaksTiesInEitherInsertionOrder()
    {
        EmergencyDispatch d = ready();
        d.assignUnit(1, 10);
        assertEquals(1, d.getHighestPriorityIncident().getId());
        d.reportIncident(incident(3, Severity.HIGH));
        assertEquals(1, d.getHighestPriorityIncident().getId());
        d.reportIncident(incident(0, Severity.HIGH));
        assertEquals(0, d.getHighestPriorityIncident().getId());
    }

    @Test
    public void unitCanServeSeveralIncidentsWithoutLosingResolvedAssignments()
    {
        EmergencyDispatch d = ready();
        d.assignUnit(1, 10);
        d.resolveIncident(1);
        d.reportIncident(incident(2, Severity.LOW));
        assertFalse(d.assignUnit(2, 10));
        assertTrue(d.returnUnitToService(10));
        assertTrue(d.assignUnit(2, 10));
        assertFalse(d.returnUnitToService(10));
        assertEquals(Integer.valueOf(10), d.getResolvedIncidents().get(0).getAssignedUnitId());
        d.resolveIncident(2);
        assertEquals(2, d.getDispatchCount());
        assertEquals(2, d.getResolvedCount(IncidentType.FIRE));
        assertEquals(2, d.getDispatchLog().size());
    }

    @Test
    public void zeroAndMaximumIdsAreSupportedAndMissingIdsAreSafe()
    {
        EmergencyDispatch d = new EmergencyDispatch();
        assertTrue(d.reportIncident(incident(0, Severity.LOW)));
        assertTrue(d.addResponseUnit(new ResponseUnit(Integer.MAX_VALUE, UnitType.EMT)));
        assertTrue(d.assignUnit(0, Integer.MAX_VALUE));
        assertFalse(d.assignUnit(0, -1));
        assertFalse(d.resolveIncident(Integer.MAX_VALUE));
        assertNull(d.findActiveIncident(Integer.MAX_VALUE));
        assertNull(d.findResponseUnit(0));
        assertTrue(d.resolveIncident(0));
        assertTrue(d.returnUnitToService(Integer.MAX_VALUE));
    }

    private Incident incident(int id, Severity severity)
    {
        return new Incident(id, IncidentType.FIRE, "Main St", severity);
    }

    private EmergencyDispatch ready()
    {
        EmergencyDispatch d = new EmergencyDispatch();
        d.reportIncident(incident(1, Severity.HIGH));
        d.addResponseUnit(new ResponseUnit(10, UnitType.FIRE_RESCUE));
        return d;
    }

    @Test
    public void emptySystemAllQueriesAndActionsAreSafe()
    {
        EmergencyDispatch d = new EmergencyDispatch();
        assertTrue(d.getActiveIncidents().isEmpty());
        assertTrue(d.getResolvedIncidents().isEmpty());
        assertTrue(d.getResponseUnits().isEmpty());
        assertTrue(d.getDispatchLog().isEmpty());
        assertNull(d.findActiveIncident(1));
        assertNull(d.findActiveIncident(-1));
        assertNull(d.findResponseUnit(10));
        assertNull(d.findResponseUnit(-1));
        assertNull(d.getHighestPriorityIncident());
        assertFalse(d.assignUnit(1, 10));
        assertFalse(d.resolveIncident(1));
        assertFalse(d.returnUnitToService(10));
        assertFalse(d.reportIncident(null));
        assertFalse(d.addResponseUnit(null));
    }

    @Test
    public void reportsRejectDuplicateResolvedAndAlreadyAssignedIncidents()
    {
        EmergencyDispatch d = ready();
        assertEquals(1, d.getActiveIncidents().size());
        assertEquals(1, d.findActiveIncident(1).getId());
        assertFalse(d.reportIncident(incident(1, Severity.LOW)));
        assertFalse(d.reportIncident(null));
        Incident resolved = incident(2, Severity.LOW);
        resolved.resolve();
        assertFalse(d.reportIncident(resolved));
        Incident assigned = incident(3, Severity.LOW);
        assigned.assignUnit(10);
        assertFalse(d.reportIncident(assigned));
        assertTrue(d.resolveIncident(1));
        assertFalse(d.reportIncident(incident(1, Severity.LOW)));
        assertEquals(1, d.getResolvedIncidents().size());
        assertNull(d.findActiveIncident(1));
    }

    @Test
    public void unitRegistrationRejectsDuplicatesAndNull()
    {
        EmergencyDispatch d = ready();
        assertEquals(10, d.findResponseUnit(10).getId());
        assertEquals(1, d.getResponseUnits().size());
        assertFalse(d.addResponseUnit(new ResponseUnit(10, UnitType.EMT)));
        assertFalse(d.addResponseUnit(null));
        assertEquals(UnitType.FIRE_RESCUE, d.findResponseUnit(10).getType());
    }

    @Test
    public void fullMvpAndThirdTypeStorage()
    {
        EmergencyDispatch d = ready();
        assertTrue(d.assignUnit(1, 10));
        assertFalse(d.findResponseUnit(10).isAvailable());
        assertEquals(Integer.valueOf(10), d.findActiveIncident(1).getAssignedUnitId());
        LinkedChain<String> log = d.getDispatchLog();
        assertEquals("Assigned unit 10 to incident 1", log.get(0));
        assertFalse(d.returnUnitToService(10));
        assertFalse(d.findResponseUnit(10).isAvailable());
        assertTrue(d.resolveIncident(1));
        assertTrue(d.getActiveIncidents().isEmpty());
        assertEquals(IncidentStatus.RESOLVED, d.getResolvedIncidents().get(0).getStatus());
        assertFalse(d.findResponseUnit(10).isAvailable());
        assertTrue(d.returnUnitToService(10));
        assertTrue(d.findResponseUnit(10).isAvailable());
        assertTrue(d.returnUnitToService(10));
        assertFalse(d.resolveIncident(1));
        assertFalse(d.assignUnit(1, 10));
    }

    @Test
    public void rejectedAssignmentsDoNotChangeStateOrLog()
    {
        EmergencyDispatch d = ready();
        assertFalse(d.assignUnit(999, 10));
        assertFalse(d.assignUnit(1, 999));
        assertFalse(d.assignUnit(-1, 10));
        assertNull(d.findActiveIncident(1).getUnitId());
        assertTrue(d.findResponseUnit(10).isAvailable());
        assertTrue(d.getDispatchLog().isEmpty());
        assertTrue(d.assignUnit(1, 10));
        d.reportIncident(incident(2, Severity.LOW));
        d.addResponseUnit(new ResponseUnit(11, UnitType.EMT));
        assertFalse(d.assignUnit(2, 10));
        assertFalse(d.assignUnit(1, 11));
        assertNull(d.findActiveIncident(2).getUnitId());
        assertTrue(d.findResponseUnit(11).isAvailable());
        assertEquals(1, d.getDispatchLog().size());
        assertFalse(d.returnUnitToService(-1));
        assertFalse(d.resolveIncident(-1));
    }

    @Test
    public void priorityUsesSeverityThenIdAndExcludesResolvedIncidents()
    {
        EmergencyDispatch d = ready();
        d.reportIncident(incident(3, Severity.CRITICAL));
        d.reportIncident(incident(2, Severity.CRITICAL));
        d.reportIncident(incident(0, Severity.LOW));
        assertEquals(2, d.getHighestPriorityIncident().getId());
        d.resolveIncident(2);
        assertEquals(3, d.getHighestPriorityIncident().getId());
        d.resolveIncident(3);
        assertEquals(1, d.getHighestPriorityIncident().getId());
    }

    @Test
    public void everyCollectionGetterProtectsMembershipAndPreservesOrder()
    {
        EmergencyDispatch d = ready();
        d.reportIncident(incident(2, Severity.LOW));
        assertEquals(2, d.getActiveIncidents().get(0).getId());
        d.getActiveIncidents().clear();
        assertNotNull(d.findActiveIncident(1));
        d.getResponseUnits().clear();
        assertNotNull(d.findResponseUnit(10));
        d.assignUnit(1, 10);
        d.getDispatchLog().clear();
        assertEquals(1, d.getDispatchLog().size());
        d.resolveIncident(1);
        d.getResolvedIncidents().clear();
        assertEquals(1, d.getResolvedIncidents().size());
        assertFalse(d.reportIncident(incident(1, Severity.LOW)));
    }

    @Test
    public void priorityCoversEverySeverityAndEventuallyBecomesEmpty()
    {
        EmergencyDispatch d = new EmergencyDispatch();
        d.reportIncident(incident(4, Severity.CRITICAL));
        d.reportIncident(incident(3, Severity.HIGH));
        d.reportIncident(incident(2, Severity.MEDIUM));
        d.reportIncident(incident(1, Severity.LOW));
        for (int id = 4; id >= 1; id--)
        {
            assertEquals(id, d.getHighestPriorityIncident().getId());
            assertTrue(d.resolveIncident(id));
        }
        assertNull(d.getHighestPriorityIncident());
    }

    @Test
    public void thirdTypeLogPreservesOrderAcrossMultipleDispatches()
    {
        EmergencyDispatch d = ready();
        assertTrue(d.assignUnit(1, 10));
        assertTrue(d.resolveIncident(1));
        assertTrue(d.returnUnitToService(10));
        assertTrue(d.reportIncident(incident(2, Severity.MEDIUM)));
        assertTrue(d.assignUnit(2, 10));
        assertFalse(d.assignUnit(2, 10));
        LinkedChain<String> log = d.getDispatchLog();
        assertEquals(2, log.size());
        assertEquals("Assigned unit 10 to incident 1", log.get(0));
        assertEquals("Assigned unit 10 to incident 2", log.get(1));
    }

}
