package emergencyDispatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

// Tests the numbered menu, input recovery, and cancellation.
public class MainTest
{
    private String unit(int id, String type)
    {
        return "2\n" + id + "\n" + type + "\n";
    }

    private String incident(int id, String type, String location, String severity)
    {
        return "1\n" + id + "\n" + type + "\n" + location + "\n" + severity + "\n";
    }

    private String ready()
    {
        return unit(10, "FIRE_RESCUE") + incident(1, "FIRE", "Main Street", "HIGH");
    }

    private String run(String answers) throws Exception
    {
        return runRaw(answers).replaceAll("[ \\t]+", " ");
    }

    private String runRaw(String answers) throws Exception
    {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try
        {
            System.setIn(new ByteArrayInputStream(answers.getBytes(StandardCharsets.UTF_8)));
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
    public void fullWorkflowKeepsHistoryAndStatisticsAccurate() throws Exception
    {
        String out = run(ready() + "3\n1\n10\n5\n10\n4\n1\n5\n10\n8\n9\n6\n0\n");
        assertTrue(out.contains("Assigned unit #10 to incident #1"));
        assertTrue(out.contains("still assigned to active incident #1"));
        assertTrue(out.contains("Resolved incident #1"));
        assertTrue(out.contains("Unit #10 is back in service"));
        assertTrue(out.contains("Assigned unit 10 to incident 1"));
        assertTrue(out.contains("Resolved | Unit: #10"));
        assertTrue(out.contains("#10 Fire rescue Available"));
        assertTrue(out.contains("Active incidents: 0"));
        assertTrue(out.contains("Resolved incidents: 1"));
        assertTrue(out.contains("Successful dispatches: 1"));
        assertTrue(out.contains("Fire 1"));
        assertFalse(out.contains("Invalid input:"));
    }

    @Test
    public void fieldNamesIgnoreCaseAndSurroundingWhitespace() throws Exception
    {
        String out = run(" 2 \n 10 \n emt \n 1 \n 1 \n medical \n Clinic \n critical \n"
            + "3\n1\n10\n4\n1\n5\n10\n6\n0\n");
        assertFalse(out.contains("Invalid input:"));
        assertTrue(out.contains("#1 | Medical | Critical"));
        assertTrue(out.contains("Location: Clinic"));
        assertTrue(out.contains("Resolved | Unit: #10"));
        assertTrue(out.contains("#10 EMT Available"));
    }

    @Test
    public void idBoundariesAndOverflowAreHandledAtTheFieldPrompt() throws Exception
    {
        String out = run("2\n2147483648\n2147483647\nEMT\n"
            + incident(0, "MEDICAL", "Clinic", "LOW")
            + "3\n0\n2147483647\n4\n0\n5\n2147483647\n6\n0\n");
        assertTrue(out.contains("Invalid input:"));
        assertTrue(out.contains("#2147483647 EMT Available"));
        assertTrue(out.contains("Resolved | Unit: #2147483647"));
        assertTrue(out.contains("Unit #2147483647 is back in service"));
    }

    @Test
    public void parsingDoesNotDependOnSystemLocale() throws Exception
    {
        Locale original = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            String out = run(unit(1, "police")
                + incident(1, "missing_persons", "Park", "high") + "6\n0\n");
            assertFalse(out.contains("Invalid input:"));
            assertTrue(out.contains("Missing persons"));
            assertTrue(out.contains("Police Available"));
        }
        finally
        {
            Locale.setDefault(original);
        }
    }

    @Test
    public void prioritySelectsTheMostSevereActiveIncident() throws Exception
    {
        String out = run(ready() + incident(2, "FIRE", "Oak Avenue", "CRITICAL")
            + "7\n3\n2\n10\n4\n2\n7\n8\n9\n0\n");
        int first = out.indexOf("HIGHEST-PRIORITY INCIDENT");
        int second = out.indexOf("HIGHEST-PRIORITY INCIDENT", first + 1);
        assertTrue(first >= 0 && second > first);
        assertTrue(out.substring(first, out.indexOf("dispatch>", first)).contains("#2 | Fire | Critical"));
        assertTrue(out.substring(second, out.indexOf("dispatch>", second)).contains("#1 | Fire | High"));
        assertTrue(out.contains("Assigned unit 10 to incident 2"));
        assertTrue(out.contains("Active incidents: 1"));
        assertTrue(out.contains("Resolved incidents: 1"));
    }

    @Test
    public void everyInvalidRegistrationFieldRecoversBeforeSaving() throws Exception
    {
        String[][] cases = {
            {"2\n", "", "10\nEMT\n"},
            {"2\n", "abc", "10\nEMT\n"},
            {"2\n", "-1", "10\nEMT\n"},
            {"2\n", "2147483648", "10\nEMT\n"},
            {"2\n10\n", "INVALID", "EMT\n"},
            {"2\n10\n", "", "EMT\n"},
            {"1\n", "", "1\nFIRE\nX\nHIGH\n"},
            {"1\n", "-1", "1\nFIRE\nX\nHIGH\n"},
            {"1\n1\n", "INVALID", "FIRE\nX\nHIGH\n"},
            {"1\n1\nFIRE\n", "", "X\nHIGH\n"},
            {"1\n1\nFIRE\n", "   ", "X\nHIGH\n"},
            {"1\n1\nFIRE\nX\n", "INVALID", "HIGH\n"}
        };
        for (String[] sample : cases)
        {
            String out = run(sample[0] + sample[1] + "\n" + sample[2] + "6\n0\n");
            assertTrue(sample[0] + sample[1], out.contains("Invalid input:"));
            assertTrue(out.replaceAll("\\s+", " ").contains("Try again."));
            assertTrue(out.contains("[OK] Registered") || out.contains("[OK] Reported"));
            assertFalse(out.contains("Action cancelled."));
        }
    }

    @Test
    public void eofAndZeroExitWithoutProcessingLaterInput() throws Exception
    {
        assertTrue(run("").contains("EMERGENCY DISPATCH"));
        assertTrue(run("").contains("Dispatch session ended."));
        String out = run("0\n2\n10\nEMT\n");
        assertFalse(out.contains("REGISTER A RESPONSE UNIT"));
        assertFalse(out.contains("Invalid input:"));
    }

    @Test
    public void guidedFieldsRetryOnlyTheInvalidField() throws Exception
    {
        String out = run("1\nabc\n-1\n1\nwrong\n1\n \nMain Street\n0\n3\n6\n0\n");
        assertEquals(5, out.split("Invalid input:", -1).length - 1);
        assertTrue(out.contains("Reported incident #1 (Fire, High)"));
        assertTrue(out.contains("Location: Main Street"));
        assertTrue(out.contains("Active: 1"));
        assertFalse(out.contains("Action cancelled"));
    }

    @Test
    public void rejectedActionsExplainTheProblemAndPreserveState() throws Exception
    {
        String setup = ready() + unit(11, "EMT") + incident(2, "FIRE", "Park", "LOW");
        String out = run(setup + "3\n77\n10\n3\n1\n99\n3\n1\n10\n"
            + "3\n2\n10\n3\n1\n11\n5\n10\n4\n99\n5\n99\n"
            + "4\n1\n4\n1\n3\n1\n10\n6\n9\n0\n");
        String joined = out.replaceAll("\\s+", " ");
        assertTrue(joined.contains("Incident #77 does not exist"));
        assertTrue(joined.contains("Unit #99 does not exist"));
        assertTrue(joined.contains("Unit #10 is busy"));
        assertTrue(joined.contains("Incident #1 already has unit #10 assigned"));
        assertTrue(joined.contains("still assigned to active incident #1"));
        assertTrue(joined.contains("Incident #99 does not exist"));
        assertTrue(joined.contains("Incident #1 is already resolved"));
        assertTrue(joined.contains("Successful dispatches: 1"));
        assertTrue(joined.contains("Resolved incidents: 1"));
        assertTrue(joined.contains("#11 EMT Available"));
        assertTrue(joined.contains("#10 Fire rescue Busy"));
    }

    @Test
    public void cancellingAtAnyRegistrationPromptDoesNotSaveAnything() throws Exception
    {
        String[] cancelled = {"1\nback", "1\n1\nback", "1\n1\n1\nback",
            "1\n1\n1\nMain Street\nback", "2\nback", "2\n10\nback"};
        for (String answers : cancelled)
        {
            String out = run(answers + "\n6\n0\n");
            assertTrue(answers, out.contains("Action cancelled. Nothing was changed."));
            assertTrue(answers, out.contains("Active: 0 | Units: 0 | Resolved: 0"));
            assertFalse(answers, out.contains("[OK] Registered"));
            assertFalse(answers, out.contains("[OK] Reported"));
        }
    }

    @Test
    public void cancellingActionsLeavesBothModelsUnchanged() throws Exception
    {
        for (String action : new String[] {"3\nback", "3\n1\nback", "4\nback", "5\nback"})
        {
            String out = run(ready() + action + "\n6\n9\n0\n");
            assertTrue(action, out.contains("Action cancelled."));
            assertTrue(action, out.contains("Active | Unit: Not assigned"));
            assertTrue(action, out.contains("#10 Fire rescue Available"));
            assertTrue(action, out.contains("Successful dispatches: 0"));
        }
    }

    @Test(timeout = 3000)
    public void eofAtEveryGuidedStageExitsWithoutPartialChanges() throws Exception
    {
        String[] partial = {"1\n", "1\n1\n", "1\n1\n1\n", "1\n1\n1\nX\n", "2\n", "2\n10\n"};
        for (String answers : partial)
        {
            String out = run(answers);
            assertTrue(answers, out.contains("Dispatch session ended."));
            assertFalse(answers, out.contains("[OK]"));
        }
        for (String answers : new String[] {"3\n", "3\n1\n", "4\n", "5\n"})
        {
            String out = run(ready() + answers);
            assertTrue(out.contains("Dispatch session ended."));
            assertFalse(out.contains("[OK] Assigned"));
            assertFalse(out.contains("[OK] Resolved"));
            assertFalse(out.contains("is back in service"));
        }
    }

    @Test
    public void missingPrerequisitesReturnToTheMenu() throws Exception
    {
        String out = run("3\n4\n5\n7\n0\n");
        assertTrue(out.contains("No active incidents. Report an incident first"));
        assertTrue(out.contains("No registered units. Register a response unit first"));
        assertTrue(out.contains("No active incidents."));
        String noUnits = run(incident(1, "FIRE", "X", "HIGH") + "3\n0\n");
        assertTrue(noUnits.contains("No registered units. Register a response unit first"));
    }

    @Test
    public void boardUsesReadableIncidentRecordsAndUnitColumns() throws Exception
    {
        String out = run(ready() + "6\n0\n");
        out = out.substring(out.indexOf("DISPATCH BOARD"));
        assertTrue(out.contains("ACTIVE INCIDENTS (1)"));
        assertTrue(out.contains("#1 | Fire | High"));
        assertTrue(out.contains("Active | Unit: Not assigned"));
        assertTrue(out.contains("#10 Fire rescue Available"));
        assertFalse(out.contains("Incident ID:"));
        assertFalse(out.contains("null"));
    }

    @Test
    public void statisticsOmitUnusedCategoriesAndHandleEmptyHistory() throws Exception
    {
        String out = run("8\n9\n" + incident(1, "FIRE", "X", "LOW") + "4\n1\n9\n0\n");
        assertTrue(out.contains("No successful dispatches yet."));
        assertTrue(out.contains("No resolved incidents yet."));
        out = out.substring(out.lastIndexOf("STATISTICS"));
        assertTrue(out.contains("Fire 1"));
        assertFalse(out.contains("Hostage"));
        assertFalse(out.contains("Medical 0"));
        assertTrue(out.contains("Successful dispatches: 0"));
    }

    @Test
    public void invalidMenuChoicesAndRemovedActionAliasesDoNotRunActions() throws Exception
    {
        String[] invalid = {"-1", "10", "1.5", "abc", "incident", "unit", "assign",
            "resolve", "return", "view", "priority", "history", "stats", "help", "quit", "q", "exit"};
        for (String selection : invalid)
        {
            String out = run(selection + "\n6\n0\n");
            assertTrue(selection, out.contains("Invalid input:"));
            assertTrue(selection, out.contains("Choose a number from 0 to 9"));
            assertTrue(selection, out.contains("Active: 0 | Units: 0 | Resolved: 0"));
            assertFalse(selection, out.contains("REGISTER A RESPONSE UNIT"));
            assertFalse(selection, out.contains("REPORT AN INCIDENT"));
        }
    }

    @Test
    public void removedPipeSyntaxCannotChangeStateOrExitTheSession() throws Exception
    {
        String[] removed = {"unit|20|EMT", "incident|2|FIRE|X|HIGH", "assign|1|10",
            "resolve|1", "return|10", "view|extra", "0|extra", "quit|extra"};
        for (String selection : removed)
        {
            String out = run(ready() + selection + "\n6\n9\n0\n");
            assertTrue(selection, out.contains("Invalid input:"));
            assertTrue(selection, out.contains("Active: 1 | Units: 1 | Resolved: 0"));
            assertTrue(selection, out.contains("Active | Unit: Not assigned"));
            assertTrue(selection, out.contains("#10 Fire rescue Available"));
            assertTrue(selection, out.contains("Successful dispatches: 0"));
        }
    }

    @Test
    public void menuCanBeRepeatedAndBlankInputIsHarmless() throws Exception
    {
        String out = run("\n MENU \n \n2\n10\n1\n6\n0\n");
        assertEquals(2, out.split("1 Report an incident", -1).length - 1);
        assertTrue(out.contains("#10 Fire rescue Available"));
        assertFalse(out.contains("Invalid input:"));
        assertFalse(out.contains("One-line"));
        assertFalse(out.contains("COMMAND HELP"));
        assertFalse(out.contains("type help"));
    }

    @Test
    public void freeTextLocationsWrapWithoutLosingInformation() throws Exception
    {
        String location = "North entrance | community center beside the library and the "
            + "large parking garage near the intersection of Main Street and Oak Avenue";
        String raw = runRaw(incident(1, "FIRE", location, "HIGH") + "6\n0\n");
        assertTrue(raw.replaceAll("\\s+", " ").contains("Location: " + location));
        assertFalse(raw.contains("Invalid input:"));
        for (String line : raw.split("\\R"))
        {
            assertTrue("Overlong display line: " + line, line.length() <= 72);
        }
    }

    @Test
    public void duplicateIdsRemainRejectedAfterResolution() throws Exception
    {
        String out = run(ready() + unit(10, "POLICE")
            + incident(1, "MEDICAL", "Clinic", "LOW") + "4\n1\n"
            + incident(1, "FIRE", "Other street", "HIGH") + "6\n9\n0\n");
        String joined = out.replaceAll("\\s+", " ");
        assertTrue(joined.contains("Unit #10 is already registered"));
        assertEquals(2, joined.split("Incident #1 is already registered", -1).length - 1);
        assertTrue(joined.contains("Active: 0 | Units: 1 | Resolved: 1"));
        assertTrue(joined.contains("#10 Fire rescue Available"));
        assertTrue(joined.contains("Fire 1"));
        assertTrue(joined.contains("Successful dispatches: 0"));
    }
}
