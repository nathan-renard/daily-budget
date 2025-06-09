from sys import argv
from datetime import date, datetime, timedelta

def main():
    print(f"Today's date is {date.today()}.")
    print(f"There's {get_remaining_month_days()} days left in this month.")
    print(f"Your budget is {argv[1]} dollars.")
    print(f"Your budget per day for the remaining days is {get_budget_divided_by_days(float(argv[1])):.2f} dollars.")

def get_remaining_month_days() -> int:
    """
    Calculate the number of days remaining in the current month.
    Returns:
        int: Number of days remaining in the current month.
    """
    today = date.today()
    next_month = today.replace(day=1) + timedelta(days=32)
    first_of_next_month = next_month.replace(day=1)
    remaining_days = (first_of_next_month - today).days
    return remaining_days

def get_budget_divided_by_days(budget: float) -> float:
    """
    Calculate the budget divided by the number of days remaining in the month.
    Args:
        budget (float): The total budget.
    Returns:
        float: Budget per day for the remaining days in the month.
    """
    remaining_days = get_remaining_month_days()
    return budget / remaining_days if remaining_days > 0 else 0.0

main()