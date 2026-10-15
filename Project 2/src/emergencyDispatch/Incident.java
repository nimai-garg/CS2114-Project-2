package emergencyDispatch;
import java.util.Objects;

public class Incident {
    //~ Fields ................................................................
    private final int id;
    private final IncidentType type;
    private final String location;
    private final Severity severity;
    private IncidentStatus status;
    private Integer unitId;
    //~ Constructors ..........................................................
    public Incident(int id, IncidentType type, String location, Severity severity) 
    {
        if (id < 0)
        {
            throw new IllegalArgumentException("The incident Id can't be negative");
        }

        if (type == null || severity == null)
        {
            throw new IllegalArgumentException("The type and severity cannot be null");
        }

        if (location == null || location.trim().isEmpty())
        {
            throw new IllegalArgumentException("The location cannot be empty");
        }

        this.id = id;
        this.type = type;
        this.location = location.trim();
        this.severity = severity;
        this.status = IncidentStatus.ACTIVE;
        this.unitId = null;
    }
    //~Public  Methods ........................................................
    public int getId()
    {
        return id;
    }

    public IncidentType getType()
    {
        return type;
    }

    public String getLocation()
    {
        return location;
    }

    public Severity getSeverity()
    {
        return severity;
    }

    public IncidentStatus getStatus()
    {
        return status;
    }

    public Integer getUnitId()
    {
        return unitId;
    }

        public void assignUnit(int unitId1)
    {
        if (unitId1 < 0)
        {
            throw new IllegalArgumentException("Unit ID cannot be negative");
        }

        if (status != IncidentStatus.ACTIVE)
        {
            throw new IllegalStateException("Cannot assign a unit to a resolved incident");
        }

        if (unitId != null)
        {
            throw new IllegalStateException("Incident already has a unit");
        }

        unitId = unitId1;
    }

        public void resolve()
    {
        status = IncidentStatus.RESOLVED;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof Incident))
        {
            return false;
        }

        Incident otherIncident = (Incident)other;

        return id == otherIncident.id;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }

    @Override
    public String toString()
    {
        return "Incident ID: " + id + ", Type: " + type + ", Location: " + location + ", Severity: " + severity + ", Status: " + status 
        + ",Unit: " + unitId;
    }
}
