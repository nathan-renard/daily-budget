# Daily Budget Manager

A minimal Python application to help you manage your monthly budget and calculate your daily spending limit.  
Now includes a simple graphical user interface (GUI) for ease of use!

---

## Version

- v2.0.0 - Client now has a GUI based

---

## Features

- Calculates how many days are left in the current month
- Divides your remaining budget by the number of days left
- Lets you update your budget at any time
- Simple, intuitive GUI (Tkinter-based)
- Handles invalid or missing input gracefully

---

## Requirements

- Python 3.6 or newer

---

## Usage

### Run the GUI

```bash
python gui.py
```

### (Optional) Run the CLI

If you still have `main.py` for CLI usage:

```bash
python main.py
```

---

## Example (GUI)

- Shows your current budget, days left in the month, and your daily spending limit.
- Lets you update your budget with a button click.

---

## Example (CLI Output)

```
No budget found. Please set your budget.
Current budget is 0.00 dollars.
Please set your updated budget: 4300
Today's date is 2025-06-14.
There's 17 days left in this month.
Your budget is 4,300.00 dollars.
Your budget per day for the remaining days is 252.94 dollars.
```

---

## Input Validation

- Budget must be a non-negative number
- Handles invalid or missing input with clear error messages

---

## Project Structure

```
budget.py      # Core logic for budget calculations
gui.py         # Tkinter GUI application
budget.json    # Stores your current budget (auto-created)
README.md      # This file
```

---

## License

MIT

