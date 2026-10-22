package emergencyDispatch;

/**
 * Application rules; all persistent collection storage uses LinkedChain.
 * Inputs and returned models are copied so only this class changes stored state.
 * Snapshot queries do not update automatically after later dispatcher actions.
 */
public class EmergencyDispatch
{
    private final LinkedChain<Incident> activeIncidents = new LinkedChain<>();
    private final LinkedChain<Incident> resolvedIncidents = new LinkedChain<>();
    private final LinkedChain<ResponseUnit> responseUnits = new LinkedChain<>();
    private final LinkedChain<String> dispatchLog = new LinkedChain<>();
    /** Registers an active, unassigned incident; rejects null or reused IDs. */
    public boolean reportIncident(Incident incident)
    {
        if (incident == null)
        {
            return false;
        }
        if (incident.getStatus() != IncidentStatus.ACTIVE || incident.getUnitId() != null)
        {
            return false;
        }
        if (findStoredIncident(incident.getId()) != null)
        {
            return false;
        }
        Incident[] entries = resolvedIncidents.toArray(new Incident[resolvedIncidents.size()]);
        for (int i = 0; i < entries.length; i++)
        {
            Incident resolved = entries[i];
            if (resolved.getId() == incident.getId())
            {
                return false;
            }
        }
        return activeIncidents.add(incident.copy());
    }

    /** Registers a detached copy of a unit; rejects null or duplicate IDs. */
    public boolean addResponseUnit(ResponseUnit unit)
    {
        if (unit == null || findStoredUnit(unit.getId()) != null)
        {
            return false;
        }
        return responseUnits.add(unit.copy());
    }

    /** Assigns an available unit to an unassigned active incident, or returns false. */
    public boolean assignUnit(int incidentId, int unitId)
    {
        Incident incident = findStoredIncident(incidentId);
        ResponseUnit unit = findStoredUnit(unitId);
        if (incident == null || unit == null || !unit.isAvailable()
            || incident.getStatus() != IncidentStatus.ACTIVE || incident.getUnitId() != null)
        {
            return false;
        }
        incident.assignUnit(unitId);
        unit.setAvailable(false);
        dispatchLog.addToEnd("Assigned unit " + unitId + " to incident " + incidentId);
        return true;
    }

    /** Moves an active incident into resolved history; its unit stays busy. */
    public boolean resolveIncident(int incidentId)
    {
        Incident incident = findStoredIncident(incidentId);
        if (incident == null || incident.getStatus() != IncidentStatus.ACTIVE)
        {
            return false;
        }
        activeIncidents.remove(incident);
        incident.resolve();
        resolvedIncidents.add(incident);
        return true;
    }

    /** Returns false for a missing unit or one still serving an active incident. */
    public boolean returnUnitToService(int unitId)
    {
        ResponseUnit unit = findStoredUnit(unitId);
        if (unit == null)
        {
            return false;
        }
        Incident[] entries = activeIncidents.toArray(new Incident[activeIncidents.size()]);
        for (int i = 0; i < entries.length; i++)
        {
            Incident incident = entries[i];
            if (incident.getUnitId() != null && incident.getUnitId() == unitId)
            {
                return false;
            }
        }
        unit.setAvailable(true);
        return true;
    }

    /** Returns a detached incident snapshot, or null for a missing/invalid ID. */
    public Incident findActiveIncident(int incidentId)
    {
        Incident incident = findStoredIncident(incidentId);
        return incident == null ? null : incident.copy();
    }

    private Incident findStoredIncident(int incidentId)
    {
        Incident[] entries = activeIncidents.toArray(new Incident[activeIncidents.size()]);
        for (int i = 0; i < entries.length; i++)
        {
            Incident incident = entries[i];
            if (incident.getId() == incidentId)
            {
                return incident;
            }
        }
        return null;
    }

    /** Returns a detached unit snapshot, or null for a missing/invalid ID. */
    public ResponseUnit findResponseUnit(int unitId)
    {
        ResponseUnit unit = findStoredUnit(unitId);
        return unit == null ? null : unit.copy();
    }

    private ResponseUnit findStoredUnit(int unitId)
    {
        ResponseUnit[] entries = responseUnits.toArray(new ResponseUnit[responseUnits.size()]);
        for (int i = 0; i < entries.length; i++)
        {
            ResponseUnit unit = entries[i];
            if (unit.getId() == unitId)
            {
                return unit;
            }
        }
        return null;
    }

    /**
     * Highest severity first, then smallest ID; null when empty.
     * Includes assigned incidents because they remain active until resolved.
     * Returns a detached snapshot.
     */
    public Incident getHighestPriorityIncident()
    {
        Incident best = null;
        Incident[] entries = activeIncidents.toArray(new Incident[activeIncidents.size()]);
        for (int i = 0; i < entries.length; i++)
        {
            Incident incident = entries[i];
            if (best == null)
            {
                best = incident;
            }
            else if (severityLevel(incident.getSeverity()) > severityLevel(best.getSeverity()))
            {
                best = incident;
            }
            else if (incident.getSeverity() == best.getSeverity()
                && incident.getId() < best.getId())
            {
                best = incident;
            }
        }
        return best == null ? null : best.copy();
    }

    // Bigger numbers mean a more urgent emergency.
    private int severityLevel(Severity severity)
    {
        if (severity == Severity.CRITICAL)
        {
            return 4;
        }
        if (severity == Severity.HIGH)
        {
            return 3;
        }
        if (severity == Severity.MEDIUM)
        {
            return 2;
        }
        return 1;
    }

    // Copy both the collection and its models to protect all stored state.
    // Adding the array from back to front keeps the same order, since add inserts first.
    /** Returns a detached chain and detached models for all active incidents. */
    public LinkedChain<Incident> getActiveIncidents()
    {
        LinkedChain<Incident> result = new LinkedChain<>();
        Incident[] values = activeIncidents.toArray(new Incident[activeIncidents.size()]);
        for (int i = values.length - 1; i >= 0; i--)
        {
            result.add(values[i].copy());
        }
        return result;
    }

    /** Returns detached snapshots of all registered units, including busy ones. */
    public LinkedChain<ResponseUnit> getResponseUnits()
    {
        LinkedChain<ResponseUnit> result = new LinkedChain<>();
        ResponseUnit[] values = responseUnits.toArray(new ResponseUnit[responseUnits.size()]);
        for (int i = values.length - 1; i >= 0; i--)
        {
            result.add(values[i].copy());
        }
        return result;
    }

    /** Returns detached resolved incidents, retaining the unit IDs used. */
    public LinkedChain<Incident> getResolvedIncidents()
    {
        LinkedChain<Incident> result = new LinkedChain<>();
        Incident[] values = resolvedIncidents.toArray(new Incident[resolvedIncidents.size()]);
        for (int i = values.length - 1; i >= 0; i--)
        {
            result.add(values[i].copy());
        }
        return result;
    }

    /** Returns assignment messages in chronological order in a separate chain. */
    public LinkedChain<String> getDispatchLog()
    {
        LinkedChain<String> result = new LinkedChain<>();
        String[] values = dispatchLog.toArray(new String[dispatchLog.size()]);
        for (int i = values.length - 1; i >= 0; i--)
        {
            result.add(values[i]);
        }
        return result;
    }

    /** Total successful dispatches, including assignments to still-active incidents. */
    public int getDispatchCount()
    {
        return dispatchLog.size();
    }

    /**
     * Counts resolved incidents of a type. Active incidents are excluded.
     * @param type the emergency type to count
     * @return the resolved count, including zero
     * @throws IllegalArgumentException if type is null
     */
    public int getResolvedCount(IncidentType type)
    {
        if (type == null)
        {
            throw new IllegalArgumentException("Incident type cannot be null");
        }
        int count = 0;
        Incident[] entries = resolvedIncidents.toArray(new Incident[resolvedIncidents.size()]);
        for (int i = 0; i < entries.length; i++)
        {
            if (entries[i].getType() == type)
            {
                count++;
            }
        }
        return count;
    }
}
