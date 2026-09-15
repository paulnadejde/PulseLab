package ro.aquanano.pulselab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Square, text-free navigation button drawn locally for the four AquaRitm instruments. */
final class NavigationIconView extends View {
    static final int METRONOME = 0;
    static final int BIOSTIM = 1;
    static final int MINDEXTRA = 2;
    static final int SOLARITM = 3;

    private final int icon;
    private final boolean active;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    NavigationIconView(Context context, int icon, boolean active, String description) {
        super(context);
        this.icon = icon;
        this.active = active;
        setContentDescription(description);
        setSelected(active);
        setClickable(true);
        setFocusable(true);
        setElevation(UiStyle.dp(context, 3));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float inset = UiStyle.dp(getContext(), isPressed() ? 4 : 3);
        RectF face = new RectF(inset, inset, w - inset, h - inset);
        int top = active ? Color.rgb(104, 244, 224) : Color.rgb(82, 92, 101);
        int bottom = active ? Color.rgb(30, 139, 128) : Color.rgb(31, 38, 44);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, face.top, 0, face.bottom,
            top, bottom, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(face, UiStyle.dp(getContext(), 8), UiStyle.dp(getContext(), 8), paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(UiStyle.dp(getContext(), active ? 2 : 1));
        paint.setColor(active ? Color.WHITE : Color.rgb(126, 139, 149));
        canvas.drawRoundRect(face, UiStyle.dp(getContext(), 8), UiStyle.dp(getContext(), 8), paint);

        canvas.save();
        if (isPressed()) canvas.translate(0, UiStyle.dp(getContext(), 2));
        switch (icon) {
            case METRONOME: drawMetronome(canvas, w, h); break;
            case BIOSTIM: drawPulse(canvas, w, h); break;
            case MINDEXTRA: drawBrain(canvas, w, h); break;
            default: drawSunrise(canvas, w, h); break;
        }
        canvas.restore();
    }

    private void drawMetronome(Canvas canvas, float w, float h) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(235, 235, 226));
        path.reset();
        path.moveTo(w * .34f, h * .76f);
        path.lineTo(w * .43f, h * .24f);
        path.lineTo(w * .57f, h * .24f);
        path.lineTo(w * .68f, h * .76f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(w * .055f);
        paint.setColor(Color.rgb(66, 45, 38));
        canvas.drawLine(w * .49f, h * .63f, w * .64f, h * .27f, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(w * .64f, h * .27f, w * .055f, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawPulse(Canvas canvas, float w, float h) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(w * .07f);
        paint.setColor(Color.rgb(255, 64, 83));
        path.reset();
        path.moveTo(w * .16f, h * .55f);
        path.lineTo(w * .32f, h * .55f);
        path.lineTo(w * .41f, h * .70f);
        path.lineTo(w * .51f, h * .25f);
        path.lineTo(w * .61f, h * .61f);
        path.lineTo(w * .70f, h * .48f);
        path.lineTo(w * .84f, h * .48f);
        canvas.drawPath(path, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawBrain(Canvas canvas, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(w * .5f, h * .5f, w * .38f,
            new int[]{0x66ffe79b, 0x22ffd45a, 0x00ffd45a}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * .5f, h * .5f, w * .38f, paint);
        paint.setShader(null);
        path.reset();
        path.moveTo(w * .68f, h * .78f);
        path.cubicTo(w * .65f, h * .67f, w * .68f, h * .57f, w * .72f, h * .48f);
        path.cubicTo(w * .78f, h * .31f, w * .66f, h * .19f, w * .49f, h * .18f);
        path.cubicTo(w * .31f, h * .17f, w * .23f, h * .28f, w * .22f, h * .42f);
        path.lineTo(w * .15f, h * .53f);
        path.lineTo(w * .22f, h * .58f);
        path.lineTo(w * .19f, h * .64f);
        path.lineTo(w * .26f, h * .68f);
        path.cubicTo(w * .25f, h * .76f, w * .30f, h * .79f, w * .38f, h * .76f);
        path.lineTo(w * .49f, h * .72f);
        path.cubicTo(w * .53f, h * .77f, w * .55f, h * .82f, w * .57f, h * .86f);
        path.lineTo(w * .74f, h * .86f);
        path.close();
        paint.setColor(Color.rgb(255, 226, 137));
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(w * .045f);
        paint.setColor(Color.rgb(218, 162, 35));
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(w * .31f, h * .39f);
        path.cubicTo(w * .29f, h * .29f, w * .40f, h * .25f, w * .46f, h * .31f);
        path.cubicTo(w * .49f, h * .23f, w * .61f, h * .25f, w * .61f, h * .34f);
        path.cubicTo(w * .71f, h * .32f, w * .73f, h * .43f, w * .68f, h * .48f);
        path.cubicTo(w * .74f, h * .55f, w * .66f, h * .63f, w * .59f, h * .58f);
        path.cubicTo(w * .55f, h * .67f, w * .43f, h * .63f, w * .44f, h * .55f);
        path.cubicTo(w * .35f, h * .59f, w * .28f, h * .51f, w * .33f, h * .45f);
        canvas.drawPath(path, paint);
        paint.setStrokeWidth(w * .035f);
        canvas.drawLine(w * .60f, h * .58f, w * .66f, h * .74f, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawSunrise(Canvas canvas, float w, float h) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 202, 50));
        canvas.drawArc(new RectF(w * .31f, h * .36f, w * .69f, h * .74f), 180, 180, true, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(w * .045f);
        canvas.drawLine(w * .50f, h * .26f, w * .50f, h * .18f, paint);
        canvas.drawLine(w * .30f, h * .34f, w * .23f, h * .27f, paint);
        canvas.drawLine(w * .70f, h * .34f, w * .77f, h * .27f, paint);
        paint.setColor(Color.rgb(126, 211, 230));
        canvas.drawLine(w * .16f, h * .69f, w * .84f, h * .69f, paint);
        canvas.drawLine(w * .24f, h * .79f, w * .76f, h * .79f, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }
}
