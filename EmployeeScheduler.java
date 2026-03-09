
import java.util.*;

public class EmployeeScheduler {
    private static final String[] DAYS = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    private static final String[] SHIFTS = {"Morning", "Afternoon", "Evening"};
    private static final int MIN_EMPLOYEES_PER_SHIFT = 2;
    private static final int MAX_DAYS_PER_WEEK = 5;

    private final Map<String, Map<String, List<String>>> employees;
    private final Map<String, Map<String, List<String>>> schedule;
    private final Map<String, Map<String, Boolean>> assignedDay;
    private final Map<String, Integer> workDays;
    private final List<String> logs;
    private final Random random;

    public EmployeeScheduler(Map<String, Map<String, List<String>>> employees, long seed) {
        this.employees = employees;
        this.schedule = new LinkedHashMap<>();
        this.assignedDay = new LinkedHashMap<>();
        this.workDays = new LinkedHashMap<>();
        this.logs = new ArrayList<>();
        this.random = new Random(seed);

        for (String day : DAYS) {
            Map<String, List<String>> shiftMap = new LinkedHashMap<>();
            for (String shift : SHIFTS) {
                shiftMap.put(shift, new ArrayList<>());
            }
            schedule.put(day, shiftMap);
        }

        for (String employee : employees.keySet()) {
            Map<String, Boolean> dayMap = new LinkedHashMap<>();
            for (String day : DAYS) {
                dayMap.put(day, false);
            }
            assignedDay.put(employee, dayMap);
            workDays.put(employee, 0);
        }
    }

    private boolean canWork(String employee, String day) {
        return !assignedDay.get(employee).get(day) && workDays.get(employee) < MAX_DAYS_PER_WEEK;
    }

    private boolean assign(String employee, String day, String shift, String reason) {
        if (canWork(employee, day)) {
            schedule.get(day).get(shift).add(employee);
            assignedDay.get(employee).put(day, true);
            workDays.put(employee, workDays.get(employee) + 1);
            if (reason != null) {
                logs.add(reason);
            }
            return true;
        }
        return false;
    }

    private List<String> availableEmployees(String day) {
        List<String> result = new ArrayList<>();
        for (String employee : employees.keySet()) {
            if (canWork(employee, day)) {
                result.add(employee);
            }
        }
        return result;
    }

    private void assignShiftForDay(String day) {
        List<String> unassigned = availableEmployees(day);

        for (String shift : SHIFTS) {
            while (schedule.get(day).get(shift).size() < MIN_EMPLOYEES_PER_SHIFT) {
                List<String> candidates = new ArrayList<>();
                for (String employee : unassigned) {
                    if (employees.get(employee).getOrDefault(day, Arrays.asList(SHIFTS)).get(0).equals(shift)) {
                        candidates.add(employee);
                    }
                }

                if (candidates.isEmpty()) {
                    for (String employee : unassigned) {
                        if (employees.get(employee).getOrDefault(day, Arrays.asList(SHIFTS)).contains(shift)) {
                            candidates.add(employee);
                        }
                    }
                }

                if (candidates.isEmpty()) {
                    break;
                }

                candidates.sort((a, b) -> {
                    int workCompare = Integer.compare(workDays.get(a), workDays.get(b));
                    if (workCompare != 0) return workCompare;

                    int prefCompare = Integer.compare(
                        employees.get(a).get(day).indexOf(shift),
                        employees.get(b).get(day).indexOf(shift)
                    );
                    if (prefCompare != 0) return prefCompare;

                    return a.compareTo(b);
                });

                String chosen = candidates.get(0);
                int prefIndex = employees.get(chosen).get(day).indexOf(shift);
                String reason = null;

                if (prefIndex > 0) {
                    reason = "Conflict resolved: " + chosen + " preferred "
                        + employees.get(chosen).get(day).get(0) + " on " + day
                        + ", but was moved to " + shift + " (preference rank " + (prefIndex + 1) + ")";
                }

                assign(chosen, day, shift, reason);
                unassigned.remove(chosen);
            }
        }

        for (String shift : SHIFTS) {
            while (schedule.get(day).get(shift).size() < MIN_EMPLOYEES_PER_SHIFT) {
                List<String> remaining = availableEmployees(day);
                if (remaining.isEmpty()) {
                    break;
                }

                remaining.sort((a, b) -> {
                    int workCompare = Integer.compare(workDays.get(a), workDays.get(b));
                    if (workCompare != 0) return workCompare;
                    return a.compareTo(b);
                });

                int lowestCount = workDays.get(remaining.get(0));
                List<String> pool = new ArrayList<>();
                for (String employee : remaining) {
                    if (workDays.get(employee) == lowestCount) {
                        pool.add(employee);
                    }
                }

                String chosen = pool.get(random.nextInt(pool.size()));
                assign(chosen, day, shift,
                    "Random fill: " + chosen + " added to " + day + " " + shift + " to satisfy minimum staffing");
            }
        }

        for (String employee : new ArrayList<>(unassigned)) {
            if (workDays.get(employee) >= MAX_DAYS_PER_WEEK) {
                continue;
            }

            int dayIndex = Arrays.asList(DAYS).indexOf(day);
            if (dayIndex + 1 >= DAYS.length) {
                continue;
            }

            String nextDay = DAYS[dayIndex + 1];
            if (assignedDay.get(employee).get(nextDay)) {
                continue;
            }

            for (String nextShift : employees.get(employee).getOrDefault(nextDay, Arrays.asList(SHIFTS))) {
                if (schedule.get(nextDay).get(nextShift).size() < MIN_EMPLOYEES_PER_SHIFT) {
                    boolean placed = assign(
                        employee,
                        nextDay,
                        nextShift,
                        "Next-day move: " + employee + " could not be placed on " + day
                            + ", so they were assigned to " + nextDay + " " + nextShift
                    );
                    if (placed) {
                        break;
                    }
                }
            }
        }
    }

    public void build() {
        for (String day : DAYS) {
            assignShiftForDay(day);
        }
    }

    public void printSchedule() {
        System.out.println("========================================================================");
        System.out.println("FINAL EMPLOYEE SCHEDULE");
        System.out.println("========================================================================");

        for (String day : DAYS) {
            System.out.println("\n" + day);
            System.out.println("------------------------------------------------------------------------");
            for (String shift : SHIFTS) {
                List<String> workers = schedule.get(day).get(shift);
                String workerList = workers.isEmpty() ? "No assignment" : String.join(", ", workers);
                System.out.printf("%-10s: %s%n", shift, workerList);
            }
        }

        System.out.println("\n========================================================================");
        System.out.println("EMPLOYEE DAYS WORKED");
        System.out.println("========================================================================");
        List<String> names = new ArrayList<>(workDays.keySet());
        Collections.sort(names);
        for (String name : names) {
            System.out.printf("%-10s -> %d day(s)%n", name, workDays.get(name));
        }

        System.out.println("\n========================================================================");
        System.out.println("CONFLICT / AUTO-ASSIGNMENT LOG");
        System.out.println("========================================================================");
        if (logs.isEmpty()) {
            System.out.println("No conflicts detected.");
        } else {
            for (String log : logs) {
                System.out.println("- " + log);
            }
        }
    }

    private static List<String> readPreferenceLine(Scanner scanner, String day) {
        System.out.print(day + ": ");
        String raw = scanner.nextLine().trim();
        String[] parts = raw.split(",");
        List<String> preferences = new ArrayList<>();

        for (String part : parts) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty()) {
                String normalized = cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1).toLowerCase();
                if (Arrays.asList(SHIFTS).contains(normalized) && !preferences.contains(normalized)) {
                    preferences.add(normalized);
                }
            }
        }

        for (String shift : SHIFTS) {
            if (!preferences.contains(shift)) {
                preferences.add(shift);
            }
        }

        return new ArrayList<>(preferences.subList(0, 3));
    }

    private static Map<String, Map<String, List<String>>> getManualInput() {
        Scanner scanner = new Scanner(System.in);
        Map<String, Map<String, List<String>>> employees = new LinkedHashMap<>();

        System.out.print("Enter number of employees: ");
        int numberOfEmployees = Integer.parseInt(scanner.nextLine().trim());

        for (int i = 0; i < numberOfEmployees; i++) {
            System.out.print("Employee name: ");
            String name = scanner.nextLine().trim();
            Map<String, List<String>> weeklyPrefs = new LinkedHashMap<>();

            System.out.println("Enter ranked shift preferences for " + name + ".");
            System.out.println("Use one line per day with three comma-separated values, for example: Morning,Evening,Afternoon");

            for (String day : DAYS) {
                weeklyPrefs.put(day, readPreferenceLine(scanner, day));
            }

            employees.put(name, weeklyPrefs);
        }

        return employees;
    }

    private static Map<String, Map<String, List<String>>> getSampleEmployees() {
        Map<String, Map<String, List<String>>> data = new LinkedHashMap<>();

        data.put("Alice", createWeek(
            arr("Morning", "Afternoon", "Evening"),
            arr("Morning", "Evening", "Afternoon"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Morning", "Evening", "Afternoon"),
            arr("Evening", "Afternoon", "Morning"),
            arr("Morning", "Afternoon", "Evening")
        ));

        data.put("Bob", createWeek(
            arr("Morning", "Evening", "Afternoon"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Evening", "Afternoon", "Morning"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Evening", "Morning", "Afternoon")
        ));

        data.put("Carol", createWeek(
            arr("Afternoon", "Morning", "Evening"),
            arr("Afternoon", "Evening", "Morning"),
            arr("Evening", "Afternoon", "Morning"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Evening", "Afternoon", "Morning"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Morning", "Evening", "Afternoon")
        ));

        data.put("David", createWeek(
            arr("Evening", "Afternoon", "Morning"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Evening", "Morning", "Afternoon"),
            arr("Morning", "Evening", "Afternoon"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Evening", "Morning", "Afternoon"),
            arr("Afternoon", "Morning", "Evening")
        ));

        data.put("Emma", createWeek(
            arr("Morning", "Afternoon", "Evening"),
            arr("Evening", "Morning", "Afternoon"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Afternoon", "Evening", "Morning"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Afternoon", "Evening", "Morning"),
            arr("Evening", "Afternoon", "Morning")
        ));

        data.put("Frank", createWeek(
            arr("Afternoon", "Evening", "Morning"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Evening", "Morning", "Afternoon"),
            arr("Afternoon", "Evening", "Morning"),
            arr("Morning", "Evening", "Afternoon"),
            arr("Morning", "Afternoon", "Evening")
        ));

        data.put("Grace", createWeek(
            arr("Evening", "Morning", "Afternoon"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Morning", "Evening", "Afternoon"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Evening", "Morning", "Afternoon"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Afternoon", "Evening", "Morning")
        ));

        data.put("Henry", createWeek(
            arr("Morning", "Afternoon", "Evening"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Evening", "Afternoon", "Morning"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Evening", "Afternoon", "Morning"),
            arr("Morning", "Evening", "Afternoon")
        ));

        data.put("Ivy", createWeek(
            arr("Afternoon", "Morning", "Evening"),
            arr("Evening", "Afternoon", "Morning"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Morning", "Evening", "Afternoon"),
            arr("Evening", "Morning", "Afternoon"),
            arr("Afternoon", "Morning", "Evening"),
            arr("Evening", "Morning", "Afternoon")
        ));

        data.put("Jack", createWeek(
            arr("Evening", "Afternoon", "Morning"),
            arr("Morning", "Evening", "Afternoon"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Evening", "Morning", "Afternoon"),
            arr("Morning", "Afternoon", "Evening"),
            arr("Afternoon", "Evening", "Morning"),
            arr("Morning", "Afternoon", "Evening")
        ));

        return data;
    }

    private static Map<String, List<String>> createWeek(
        List<String> monday, List<String> tuesday, List<String> wednesday,
        List<String> thursday, List<String> friday, List<String> saturday, List<String> sunday
    ) {
        Map<String, List<String>> week = new LinkedHashMap<>();
        week.put("Monday", monday);
        week.put("Tuesday", tuesday);
        week.put("Wednesday", wednesday);
        week.put("Thursday", thursday);
        week.put("Friday", friday);
        week.put("Saturday", saturday);
        week.put("Sunday", sunday);
        return week;
    }

    private static List<String> arr(String a, String b, String c) {
        return new ArrayList<>(Arrays.asList(a, b, c));
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Employee Schedule Manager");
        System.out.println("1. Use sample data");
        System.out.println("2. Enter employees manually");
        System.out.print("Choose an option (1 or 2): ");
        String choice = scanner.nextLine().trim();

        Map<String, Map<String, List<String>>> employees;
        if ("2".equals(choice)) {
            employees = getManualInput();
        } else {
            employees = getSampleEmployees();
        }

        EmployeeScheduler scheduler = new EmployeeScheduler(employees, 42L);
        scheduler.build();
        scheduler.printSchedule();
    }
}
