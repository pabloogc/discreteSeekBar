package com.bq.lib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ThumbSeekBarBackup extends View {

    //Util
    private final Rect seekBarRelativeBounds = new Rect();

    //Visual properties
    private int cy;
    private float progressPercent;
    private float visualProgress;

    //Progress
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    //Track
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    //Thumb
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect thumbDrawableBounds = new Rect();

    //Dots
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float dotSpacing;
    private boolean dotsEnabled;
    private int dotColor;
    private float dotSize;

    private int barHorizontalOffset;
    private Drawable thumbDrawable;

    private int progress;
    private int min, max;

    public ThumbSeekBarBackup(Context context) {
        this(context, null);
    }

    public ThumbSeekBarBackup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThumbSeekBarBackup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float density = context.getResources().getDisplayMetrics().density;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PopupSeekBar);
        setMin(a.getInt(R.styleable.PopupSeekBar_min, 0));
        setMax(a.getInt(R.styleable.PopupSeekBar_max, 10));
        setProgress(a.getInt(R.styleable.PopupSeekBar_progress, 0));

        thumbDrawable = a.getDrawable(R.styleable.PopupSeekBar_thumbDrawable);
        if (thumbDrawable == null) {
            thumbDrawable = ContextCompat.getDrawable(getContext(), 0);
        }
        setThumbDrawable(thumbDrawable);

        progressPaint.setStrokeWidth(a.getDimension(R.styleable.PopupSeekBar_progressHeight, 2 * density));
        progressPaint.setColor(a.getColor(R.styleable.PopupSeekBar_progressColor, Color.RED));

        trackPaint.setStrokeWidth(a.getDimension(R.styleable.PopupSeekBar_trackHeight, 1 * density));
        trackPaint.setColor(a.getColor(R.styleable.PopupSeekBar_trackColor, Color.BLUE));

        dotsEnabled = a.getBoolean(R.styleable.PopupSeekBar_dotsEnabled, false);
        dotSize = a.getDimension(R.styleable.PopupSeekBar_dotSize, 4 * density);
        dotPaint.setColor(a.getColor(R.styleable.PopupSeekBar_dotColor, Color.BLUE));

        a.recycle();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        canvas.drawLine(seekBarRelativeBounds.left, cy, seekBarRelativeBounds.left + visualProgress, cy, progressPaint);
        canvas.drawLine(seekBarRelativeBounds.left + visualProgress, cy, seekBarRelativeBounds.right, cy, trackPaint);

        if (dotsEnabled) {
            for (int i = 0; i <= (max - min); i++) {
                canvas.drawCircle(barHorizontalOffset + i * dotSpacing, cy, dotSize, dotPaint);
            }
        }

        thumbDrawable.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //Only handle motion events that land inside the bar bounds
                boolean touchedInside = seekBarRelativeBounds.contains(x, y);
                if (touchedInside) handleTouch(x, y);
                return touchedInside;
            case MotionEvent.ACTION_MOVE:
                handleTouch(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                return false;
        }
        return false;
    }

    private void handleTouch(int x, int y) {
        int adjustedOffset = x - seekBarRelativeBounds.left;
        float progressPercent = ((float) adjustedOffset) / seekBarRelativeBounds.width();
        setProgress(Math.round(progressPercent * getAmplitude()));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        this.cy = h / 2;
        seekBarRelativeBounds.left = barHorizontalOffset;
        seekBarRelativeBounds.right = w - barHorizontalOffset;
        seekBarRelativeBounds.bottom = cy + thumbDrawable.getIntrinsicHeight() / 2;
        seekBarRelativeBounds.top = cy - thumbDrawable.getIntrinsicHeight() / 2;
        updateProgressVisual();
    }

    private void updateProgressVisual() {
        progressPercent = ((float) progress - min) / getAmplitude();
        visualProgress = progressPercent * seekBarRelativeBounds.width();
        dotSpacing = ((float) seekBarRelativeBounds.width()) / getAmplitude();

        thumbDrawableBounds.offsetTo((int) (visualProgress), thumbDrawableBounds.top);
        if (thumbDrawable != null) {
            thumbDrawable.setBounds(thumbDrawableBounds);
        }
    }

    //#################################
    // Properties
    //#################################

    public int getAmplitude() {
        return max - min;
    }


    public void setThumbDrawable(Drawable thumbDrawable) {
        this.thumbDrawable = thumbDrawable;
        barHorizontalOffset = thumbDrawable.getIntrinsicWidth() / 2;

        thumbDrawableBounds.top = 0;
        thumbDrawableBounds.bottom = thumbDrawable.getIntrinsicHeight();
        thumbDrawableBounds.right = thumbDrawable.getIntrinsicWidth();

        requestLayout();
    }

    public void setProgress(int progress) {
        if (this.progress == progress) return;
        this.progress = Math.max(min, Math.min(progress, max)); //clamp
        updateProgressVisual();
        invalidate();
    }

    public void setMin(int min) {
        if (this.min == min) return;
        this.min = min;
        updateProgressVisual();
        invalidate();
    }

    public void setMax(int max) {
        if (this.max == max) return;
        this.max = max;
        updateProgressVisual();
        invalidate();
    }
}
