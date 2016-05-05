package com.bq.lib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.Paint;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

public class PopupSeekBar extends AppCompatSeekBar implements SeekBar.OnSeekBarChangeListener {

    private int[] windowLocation = new int[2];

    private static final int ANIMATION_SHOW_DURATION = 300;
    private static final int ANIMATION_SHOW_DELAY = 333;
    private static final Interpolator ANIMATION_SHOW_INTERPOLATOR = new DecelerateInterpolator();

    private static final int ANIMATION_HIDE_DURATION = 100;
    private static final Interpolator ANIMATION_HIDE_INTERPOLATOR = new DecelerateInterpolator();


    private final MarkerDrawable markerDrawable;


    private final PopupWindow popupWindow;
    private final ViewGroup popupContentView;
    private final TextView popupTextView;
    private final ViewGroup popupMarkerContainer;

    private float popupScale = 0;
    private int popupVerticalSeparation = 14;
    private int popupOffsetCorrection = 14;
    private int popupWidth = 128;
    private int popupHeight = 128;


    //Dots
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float dotSpacing;
    private boolean dotsEnabled;
    private int dotColor;
    private float dotSize;

    public PopupSeekBar(Context context) {
        this(context, null);
    }

    public PopupSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.appcompat.R.attr.seekBarStyle);
    }

    @SuppressLint("InflateParams")
    public PopupSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final float density = context.getResources().getDisplayMetrics().density;

        markerDrawable = new MarkerDrawable();
        markerDrawable.setSize(popupWidth, popupHeight);

        popupContentView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.thumb_popup, null, false);
        popupWindow = new PopupWindow(popupContentView, popupWidth, popupHeight, false);
        popupWindow.setClippingEnabled(false); //Allow to draw outside screen

        popupContentView.setBackground(markerDrawable);
        popupTextView = (TextView) popupContentView.findViewById(R.id.popUpText);
        popupMarkerContainer = (ViewGroup) popupContentView.findViewById(R.id.popupMarkerContainer);
        popupMarkerContainer.setBackground(markerDrawable);
        //popupMarkerContainer.setLayerType(LAYER_TYPE_SOFTWARE, null);

//        popupContentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                if (popupContentView.getWidth() != 0 && popupContentView.getHeight() != 0) {
//                    markerDrawable.setSize(popupContentView.getWidth(), popupContentView.getHeight());
//                    //Move the textView baseline to the center of the circle
//                    popupTextView.setTranslationY(markerDrawable.getCircleCenterY() - popupTextView.getHeight());
//                    updatePopupPosition();
//                }
//            }
//        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewCompat.setElevation(popupMarkerContainer, 4 * density);
            popupMarkerContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                @SuppressLint("NewApi")
                public void getOutline(View view, Outline outline) {
                    outline.setConvexPath(markerDrawable.getPath());
                }
            });
        }

        setOnSeekBarChangeListener(null);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updatePopupPosition();
        popupTextView.setText(String.valueOf(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, "popupScale", popupScale, 1);
        anim.setAutoCancel(true);
        anim.setInterpolator(ANIMATION_SHOW_INTERPOLATOR);
        anim.setDuration(ANIMATION_SHOW_DURATION);
        anim.setStartDelay(ANIMATION_SHOW_DELAY);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                popupWindow.showAtLocation(PopupSeekBar.this, Gravity.NO_GRAVITY, 0, 0);
                updatePopupPosition();
            }
        });
        anim.start();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, "popupScale", popupScale, 0);
        anim.setAutoCancel(true);
        anim.setInterpolator(ANIMATION_HIDE_INTERPOLATOR);
        anim.setDuration(ANIMATION_HIDE_DURATION);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                popupWindow.dismiss();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                popupWindow.dismiss();
            }
        });
        anim.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePopupPosition();
    }

    private void updatePopupPosition() {
        getLocationInWindow(windowLocation);
        int popUpX = windowLocation[0] + getThumb().getBounds().left + getThumbOffset() - popupWidth / 2 + popupOffsetCorrection;
        int popUpY = windowLocation[1] - popupHeight + getThumb().getIntrinsicHeight() / 2 - popupVerticalSeparation;
        popupWindow.update(popUpX, popUpY, popupWidth, popupHeight);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        super.setOnSeekBarChangeListener(new WrappedSeekBarListener(listener));
    }

    //#########################
    // Properties
    //#########################


    public void setPopupOffsetCorrection(int popupOffsetCorrection) {
        this.popupOffsetCorrection = popupOffsetCorrection;
        updatePopupPosition();
    }

    public void setPopupWidth(int popupWidth) {
        this.popupWidth = popupWidth;
        markerDrawable.setWidth(popupWidth);
        updatePopupPosition();
    }

    public void setPopupHeight(int popupHeight) {
        this.popupHeight = popupHeight;
        markerDrawable.setHeight(popupHeight);
        updatePopupPosition();
    }

    public void setPopupScale(float popupScale) {
        this.popupScale = popupScale;
        //markerDrawable.setScale(popupScale);
        popupContentView.setPivotX(popupContentView.getWidth() / 2);
        popupContentView.setPivotY(popupContentView.getHeight());
        popupContentView.setScaleX(popupScale);
        popupContentView.setScaleY(popupScale);
        popupContentView.invalidate();
    }

    public float getPopupScale() {
        return popupScale;
    }

    private class WrappedSeekBarListener implements OnSeekBarChangeListener {

        private final OnSeekBarChangeListener wrappedListener;

        public WrappedSeekBarListener(OnSeekBarChangeListener wrappedListener) {
            this.wrappedListener = wrappedListener;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            PopupSeekBar.this.onProgressChanged(seekBar, progress, fromUser);
            if (wrappedListener != null)
                wrappedListener.onProgressChanged(seekBar, progress, fromUser);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            PopupSeekBar.this.onStartTrackingTouch(seekBar);
            if (wrappedListener != null)
                wrappedListener.onStartTrackingTouch(seekBar);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            PopupSeekBar.this.onStopTrackingTouch(seekBar);
            if (wrappedListener != null)
                wrappedListener.onStopTrackingTouch(seekBar);
        }
    }
}
