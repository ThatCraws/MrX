package com.craws.mrx.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.craws.mrx.state.Place;

public class City implements Render {

    private Bitmap originalBitmap;
    private Bitmap bitmap;

    private float x;
    private float y;

    // don't touch here
    private Place
        place;

    public City(final Context context, final Place place, final float x, final float y) {
        this.place = place;

        this.x = x;
        this.y = y;

        int width = 80;
        int height = 40;

        originalBitmap = BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.city);
        bitmap = originalBitmap;
    }

    public City(final Context context, final Place place, final float x, final float y, final int width, final int height) {
        this.place = place;

        this.x = x;
        this.y = y;

        bitmap = BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.city);
        bitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);
    }

    @Override
    public void update() {
    }

    @Override
    public Bitmap getBitmap() {
        return bitmap;
    }


    @Override
    public float getX() {
        return x;
    }

    @Override
    public void setX(final float x) {
        this.x = x;
    }


    @Override
    public float getY() {
        return y;
    }

    @Override
    public void setY(final float y) {
        this.y = y;
    }


    @Override
    public int getWidth() {
        return bitmap.getWidth();
    }

    @Override
    public int getHeight() {
        return bitmap.getHeight();
    }

    @Override
    public void resize(final int width, final int height) {
        bitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);
    }
}
