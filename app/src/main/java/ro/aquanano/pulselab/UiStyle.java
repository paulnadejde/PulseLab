package ro.aquanano.pulselab;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;

/** Shared compact geometry for every AquaRitm button. */
final class UiStyle {
    private UiStyle() { }

    static void compactButton(Button button, Context context) {
        button.setTextSize(11);
        button.setSingleLine(true);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(context, 8), 0, dp(context, 8), 0);
        button.setAutoSizeTextTypeUniformWithConfiguration(
            8, 11, 1, TypedValue.COMPLEX_UNIT_SP);
    }

    static LinearLayout.LayoutParams centeredButton(Context context) {
        int available = context.getResources().getDisplayMetrics().widthPixels - dp(context, 48);
        int width = Math.min(dp(context, 280), Math.max(dp(context, 180), available));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, dp(context, 40));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.setMargins(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));
        return params;
    }

    static LinearLayout.LayoutParams weightedButton(Context context) {
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(0, dp(context, 40), 1f);
        params.setMargins(dp(context, 4), dp(context, 3), dp(context, 4), dp(context, 3));
        return params;
    }

    static LinearLayout.LayoutParams frequencyDial(Context context) {
        int available = context.getResources().getDisplayMetrics().widthPixels - dp(context, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            Math.round(available * 0.75f), LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.START;
        params.setMargins(0, dp(context, 3), 0, dp(context, 3));
        return params;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
