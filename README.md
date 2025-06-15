# Monthly Budget Calculator

A minimal Python CLI tool to calculate your daily spending limit based on your remaining monthly budget.

## Version

**1.0.3**  
- User is now allowed to update the budget, or not. Freedom.

## Features

- Calculates how many days are left in the current month
- Divides a user-provided budget by the number of remaining days
- Handles invalid or missing input

## Requirements

- Python 3.6 or newer

## Usage

```bash
python main.py
```

## Example output 

```
No budget found. Please set your budget.
Current budget is 0.00 dollars.
Please set your udpated budget: 4300
Today's date is 2025-06-14.
There's 17 days left in this month.
Your budget is 4,300.00 dollars.
Your budget per day for the remaining days is 252.94 dollars.
```

## Input Validation

- Requires exactly one argument: a non-negative number
- Displays usage instructions or error messages for invalid input

