package com.craws.mrx.graphics;

import android.graphics.Bitmap;

public interface Render {

    void update();
    Bitmap getBitmap();
    int getX();
    int getY();
}
