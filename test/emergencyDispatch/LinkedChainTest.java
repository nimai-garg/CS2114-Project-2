package emergencyDispatch;

import org.junit.Test;
import static org.junit.Assert.*;
// Tests normal operations and invalid input for the generic chain.
public class LinkedChainTest
{
    @Test
    public void emptiedChainCanBeReusedAfterEveryRemovalPath()
    {
        for (int removal = 0; removal < 3; removal++)
        {
            LinkedChain<String> c = new LinkedChain<>();
            c.add("only");
            if (removal == 0)
            {
                assertEquals("only", c.remove());
            }
            else if (removal == 1)
            {
                assertTrue(c.remove("only"));
            }
            else
            {
                c.clear();
            }
            assertTrue(c.isEmpty());
            assertEquals(0, c.size());
            assertNull(c.remove());
            assertFalse(c.remove("only"));
            assertArrayEquals(new String[0], c.toArray(new String[0]));
            assertTrue(c.addToEnd("tail"));
            assertTrue(c.add("head"));
            assertArrayEquals(new String[] {"head", "tail"}, c.toArray(new String[2]));
            assertEquals(2, c.size());
        }
    }

    @Test
    public void duplicateRemovalDeletesOnlyFirstEqualEntry()
    {
        LinkedChain<String> c = new LinkedChain<>();
        c.addToEnd(new String("duplicate"));
        c.addToEnd("middle");
        c.addToEnd(new String("duplicate"));
        assertTrue(c.remove(new String("duplicate")));
        assertEquals(2, c.size());
        assertEquals(1, c.count("duplicate"));
        assertArrayEquals(new String[] {"middle", "duplicate"}, c.toArray(new String[2]));
        assertTrue(c.remove("duplicate"));
        assertFalse(c.remove("duplicate"));
        assertEquals(1, c.size());
        assertEquals("middle", c.get(0));
    }

    @Test
    public void emptyAndMissingOperations()
    {
        LinkedChain<String> c = new LinkedChain<>();
        assertTrue(c.isEmpty());
        assertEquals(0, c.size());
        assertNull(c.remove());
        assertFalse(c.remove("missing"));
        assertFalse(c.remove(null));
        assertFalse(c.contains("missing"));
        assertFalse(c.contains(null));
        assertEquals(0, c.count(null));
        assertEquals(0, c.count("missing"));
        assertEquals("[]", c.toString());
        try
        {
            c.get(0);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException exception)
        {
        }
        c.clear();
        assertTrue(c.isEmpty());
        assertArrayEquals(new String[0], c.toArray(new String[0]));
    }

    @Test
    public void addRetrieveDuplicatesAndRemoveAllPositions()
    {
        LinkedChain<String> c = new LinkedChain<>();
        assertTrue(c.addToEnd("b"));
        assertTrue(c.add("a"));
        assertTrue(c.addToEnd("c"));
        c.addToEnd("b");
        assertFalse(c.isEmpty());
        assertEquals(4, c.size());
        assertEquals("a", c.get(0));
        assertEquals("b", c.get(1));
        assertEquals("b", c.get(3));
        assertTrue(c.contains("b"));
        assertEquals(2, c.count("b"));
        assertEquals("[a, b, c, b]", c.toString());
        assertArrayEquals(new String[] {"a", "b", "c", "b"}, c.toArray(new String[4]));
        assertFalse(c.remove("missing"));
        assertEquals(4, c.size());
        assertTrue(c.remove("b"));
        assertEquals(1, c.count("b"));
        assertTrue(c.remove("b"));
        assertTrue(c.remove("a"));
        assertEquals("c", c.remove());
        assertTrue(c.isEmpty());
        c.add("x");
        c.clear();
        assertEquals(0, c.size());
    }

    @Test
    public void invalidArgumentsDoNotModifyChain()
    {
        LinkedChain<String> c = new LinkedChain<>();
        c.add("safe");
        try
        {
            c.add(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException exception)
        {
        }
        try
        {
            c.addToEnd(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException exception)
        {
        }
        try
        {
            c.toArray(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException exception)
        {
        }
        try
        {
            c.toArray(new String[0]);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException exception)
        {
        }
        try
        {
            c.get(-1);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException exception)
        {
        }
        try
        {
            c.get(1);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException exception)
        {
        }
        assertEquals(1, c.size());
        assertEquals("safe", c.get(0));
    }
}
