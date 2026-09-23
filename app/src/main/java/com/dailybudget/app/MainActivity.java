package com.dailybudget.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.window.OnBackInvokedDispatcher;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

/**
 * Hosts the bundled web UI (assets/www) in a full-screen WebView.
 *
 * The status and navigation bar areas are coloured by the web UI's theme
 * (see {@link Bridge#setSystemBars}). On Android 11+ the app draws edge-to-edge
 * and two spacer views fill the bar areas; older versions colour the bars directly.
 */
public class MainActivity extends Activity {

    private WebView webView;
    private View statusSpacer;
    private View navSpacer;

    private int barColor = Color.parseColor("#111111");
    private int navColor = Color.parseColor("#1b1b1b");
    private boolean lightBars = false;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                Log.d("DailyBudget", m.message() + " (" + m.sourceId() + ":" + m.lineNumber() + ")");
                return true;
            }
        });
        webView.addJavascriptInterface(new Bridge(), "Android");

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true); // localStorage holds the budget and expenses

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        statusSpacer = new View(this);
        navSpacer = new View(this);
        root.addView(statusSpacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(navSpacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        setContentView(root);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                setHeight(statusSpacer, bars.top);
                setHeight(navSpacer, Math.max(bars.bottom, ime.bottom)); // keyboard pushes content up
                v.setPadding(bars.left, 0, bars.right, 0);
                return WindowInsets.CONSUMED;
            });
        }
        applySystemBars();

        // Android 13+ (predictive back) no longer calls onBackPressed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl("file:///android_asset/www/index.html");
        }
    }

    private static void setHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp.height != height) {
            lp.height = height;
            view.setLayoutParams(lp);
        }
    }

    @SuppressWarnings("deprecation")
    private void applySystemBars() {
        webView.setBackgroundColor(barColor);
        statusSpacer.setBackgroundColor(barColor);
        navSpacer.setBackgroundColor(navColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(lightBars ? mask : 0, mask);
            }
        } else {
            getWindow().setStatusBarColor(barColor);
            getWindow().setNavigationBarColor(navColor);
            View decor = getWindow().getDecorView();
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            int current = decor.getSystemUiVisibility();
            decor.setSystemUiVisibility(lightBars ? current | flags : current & ~flags);
        }
    }

    /** Called from JavaScript as window.Android.* */
    private class Bridge {
        @JavascriptInterface
        public void setSystemBars(String bar, String nav, boolean light) {
            runOnUiThread(() -> {
                try {
                    barColor = Color.parseColor(bar);
                    navColor = Color.parseColor(nav);
                } catch (IllegalArgumentException ignored) {
                    return;
                }
                lightBars = light;
                applySystemBars();
            });
        }

        /** Snapshot of budget, currency and this month's expenses, for the notification. */
        @JavascriptInterface
        public void syncData(String json) {
            Reminder.saveData(MainActivity.this, json);
        }

        @JavascriptInterface
        public void setReminder(boolean enabled, int hour, int minute) {
            Reminder.saveSettings(MainActivity.this, enabled, hour, minute);
            Reminder.schedule(MainActivity.this);
            if (enabled) runOnUiThread(MainActivity.this::requestNotificationPermission);
        }

        @JavascriptInterface
        public void testReminder() {
            Reminder.show(MainActivity.this);
        }
    }

    private static final int REQUEST_NOTIFICATIONS = 1;

    /** Android 13+ asks the user before an app may post notifications. */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode != REQUEST_NOTIFICATIONS) return;
        boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
        webView.evaluateJavascript(
                "window.onReminderPermission && onReminderPermission(" + granted + ")", null);
    }

    /** Let the web UI close sheets / return to the Budget tab before exiting. */
    private void handleBack() {
        webView.evaluateJavascript("window.handleBack ? handleBack() : false", handled -> {
            if (!"true".equals(handled)) finish();
        });
    }

    /** Android 12 and below. */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBack();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
}
