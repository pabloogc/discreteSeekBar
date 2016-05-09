package com.bq.lib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

  private final PopupWindow popupWindow;
  private final ViewGroup popupContentView;
  private final TextView popupTextView;


  private final MarkerView markerView;
  private float markerShadowRadius;
  private float markerShadowColor;

  private float popupScale = 0;
  private int popupVerticalSeparation = 14;
  private int popupWindowOffsetCorrection;
  private int popupWindowSize;

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

    popupWindowSize = (int) (64 * density);

    markerShadowRadius = 4 * density;
    markerShadowColor = Color.parseColor("#331d1d1d");

    popupContentView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.thumb_popup, null, false);

    markerView = (MarkerView) popupContentView.findViewById(R.id.marker);
    markerView.onSizeChanged(popupWindowSize, popupWindowSize, 0, 0);

    popupTextView = (TextView) popupContentView.findViewById(R.id.popUpText);
    popupTextView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override public void onGlobalLayout() {
        if (popupTextView.getHeight() > 0) {
          popupTextView.setTranslationY(markerView.getCircleMidY() - popupTextView.getHeight() / 2);
        }
      }
    });

    popupWindowOffsetCorrection = (int) (6 * density); //In material SeekBar thumbs is off by 6 dp
    popupWindow = new PopupWindow(popupContentView, popupWindowSize, popupWindowSize, false);
    popupWindow.setClippingEnabled(false); //Allow to draw outside screen

    setOnSeekBarChangeListener(null);
  }


  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    updatePopupLayout();
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
        updatePopupLayout();
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
  protected void onSizeChanged(int w, int h, int oldW, int oldH) {
    super.onSizeChanged(w, h, oldW, oldH);
    updatePopupLayout();
  }

  private void updatePopupLayout() {
    getLocationInWindow(windowLocation);
    int popUpX = windowLocation[0] + getThumb().getBounds().left + getThumbOffset() - popupWindowSize / 2 + popupWindowOffsetCorrection;
    int popUpY = windowLocation[1] - popupWindowSize + getThumb().getIntrinsicHeight() / 2 - popupVerticalSeparation;
    popupWindow.update(popUpX, popUpY, popupWindowSize, popupWindowSize);
  }

  @Override
  public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
    super.setOnSeekBarChangeListener(new WrappedSeekBarListener(listener));
  }

  public void ensureMarkerSize(String text) {
    if (popupTextView == null) return;
    Paint p = popupTextView.getPaint();
    int textSize = (int) p.measureText(text);
    if (textSize > 2 * markerView.getCircleRad()) {
      int diff = textSize - 2 * markerView.getCircleRad();
      popupWindowSize = 2 * (int) (markerView.getCircleRad() + diff + markerShadowRadius);
      updatePopupLayout();
    }
  }

  //#########################
  // Properties
  //#########################

  @Override
  public synchronized void setMax(final int max) {
    super.setMax(max);
    if (popupTextView == null) { //Called during Seekbar constructor
      post(new Runnable() {
        @Override public void run() {
          ensureMarkerSize(String.valueOf(max));
        }
      });
    }
    ensureMarkerSize(String.valueOf(max));
  }

  public void setPopupWindowOffsetCorrection(int popupWindowOffsetCorrection) {
    this.popupWindowOffsetCorrection = popupWindowOffsetCorrection;
    updatePopupLayout();
  }

  public void setPopupVerticalSeparation(int popupVerticalSeparation) {
    this.popupVerticalSeparation = popupVerticalSeparation;
    updatePopupLayout();
  }

  public void setPopupScale(float popupScale) {
    this.popupScale = popupScale;
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
