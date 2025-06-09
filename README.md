# Monthly Budget Calculator

A minimal Python CLI tool to calculate your daily spending limit based on your remaining monthly budget.

## Features

- Calculates how many days are left in the current month
- Divides a user-provided budget by the number of remaining days
- Handles invalid or missing input

## Requirements

- Python 3.6 or newer

## Usage

```bash
python main.py <budget>
```

## Example

```
python main.py 300
```

## Example output

Today's date is 2025-06-08.
There's 22 days left in this month.
Your budget is 300 dollars.
Your budget per day for the remaining days is 13.64 dollars.

## Input Validation

- Requires exactly one argument: a non-negative number
- Displays usage instructions or error messages for invalid input

