package com.craws.mrx.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public interface Render {

    void draw(final Canvas canvas, final Paint paint);
    void update();
    Bitmap getBitmap();

    void setX(final float x);
    void setY(final float y);
    float getX();
    float getY();

    void resize(final int width, final int height);
    int getWidth();
    int getHeight();
}
