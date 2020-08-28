package com.craws.mrx.graphics;

import android.graphics.Bitmap;

public interface Render {

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
