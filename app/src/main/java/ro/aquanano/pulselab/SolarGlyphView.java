package ro.aquanano.pulselab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Draws the five symbolic states used by the nested SolaRitm cycles. */
final class SolarGlyphView extends View {
    private static final int PANEL_COLOR = Color.rgb(25, 21, 18);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int value = 5;

    SolarGlyphView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        updateDescription();
    }

    void setValue(int value) {
        int bounded = Math.max(1, Math.min(5, value));
        if (this.value == bounded) return;
        this.value = bounded;
        updateDescription();
        invalidate();
    }

    private void updateDescription() {
        String[] names = {"pătrat galben", "semilună argintie", "triunghi roșu",
            "cerc verde", "oval indigo"};
        setContentDescription("Simbol " + names[value - 1]);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h) * .70f;
        float cx = w / 2f;
        float cy = h / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        path.reset();
        switch (value) {
            case 5:
                paint.setColor(Color.rgb(75, 34, 143));
                canvas.drawOval(new RectF(cx - size * .50f, cy - size * .34f,
                    cx + size * .50f, cy + size * .34f), paint);
                break;
            case 4:
                paint.setColor(Color.rgb(49, 184, 91));
                canvas.drawCircle(cx, cy, size * .45f, paint);
                break;
            case 3:
                paint.setColor(Color.rgb(225, 55, 55));
                path.moveTo(cx, cy - size * .52f);
                path.lineTo(cx + size * .52f, cy + size * .42f);
                path.lineTo(cx - size * .52f, cy + size * .42f);
                path.close();
                canvas.drawPath(path, paint);
                break;
            case 2:
                paint.setColor(Color.rgb(210, 216, 224));
                canvas.drawOval(new RectF(cx - size * .50f, cy - size * .46f,
                    cx + size * .50f, cy + size * .46f), paint);
                paint.setColor(PANEL_COLOR);
                canvas.drawOval(new RectF(cx - size * .40f, cy - size * .52f,
                    cx + size * .40f, cy + size * .22f), paint);
                break;
            default:
                paint.setColor(Color.rgb(255, 216, 45));
                canvas.drawRect(cx - size * .43f, cy - size * .43f,
                    cx + size * .43f, cy + size * .43f, paint);
                break;
        }
    }
}
