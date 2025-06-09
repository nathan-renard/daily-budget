from sys import argv
from datetime import date, datetime, timedelta

def main():
    """
    Main function to execute the budget calculation and display information.
    It checks if the budget is provided as a command line argument,
    calculates the remaining days in the month, and computes the budget per day.
    """

    # Check if the budget is provided as a command line argument
    if len(argv) != 2:
        print("Usage: python main.py <budget>")
        return
    try:
        budget = float(argv[1])
        if budget < 0:
            raise ValueError("Budget must be a non-negative number.")
        if budget == 0:
            print("Time to save money!")
        if budget > 1000000:
            print("...buddy, are you rich?")
            print("Wow, that's a lot of money!")
            print("You can buy a mansion with that!")
            print("Or maybe a yacht?")
            print("You definitely don't need to worry about your budget!")
            print("Just enjoy your wealth!")
            exit()
    except ValueError as e:
        print(f"Invalid budget: {e}")
        return
    
    # Display today's date, remaining days in the month, and budget information
    print(f"Today's date is {date.today()}.")
    print(f"There's {get_remaining_month_days()} days left in this month.")
    print(f"Your budget is {float(argv[1]):,.2f} dollars.")
    print(f"Your budget per day for the remaining days is {get_budget_divided_by_days(float(argv[1])):,.2f} dollars.")

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