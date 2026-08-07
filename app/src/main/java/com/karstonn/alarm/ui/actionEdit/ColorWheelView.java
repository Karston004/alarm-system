package com.karstonn.alarm.ui.actionEdit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorWheelView extends View {

    private Bitmap wheelBitmap;

    private float hue = 0f;
    private float saturation = 0f;

    private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(5f);
        selectorPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        int size = Math.min(width, height);

        wheelBitmap = Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888
        );

        float radius = size / 2f;
        float centre = size / 2f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {

                float dx = x - centre;
                float dy = y - centre;

                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance > radius) {
                    wheelBitmap.setPixel(x, y, Color.TRANSPARENT);
                    continue;
                }

                float saturation = distance / radius;

                float hue = (float) Math.toDegrees(
                        Math.atan2(dy, dx)
                );

                if (hue < 0) {
                    hue += 360;
                }

                wheelBitmap.setPixel(
                        x,
                        y,
                        Color.HSVToColor(
                                new float[]{hue, saturation, 1f}
                        )
                );
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (wheelBitmap == null) {
            return;
        }

        float left = (getWidth() - wheelBitmap.getWidth()) / 2f;
        float top = (getHeight() - wheelBitmap.getHeight()) / 2f;

        canvas.drawBitmap(wheelBitmap, left, top, null);


        float radius = wheelBitmap.getWidth() / 2f;
        float centreX = getWidth() / 2f;
        float centreY = getHeight() / 2f;

        double angle = Math.toRadians(hue);

        float x = centreX
                + (float) Math.cos(angle)
                * saturation
                * radius;

        float y = centreY
                + (float) Math.sin(angle)
                * saturation
                * radius;

        canvas.drawCircle(x, y, 12f, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() != MotionEvent.ACTION_DOWN
                && event.getAction() != MotionEvent.ACTION_MOVE) {
            return true;
        }

        float centreX = getWidth() / 2f;
        float centreY = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f;

        float dx = event.getX() - centreX;
        float dy = event.getY() - centreY;

        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        saturation = Math.min(distance / radius, 1f);

        hue = (float) Math.toDegrees(
                Math.atan2(dy, dx)
        );

        if (hue < 0) {
            hue += 360;
        }

        invalidate();

        return true;
    }

    public int getColor() {
        return Color.HSVToColor(
                new float[]{hue, saturation, 1f}
        );
    }

    public void setColor(int color) {
        float[] hsv = new float[3];

        Color.colorToHSV(color, hsv);

        hue = hsv[0];
        saturation = hsv[1];

        invalidate();
    }
}