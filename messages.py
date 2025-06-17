WIDTH = 80
PADDING = 4
VERSION = "1.2.0"

def __separator():
    """
    Generate a separator line for the console output.   
    """
    return "=" * WIDTH

def __center_text(text):
    """
    Center the given text within a border of "=" characters.
    Args:
        text (str): The text to center.
    Returns:
        str: The centered text with borders.
    """    
    return f"= {text.center(WIDTH - PADDING)} ="

def __left_text(text):
    """
    Right-align the given text within a border of "=" characters.
    Args:
        text (str): The text to right-align.
    Returns:
        str: The right-aligned text with borders.
    """
    return f"= {text.ljust(WIDTH - PADDING)} ="

def software_header():
    """
    Print the software header with a welcome message and version.
    """
    print(__separator())
    print(__center_text("Welcome to the Simple Budget Tracker Software!"))
    print(__center_text(f"Version {VERSION}"))
    print(__separator())

def software_execution(today, remaining_days, original_budget, budget_divided):
    software_header()
    print(__left_text(f"Today's date is {today}."))
    print(__left_text(f"There's {remaining_days} days left in this month."))
    print(__left_text(f"Your budget is {original_budget}"))
    print(__left_text(f"Your budget per day for the remaining days is {budget_divided}"))
    software_footer()

def software_footer():
    """
    Print the software footer with a thank you message.
    """
    print(__separator())
    print(__center_text("Thank you for using the Simple Budget Tracker Software!"))
    print(__separator())