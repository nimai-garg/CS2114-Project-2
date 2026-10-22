package emergencyDispatch;

// Manages incidents, units, and dispatch rules.
public class EmergencyDispatch
{
    private final LinkedChain<Incident> activeIncidents = new LinkedChain<>();
    private final LinkedChain<Incident> resolvedIncidents = new LinkedChain<>();
    private final LinkedChain<ResponseUnit> responseUnits = new LinkedChain<>();
    private final LinkedChain<String> dispatchLog = new LinkedChain<>();

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

    public boolean addResponseUnit(ResponseUnit unit)
    {
        if (unit == null || findStoredUnit(unit.getId()) != null)
        {
            return false;
        }
        return responseUnits.add(unit.copy());
    }

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

    // Copies the models as well as the chain to protect stored state.
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

    public int getDispatchCount()
    {
        return dispatchLog.size();
    }

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
