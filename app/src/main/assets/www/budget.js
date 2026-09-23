// Pure budget math, kept separate from the UI so it is easy to reason about.
(function (global) {
  'use strict';

  function monthKey(date) {
    return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0');
  }

  function dayKey(date) {
    return monthKey(date) + '-' + String(date.getDate()).padStart(2, '0');
  }

  function daysInMonth(date) {
    return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
  }

  /**
   * Compute the budget snapshot for `now`.
   *
   * The daily allowance is fixed at the start of each day:
   *   (budget - spent before today) / days left including today
   * so spending today lowers "left today" but not today's allowance,
   * and any over/under-spend is spread across the remaining days from tomorrow.
   */
  function compute(budget, expenses, now) {
    const month = monthKey(now);
    const today = dayKey(now);
    const totalDays = daysInMonth(now);
    const daysLeft = totalDays - now.getDate() + 1;

    let spentBeforeToday = 0;
    let spentToday = 0;
    for (const e of expenses) {
      if (!e.day.startsWith(month)) continue;
      if (e.day === today) spentToday += e.amount;
      else if (e.day < today) spentBeforeToday += e.amount;
    }

    const spentMonth = spentBeforeToday + spentToday;
    const remainingAtDayStart = budget - spentBeforeToday;
    const dailyAllowance = Math.max(0, remainingAtDayStart) / daysLeft;

    // Projected allowance for tomorrow if nothing more is spent today.
    // On the last day of the month tomorrow starts a fresh month.
    const nextMonthDays = new Date(now.getFullYear(), now.getMonth() + 2, 0).getDate();
    const tomorrowAllowance = daysLeft > 1
      ? Math.max(0, budget - spentMonth) / (daysLeft - 1)
      : budget / nextMonthDays;

    return {
      month,
      today,
      totalDays,
      daysLeft,
      spentToday,
      spentMonth,
      dailyAllowance,
      tomorrowAllowance,
      todayLeft: dailyAllowance - spentToday,
      monthRemaining: budget - spentMonth,
    };
  }

  global.Budget = { compute, monthKey, dayKey, daysInMonth };
})(typeof window !== 'undefined' ? window : globalThis);
