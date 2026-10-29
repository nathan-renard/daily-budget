// Every piece of text the web UI shows, in English (en), Brazilian Portuguese (pt) and Spanish (es).
// The widget and the notification have their own in res/values*/strings.xml.
//
// {name} is filled in by the code. An entry with one/other forms picks one by {count}, using the
// language's plural rules. A missing translation falls back to English.
(function (global) {
  'use strict';

  const STRINGS = {
    // ---- Shared ----------------------------------------------------------
    budget: { en: 'Budget', pt: 'Orçamento', es: 'Presupuesto' },
    expenses: { en: 'Expenses', pt: 'Gastos', es: 'Gastos' },
    calendar: { en: 'Calendar', pt: 'Calendário', es: 'Calendario' },
    settings: { en: 'Settings', pt: 'Configurações', es: 'Ajustes' },
    back: { en: 'Back', pt: 'Voltar', es: 'Atrás' },
    today: { en: 'Today', pt: 'Hoje', es: 'Hoy' },
    cancel: { en: 'Cancel', pt: 'Cancelar', es: 'Cancelar' },
    save: { en: 'Save', pt: 'Salvar', es: 'Guardar' },
    add: { en: 'Add', pt: 'Adicionar', es: 'Agregar' },
    delete: { en: 'Delete', pt: 'Excluir', es: 'Eliminar' },
    addExpense: { en: 'Add expense', pt: 'Adicionar gasto', es: 'Agregar gasto' },

    // ---- Budget tab: the headline, the line under it, and the stats -------
    canSpendToday: {
      en: 'You can spend today',
      pt: 'Você pode gastar hoje',
      es: 'Puedes gastar hoy',
    },
    setBudgetToStart: {
      en: 'Set a monthly budget to start',
      pt: 'Defina um orçamento mensal para começar',
      es: 'Define un presupuesto mensual para empezar',
    },
    tapGear: {
      en: 'Tap the gear icon above',
      pt: 'Toque na engrenagem acima',
      es: 'Toca el engranaje de arriba',
    },
    // Followed by the amount on the next line.
    overMonthBy: {
      en: 'Over this month\'s budget by',
      pt: 'Acima do orçamento do mês em',
      es: 'Excediste el presupuesto del mes en',
    },
    overTodayBy: {
      en: 'Over today\'s budget by',
      pt: 'Acima do orçamento de hoje em',
      es: 'Excediste el presupuesto de hoy en',
    },
    usedUpToday: {
      en: 'Today\'s budget is used up',
      pt: 'O orçamento de hoje acabou',
      es: 'Se acabó el presupuesto de hoy',
    },
    noAllowanceLeft: {
      en: 'No allowance left until next month',
      pt: 'Sem limite diário até o próximo mês',
      es: 'Sin límite diario hasta el próximo mes',
    },
    spentOfAllowance: {
      en: '{spent} spent of {allowance}',
      pt: '{spent} gastos de {allowance}',
      es: '{spent} gastados de {allowance}',
    },
    dailyAllowance: {
      en: 'Daily allowance {amount}',
      pt: 'Limite diário {amount}',
      es: 'Límite diario {amount}',
    },
    allowanceEnded: {
      en: 'Allowance ended for this month',
      pt: 'Não sobrou limite para este mês',
      es: 'No queda límite para este mes',
    },
    // Tomorrow's allowance; {amount} may start with an arrow (↑ or ↓).
    tomorrowPerDay: {
      en: 'Tomorrow: {amount} / day',
      pt: 'Amanhã: {amount} / dia',
      es: 'Mañana: {amount} / día',
    },
    tomorrowNewMonth: {
      en: 'Tomorrow (new month): {amount} / day',
      pt: 'Amanhã (novo mês): {amount} / dia',
      es: 'Mañana (nuevo mes): {amount} / día',
    },
    // Under the amount and the number of days.
    leftThisMonth: { en: 'left this month', pt: 'restam no mês', es: 'quedan este mes' },
    daysLeft: {
      en: { one: 'day left', other: 'days left' },
      pt: { one: 'dia restante', other: 'dias restantes' },
      es: { one: 'día restante', other: 'días restantes' },
    },
    spentOfBudget: {
      en: '{spent} of {budget} spent',
      pt: '{spent} de {budget} gastos',
      es: '{spent} de {budget} gastados',
    },

    // ---- Expenses tab ----------------------------------------------------
    expensesTotal: { en: '{amount} total', pt: '{amount} no total', es: '{amount} en total' },
    noExpenses: {
      en: 'No expenses this month.',
      pt: 'Nenhum gasto neste mês.',
      es: 'Sin gastos este mes.',
    },
    // Shown for an expense saved without a note.
    expense: { en: 'Expense', pt: 'Gasto', es: 'Gasto' },
    deleteExpense: {
      en: 'Delete this expense?',
      pt: 'Excluir este gasto?',
      es: '¿Eliminar este gasto?',
    },

    // ---- Calendar tab ----------------------------------------------------
    withinBudget: { en: 'Within budget', pt: 'Dentro do orçamento', es: 'Dentro del presupuesto' },
    overBudget: { en: 'Over budget', pt: 'Fora do orçamento', es: 'Fuera del presupuesto' },
    noBudgetLeft: { en: 'No budget left', pt: 'Sem orçamento restante', es: 'Sin presupuesto restante' },
    daysOnBudget: {
      en: { one: '{ok} of {count} day on budget', other: '{ok} of {count} days on budget' },
      pt: { one: '{ok} de {count} dia no orçamento', other: '{ok} de {count} dias no orçamento' },
      es: { one: '{ok} de {count} día dentro del presupuesto', other: '{ok} de {count} días dentro del presupuesto' },
    },
    tapDay: {
      en: 'Tap a day for details',
      pt: 'Toque em um dia para ver os detalhes',
      es: 'Toca un día para ver los detalles',
    },
    // {day} is a short date, e.g. "Fri, 18 Sep".
    dayNoData: { en: '{day} · no data', pt: '{day} · sem dados', es: '{day} · sin datos' },
    dayNoBudget: {
      en: '{day} · no budget left for the month',
      pt: '{day} · sem orçamento restante no mês',
      es: '{day} · sin presupuesto restante en el mes',
    },
    daySpent: {
      en: '{day} · spent {spent} of {allowance}',
      pt: '{day} · gastou {spent} de {allowance}',
      es: '{day} · gastaste {spent} de {allowance}',
    },

    // ---- Add expense dialog and money fields ------------------------------
    amount: { en: 'Amount', pt: 'Valor', es: 'Monto' },
    whatFor: { en: 'What for? (optional)', pt: 'Com o quê? (opcional)', es: '¿En qué? (opcional)' },
    enterAmount: { en: 'Enter an amount.', pt: 'Digite um valor.', es: 'Escribe un monto.' },
    // {example} is 12.50 or 12,50, with the phone's decimal mark.
    enterNumber: {
      en: 'Enter a number, like {example}.',
      pt: 'Digite um número, como {example}.',
      es: 'Escribe un número, como {example}.',
    },
    notNegative: {
      en: 'The amount can\'t be negative.',
      pt: 'O valor não pode ser negativo.',
      es: 'El monto no puede ser negativo.',
    },
    twoDecimals: {
      en: 'Use at most two decimal places.',
      pt: 'Use no máximo duas casas decimais.',
      es: 'Usa como máximo dos decimales.',
    },
    enterAtLeast: {
      en: 'Enter at least {amount}.',
      pt: 'Digite pelo menos {amount}.',
      es: 'Escribe al menos {amount}.',
    },

    // ---- Settings page ---------------------------------------------------
    monthlyBudget: { en: 'Monthly budget', pt: 'Orçamento mensal', es: 'Presupuesto mensual' },
    currencySymbol: { en: 'Currency symbol', pt: 'Símbolo da moeda', es: 'Símbolo de moneda' },
    appearance: { en: 'Appearance', pt: 'Aparência', es: 'Apariencia' },
    theme: { en: 'Theme', pt: 'Tema', es: 'Tema' },
    widgetColors: { en: 'Widget colours', pt: 'Cores do widget', es: 'Colores del widget' },
    homeScreenWidget: {
      en: 'Home-screen widget',
      pt: 'Widget na tela inicial',
      es: 'Widget en la pantalla de inicio',
    },
    notifications: { en: 'Notifications', pt: 'Notificações', es: 'Notificaciones' },
    morningReminder: { en: 'Morning reminder', pt: 'Lembrete matinal', es: 'Recordatorio matutino' },
    reminderTime: { en: 'Reminder time', pt: 'Horário do lembrete', es: 'Hora del recordatorio' },
    sendTest: {
      en: 'Send test notification',
      pt: 'Enviar notificação de teste',
      es: 'Enviar notificación de prueba',
    },
    about: { en: 'About', pt: 'Sobre', es: 'Acerca de' },
    privacyPolicy: { en: 'Privacy policy', pt: 'Política de privacidade', es: 'Política de privacidad' },
    resetAllData: { en: 'Reset all data', pt: 'Apagar todos os dados', es: 'Borrar todos los datos' },
    resetConfirm: {
      en: 'Delete your budget and all expenses? This cannot be undone.',
      pt: 'Excluir seu orçamento e todos os gastos? Não é possível desfazer.',
      es: '¿Eliminar tu presupuesto y todos los gastos? No se puede deshacer.',
    },
    notSet: { en: 'Not set', pt: 'Não definido', es: 'Sin definir' },
    // Title of the first-run budget prompt.
    setYourBudget: {
      en: 'Set your monthly budget',
      pt: 'Defina seu orçamento mensal',
      es: 'Define tu presupuesto mensual',
    },
    budgetHint: {
      en: 'How much are you willing to spend each month? This amount repeats every month.',
      pt: 'Quanto você pretende gastar por mês? Esse valor se repete todo mês.',
      es: '¿Cuánto quieres gastar cada mes? Este monto se repite todos los meses.',
    },
    budgetExample: { en: 'e.g. 1500', pt: 'ex.: 1500', es: 'p. ej., 1500' },
    currencyHint: {
      en: 'Shown in front of every amount, e.g. $, €, £, R$.',
      pt: 'Aparece antes de cada valor, ex.: R$, $, €, £.',
      es: 'Aparece delante de cada monto, p. ej., $, €, £, R$.',
    },

    // Theme and widget colour choices.
    themeDark: { en: 'Dark', pt: 'Escuro', es: 'Oscuro' },
    themeAmoled: { en: 'AMOLED black', pt: 'Preto AMOLED', es: 'Negro AMOLED' },
    themeLight: { en: 'Light', pt: 'Claro', es: 'Claro' },
    themeSystem: { en: 'Same as phone', pt: 'Igual ao celular', es: 'Igual que el teléfono' },
    colorsApp: { en: 'Same as the app', pt: 'Como no app', es: 'Como en la app' },
    colorsWallpaper: {
      en: 'From your wallpaper',
      pt: 'Do seu papel de parede',
      es: 'De tu fondo de pantalla',
    },
    // The same choice, shortened for the settings row.
    colorsWallpaperShort: { en: 'From wallpaper', pt: 'Do papel de parede', es: 'Del fondo de pantalla' },

    // Morning reminder notes.
    notificationsInApp: {
      en: 'Notifications are sent by the Android app.',
      pt: 'As notificações são enviadas pelo app para Android.',
      es: 'Las notificaciones las envía la app de Android.',
    },
    notificationsBlocked: {
      en: 'Notifications are blocked. Allow them for Daily Budget in Android settings.',
      pt: 'As notificações estão bloqueadas. Permita-as para o Daily Budget nas configurações do Android.',
      es: 'Las notificaciones están bloqueadas. Permítelas para Daily Budget en los ajustes de Android.',
    },

    // ---- Pro dialog ------------------------------------------------------
    payWhatYouWant: { en: 'Pay what you want', pt: 'Pague quanto quiser', es: 'Paga lo que quieras' },
    proUnlocked: { en: 'Unlocked', pt: 'Desbloqueado', es: 'Desbloqueado' },
    thankYou: { en: 'Thank you!', pt: 'Obrigado!', es: '¡Gracias!' },
    proPitch: {
      en: 'Pro adds the home-screen widget. Pick any amount: they all unlock the same thing, ' +
        'and the bigger ones help keep the app going.',
      pt: 'O Pro adiciona o widget na tela inicial. Escolha qualquer valor: todos desbloqueiam ' +
        'a mesma coisa, e os maiores ajudam a manter o app.',
      es: 'Pro añade el widget en la pantalla de inicio. Elige cualquier monto: todos desbloquean ' +
        'lo mismo, y los más altos ayudan a mantener la app.',
    },
    proThanks: {
      en: 'Pro is unlocked. If you\'d like to give a little more, the other amounts are one-off tips.',
      pt: 'O Pro está desbloqueado. Se quiser contribuir com um pouco mais, os outros valores são gorjetas únicas.',
      es: 'Pro está desbloqueado. Si quieres aportar un poco más, los otros montos son propinas únicas.',
    },
    // The three amounts, smallest first.
    proSmall: { en: 'Small', pt: 'Pequeno', es: 'Pequeño' },
    proMedium: { en: 'Medium', pt: 'Médio', es: 'Mediano' },
    proLarge: { en: 'Large', pt: 'Grande', es: 'Grande' },
    paid: { en: 'Paid ✓', pt: 'Pago ✓', es: 'Pagado ✓' },
    purchasesInApp: {
      en: 'Purchases are made in the Android app.',
      pt: 'As compras são feitas no app para Android.',
      es: 'Las compras se hacen en la app de Android.',
    },
    paymentPending: {
      en: 'Your payment is pending. Pro unlocks as soon as it goes through.',
      pt: 'Seu pagamento está pendente. O Pro será desbloqueado assim que ele for aprovado.',
      es: 'Tu pago está pendiente. Pro se desbloqueará en cuanto se apruebe.',
    },
    connectingPlay: {
      en: 'Connecting to Google Play…',
      pt: 'Conectando ao Google Play…',
      es: 'Conectando con Google Play…',
    },
    playUnavailable: {
      en: 'Google Play isn\'t available, so purchases can\'t be made right now.',
      pt: 'O Google Play não está disponível, então não é possível comprar agora.',
      es: 'Google Play no está disponible, así que no se pueden hacer compras ahora.',
    },
    pricesUnavailable: {
      en: 'Prices couldn\'t be loaded from Google Play. Try again later.',
      pt: 'Não foi possível carregar os preços do Google Play. Tente novamente mais tarde.',
      es: 'No se pudieron cargar los precios de Google Play. Inténtalo de nuevo más tarde.',
    },
    addWidget: {
      en: 'Add widget to home screen',
      pt: 'Adicionar widget à tela inicial',
      es: 'Agregar widget a la pantalla de inicio',
    },
  };

  // The Android app says which language it uses: it picks one of its strings.xml files by the
  // phone's (or the app's own) language list, so the page always matches the widget and the
  // notification. A browser uses its own languages, and ?lang=pt previews one.
  const LANGUAGES = ['en', 'pt', 'es'];

  function pickLanguage() {
    const wanted = [
      new URLSearchParams(location.search).get('lang'),
      global.Android && global.Android.language && global.Android.language(),
      ...(navigator.languages || [navigator.language]),
    ];
    for (const tag of wanted) {
      const base = tag && tag.split('-')[0].toLowerCase();
      if (LANGUAGES.includes(base)) return base;
    }
    return 'en';
  }

  const lang = pickLanguage();
  const plurals = new Intl.PluralRules(lang);

  function t(key, vars) {
    const entry = STRINGS[key];
    if (!entry) return key; // a typo shows up on screen instead of breaking the page
    let text = entry[lang] || entry.en;
    if (typeof text === 'object') text = text[plurals.select(vars.count)] || text.other;
    return text.replace(/\{(\w+)\}/g, (_, name) => vars[name]);
  }

  // Fills in the page's fixed text: data-i18n sets an element's text, data-i18n-placeholder its
  // placeholder and data-i18n-label its aria-label.
  function translatePage() {
    document.documentElement.lang = lang === 'pt' ? 'pt-BR' : lang;
    for (const el of document.querySelectorAll('[data-i18n]')) el.textContent = t(el.dataset.i18n);
    for (const el of document.querySelectorAll('[data-i18n-placeholder]')) {
      el.placeholder = t(el.dataset.i18nPlaceholder);
    }
    for (const el of document.querySelectorAll('[data-i18n-label]')) {
      el.setAttribute('aria-label', t(el.dataset.i18nLabel));
    }
  }

  global.I18n = { lang, t, translatePage };
})(window);
