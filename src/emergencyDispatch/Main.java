package emergencyDispatch;

import java.util.Scanner;
/** Small console prototype. Enter one command per line; EOF also exits. */
public class Main
{
    public static void main(String[] args)
    {
        EmergencyDispatch dispatch = new EmergencyDispatch();
        System.out.println("Emergency Dispatch prototype");
        System.out.println("incident|ID|TYPE|LOCATION|SEVERITY; unit|ID|TYPE");
        System.out.println("assign|INCIDENT_ID|UNIT_ID; resolve|ID; return|ID");
        System.out.println("view; priority; history; quit (enum names use uppercase)");
        System.out.println("Incident types: " + java.util.Arrays.toString(IncidentType.values()));
        System.out.println("Unit types: " + java.util.Arrays.toString(UnitType.values()));
        System.out.println("Severity: LOW, MEDIUM, HIGH, CRITICAL");
        Scanner input = new Scanner(System.in);
        while (input.hasNextLine())
        {
            // Split the command at each |. The -1 keeps blank fields so we can reject them.
            String[] fields = input.nextLine().split("\\|", -1);
            for (int i = 0; i < fields.length; i++)
            {
                fields[i] = fields[i].trim();
            }
            String command = fields[0].toLowerCase();
            try
            {
                if (command.equals("quit"))
                {
                    requireLength(fields, 1);
                    break;
                }
                switch (command)
                {
                    case "incident":
                        requireLength(fields, 5);
                        int incidentId = Integer.parseInt(fields[1]);
                        IncidentType incidentType = IncidentType.valueOf(fields[2]);
                        String location = fields[3];
                        Severity severity = Severity.valueOf(fields[4]);
                        Incident incident = new Incident(incidentId, incidentType, location, severity);
                        show(dispatch.reportIncident(incident));
                        break;
                    case "unit":
                        requireLength(fields, 3);
                        int unitId = Integer.parseInt(fields[1]);
                        UnitType unitType = UnitType.valueOf(fields[2]);
                        ResponseUnit unit = new ResponseUnit(unitId, unitType);
                        show(dispatch.addResponseUnit(unit));
                        break;
                    case "assign":
                        requireLength(fields, 3);
                        show(dispatch.assignUnit(Integer.parseInt(fields[1]), Integer.parseInt(fields[2])));
                        break;
                    case "resolve":
                        requireLength(fields, 2);
                        show(dispatch.resolveIncident(Integer.parseInt(fields[1])));
                        break;
                    case "return":
                        requireLength(fields, 2);
                        show(dispatch.returnUnitToService(Integer.parseInt(fields[1])));
                        break;
                    case "view":
                        requireLength(fields, 1);
                        System.out.println("Active: " + dispatch.getActiveIncidents());
                        System.out.println("Units: " + dispatch.getResponseUnits());
                        System.out.println("Resolved: " + dispatch.getResolvedIncidents());
                        break;
                    case "priority":
                        requireLength(fields, 1);
                        Incident next = dispatch.getHighestPriorityIncident();
                        if (next == null)
                        {
                            System.out.println("No active incidents");
                        }
                        else
                        {
                            System.out.println(next);
                        }
                        break;
                    case "history":
                        requireLength(fields, 1);
                        System.out.println(dispatch.getDispatchLog());
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown command");
                }
            }
            catch (IllegalArgumentException exception)
            {
                System.out.println("Invalid input: " + exception.getMessage() + ". Try again.");
            }
        }
    }

    private static void requireLength(String[] fields, int expected)
    {
        if (fields.length != expected)
        {
            throw new IllegalArgumentException("Wrong number of fields");
        }
    }

    private static void show(boolean success)
    {
        if (success)
        {
            System.out.println("OK");
        }
        else
        {
            System.out.println("Rejected: check IDs, assignment, and availability. Try again.");
        }
    }
}
