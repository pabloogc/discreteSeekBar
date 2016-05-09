/*
 * Copyright (c) Gustavo Claramunt (AnderWeb) 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bq.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;


public class MarkerView extends View {

  private final Matrix matrix = new Matrix();
  private final RectF rect = new RectF();
  private final Path path = new Path();
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private int padding; //calculated
  private int shadowRadius = 10;
  private int shadowColor = Color.parseColor("#331d1d1d");
  private int width, height;
  private int cx, cy;
  private float rad, triangleHeight;

  private Bitmap drawCache;

  public MarkerView(Context context) {
    this(context, null);
  }

  public MarkerView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MarkerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Color.WHITE);
  }

  public int getCircleMidX() {
    return (int) (rad + padding);
  }

  public int getCircleMidY() {
    return (int) (rad + padding - triangleHeight / Math.sqrt(2));
  }

  public int getCircleRad() {
    return (int) (rad - 2 * padding);
  }

  private void preDraw() {
    if (drawCache != null) return;
    drawCache = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(drawCache);

    if (path.isEmpty()) {
      computePath();
    }

    setLayerType(LAYER_TYPE_SOFTWARE, null);
    if (!isInEditMode()) paint.setShadowLayer(shadowRadius, 0, 0, shadowColor);
    canvas.drawPath(path, paint);
    setLayerType(LAYER_TYPE_HARDWARE, null);
    paint.setShadowLayer(0, 0, 0, 0);
  }

  @Override protected void onDraw(Canvas canvas) {
    preDraw();
    canvas.drawBitmap(drawCache, 0, 0, bitmapPaint);
  }

  private void computePath() {
    path.reset();
    matrix.reset();

    float d = 2 * rad;
    rect.set(0, 0, d - triangleHeight, d - triangleHeight);

    path.addArc(rect, -90, 270);
    path.lineTo(0, 0);
    path.close();

    matrix.postRotate(45 + 180, 0, 0);
    matrix.postTranslate(padding + rad, 2 * rad);
    path.transform(matrix);
  }

  public void setColorFilter(ColorFilter colorFilter) {
    paint.setColorFilter(colorFilter);
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    this.width = w - 2 * padding;
    this.height = h - 2 * padding;
    cx = padding + w / 2;
    cy = padding + h / 2;
    rad = Math.min(width / 2, height / 2);
    triangleHeight = (float) (0.5 * (Math.sqrt(2) - 1) * 2 * rad);
    invalidateCache();
  }

  private void invalidateCache() {
    if (drawCache != null) {
      drawCache.recycle();
    }
    drawCache = null;
    path.reset();
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    invalidateCache();
  }
}
