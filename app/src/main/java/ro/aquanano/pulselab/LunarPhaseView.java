package ro.aquanano.pulselab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Moon silhouette and optional 360° cycle gauge, drawn without image assets. */
public final class LunarPhaseView extends View {
    private static final int GOLD = 0xffffd463;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double degrees;
    private final boolean gauge;

    public LunarPhaseView(Context context, boolean gauge) {
        super(context);
        this.gauge = gauge;
        setContentDescription("Faza Lunii");
    }

    public void setDegrees(double degrees) {
        this.degrees = degrees;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float size = Math.min(getWidth(), getHeight());
        float radius = size * (gauge ? .29f : .40f);
        if (gauge) {
            float ringRadius = size * .44f;
            RectF ring = new RectF(cx-ringRadius, cy-ringRadius, cx+ringRadius, cy+ringRadius);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * .035f);
            paint.setColor(0xff52472d);
            canvas.drawArc(ring, -90, 360, false, paint);
            paint.setColor(GOLD);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawArc(ring, -90, (float) degrees, false, paint);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.FILL);
        }
        paint.setColor(0xff373844);
        canvas.drawCircle(cx, cy, radius, paint);
        double angle = Math.toRadians(degrees);
        Path illuminated = new Path();
        // Trace the visible outer rim, then the curved day/night terminator.
        for (int i = 0; i <= 80; i++) {
            float y = radius * (2f * i / 80 - 1f);
            float width = (float) Math.sqrt(Math.max(0, radius * radius - y * y));
            float x = degrees < 180 ? width : -width;
            if (i == 0) illuminated.moveTo(cx+x, cy+y);
            else illuminated.lineTo(cx+x, cy+y);
        }
        for (int i = 80; i >= 0; i--) {
            float y = radius * (2f * i / 80 - 1f);
            float width = (float) Math.sqrt(Math.max(0, radius * radius - y * y));
            float x = (float) ((degrees < 180 ? 1 : -1) * Math.cos(angle) * width);
            illuminated.lineTo(cx+x, cy+y);
        }
        illuminated.close();
        paint.setColor(GOLD);
        canvas.drawPath(illuminated, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, size * .009f));
        paint.setColor(Color.rgb(214, 184, 98));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
