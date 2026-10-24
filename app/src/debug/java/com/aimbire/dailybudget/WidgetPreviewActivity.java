package com.aimbire.dailybudget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;

/**
 * Debug builds only: every widget layout on one screen, at typical home-screen sizes.
 *
 *   adb shell am start -n com.aimbire.dailybudget/.WidgetPreviewActivity \
 *       --es theme light --es colors wallpaper --ez sample true
 *
 * theme / colors override the app's settings and sample swaps in example data,
 * all only while the preview is drawn; the app's saved data is left as it was.
 */
public class WidgetPreviewActivity extends Activity {

    // Roughly a Pixel's 2×1, 2×2, 4×1 and 4×2 in portrait.
    private static final int[][] SIZES = {{130, 100}, {160, 190}, {330, 100}, {330, 210}};
    private static final String[] NAMES = {"A · 2×1", "B · 2×2", "C · 4×1", "D · 4×2"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(0, dp(48), 0, dp(48));

        SharedPreferences prefs = getSharedPreferences("reminder", Context.MODE_PRIVATE);
        String saved = prefs.getString("data", null);
        try {
            JSONObject data = getIntent().getBooleanExtra("sample", false)
                    ? sample() : new JSONObject(saved == null ? "{}" : saved);
            String theme = getIntent().getStringExtra("theme");
            String colors = getIntent().getStringExtra("colors");
            if (theme != null) data.put("theme", theme);
            if (colors != null) data.put("widgetColors", colors);
            prefs.edit().putString("data", data.toString()).commit();

            for (int kind = BudgetWidget.SMALL; kind <= BudgetWidget.LARGE; kind++) {
                TextView label = new TextView(this);
                label.setText(NAMES[kind]);
                label.setTextColor(Color.WHITE);
                label.setPadding(0, dp(16), 0, dp(6));
                column.addView(label);

                FrameLayout frame = new FrameLayout(this);
                View widget = BudgetWidget.views(this, kind).apply(this, frame);
                frame.addView(widget, new FrameLayout.LayoutParams(-1, -1));
                column.addView(frame, new LinearLayout.LayoutParams(dp(SIZES[kind][0]), dp(SIZES[kind][1])));
            }
        } catch (Exception e) {
            TextView error = new TextView(this);
            error.setText(String.valueOf(e));
            error.setTextColor(Color.RED);
            column.addView(error);
        } finally {
            prefs.edit().putString("data", saved).commit();
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#3c5a74"));
        scroll.addView(column);
        setContentView(scroll);
    }

    /** The store screenshots' 18 days of spending ($1,500 budget), shifted to end today. */
    private static JSONObject sample() throws Exception {
        double[][] spend = {{42.80, 4.50}, {12.90}, {38.00, 4.50}, {64.20}, {27.35}, {}, {24.00, 8.50},
                {4.50, 11.20}, {51.10}, {18.75}, {30.00}, {85.00}, {}, {36.40}, {4.50, 22.99}, {13.60},
                {35.00}, {4.50, 12.80}};
        LocalDate today = LocalDate.now();
        JSONArray expenses = new JSONArray();
        for (int i = 0; i < spend.length; i++) {
            LocalDate day = today.minusDays(spend.length - 1 - i);
            for (double amount : spend[i]) {
                expenses.put(new JSONObject().put("day", day.toString()).put("amount", amount));
            }
        }
        return new JSONObject()
                .put("budget", 1500).put("currency", "$")
                .put("since", today.minusDays(spend.length - 1).toString())
                .put("theme", "dark").put("widgetColors", "app")
                .put("expenses", expenses);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
