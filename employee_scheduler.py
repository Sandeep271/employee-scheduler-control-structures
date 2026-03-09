
import random

DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
SHIFTS = ["Morning", "Afternoon", "Evening"]
MIN_EMPLOYEES_PER_SHIFT = 2
MAX_DAYS_PER_WEEK = 5

class Scheduler:
    def __init__(self, employees, seed=42):
        self.employees = employees
        self.random = random.Random(seed)
        self.schedule = {day: {shift: [] for shift in SHIFTS} for day in DAYS}
        self.assigned_day = {name: {day: False for day in DAYS} for name in employees}
        self.work_days = {name: 0 for name in employees}
        self.logs = []

    def can_work(self, employee, day):
        return (not self.assigned_day[employee][day]) and self.work_days[employee] < MAX_DAYS_PER_WEEK

    def assign(self, employee, day, shift, reason=None):
        if self.can_work(employee, day):
            self.schedule[day][shift].append(employee)
            self.assigned_day[employee][day] = True
            self.work_days[employee] += 1
            if reason:
                self.logs.append(reason)
            return True
        return False

    def available_employees(self, day):
        return [employee for employee in self.employees if self.can_work(employee, day)]

    def assign_shift_for_day(self, day):
        unassigned = self.available_employees(day)

        for shift in SHIFTS:
            while len(self.schedule[day][shift]) < MIN_EMPLOYEES_PER_SHIFT:
                candidates = [
                    employee for employee in unassigned
                    if self.employees[employee].get(day, SHIFTS)[0] == shift
                ]

                if not candidates:
                    candidates = [
                        employee for employee in unassigned
                        if shift in self.employees[employee].get(day, SHIFTS)
                    ]

                if not candidates:
                    break

                candidates.sort(key=lambda name: (self.work_days[name], self.employees[name][day].index(shift), name))
                chosen = candidates[0]
                pref_index = self.employees[chosen][day].index(shift)

                if pref_index == 0:
                    reason = None
                else:
                    reason = (
                        f"Conflict resolved: {chosen} preferred {self.employees[chosen][day][0]} on {day}, "
                        f"but was moved to {shift} (preference rank {pref_index + 1})"
                    )

                self.assign(chosen, day, shift, reason)
                unassigned.remove(chosen)

        # If any shifts are still below the minimum, fill randomly from remaining employees.
        for shift in SHIFTS:
            while len(self.schedule[day][shift]) < MIN_EMPLOYEES_PER_SHIFT:
                remaining = self.available_employees(day)
                if not remaining:
                    break

                remaining.sort(key=lambda name: (self.work_days[name], name))
                lowest_count = self.work_days[remaining[0]]
                pool = [name for name in remaining if self.work_days[name] == lowest_count]
                chosen = self.random.choice(pool)
                self.assign(
                    chosen,
                    day,
                    shift,
                    f"Random fill: {chosen} added to {day} {shift} to satisfy minimum staffing"
                )

        # Optional extra placement for employees who could not be used on this day:
        # try the next day if one exists and a shift there is still understaffed.
        for employee in list(unassigned):
            if self.work_days[employee] >= MAX_DAYS_PER_WEEK:
                continue
            day_index = DAYS.index(day)
            if day_index + 1 >= len(DAYS):
                continue

            next_day = DAYS[day_index + 1]
            if self.assigned_day[employee][next_day]:
                continue

            for next_shift in self.employees[employee].get(next_day, SHIFTS):
                if len(self.schedule[next_day][next_shift]) < MIN_EMPLOYEES_PER_SHIFT:
                    self.assign(
                        employee,
                        next_day,
                        next_shift,
                        f"Next-day move: {employee} could not be placed on {day}, so they were assigned to {next_day} {next_shift}"
                    )
                    break

    def build(self):
        for day in DAYS:
            self.assign_shift_for_day(day)
        return self.schedule

    def print_schedule(self):
        print("=" * 72)
        print("FINAL EMPLOYEE SCHEDULE")
        print("=" * 72)
        for day in DAYS:
            print(f"\n{day}")
            print("-" * 72)
            for shift in SHIFTS:
                workers = ", ".join(self.schedule[day][shift]) if self.schedule[day][shift] else "No assignment"
                print(f"{shift:<10}: {workers}")

        print("\n" + "=" * 72)
        print("EMPLOYEE DAYS WORKED")
        print("=" * 72)
        for employee, days_worked in sorted(self.work_days.items()):
            print(f"{employee:<10} -> {days_worked} day(s)")

        print("\n" + "=" * 72)
        print("CONFLICT / AUTO-ASSIGNMENT LOG")
        print("=" * 72)
        if self.logs:
            for item in self.logs:
                print(f"- {item}")
        else:
            print("No conflicts detected.")

def get_sample_employees():
    return {
        "Alice": {
            "Monday": ["Morning", "Afternoon", "Evening"],
            "Tuesday": ["Morning", "Evening", "Afternoon"],
            "Wednesday": ["Afternoon", "Morning", "Evening"],
            "Thursday": ["Morning", "Afternoon", "Evening"],
            "Friday": ["Morning", "Evening", "Afternoon"],
            "Saturday": ["Evening", "Afternoon", "Morning"],
            "Sunday": ["Morning", "Afternoon", "Evening"],
        },
        "Bob": {
            "Monday": ["Morning", "Evening", "Afternoon"],
            "Tuesday": ["Afternoon", "Morning", "Evening"],
            "Wednesday": ["Morning", "Afternoon", "Evening"],
            "Thursday": ["Evening", "Afternoon", "Morning"],
            "Friday": ["Afternoon", "Morning", "Evening"],
            "Saturday": ["Morning", "Afternoon", "Evening"],
            "Sunday": ["Evening", "Morning", "Afternoon"],
        },
        "Carol": {
            "Monday": ["Afternoon", "Morning", "Evening"],
            "Tuesday": ["Afternoon", "Evening", "Morning"],
            "Wednesday": ["Evening", "Afternoon", "Morning"],
            "Thursday": ["Afternoon", "Morning", "Evening"],
            "Friday": ["Evening", "Afternoon", "Morning"],
            "Saturday": ["Afternoon", "Morning", "Evening"],
            "Sunday": ["Morning", "Evening", "Afternoon"],
        },
        "David": {
            "Monday": ["Evening", "Afternoon", "Morning"],
            "Tuesday": ["Morning", "Afternoon", "Evening"],
            "Wednesday": ["Evening", "Morning", "Afternoon"],
            "Thursday": ["Morning", "Evening", "Afternoon"],
            "Friday": ["Morning", "Afternoon", "Evening"],
            "Saturday": ["Evening", "Morning", "Afternoon"],
            "Sunday": ["Afternoon", "Morning", "Evening"],
        },
        "Emma": {
            "Monday": ["Morning", "Afternoon", "Evening"],
            "Tuesday": ["Evening", "Morning", "Afternoon"],
            "Wednesday": ["Morning", "Afternoon", "Evening"],
            "Thursday": ["Afternoon", "Evening", "Morning"],
            "Friday": ["Morning", "Afternoon", "Evening"],
            "Saturday": ["Afternoon", "Evening", "Morning"],
            "Sunday": ["Evening", "Afternoon", "Morning"],
        },
        "Frank": {
            "Monday": ["Afternoon", "Evening", "Morning"],
            "Tuesday": ["Morning", "Afternoon", "Evening"],
            "Wednesday": ["Afternoon", "Morning", "Evening"],
            "Thursday": ["Evening", "Morning", "Afternoon"],
            "Friday": ["Afternoon", "Evening", "Morning"],
            "Saturday": ["Morning", "Evening", "Afternoon"],
            "Sunday": ["Morning", "Afternoon", "Evening"],
        },
        "Grace": {
            "Monday": ["Evening", "Morning", "Afternoon"],
            "Tuesday": ["Afternoon", "Morning", "Evening"],
            "Wednesday": ["Morning", "Evening", "Afternoon"],
            "Thursday": ["Afternoon", "Morning", "Evening"],
            "Friday": ["Evening", "Morning", "Afternoon"],
            "Saturday": ["Morning", "Afternoon", "Evening"],
            "Sunday": ["Afternoon", "Evening", "Morning"],
        },
        "Henry": {
            "Monday": ["Morning", "Afternoon", "Evening"],
            "Tuesday": ["Morning", "Afternoon", "Evening"],
            "Wednesday": ["Evening", "Afternoon", "Morning"],
            "Thursday": ["Morning", "Afternoon", "Evening"],
            "Friday": ["Afternoon", "Morning", "Evening"],
            "Saturday": ["Evening", "Afternoon", "Morning"],
            "Sunday": ["Morning", "Evening", "Afternoon"],
        },
        "Ivy": {
            "Monday": ["Afternoon", "Morning", "Evening"],
            "Tuesday": ["Evening", "Afternoon", "Morning"],
            "Wednesday": ["Afternoon", "Morning", "Evening"],
            "Thursday": ["Morning", "Evening", "Afternoon"],
            "Friday": ["Evening", "Morning", "Afternoon"],
            "Saturday": ["Afternoon", "Morning", "Evening"],
            "Sunday": ["Evening", "Morning", "Afternoon"],
        },
        "Jack": {
            "Monday": ["Evening", "Afternoon", "Morning"],
            "Tuesday": ["Morning", "Evening", "Afternoon"],
            "Wednesday": ["Morning", "Afternoon", "Evening"],
            "Thursday": ["Evening", "Morning", "Afternoon"],
            "Friday": ["Morning", "Afternoon", "Evening"],
            "Saturday": ["Afternoon", "Evening", "Morning"],
            "Sunday": ["Morning", "Afternoon", "Evening"],
        },
    }

def get_manual_input():
    employees = {}
    number_of_employees = int(input("Enter number of employees: "))

    for _ in range(number_of_employees):
        name = input("Employee name: ").strip()
        employees[name] = {}
        print(f"Enter ranked shift preferences for {name}.")
        print("Use one line per day with three comma-separated values, for example: Morning,Evening,Afternoon")

        for day in DAYS:
            raw = input(f"{day}: ").strip()
            prefs = [part.strip().title() for part in raw.split(",") if part.strip()]
            validated = [shift for shift in prefs if shift in SHIFTS]

            for shift in SHIFTS:
                if shift not in validated:
                    validated.append(shift)

            employees[name][day] = validated[:3]

    return employees

def main():
    print("Employee Schedule Manager")
    print("1. Use sample data")
    print("2. Enter employees manually")
    choice = input("Choose an option (1 or 2): ").strip()

    employees = get_manual_input() if choice == "2" else get_sample_employees()
    scheduler = Scheduler(employees, seed=42)
    scheduler.build()
    scheduler.print_schedule()

if __name__ == "__main__":
    main()
