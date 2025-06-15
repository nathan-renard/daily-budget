import json
from datetime import date, timedelta

def main():
    """
    Main function to execute the budget management program.
    It prompts the user to set a budget, calculates the remaining days in the month,
    and displays the budget per day for the remaining days.
    """    
    budget = set_budget()
    
    print(f"Today's date is {date.today()}.")
    print(f"There's {get_remaining_month_days()} days left in this month.")
    print(f"Your budget is {float(budget):,.2f} dollars.")
    print(f"Your budget per day for the remaining days is {get_budget_divided_by_days(float(budget)):,.2f} dollars.")

def get_remaining_month_days() -> int:
    """;
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

def get_budget() -> float:
    """
    Retrieve the budget from a JSON file.
    If the file does not exist or is empty, prompt the user to set a budget.
    Returns:
        float: The budget value.
    """
    try:
        with open('budget.json', 'r') as file:
            data = json.load(file)
            return data.get('budget', 0.0)
    except (FileNotFoundError, json.JSONDecodeError):
        print("No budget found. Please set your budget.")
        return 0.0

def set_budget() -> float:
    """
    Prompt the user to set a budget and validate the input.
    Returns:
        float: The updated budget value.
    """
    current_budget = get_budget()
    print(f"Current budget is {current_budget:,.2f} dollars.")
    while True:
        try:
            budget = float(input("Please set your udpated budget: "))
            break
        except ValueError:
            print("Invalid input. Please enter a valid number for your budget.")
    try:        
        if budget < 0:
            raise ValueError("Budget must be a non-negative number.")
        if budget == 0:
            print("Time to save money!")
        if budget > 1000000:
            print_rich_message()            
    except ValueError as e:
        print(f"Invalid budget: {e}")
        return

    # Save the budget to a JSON file
    with open('budget.json', 'w') as file:
        json.dump({'budget': budget}, file)
    return budget

def print_rich_message():
    """
    Print a message for users with a budget greater than 1,000,000.
    """
    print("Wow, that's a lot of money!")
    print("...buddy, are you rich?")    
    print("You can buy a mansion with that!")
    print("Or maybe a yacht?")
    print("You definitely don't need to worry about your budget!")
    print("Just enjoy your wealth!")
    exit()

main()