package com.craws.mrx.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.craws.mrx.state.Place;

public class City implements Render {
    private Place place;

    private Bitmap bitmap;

    private int x;
    private int y;

    private  int width = 80;
    private  int height = 40;

    public City(Context context, String name, int x, int y) {
        place = new Place(this, name);

        this.x = x;
        this.y = y;

        bitmap = BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.city);
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    public void update() {
    }

    public Place getPlace() {
        return place;
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
