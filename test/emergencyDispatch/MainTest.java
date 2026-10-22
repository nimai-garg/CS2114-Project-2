package emergencyDispatch;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;
public class MainTest
{
    @Test
    public void statisticsAndResolvedHistoryAreVisible() throws Exception
    {
        String out = run("stats\nunit|10|EMT\nincident|1|MEDICAL|Clinic|CRITICAL\n"
            + "assign|1|10\nresolve|1\nresolve|1\nreturn|10\nhistory\nstats\nquit\n");
        assertTrue(out.contains("Successful dispatches: 0"));
        assertTrue(out.contains("Successful dispatches: 1"));
        assertTrue(out.contains("Resolved incidents: 1"));
        assertTrue(out.contains("Resolved MEDICAL: 1"));
        assertTrue(out.contains("Resolved FIRE: 0"));
        assertTrue(out.contains("Assigned unit 10 to incident 1"));
        assertTrue(out.contains("Status: RESOLVED"));
        assertFalse(out.contains("Successful dispatches: 2"));
    }

    @Test
    public void commandsAndEnumsIgnoreCaseAndTrimFields() throws Exception
    {
        String out = run("  UnIt | 10 | emt \nInCiDeNt|1|medical| Clinic |critical\n"
            + "AsSiGn|1|10\nRESOLVE|1\nRETURN|10\nVIEW\nHELP\nQUIT\n");
        assertFalse(out.contains("Invalid input:"));
        assertTrue(out.contains("Status: RESOLVED"));
        assertTrue(out.contains("Available: true"));
        assertTrue(out.contains("Location: Clinic"));
    }

    @Test
    public void extraFieldsOverflowAndBlankNamesRecoverBeforeValidCommand() throws Exception
    {
        String[] invalid = {"stats|extra", "history|extra", "help|extra", "view|extra",
            "priority|extra", "quit|extra", "unit|2147483648|EMT", "assign|-1|1",
            "unit|1|", "incident|1||X|HIGH", "incident|1|FIRE|X|",
            "incident|1|FIRE|A|B|HIGH", "return|1|extra", "resolve|1|extra"};
        for (String command : invalid)
        {
            String out = run(command + "\nunit|2147483647|emt\nview\nquit\n");
            assertTrue(command, out.contains("Invalid input:"));
            assertTrue(command, out.contains("Unit ID: 2147483647"));
        }
    }

    @Test
    public void parsingDoesNotDependOnSystemLocale() throws Exception
    {
        java.util.Locale original = java.util.Locale.getDefault();
        try
        {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            String out = run("UNIT|1|police\nINCIDENT|1|missing_persons|Park|high\nview\nquit\n");
            assertFalse(out.contains("Invalid input:"));
            assertTrue(out.contains("Type: MISSING_PERSONS"));
            assertTrue(out.contains("Type: POLICE"));
        }
        finally
        {
            java.util.Locale.setDefault(original);
        }
    }

    // Supply test keyboard input and collect printed output, then restore both.
    private String run(String commands) throws Exception
    {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try
        {
            System.setIn(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output, true, "UTF-8"));
            Main.main(new String[0]);
            return output.toString("UTF-8");
        }
        finally
        {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    @Test
    public void consoleFullMvpPriorityAndHistory() throws Exception
    {
        String out = run("unit|10|FIRE_RESCUE\nincident|1|FIRE|Main St|HIGH\npriority\nview\nassign|1|10\nhistory\nreturn|10\nresolve|1\nreturn|10\nview\nquit\n");
        assertTrue(out.contains("Assigned unit 10 to incident 1"));
        assertTrue(out.contains("Rejected:"));
        assertTrue(out.contains("Active: []"));
        assertTrue(out.contains("Status: RESOLVED"));
        assertTrue(out.contains("Available: true"));
    }

    @Test
    public void everyScopedBadInputRepromptsThenValidInputWorks() throws Exception
    {
        String out = run("\nunknown\nincident|1\nincident||FIRE|X|HIGH\nincident|x|FIRE|X|HIGH\n"
        + "incident|-1|FIRE|X|HIGH\nincident|1|INVALID|X|HIGH\nincident|1|FIRE| |HIGH\n"
        + "incident|1|FIRE|X|INVALID\nunit|1|INVALID\nunit||EMT\n"
        + "assign|1|99\nresolve|99\nreturn|99\npriority\nhistory\n"
        + "unit|10|EMT\nincident|1|MEDICAL|X|LOW\nassign|1|10\nassign|1|10\nresolve|1\nassign|1|10\nreturn|10\nview\n");
        assertTrue(out.contains("Invalid input:"));
        assertTrue(out.contains("Try again."));
        assertTrue(out.contains("Rejected:"));
        assertTrue(out.contains("No active incidents"));
        assertTrue(out.contains("Status: RESOLVED"));
        assertTrue(out.contains("Available: true"));
    }

    @Test
    public void eofAndQuitNeedNoMoreInput() throws Exception
    {
        assertTrue(run("").contains("Final Build"));
        assertFalse(run("quit\nunknown\n").contains("Invalid input:"));
    }

    @Test
    public void eachInvalidFieldGetsAnErrorAndAllowsAnotherCommand() throws Exception
    {
        String[] invalidCommands = {
            "incident||FIRE|X|HIGH",
            "incident|abc|FIRE|X|HIGH",
            "incident|-1|FIRE|X|HIGH",
            "incident|1|INVALID|X|HIGH",
            "incident|1|FIRE||HIGH",
            "incident|1|FIRE|   |HIGH",
            "incident|1|FIRE|X|INVALID",
            "unit||EMT",
            "unit|abc|EMT",
            "unit|-1|EMT",
            "unit|1|INVALID",
            "assign||10",
            "resolve|abc",
            "return|abc",
            "incident|1",
            "unknown"
        };
        for (int i = 0; i < invalidCommands.length; i++)
        {
            String out = run(invalidCommands[i] + "\nunit|50|EMT\nview\nquit\n");
            assertTrue(invalidCommands[i], out.contains("Invalid input:"));
            assertTrue(invalidCommands[i], out.contains("Try again."));
            assertTrue(invalidCommands[i], out.contains("Units: [Unit ID: 50"));
            assertTrue(invalidCommands[i], out.contains("Active: []"));
        }
    }

    @Test
    public void eachInvalidAssignmentGetsTheSpecifiedRejection() throws Exception
    {
        String setup = "unit|10|FIRE_RESCUE\nunit|11|EMT\n"
            + "incident|1|FIRE|X|HIGH\nincident|2|FIRE|Y|LOW\nassign|1|10\n";
        String[] invalidCommands = {
            "assign|99|11",       // Missing incident.
            "assign|2|99",        // Missing unit.
            "assign|2|10",        // Busy unit.
            "assign|1|11",        // Incident already has a unit.
            "return|10",          // Unit still serves an active incident.
            "resolve|99",
            "return|99",
            "resolve|1\nassign|1|11" // Resolved incident.
        };
        for (int i = 0; i < invalidCommands.length; i++)
        {
            String out = run(setup + invalidCommands[i] + "\nview\nquit\n");
            assertTrue(invalidCommands[i], out.contains("Rejected:"));
            assertTrue(invalidCommands[i], out.contains("Try again."));
            assertTrue(invalidCommands[i], out.contains("Unit ID: 11, Type: EMT, Available: true"));
            assertTrue(invalidCommands[i], out.contains("Unit ID: 10, Type: FIRE_RESCUE, Available: false"));
        }
    }

}
