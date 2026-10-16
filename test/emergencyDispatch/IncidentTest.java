package emergencyDispatch;

import org.junit.Test;
import static org.junit.Assert.*;
public class IncidentTest
{
    private Incident incident(int id)
    {
        return new Incident(id, IncidentType.FIRE, " Main St ", Severity.HIGH);
    }

    @Test
    public void constructorAndEveryGetter()
    {
        Incident i = incident(0);
        assertEquals(0, i.getId());
        assertEquals(IncidentType.FIRE, i.getType());
        assertEquals("Main St", i.getLocation());
        assertEquals(Severity.HIGH, i.getSeverity());
        assertEquals(IncidentStatus.ACTIVE, i.getStatus());
        assertNull(i.getUnitId());
        assertNull(i.getAssignedUnitId());
        i.assignUnit(0);
        assertEquals(Integer.valueOf(0), i.getUnitId());
        assertEquals(i.getUnitId(), i.getAssignedUnitId());
        i.resolve();
        assertEquals(IncidentStatus.RESOLVED, i.getStatus());
    }

    @Test
    public void constructorRejectsEveryInvalidField()
    {
        try
        {
            incident(-1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException exception)
        {
            // The invalid input should be rejected here.
        }
        try
        {
            new Incident(1, null, "X", Severity.LOW);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException exception)
        {
            // The invalid input should be rejected here.
        }
        try
        {
            new Incident(1, IncidentType.FIRE, "X", null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException exception)
        {
            // The invalid input should be rejected here.
        }
        for (String location : new String[] {null, "", "  "})
        {
            try
            {
                new Incident(1, IncidentType.FIRE, location, Severity.LOW);
                fail("Expected IllegalArgumentException");
            }
            catch (IllegalArgumentException exception)
            {
                // The invalid input should be rejected here.
            }
        }
    }

    @Test
    public void assignmentRejectsInvalidStateWithoutChangingIt()
    {
        Incident i = incident(1);
        try
        {
            i.assignUnit(-1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException exception)
        {
            // The invalid input should be rejected here.
        }
        assertNull(i.getUnitId());
        i.assignUnit(2);
        try
        {
            i.assignUnit(3);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException exception)
        {
            // The invalid input should be rejected here.
        }
        assertEquals(Integer.valueOf(2), i.getAssignedUnitId());
        Incident resolved = incident(2);
        resolved.resolve();
        try
        {
            resolved.assignUnit(2);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException exception)
        {
            // The invalid input should be rejected here.
        }
        assertNull(resolved.getUnitId());
    }

    @Test
    public void repeatedResolutionIsSafe()
    {
        Incident i = incident(1);
        i.resolve();
        i.resolve();
        assertEquals(IncidentStatus.RESOLVED, i.getStatus());
    }

    @Test
    public void identityHashAndDisplayStayValidAcrossStateChanges()
    {
        Incident a = incident(1), b = incident(1);
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, incident(2));
        assertNotEquals(a, null);
        assertNotEquals(a, "1");
        assertTrue(a.toString().contains("Unit: null"));
        int hash = a.hashCode();
        a.assignUnit(0);
        a.resolve();
        assertEquals(hash, a.hashCode());
        assertEquals(a, b);
        assertTrue(a.toString().contains("RESOLVED"));
    }
}
