package com.bq.markerseekbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MarkerSeekBar extends AppCompatSeekBar implements SeekBar.OnSeekBarChangeListener {


    private static final int ANIMATION_SHOW_DURATION = 300;
    private static final int ANIMATION_SHOW_DELAY = 333;
    private static final Interpolator ANIMATION_SHOW_INTERPOLATOR = new DecelerateInterpolator();

    private static final int ANIMATION_HIDE_DURATION = 100;
    private static final Interpolator ANIMATION_HIDE_INTERPOLATOR = new DecelerateInterpolator();

    private TextTransformer textTransformer = new TextTransformer.Default();

    private final int[] windowLocation = new int[2];
    private PopupWindow popupWindow;
    private final ViewGroup popUpRootView;
    private final MarkerView markerView;
    private final TextView markerTextView;

    private float markerAnimationFrame = 0;
    private int popupVerticalOffset = 14;
    private int popupHorizontalOffset;
    private int popupWindowSize;

    public MarkerSeekBar(Context context) {
        this(context, null);
    }

    public MarkerSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.appcompat.R.attr.seekBarStyle);
    }

    @SuppressWarnings("deprecation")
    public MarkerSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOnSeekBarChangeListener(null);

        final float density = context.getResources().getDisplayMetrics().density;

        popUpRootView = new RelativeLayout(getContext());
        popUpRootView.setLayoutParams(new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        markerView = new MarkerView(getContext());
        RelativeLayout.LayoutParams markerParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        markerView.setLayoutParams(markerParams);
        popUpRootView.addView(markerView);

        markerTextView = new TextView(getContext());
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        markerTextView.setLayoutParams(textParams);
        markerTextView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (markerTextView.getHeight() > 0) {
                    markerTextView.setTranslationY(markerView.getCircleCenterY() - markerTextView.getHeight() / 2);
                }
            }
        });
        popUpRootView.addView(markerTextView);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MarkerSeekBar);

        popupWindowSize = a.getDimensionPixelSize(R.styleable.MarkerSeekBar_markerPopUpWindowSize, (int) (80 * density));
        markerView.onSizeChanged(popupWindowSize, popupWindowSize, 0, 0);

        markerView.setMarkerColor(a.getColor(R.styleable.MarkerSeekBar_markerColor, getAccentColor()));
        markerView.setShadowRadius(a.getDimension(R.styleable.MarkerSeekBar_markerShadowRadius, 4 * density));
        markerView.setShadowColor(a.getColor(R.styleable.MarkerSeekBar_markerShadowColor, Color.parseColor("#331d1d1d")));

        markerTextView.setTextAppearance(context,
                a.getResourceId(R.styleable.MarkerSeekBar_markerTextAppearance, R.style.Widget_MarkerSeekBar_TextAppearance));
        markerTextView.setTextColor(a.getColor(R.styleable.MarkerSeekBar_markerTextColor, Color.WHITE));

        //In material SeekBar thumbs is off by 6.5 dp
        popupHorizontalOffset = a.getDimensionPixelSize(R.styleable.MarkerSeekBar_markerHorizontalOffset, (int) (6.5 * density));
        popupVerticalOffset = a.getDimensionPixelSize(R.styleable.MarkerSeekBar_markerVerticalOffset, 0);

        a.recycle();

        popupWindow = new PopupWindow(popUpRootView, popupWindowSize, popupWindowSize, false);
        popupWindow.setClippingEnabled(false); //Allow to draw outside screen
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        markerTextView.setText(textTransformer.toText(progress));
        updatePopupLayout();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, "markerAnimationFrame", markerAnimationFrame, 1);
        anim.setAutoCancel(true);
        anim.setInterpolator(ANIMATION_SHOW_INTERPOLATOR);
        anim.setDuration(ANIMATION_SHOW_DURATION);
        anim.setStartDelay(ANIMATION_SHOW_DELAY);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                showPopUp();
            }
        });
        anim.start();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, "markerAnimationFrame", markerAnimationFrame, 0);
        anim.setAutoCancel(true);
        anim.setInterpolator(ANIMATION_HIDE_INTERPOLATOR);
        anim.setDuration(ANIMATION_HIDE_DURATION);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                hidePopUp();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                hidePopUp();
            }
        });
        anim.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        updatePopupLayout();
    }

    private void updatePopupLayout() {
        getLocationInWindow(windowLocation);
        int popUpX = windowLocation[0] + getThumb().getBounds().left + getThumbOffset() - popupWindowSize / 2 + popupHorizontalOffset;
        int popUpY = windowLocation[1] - popupWindowSize + getThumb().getIntrinsicHeight() / 2 - popupVerticalOffset;
        popupWindow.update(popUpX, popUpY, popupWindowSize, popupWindowSize);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        super.setOnSeekBarChangeListener(new WrappedSeekBarListener(listener));
    }

    public void ensureMarkerSize(String text) {
        if (markerTextView == null) return;
        Paint p = markerTextView.getPaint();
        int textSize = (int) p.measureText(text);
        if (textSize > 2 * markerView.getCircleRad()) {
            float diff = textSize - 2 * markerView.getCircleRad();
            popupWindowSize += diff * Math.sqrt(2);
            updatePopupLayout();
        }
    }

    //#########################
    // Properties
    //#########################

    @Override
    public synchronized void setMax(final int max) {
        super.setMax(max);
        if (isInEditMode()) return;

        if (markerTextView == null) { //Called during SeekBar constructor
            post(new Runnable() {
                @Override
                public void run() {
                    ensureMarkerSize(textTransformer.onMeasureLongestText(max));
                }
            });
        } else {
            ensureMarkerSize(textTransformer.onMeasureLongestText(max));
        }
    }

    public void setPopupHorizontalOffset(int popupHorizontalOffset) {
        this.popupHorizontalOffset = popupHorizontalOffset;
        updatePopupLayout();
    }

    public void setPopupVerticalOffset(int popupVerticalOffset) {
        this.popupVerticalOffset = popupVerticalOffset;
        updatePopupLayout();
    }

    public void setMarkerAnimationFrame(@FloatRange(from = 0, to = 1) float frame) {
        this.markerAnimationFrame = frame;
        popUpRootView.setPivotX(popUpRootView.getWidth() / 2);
        popUpRootView.setPivotY(popUpRootView.getHeight());
        popUpRootView.setScaleX(frame);
        popUpRootView.setScaleY(frame);
        popUpRootView.invalidate();
    }

    public float getMarkerAnimationFrame() {
        return markerAnimationFrame;
    }

    public void setTextTransformer(@NonNull TextTransformer textTransformer) {
        this.textTransformer = textTransformer;
    }

    public PopupWindow getPopupWindow() {
        return popupWindow;
    }

    public ViewGroup getPopUpRootView() {
        return popUpRootView;
    }

    public TextView getMarkerTextView() {
        return markerTextView;
    }

    public MarkerView getMarkerView() {
        return markerView;
    }

    //#########################
    // Utility
    //#########################

    private void showPopUp() {
        popupWindow.showAtLocation(MarkerSeekBar.this, Gravity.NO_GRAVITY, 0, 0);
        updatePopupLayout();
    }

    private void hidePopUp() {
        popupWindow.dismiss();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        popupWindow.dismiss();
    }

    private int getAccentColor() {
        int colorAttr;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorAttr = android.R.attr.colorAccent;
        } else {
            //Get colorAccent defined for AppCompat
            colorAttr = getContext().getResources().getIdentifier("colorAccent", "attr", getContext().getPackageName());
        }
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue.data;
    }

    public interface TextTransformer {
        String toText(int progress);

        String onMeasureLongestText(int seekBarMax);

        final class Default implements TextTransformer {

            @Override
            public String toText(int progress) {
                return String.valueOf(progress);
            }

            @Override
            public String onMeasureLongestText(int seekBarMax) {
                //All 0's
                return String.valueOf(seekBarMax).replaceAll("\\d", "0");
            }
        }
    }

    private class WrappedSeekBarListener implements OnSeekBarChangeListener {

        private final OnSeekBarChangeListener wrappedListener;

        public WrappedSeekBarListener(OnSeekBarChangeListener wrappedListener) {
            this.wrappedListener = wrappedListener;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            MarkerSeekBar.this.onProgressChanged(seekBar, progress, fromUser);
            if (wrappedListener != null)
                wrappedListener.onProgressChanged(seekBar, progress, fromUser);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            MarkerSeekBar.this.onStartTrackingTouch(seekBar);
            if (wrappedListener != null)
                wrappedListener.onStartTrackingTouch(seekBar);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            MarkerSeekBar.this.onStopTrackingTouch(seekBar);
            if (wrappedListener != null)
                wrappedListener.onStopTrackingTouch(seekBar);
        }
    }

    public static final class MarkerView extends View {

        private static final float SQRT_2 = 1.4142135f;
        private static final int SHADOW_CLIP_PADDING_FIX = 0;

        private final Paint debugPaint = new Paint();
        private final Path debugPath = new Path();

        private final Matrix matrix = new Matrix();
        private final RectF rect = new RectF();

        private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Path markerPath = new Path();
        private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private float padding; //calculated
        private float shadowRadius = 5;
        private float rad;
        private float radCorrection;

        private int shadowColor = Color.GRAY;
        private int width, height;

        private Bitmap shadowBitmap;

        public MarkerView(Context context) {
            this(context, null);
        }

        public MarkerView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public MarkerView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            markerPaint.setStyle(Paint.Style.FILL);
            markerPaint.setColor(Color.WHITE);
        }

        private void buildShadowBitmap() {
            if (shadowBitmap != null) return;

            shadowBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            if (isInEditMode()) return;

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(markerPaint.getColor());
            paint.setStyle(Paint.Style.FILL);
            Canvas canvas = new Canvas(shadowBitmap);
            Path shadowPath = new Path();

            setLayerType(LAYER_TYPE_SOFTWARE, null);

            //Draw the shadow with the shape filled
            computeConvexPath(shadowPath, rad - SHADOW_CLIP_PADDING_FIX);
            paint.setShadowLayer(shadowRadius, 0, 0, shadowColor);
            canvas.drawPath(shadowPath, paint);
            paint.setShadowLayer(0, 0, 0, 0);

            //Remove the inside
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            computeConvexPath(shadowPath, rad);
            canvas.drawPath(shadowPath, paint);

            setLayerType(LAYER_TYPE_HARDWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            buildShadowBitmap();
            canvas.drawBitmap(shadowBitmap, 0, 0, shadowPaint);
            canvas.drawPath(markerPath, markerPaint);

//            debugPath.reset();
//            matrix.reset();
//
//            rect.set(0, 0, 2 * rad, 2 * rad);
//            debugPath.addRect(rect, Path.Direction.CCW);
//
//            matrix.postRotate(45, 0, 0);
//            matrix.postTranslate(width / 2, 0);
//            debugPath.transform(matrix);
//
//            debugPaint.setStyle(Paint.Style.STROKE);
//            debugPaint.setColor(Color.MAGENTA);
//            canvas.drawPath(debugPath, debugPaint);

        }

        private void computeConvexPath(Path path, float rad) {
            path.reset();
            matrix.reset();

            rect.set(0, 0, 2 * rad, 2 * rad);
            float[] rads = new float[]{0, 0, rad, rad, rad, rad, rad, rad};
            path.addRoundRect(rect, rads, Path.Direction.CCW);

            matrix.postRotate(45 + 180, 0, 0);
            matrix.postTranslate(width / 2, height - padding);
            path.transform(matrix);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            this.width = w;
            this.height = h;
            this.padding = shadowRadius;

            float halfSide = Math.min(width / 2, height / 2);
            //Subtract the distance from the enclosing square to the circle, after rotation
            //We have to make sure the shape fits
            rad = (0.5f * SQRT_2 * halfSide) - padding;

            computeConvexPath(markerPath, rad);
            rebuildShadowBitmap();
        }

        public void setShadowColor(int shadowColor) {
            this.shadowColor = shadowColor;
            rebuildShadowBitmap();
        }

        public void setShadowRadius(float shadowRadius) {
            this.shadowRadius = shadowRadius;
            rebuildShadowBitmap();
        }

        public void setMarkerColor(int color) {
            this.markerPaint.setColor(color);
            invalidate();
        }

        public void setMarkerColorFilter(ColorFilter colorFilter) {
            this.markerPaint.setColorFilter(colorFilter);
            invalidate();
        }

        public void setShadowColorFilter(ColorFilter colorFilter) {
            this.shadowPaint.setColorFilter(colorFilter);
            invalidate();
        }

        public int getCircleCenterY() {
            //The view is top aligned, so rad + padding gives the center
            //of the oval
            return (int) (height / 2 + padding);
        }

        public float getCircleRad() {
            return rad;
        }

        private void rebuildShadowBitmap() {
            if (shadowBitmap != null) {
                shadowBitmap.recycle();
            }
            shadowBitmap = null;
            invalidate();
        }
    }
}
