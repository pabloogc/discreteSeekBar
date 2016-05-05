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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;


public class MarkerDrawable extends Drawable {

    private final Matrix matrix = new Matrix();
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int padding = 10;
    private int width, height;
    private int cx, cy;
    private float rad;

    private Bitmap drawCache;

    public MarkerDrawable() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
    }

    private void preDraw() {
        if (drawCache != null) return;
        drawCache = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(drawCache);

        if (path.isEmpty()) {
            computePath();
        }
        canvas.drawColor(Color.GREEN);
        canvas.drawPath(path, paint);
    }

    @Override
    public void draw(Canvas canvas) {
        preDraw();
        canvas.drawBitmap(drawCache, 0, 0, bitmapPaint);
    }

    private void computePath() {
        path.reset();
        matrix.reset();

        float radCorrection = (float) (0.5 * (Math.sqrt(2) - 1) * rad);
        float correctedRad = rad - radCorrection;

        rect.set(0, 0, 2 * correctedRad, 2 * correctedRad);
        float[] corners = new float[]{correctedRad, correctedRad, correctedRad, correctedRad, correctedRad, correctedRad, 0, 0};
        path.addRoundRect(rect, corners, Path.Direction.CCW);

        matrix.postRotate(-45, correctedRad, correctedRad);
        matrix.postTranslate(padding, padding * 0.5f);
        path.transform(matrix);
    }

    @Override
    public void setAlpha(int alpha) {
        bitmapPaint.setAlpha(alpha);
    }

    @Override
    public int getAlpha() {
        return bitmapPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setSize(int w, int h) {
        this.width = w - 2 * padding;
        this.height = h - 2 * padding;
        cx = padding + w / 2;
        cy = padding + h / 2;
        rad = Math.min(width / 2, height / 2);
        invalidate();
    }

    public void setWidth(int w) {
        setSize(w, this.height);
    }

    public void setHeight(int h) {
        setSize(this.width, h);
    }

    public int getCircleCenterX() {
        return cx; //from the top left, cx is in the middle
    }

    public int getCircleCenterY() {
        return (int) rad; //from top left, the rad is the Y of the circle
    }

    @Override
    public int getIntrinsicWidth() {
        return width + 2 * padding;
    }

    @Override
    public int getIntrinsicHeight() {
        return height + 2 * padding;
    }

    public Path getPath() {
        if (path.isEmpty()) {
            computePath();
        }
        return path;
    }

    @Override
    public boolean isStateful() {
        return false;
    }

    public void invalidate() {
        if (drawCache != null) {
            drawCache.recycle();
        }
        drawCache = null;
        path.reset();
    }
}
