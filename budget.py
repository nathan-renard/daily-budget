import json
from datetime import date, timedelta

def get_remaining_month_days() -> int:
    today = date.today()
    next_month = today.replace(day=1) + timedelta(days=32)
    first_of_next_month = next_month.replace(day=1)
    remaining_days = (first_of_next_month - today).days
    return remaining_days

def get_budget_divided_by_days(budget: float) -> float:
    remaining_days = get_remaining_month_days()
    return budget / remaining_days if remaining_days > 0 else 0.0

def get_budget() -> float:
    try:
        with open('budget.json', 'r') as file:
            data = json.load(file)
            return data.get('budget', 0.0)
    except (FileNotFoundError, json.JSONDecodeError):
        return 0.0

def set_budget(budget: float) -> float:
    if budget < 0:
        raise ValueError("Budget must be a non-negative number.")
    with open('budget.json', 'w') as file:
        json.dump({'budget': budget}, file)
    return budget

def format_budget(value: float) -> str:
    return f"${value:,.2f}"