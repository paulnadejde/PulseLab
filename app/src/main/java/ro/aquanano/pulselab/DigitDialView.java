package ro.aquanano.pulselab;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

/** Five decimal digit wheels with a fixed decimal separator. */
public final class DigitDialView extends LinearLayout {
    private final NumberPicker[] digits = new NumberPicker[5];
    private final int decimals;

    public DigitDialView(Context context, int decimals) {
        super(context);
        this.decimals = decimals;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        for (int i = 0; i < digits.length; i++) {
            if (i == digits.length - decimals) {
                TextView dot = new TextView(context);
                dot.setText(".");
                dot.setTextColor(Color.WHITE);
                dot.setTextSize(26);
                addView(dot);
            }
            NumberPicker p = new NumberPicker(context);
            p.setMinValue(0);
            p.setMaxValue(9);
            p.setWrapSelectorWheel(true);
            p.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
            digits[i] = p;
            addView(p, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
        }
    }

    public double getValue() {
        int raw = 0;
        for (NumberPicker p : digits) raw = raw * 10 + p.getValue();
        return raw / Math.pow(10, decimals);
    }

    public void setValue(double value) {
        int raw = (int) Math.round(Math.max(0, value) * Math.pow(10, decimals));
        raw = Math.min(99_999, raw);
        for (int i = digits.length - 1; i >= 0; i--) {
            digits[i].setValue(raw % 10);
            raw /= 10;
        }
    }
}
