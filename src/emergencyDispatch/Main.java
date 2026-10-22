package emergencyDispatch;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

// Runs the numbered menu and guided prompts.
public class Main
{
    private final EmergencyDispatch dispatch = new EmergencyDispatch();
    private final Scanner input;
    private boolean endOfInput;

    private Main(Scanner input)
    {
        this.input = input;
    }

    public static void main(String[] args)
    {
        new Main(new Scanner(System.in, StandardCharsets.UTF_8)).run();
    }

    private void run()
    {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("  EMERGENCY DISPATCH");
        System.out.println("  Report emergencies. Coordinate response units.");
        System.out.println("============================================================");
        printMenu();
        while (!endOfInput)
        {
            System.out.print("\ndispatch> ");
            if (!input.hasNextLine())
            {
                break;
            }
            String selection = input.nextLine().trim();
            if (selection.isEmpty())
            {
                continue;
            }
            if (selection.equals("0"))
            {
                break;
            }
            try
            {
                execute(selection);
            }
            catch (IllegalArgumentException exception)
            {
                inputError(exception.getMessage());
            }
        }
        System.out.println("\nDispatch session ended.");
    }

    private void execute(String selection)
    {
        switch (selection.toLowerCase(Locale.ROOT))
        {
            case "1":
                reportGuided();
                break;
            case "2":
                registerGuided();
                break;
            case "3":
                assignGuided();
                break;
            case "4":
                resolveGuided();
                break;
            case "5":
                returnGuided();
                break;
            case "6":
                printBoard();
                break;
            case "7":
                heading("HIGHEST-PRIORITY INCIDENT");
                Incident next = dispatch.getHighestPriorityIncident();
                if (next == null)
                {
                    System.out.println("  No active incidents.");
                }
                else
                {
                    printIncident(next);
                    System.out.println("  Priority: severity first; smaller ID breaks a tie.");
                }
                break;
            case "8":
                printHistory();
                break;
            case "9":
                printStatistics();
                break;
            case "menu":
                printMenu();
                break;
            default:
                throw new IllegalArgumentException(
                    "Choose a number from 0 to 9, or type menu to see the options");
        }
    }

    private void printMenu()
    {
        System.out.println();
        System.out.println("  1  Report an incident       2  Register a response unit");
        System.out.println("  3  Dispatch a unit          4  Resolve an incident");
        System.out.println("  5  Return unit to service   6  View dispatch board");
        System.out.println("  7  View priority            8  View history");
        System.out.println("  9  View statistics          0  Quit");
        System.out.println();
        System.out.println("Choose a number. Type menu to see the options again.");
    }

    private void resolveGuided()
    {
        heading("RESOLVE AN INCIDENT");
        LinkedChain<Incident> incidents = dispatch.getActiveIncidents();
        if (incidents.isEmpty())
        {
            reject("No active incidents. Report an incident first");
            return;
        }
        printIncidents("ACTIVE INCIDENTS", incidents);
        System.out.println("Enter back to cancel.");
        Integer id = promptId("Incident ID");
        if (id == null)
        {
            cancelled();
            return;
        }
        resolve(id);
    }

    private void returnGuided()
    {
        heading("RETURN A UNIT");
        if (dispatch.getResponseUnits().isEmpty())
        {
            reject("No registered units. Register a response unit first");
            return;
        }
        printUnits();
        System.out.println("\nEnter back to cancel.");
        Integer id = promptId("Unit ID");
        if (id == null)
        {
            cancelled();
            return;
        }
        returnUnit(id);
    }

    // Collects all incident details before saving anything.
    private void reportGuided()
    {
        heading("REPORT AN INCIDENT");
        System.out.println("Enter back at any prompt to cancel.");
        Integer id = promptId("Incident ID");
        if (id == null)
        {
            cancelled();
            return;
        }
        IncidentType type = promptChoice("Incident type", IncidentType.values());
        if (type == null)
        {
            cancelled();
            return;
        }
        String location = promptLocation();
        if (location == null)
        {
            cancelled();
            return;
        }
        Severity severity = promptChoice("Severity", Severity.values());
        if (severity == null)
        {
            cancelled();
            return;
        }
        report(new Incident(id, type, location, severity));
    }

    private void registerGuided()
    {
        heading("REGISTER A RESPONSE UNIT");
        System.out.println("Enter back at any prompt to cancel.");
        Integer id = promptId("Unit ID");
        if (id == null)
        {
            cancelled();
            return;
        }
        UnitType type = promptChoice("Unit type", UnitType.values());
        if (type == null)
        {
            cancelled();
            return;
        }
        register(new ResponseUnit(id, type));
    }

    private void assignGuided()
    {
        heading("DISPATCH A UNIT");
        if (dispatch.getActiveIncidents().isEmpty())
        {
            reject("No active incidents. Report an incident first");
            return;
        }
        if (dispatch.getResponseUnits().isEmpty())
        {
            reject("No registered units. Register a response unit first");
            return;
        }
        printIncidents("ACTIVE INCIDENTS", dispatch.getActiveIncidents());
        printUnits();
        System.out.println("\nEnter back at any prompt to cancel.");
        Integer incidentId = promptId("Incident ID");
        if (incidentId == null)
        {
            cancelled();
            return;
        }
        Integer unitId = promptId("Unit ID");
        if (unitId == null)
        {
            cancelled();
            return;
        }
        assign(incidentId, unitId);
    }

    private String readField(String label)
    {
        System.out.print("  " + label + ": ");
        if (!input.hasNextLine())
        {
            endOfInput = true;
            return null;
        }
        String value = input.nextLine().trim();
        return value.equalsIgnoreCase("back") ? null : value;
    }

    private Integer promptId(String label)
    {
        while (true)
        {
            String value = readField(label);
            if (value == null)
            {
                return null;
            }
            try
            {
                return parseId(value);
            }
            catch (IllegalArgumentException exception)
            {
                inputError(exception.getMessage());
            }
        }
    }

    private String promptLocation()
    {
        while (true)
        {
            String value = readField("Location");
            if (value == null)
            {
                return null;
            }
            if (value.isEmpty())
            {
                inputError("Location must not be blank");
            }
            else
            {
                return value;
            }
        }
    }

    private <E extends Enum<E>> E promptChoice(String label, E[] choices)
    {
        printChoices(label, choices);
        while (true)
        {
            String value = readField(label + " (number or name)");
            if (value == null)
            {
                return null;
            }
            try
            {
                return parseChoice(value, choices, label.toLowerCase(Locale.ROOT));
            }
            catch (IllegalArgumentException exception)
            {
                inputError(exception.getMessage());
            }
        }
    }

    private <E extends Enum<E>> void printChoices(String label, E[] choices)
    {
        System.out.println("\n  " + label);
        for (int i = 0; i < choices.length; i++)
        {
            System.out.printf("  %d  %-19s", i + 1, displayName(choices[i]));
            if (i % 2 == 1 || i == choices.length - 1)
            {
                System.out.println();
            }
        }
    }

    private <E extends Enum<E>> E parseChoice(String value, E[] choices, String label)
    {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        for (int i = 0; i < choices.length; i++)
        {
            if (normalized.equals(choices[i].name()) || normalized.equals(String.valueOf(i + 1)))
            {
                return choices[i];
            }
        }
        throw new IllegalArgumentException("Choose a valid " + label
            + " (1-" + choices.length + " or a listed name)");
    }

    private void report(Incident incident)
    {
        if (dispatch.reportIncident(incident))
        {
            success("Reported incident #" + incident.getId() + " ("
                + displayName(incident.getType()) + ", " + displayName(incident.getSeverity()) + ")");
            printWrapped("  Location: ", incident.getLocation());
        }
        else
        {
            reject("Incident #" + incident.getId()
                + " is already registered, including resolved incidents. Use a different ID");
        }
    }

    private void register(ResponseUnit unit)
    {
        if (dispatch.addResponseUnit(unit))
        {
            success("Registered unit #" + unit.getId() + " ("
                + displayName(unit.getType()) + "). Ready for dispatch");
        }
        else
        {
            reject("Unit #" + unit.getId() + " is already registered. Use a different ID");
        }
    }

    private void assign(int incidentId, int unitId)
    {
        if (dispatch.assignUnit(incidentId, unitId))
        {
            success("Assigned unit #" + unitId + " to incident #" + incidentId);
            System.out.println("  Resolve incident #" + incidentId
                + ", then return unit #" + unitId + " to service.");
            return;
        }
        Incident incident = dispatch.findActiveIncident(incidentId);
        ResponseUnit unit = dispatch.findResponseUnit(unitId);
        if (incident == null)
        {
            reject(incidentUnavailable(incidentId));
        }
        else if (unit == null)
        {
            reject("Unit #" + unitId + " does not exist. Register it first");
        }
        else if (incident.getAssignedUnitId() != null)
        {
            reject("Incident #" + incidentId + " already has unit #"
                + incident.getAssignedUnitId() + " assigned");
        }
        else
        {
            reject("Unit #" + unitId
                + " is busy. Resolve its incident and return it to service first");
        }
    }

    private void resolve(int incidentId)
    {
        Incident incident = dispatch.findActiveIncident(incidentId);
        if (dispatch.resolveIncident(incidentId))
        {
            success("Resolved incident #" + incidentId + ". Saved to history");
            if (incident.getAssignedUnitId() != null)
            {
                System.out.println("  Unit #" + incident.getAssignedUnitId()
                    + " is still busy. Choose 5 to return it to service.");
            }
        }
        else
        {
            reject(incidentUnavailable(incidentId));
        }
    }

    private String incidentUnavailable(int id)
    {
        Incident[] resolved = dispatch.getResolvedIncidents().toArray(
            new Incident[dispatch.getResolvedIncidents().size()]);
        for (Incident incident : resolved)
        {
            if (incident.getId() == id)
            {
                return "Incident #" + id + " is already resolved. Choose an active incident";
            }
        }
        return "Incident #" + id + " does not exist. Report it first";
    }

    private void returnUnit(int id)
    {
        ResponseUnit unit = dispatch.findResponseUnit(id);
        if (dispatch.returnUnitToService(id))
        {
            success("Unit #" + id + (unit.isAvailable() ? " is already available" : " is back in service"));
            return;
        }
        if (unit == null)
        {
            reject("Unit #" + id + " does not exist. Register it first");
            return;
        }
        Incident[] incidents = dispatch.getActiveIncidents().toArray(
            new Incident[dispatch.getActiveIncidents().size()]);
        for (Incident incident : incidents)
        {
            if (incident.getAssignedUnitId() != null && incident.getAssignedUnitId() == id)
            {
                reject("Unit #" + id + " is still assigned to active incident #"
                    + incident.getId() + ". Resolve that incident before returning the unit");
                return;
            }
        }
        reject("Unit #" + id + " cannot return to service");
    }

    private void printBoard()
    {
        heading("DISPATCH BOARD");
        System.out.println("  Active: " + dispatch.getActiveIncidents().size()
            + "   |   Units: " + dispatch.getResponseUnits().size()
            + "   |   Resolved: " + dispatch.getResolvedIncidents().size());
        printIncidents("ACTIVE INCIDENTS", dispatch.getActiveIncidents());
        printUnits();
        printIncidents("RESOLVED INCIDENTS", dispatch.getResolvedIncidents());
    }

    private void printIncidents(String title, LinkedChain<Incident> chain)
    {
        System.out.println("\n" + title + " (" + chain.size() + ")");
        if (chain.isEmpty())
        {
            System.out.println("  None.");
            return;
        }
        Incident[] incidents = chain.toArray(new Incident[chain.size()]);
        for (Incident incident : incidents)
        {
            printIncident(incident);
        }
    }

    private void printIncident(Incident incident)
    {
        System.out.println("  #" + incident.getId() + "  |  "
            + displayName(incident.getType()) + "  |  " + displayName(incident.getSeverity()));
        printWrapped("    Location: ", incident.getLocation());
        System.out.println("    " + displayName(incident.getStatus()) + "  |  Unit: "
            + (incident.getAssignedUnitId() == null ? "Not assigned" : "#" + incident.getAssignedUnitId()));
        System.out.println();
    }

    private void printUnits()
    {
        LinkedChain<ResponseUnit> chain = dispatch.getResponseUnits();
        System.out.println("\nRESPONSE UNITS (" + chain.size() + ")");
        if (chain.isEmpty())
        {
            System.out.println("  None. Choose 2 to register a unit.");
            return;
        }
        System.out.printf("  %-12s %-16s %s%n", "ID", "TYPE", "STATUS");
        ResponseUnit[] units = chain.toArray(new ResponseUnit[chain.size()]);
        for (ResponseUnit unit : units)
        {
            System.out.printf("  %-12s %-16s %s%n", "#" + unit.getId(),
                displayName(unit.getType()), unit.isAvailable() ? "Available" : "Busy");
        }
    }

    private void printHistory()
    {
        heading("DISPATCH HISTORY");
        LinkedChain<String> chain = dispatch.getDispatchLog();
        if (chain.isEmpty())
        {
            System.out.println("  No successful dispatches yet.");
        }
        else
        {
            String[] messages = chain.toArray(new String[chain.size()]);
            for (int i = 0; i < messages.length; i++)
            {
                System.out.println("  " + (i + 1) + ". " + messages[i]);
            }
        }
        printIncidents("RESOLVED INCIDENTS", dispatch.getResolvedIncidents());
    }

    private void printStatistics()
    {
        heading("STATISTICS");
        System.out.println("  Active incidents:      " + dispatch.getActiveIncidents().size());
        System.out.println("  Resolved incidents:    " + dispatch.getResolvedIncidents().size());
        System.out.println("  Successful dispatches: " + dispatch.getDispatchCount());
        System.out.println("\n  Resolved by emergency type");
        boolean any = false;
        for (IncidentType type : IncidentType.values())
        {
            int count = dispatch.getResolvedCount(type);
            if (count > 0)
            {
                System.out.printf("    %-18s %d%n", displayName(type), count);
                any = true;
            }
        }
        if (!any)
        {
            System.out.println("    No resolved incidents yet.");
        }
    }

    private String displayName(Enum<?> value)
    {
        if (value == UnitType.EMT || value == UnitType.SWAT)
        {
            return value.name();
        }
        String name = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private int parseId(String value)
    {
        try
        {
            int id = Integer.parseInt(value);
            if (id < 0)
            {
                throw new IllegalArgumentException("IDs must be nonnegative integers");
            }
            return id;
        }
        catch (NumberFormatException exception)
        {
            throw new IllegalArgumentException("Enter a whole-number ID from 0 to 2147483647");
        }
    }

    private void heading(String title)
    {
        System.out.println("\n" + title);
        System.out.println("------------------------------------------------------------");
    }

    private void success(String message)
    {
        printWrapped("\n[OK] ", message + ".");
    }

    private void reject(String message)
    {
        printWrapped("\nRejected: ", message + ". Try again.");
    }

    private void inputError(String message)
    {
        printWrapped("\nInvalid input: ", message + ". Try again.");
    }

    private void cancelled()
    {
        System.out.println("\nAction cancelled. Nothing was changed.");
    }

    // Wraps long locations and messages for the terminal.
    private void printWrapped(String prefix, String text)
    {
        String indentation = " ".repeat(prefix.substring(prefix.lastIndexOf('\n') + 1).length());
        System.out.print(prefix);
        int width = Math.max(20, 72 - indentation.length());
        String remaining = text;
        while (remaining.length() > width)
        {
            int split = remaining.lastIndexOf(' ', width);
            if (split <= 0)
            {
                split = width;
            }
            System.out.println(remaining.substring(0, split));
            System.out.print(indentation);
            remaining = remaining.substring(split).stripLeading();
        }
        System.out.println(remaining);
    }
}
