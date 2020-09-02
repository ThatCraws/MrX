package com.craws.mrx.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.craws.mrx.R;
import com.craws.mrx.engine.GameView;
import com.craws.mrx.state.Place;

public class City implements Render {
    private GameView gameView;

    // Sprite information
    private Bitmap bitmap;
    // Size per frame
    private int frameSizeW;
    private int frameSizeH;
    // number of frames
    private static int framesW = 2;
    private static int framesH = 1;
    // drawing dimensions per frame (the bitmap will stay unmodified)
    private int width;
    private int height;
    // begin at the... beginning
    private int currFrame = 0;
    // viewport
    Rect viewport;

    // position in gameView
    private float x;
    private float y;
    Rect targetViewport;

    public City(final Context context, final GameView gameView, final float x, final float y) {
        this.gameView = gameView;

        this.x = x;
        this.y = y;

        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.city_sprites);
        // if no width or height is given keep the ones given to the bitmap (but still per frame)
        width = frameSizeW = bitmap.getWidth() / framesW;
        height = frameSizeH = bitmap.getHeight() / framesH;
    }

    public City(final Context context, final GameView gameView, final float x, final float y, final int width, final int height) {
        this.gameView = gameView;

        this.x = x;
        this.y = y;

        // if width and height are given the user chooses the width of the displayed frame not of the whole sprite
        this.width = width;
        this.height = height;

        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.city_sprites);

        frameSizeW = bitmap.getWidth() / framesW;
        frameSizeH = bitmap.getHeight() / framesH;
    }

    @Override
    public void update() {
        int srcX = (currFrame % framesW) * frameSizeW;
        int srcY = currFrame / framesH * frameSizeH;

        viewport = new Rect(srcX, srcY, srcX + frameSizeW, srcY + frameSizeH);
        targetViewport = new Rect((int)x, (int)y, (int)x + width, (int)y + height);
    }

    @Override
    public void draw(final Canvas canvas, final Paint paint) {
        update();

        canvas.drawBitmap(bitmap, viewport, targetViewport, paint);
    }

    @Override
    public void resize(final int width, final int height) {
        this.width = width;
        this.height = height;
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
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
