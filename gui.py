import tkinter as tk
from tkinter import messagebox, simpledialog
from budget import (
    get_budget, set_budget, format_budget,
    get_remaining_month_days, get_budget_divided_by_days
)

class BudgetApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Renard's Simple Budget")
        self.geometry("300x300")
        self.resizable(False, False)
        self.budget = get_budget()
        self.create_widgets()
        self.update_labels()

    def create_widgets(self):
        self.budget_label = tk.Label(self, text="", font=("Tahoma", 12))
        self.budget_label.pack(pady=5)

        self.days_label = tk.Label(self, text="", font=("Tahoma", 12))
        self.days_label.pack(pady=5)

        self.per_day_label = tk.Label(self, text="", font=("Tahoma", 12))
        self.per_day_label.pack(pady=5)

        self.update_btn = tk.Button(self, text="Update Budget", command=self.update_budget)
        self.update_btn.pack(pady=50)

        self.quit_btn = tk.Button(self, text="Quit", command=self.destroy, bg="red", fg="white", activebackground="#cc0000", activeforeground="white")
        self.quit_btn.place(relx=1.0, rely=1.0, anchor="se", x=-10, y=-10)

    def update_labels(self):
        self.budget_label.config(text=f"Current Budget: {format_budget(self.budget)}")
        days = get_remaining_month_days()
        self.days_label.config(text=f"Days Left: {days}")
        per_day = get_budget_divided_by_days(self.budget)
        self.per_day_label.config(text=f"Per Day: {format_budget(per_day)}")

    def update_budget(self):
        try:
            new_budget = simpledialog.askfloat(
                "Update Budget", "Enter new budget:", minvalue=0
            )
            if new_budget is None:
                return
            if new_budget == 0:
                messagebox.showinfo("No Budget", "You have no more budget this month, time to stop spending!")
                self.destroy()
                return
            if new_budget > 1_000_000:
                messagebox.showinfo("Wow!", "Wow, that's a lot of money!\n...buddy, are you rich?\nYou can buy a mansion with that!\nOr maybe a yacht?\nYou definitely don't need to worry about your budget!\nJust enjoy your wealth!")
                self.destroy()
                return
            set_budget(new_budget)
            self.budget = new_budget
            self.update_labels()
        except ValueError:
            messagebox.showerror("Invalid Input", "Please enter a valid number for your budget.")

if __name__ == "__main__":
    app = BudgetApp()
    app.mainloop()