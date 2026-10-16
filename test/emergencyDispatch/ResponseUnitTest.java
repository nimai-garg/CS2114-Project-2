package emergencyDispatch;

import org.junit.Test;
import static org.junit.Assert.*;
public class ResponseUnitTest
{
    @Test
    public void validConstructionGettersAndBothAvailabilityValues()
    {
        ResponseUnit u = new ResponseUnit(0, UnitType.EMT);
        assertEquals(0, u.getId());
        assertEquals(UnitType.EMT, u.getType());
        assertTrue(u.isAvailable());
        u.setAvailable(false);
        assertFalse(u.isAvailable());
        assertTrue(u.toString().contains("Available: false"));
        u.setAvailable(true);
        u.setAvailable(true);
        assertTrue(u.isAvailable());
        assertTrue(u.toString().contains("Available: true"));
    }

    @Test
    public void invalidConstruction()
    {
        try
        {
            new ResponseUnit(-1, UnitType.EMT);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException exception)
        {
            // The invalid input should be rejected here.
        }
        try
        {
            new ResponseUnit(1, null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException exception)
        {
            // The invalid input should be rejected here.
        }
    }

    @Test
    public void identityAndHashIgnoreAvailability()
    {
        ResponseUnit a = new ResponseUnit(1, UnitType.EMT), b = new ResponseUnit(1, UnitType.EMT);
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new ResponseUnit(2, UnitType.EMT));
        assertNotEquals(a, null);
        assertNotEquals(a, "1");
        int hash = a.hashCode();
        a.setAvailable(false);
        assertEquals(a, b);
        assertEquals(hash, a.hashCode());
    }
}
