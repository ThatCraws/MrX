package com.craws.mrx.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.craws.mrx.state.Place;

public class City implements Render {

    private Bitmap bitmap;

    private int x;
    private int y;

    private  int width = 80;
    private  int height = 40;

    public City(Context context, int x, int y) {

        this.x = x;
        this.y = y;

        bitmap = BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.city);
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    public void update() {
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}
