(function () {
  'use strict';

  const STORAGE_KEY = 'dailyBudget.v1';
  const $ = (id) => document.getElementById(id);

  // ---- State -------------------------------------------------------------
  function load() {
    try {
      const data = JSON.parse(localStorage.getItem(STORAGE_KEY));
      if (data && typeof data === 'object') {
        return {
          budget: data.budget || 0,
          currency: data.currency || '$',
          expenses: data.expenses || [],
          since: data.since || null,
          theme: data.theme || 'dark',
          reminder: data.reminder || { enabled: false, time: '08:00' },
        };
      }
    } catch (_) { /* fall through to defaults */ }
    return { budget: 0, currency: '$', expenses: [], since: null, theme: 'dark', reminder: { enabled: false, time: '08:00' } };
  }

  function save() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    syncNative();
  }

  // The Android wrapper keeps its own copy of this month's numbers so the
  // morning notification can be computed without opening the app.
  function syncNative() {
    if (!(window.Android && window.Android.syncData)) return;
    const month = Budget.monthKey(new Date());
    window.Android.syncData(JSON.stringify({
      budget: state.budget,
      currency: state.currency,
      expenses: state.expenses
        .filter((e) => e.day.startsWith(month))
        .map((e) => ({ day: e.day, amount: e.amount })),
    }));
  }

  // Keep a year of history so storage never grows unbounded.
  function prune(now) {
    const oldest = Budget.monthKey(new Date(now.getFullYear(), now.getMonth() - 11, 1));
    state.expenses = state.expenses.filter((e) => e.day.slice(0, 7) >= oldest);
  }

  const state = load();
  let currentTab = 'budget';
  let selectedDay = null;

  // Budgets set before `since` existed: start tracking from the first expense (or today).
  if (state.budget && !state.since) {
    const days = state.expenses.map((e) => e.day).sort();
    state.since = days[0] || Budget.dayKey(new Date());
  }

  // ---- Formatting --------------------------------------------------------
  function money(value) {
    const abs = Math.round(Math.abs(value) * 100) / 100;
    const sign = value < 0 && abs > 0 ? '-' : '';
    return sign + state.currency + abs.toLocaleString(undefined, {
      minimumFractionDigits: Number.isInteger(abs) ? 0 : 2,
      maximumFractionDigits: 2,
    });
  }

  function monthExpenses(month) {
    return state.expenses.filter((e) => e.day.startsWith(month));
  }

  // ---- Budget tab --------------------------------------------------------
  function renderBudget(now) {
    const s = Budget.compute(state.budget, state.expenses, now);

    // The headline changes wording depending on where the user stands today.
    let label, amount, sub;
    if (!state.budget) {
      label = 'Set a monthly budget to start';
      amount = 0;
      sub = 'Tap the gear icon above';
    } else if (s.monthRemaining < 0) {
      label = 'Over this month\'s budget by';
      amount = -s.monthRemaining;
      sub = 'No allowance left until next month';
    } else if (s.todayLeft < -0.005) {
      label = 'Over today\'s budget by';
      amount = -s.todayLeft;
      sub = money(s.spentToday) + ' spent of ' + money(s.dailyAllowance);
    } else if (s.todayLeft < 0.005) {
      label = 'Today\'s budget is used up';
      amount = 0;
      sub = money(s.spentToday) + ' spent of ' + money(s.dailyAllowance);
    } else {
      label = 'You can spend today';
      amount = s.todayLeft;
      sub = s.spentToday
        ? money(s.spentToday) + ' spent of ' + money(s.dailyAllowance)
        : 'Daily allowance ' + money(s.dailyAllowance);
    }
    const over = s.monthRemaining < 0 || s.todayLeft < -0.005;
    $('heroLabel').textContent = label;
    $('todayLeft').textContent = money(amount);
    $('todayLeft').classList.toggle('neg', over);
    $('dailyAllowance').textContent = sub;

    // Tomorrow's projected allowance, compared to today's. Hidden once the
    // month's budget is gone (the "no allowance left" line already says it).
    const projection = $('projection');
    projection.hidden = !state.budget || s.monthRemaining < 0;
    if (projection.hidden) {
      // nothing to project
    } else if (s.monthRemaining <= 0.005 && s.daysLeft > 1) {
      projection.className = 'projection down';
      projection.textContent = 'Allowance ended for this month';
    } else {
      const diff = s.tomorrowAllowance - s.dailyAllowance;
      const dir = s.daysLeft === 1 ? 'reset' : diff > 0.005 ? 'up' : diff < -0.005 ? 'down' : 'same';
      const arrow = { up: '↑ ', down: '↓ ', same: '', reset: '' }[dir];
      projection.className = 'projection ' + dir;
      projection.textContent = (s.daysLeft === 1 ? 'Tomorrow (new month): ' : 'Tomorrow: ') +
        arrow + money(s.tomorrowAllowance) + ' / day';
    }

    $('monthRemaining').textContent = money(s.monthRemaining);
    $('monthRemaining').classList.toggle('neg', s.monthRemaining < 0);
    $('daysLeft').textContent = s.daysLeft;
    $('daysLeftLabel').textContent = s.daysLeft === 1 ? 'day left' : 'days left';

    const pct = state.budget > 0 ? Math.min(100, (s.spentMonth / state.budget) * 100) : 0;
    $('barFill').style.width = pct + '%';
    $('barFill').classList.toggle('over', s.monthRemaining < 0);
    $('monthSummary').textContent = money(s.spentMonth) + ' of ' + money(state.budget) + ' spent';
  }

  // ---- Expenses tab ------------------------------------------------------
  function renderExpenses(now) {
    const items = monthExpenses(Budget.monthKey(now)).sort((a, b) => b.ts - a.ts);
    const total = items.reduce((sum, e) => sum + e.amount, 0);
    $('expenseTotal').textContent = items.length ? money(total) + ' total' : '';
    $('emptyList').hidden = items.length > 0;

    const container = $('expenseGroups');
    container.innerHTML = '';
    let group = null;
    for (const e of items) {
      if (!group || group.day !== e.day) {
        const d = new Date(e.ts);
        const wrap = document.createElement('div');
        wrap.className = 'group';
        wrap.innerHTML = '<div class="group-head"><span></span><span></span></div><ul class="list"></ul>';
        wrap.querySelector('.group-head span').textContent =
          e.day === Budget.dayKey(now) ? 'Today'
            : d.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'short' });
        container.appendChild(wrap);
        group = { day: e.day, list: wrap.querySelector('.list'), totalEl: wrap.querySelectorAll('.group-head span')[1], total: 0 };
      }
      group.total += e.amount;
      group.totalEl.textContent = money(group.total);

      const li = document.createElement('li');
      li.innerHTML =
        '<div class="item-main"><span class="item-note"></span><span class="item-time"></span></div>' +
        '<span class="item-amount"></span>' +
        '<button class="del" aria-label="Delete">×</button>';
      li.querySelector('.item-note').textContent = e.note || 'Expense';
      li.querySelector('.item-time').textContent =
        new Date(e.ts).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
      li.querySelector('.item-amount').textContent = money(e.amount);
      li.querySelector('.del').addEventListener('click', () => {
        if (!confirm('Delete this expense?')) return;
        state.expenses = state.expenses.filter((x) => x.id !== e.id);
        save();
        render();
      });
      group.list.appendChild(li);
    }
  }

  // ---- Calendar tab ------------------------------------------------------
  // A day is "ok" if what was spent that day stayed within that day's allowance.
  function dayStatus(date, now) {
    const key = Budget.dayKey(date);
    const s = Budget.compute(state.budget, state.expenses, date);
    const over = s.spentToday > s.dailyAllowance + 0.005;
    const isToday = key === Budget.dayKey(now);
    const tracked = state.budget > 0 && state.since && key >= state.since;
    let status = 'future';
    if (tracked && isToday) status = over ? 'over' : 'pending';
    else if (tracked && key < Budget.dayKey(now)) status = over ? 'over' : 'ok';
    else if (key < Budget.dayKey(now)) status = 'untracked';
    return { key, status, isToday, spent: s.spentToday, allowance: s.dailyAllowance };
  }

  function renderCalendar(now) {
    $('calendarTitle').textContent = now.toLocaleDateString(undefined, { month: 'long' });

    const weekdays = $('calWeekdays');
    if (!weekdays.childElementCount) {
      // Monday-first week labels in the user's locale (Jan 1 2024 was a Monday).
      for (let i = 0; i < 7; i++) {
        const span = document.createElement('span');
        span.textContent = new Date(2024, 0, 1 + i).toLocaleDateString(undefined, { weekday: 'narrow' });
        weekdays.appendChild(span);
      }
    }

    const grid = $('calGrid');
    grid.innerHTML = '';
    const first = new Date(now.getFullYear(), now.getMonth(), 1);
    const offset = (first.getDay() + 6) % 7; // Monday = 0
    for (let i = 0; i < offset; i++) grid.appendChild(document.createElement('div'));

    let ok = 0;
    let judged = 0;
    const total = Budget.daysInMonth(now);
    for (let d = 1; d <= total; d++) {
      const date = new Date(now.getFullYear(), now.getMonth(), d);
      const info = dayStatus(date, now);
      if (!info.isToday && (info.status === 'ok' || info.status === 'over')) {
        judged++;
        if (info.status === 'ok') ok++;
      }

      const btn = document.createElement('button');
      btn.className = 'cal-day ' + info.status +
        (info.isToday ? ' today' : '') +
        (info.status !== 'future' ? ' past' : '') +
        (selectedDay === info.key ? ' selected' : '');
      btn.innerHTML = '<span></span>';
      btn.firstChild.textContent = d;
      btn.addEventListener('click', () => {
        selectedDay = selectedDay === info.key ? null : info.key;
        renderCalendar(new Date());
      });
      grid.appendChild(btn);

      if (selectedDay === info.key) {
        const label = date.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' });
        $('dayDetail').textContent = info.status === 'future' || info.status === 'untracked'
          ? label + ' · no data'
          : label + ' · spent ' + money(info.spent) + ' of ' + money(info.allowance);
      }
    }
    if (!selectedDay) $('dayDetail').textContent = 'Tap a day for details';
    $('calendarScore').textContent = judged ? ok + ' of ' + judged + ' days on budget' : '';
  }

  // ---- Rendering ---------------------------------------------------------
  function render() {
    const now = new Date();
    $('monthLabel').textContent = now.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
    if (currentTab === 'budget') renderBudget(now);
    if (currentTab === 'expenses') renderExpenses(now);
    if (currentTab === 'calendar') renderCalendar(now);
  }

  // Slide the notch/bubble under the active tab.
  function moveIndicator(animate) {
    const indicator = $('indicator');
    const active = document.querySelector('.tab.active');
    indicator.classList.toggle('no-anim', !animate);
    indicator.style.setProperty('--x', (active.offsetLeft + active.offsetWidth / 2) + 'px');
    if (animate) {
      indicator.classList.remove('moving');
      void indicator.offsetWidth; // restart the squish animation
      indicator.classList.add('moving');
    }
  }

  function showTab(tab) {
    if (tab === currentTab) return;
    currentTab = tab;
    selectedDay = null;
    for (const btn of document.querySelectorAll('.tab')) {
      btn.classList.toggle('active', btn.dataset.tab === tab);
    }
    for (const name of ['budget', 'expenses', 'calendar']) {
      $('view-' + name).hidden = name !== tab;
    }
    moveIndicator(true);
    window.scrollTo(0, 0);
    render();
  }

  window.addEventListener('resize', () => moveIndicator(false));

  for (const btn of document.querySelectorAll('.tab')) {
    btn.addEventListener('click', () => showTab(btn.dataset.tab));
  }

  // ---- Sheets ------------------------------------------------------------
  function openSheet(id, focusId) {
    $(id).hidden = false;
    if (focusId) $(focusId).focus();
  }

  // Closes open dialogs. The first-run budget prompt cannot be dismissed.
  function closeSheets() {
    let closed = false;
    for (const sheet of document.querySelectorAll('.sheet-backdrop')) {
      if (!sheet.hidden && !sheet.dataset.locked) {
        sheet.hidden = true;
        closed = true;
      }
    }
    document.activeElement && document.activeElement.blur();
    return closed;
  }

  for (const btn of document.querySelectorAll('[data-close]')) {
    btn.addEventListener('click', closeSheets);
  }
  for (const backdrop of document.querySelectorAll('.sheet-backdrop')) {
    backdrop.addEventListener('click', (ev) => { if (ev.target === backdrop) closeSheets(); });
  }

  // ---- Theme -------------------------------------------------------------
  const lightQuery = window.matchMedia('(prefers-color-scheme: light)');
  const THEME_NAMES = { dark: 'Dark', light: 'Light', system: 'Same as phone' };

  function applyTheme() {
    const resolved = state.theme === 'system' ? (lightQuery.matches ? 'light' : 'dark') : state.theme;
    document.documentElement.dataset.theme = resolved;
    const css = getComputedStyle(document.documentElement);
    const bg = css.getPropertyValue('--bg').trim();
    const surface = css.getPropertyValue('--surface').trim();
    document.querySelector('meta[name="theme-color"]').content = bg;
    // Android wrapper: recolour the status and navigation bars to match.
    // While Settings is open it covers the tab bar, so both bars use the page colour.
    if (window.Android && window.Android.setSystemBars) {
      window.Android.setSystemBars(bg, $('settingsPage').hidden ? surface : bg, resolved === 'light');
    }
    for (const btn of $('themePicker').children) {
      btn.classList.toggle('active', btn.dataset.theme === state.theme);
    }
    $('valTheme').textContent = THEME_NAMES[state.theme];
  }

  lightQuery.addEventListener('change', () => { if (state.theme === 'system') applyTheme(); });

  $('rowTheme').addEventListener('click', () => openSheet('themeSheet'));
  for (const btn of $('themePicker').children) {
    btn.addEventListener('click', () => {
      state.theme = btn.dataset.theme;
      save();
      applyTheme();
      setTimeout(closeSheets, 150); // let the radio animate before closing
    });
  }

  // ---- Morning reminder --------------------------------------------------
  const hasBridge = !!(window.Android && window.Android.setReminder);

  function applyReminder(note) {
    const r = state.reminder;
    $('reminderToggle').checked = r.enabled;
    $('reminderTime').value = r.time;
    $('rowTime').classList.toggle('disabled', !r.enabled);
    $('reminderTest').hidden = !hasBridge || !r.enabled;
    $('reminderNote').classList.remove('neg-text');
    $('reminderNote').textContent = note ||
      (hasBridge ? '' : 'Notifications are sent by the Android app.');
    if (hasBridge) {
      const [h, m] = r.time.split(':').map(Number);
      window.Android.setReminder(r.enabled, h, m);
    }
  }

  $('reminderToggle').addEventListener('change', () => {
    state.reminder.enabled = $('reminderToggle').checked;
    save();
    applyReminder();
  });
  $('reminderTime').addEventListener('change', () => {
    if (!$('reminderTime').value) return;
    state.reminder.time = $('reminderTime').value;
    save();
    applyReminder();
  });
  // Tapping anywhere on the row opens the time picker, not just the value.
  $('rowTime').addEventListener('click', (ev) => {
    if (ev.target !== $('reminderTime') && $('reminderTime').showPicker) {
      ev.preventDefault();
      try { $('reminderTime').showPicker(); } catch (_) { $('reminderTime').focus(); }
    }
  });
  $('reminderTest').addEventListener('click', () => window.Android.testReminder());

  // Called by the Android wrapper after the notification permission prompt.
  window.onReminderPermission = function (granted) {
    if (granted) return;
    state.reminder.enabled = false;
    save();
    applyReminder('Notifications are blocked. Allow them for Daily Budget in Android settings.');
    $('reminderNote').classList.add('neg-text');
  };

  // ---- Settings page -----------------------------------------------------
  function renderSettings() {
    $('valBudget').textContent = state.budget ? money(state.budget) : 'Not set';
    $('valCurrency').textContent = state.currency;
  }

  function openSettings() {
    renderSettings();
    $('settingsPage').classList.remove('closing');
    $('settingsPage').hidden = false;
    $('settingsPage').scrollTop = 0;
    applyTheme();
  }

  function closeSettings() {
    const page = $('settingsPage');
    if (page.hidden) return false;
    page.classList.add('closing');
    page.addEventListener('animationend', () => {
      page.hidden = true;
      page.classList.remove('closing');
      applyTheme();
    }, { once: true });
    return true;
  }

  $('settingsBtn').addEventListener('click', openSettings);
  $('settingsBack').addEventListener('click', closeSettings);

  // One small dialog edits either the budget or the currency symbol.
  let editing = null;

  function openEdit(field, locked) {
    editing = field;
    const input = $('editInput');
    const sheet = $('editSheet');
    if (field === 'budget') {
      $('editTitle').textContent = locked ? 'Set your monthly budget' : 'Monthly budget';
      $('editHint').textContent = 'How much are you willing to spend each month? This amount repeats every month.';
      input.type = 'number';
      input.inputMode = 'decimal';
      input.step = '0.01';
      input.min = '1';
      input.placeholder = 'e.g. 1500';
      input.value = state.budget || '';
    } else {
      $('editTitle').textContent = 'Currency symbol';
      $('editHint').textContent = 'Shown in front of every amount, e.g. $, €, £, R$.';
      input.type = 'text';
      input.inputMode = 'text';
      input.removeAttribute('step');
      input.removeAttribute('min');
      input.maxLength = 4;
      input.placeholder = '$';
      input.value = state.currency;
    }
    $('editCancel').hidden = !!locked;
    if (locked) sheet.dataset.locked = '1'; else delete sheet.dataset.locked;
    openSheet('editSheet', 'editInput');
  }

  $('rowBudget').addEventListener('click', () => openEdit('budget'));
  $('rowCurrency').addEventListener('click', () => openEdit('currency'));

  $('editForm').addEventListener('submit', (ev) => {
    ev.preventDefault();
    const raw = $('editInput').value.trim();
    if (editing === 'budget') {
      const budget = parseFloat(raw);
      if (!(budget > 0)) return;
      state.budget = Math.round(budget * 100) / 100;
      if (!state.since) state.since = Budget.dayKey(new Date());
    } else {
      state.currency = raw || '$';
    }
    save();
    delete $('editSheet').dataset.locked;
    closeSheets();
    renderSettings();
    render();
  });

  $('resetBtn').addEventListener('click', () => {
    if (!confirm('Delete your budget and all expenses? This cannot be undone.')) return;
    state.budget = 0;
    state.expenses = [];
    state.since = null;
    save();
    closeSettings();
    render();
    openEdit('budget', true);
  });

  // Add expense
  $('addBtn').addEventListener('click', () => openSheet('addSheet', 'amountInput'));

  $('addForm').addEventListener('submit', (ev) => {
    ev.preventDefault();
    const amount = parseFloat($('amountInput').value);
    if (!(amount > 0)) return;
    const now = new Date();
    state.expenses.push({
      id: now.getTime().toString(36) + Math.random().toString(36).slice(2, 6),
      ts: now.getTime(),
      day: Budget.dayKey(now),
      amount: Math.round(amount * 100) / 100,
      note: $('noteInput').value.trim(),
    });
    save();
    $('amountInput').value = '';
    $('noteInput').value = '';
    closeSheets();
    render();
  });

  // Called by the Android wrapper on the hardware back button.
  // Returns true if the app handled it, false to let Android close the app.
  window.handleBack = function () {
    if (closeSheets()) return true;
    if (closeSettings()) return true;
    if (currentTab !== 'budget') { showTab('budget'); return true; }
    return false;
  };

  // ---- Boot --------------------------------------------------------------
  prune(new Date());
  save();
  applyTheme();
  applyReminder();
  moveIndicator(false);
  render();
  if (!state.budget) openEdit('budget', true); // first run

  // Re-render when the app comes back to the foreground or the day rolls over.
  document.addEventListener('visibilitychange', () => { if (!document.hidden) render(); });
  setInterval(render, 60 * 1000);
})();
