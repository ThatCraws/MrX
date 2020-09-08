package com.craws.mrx.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public abstract class Render {
    // Sprite information
    private final Bitmap bitmap;
    // Size per frame
    private final int frameSizeW;
    private final int frameSizeH;
    // number of frames
    private final int framesW;
    private final int framesH;
    // drawing dimensions per frame (the bitmap will stay unmodified)
    private int width;
    private int height;
    // all the frames
    protected int currFrame;

    // viewport
    protected Rect viewport;

    // position in gameView
    private float x;
    private float y;
    Rect targetViewport;

    public Render(final Bitmap bitmap, final int framesW, final int framesH, final float x, final float y) {
        this.bitmap = bitmap;

        this.framesW = framesW;
        this.framesH = framesH;

        this.x = x;
        this.y = y;

        // if no width or height is given keep the ones given to the bitmap (but still per frame)
        width = frameSizeW = bitmap.getWidth() / framesW;
        height = frameSizeH = bitmap.getHeight() / framesH;
    }

    public Render(final Bitmap bitmap, final int framesW, final int framesH, final float x, final float y, final int width, final int height) {
        this.bitmap = bitmap;

        this.framesW = framesW;
        this.framesH = framesH;

        this.x = x;
        this.y = y;

        // if width and height are given the user chooses the width of the displayed frame not of the whole sprite
        this.width = width;
        this.height = height;

        frameSizeW = bitmap.getWidth() / framesW;
        frameSizeH = bitmap.getHeight() / framesH;
    }

    public void draw(final Canvas canvas, final Paint paint) {
        canvas.drawBitmap(bitmap, viewport, targetViewport, paint);
    }

    protected void updateViewport() {
        if((float)currFrame / (float)framesW > (float)framesH) {
            return;
        }

        int srcX = (currFrame % framesW) * frameSizeW;
        int srcY = currFrame / framesW * frameSizeH;

        viewport = new Rect(srcX, srcY, srcX + frameSizeW, srcY + frameSizeH);
        targetViewport = new Rect((int)x, (int)y, (int)x + width, (int)y + height);
    }

    // Should call updateViewport
    public abstract void update();

    public boolean collisionCheck(final float x, final float y) {
        // I could use the intern x and y points too, but left and right is easier to read
        return ((targetViewport.left < x && x < targetViewport.right) && (targetViewport.top < y && y < targetViewport.bottom));
    }

    public void resize(final int width, final int height) {
        this.width = width;
        this.height = height;
    }

    public void moveTo(final float x, final float y) {
        this.x = x;
        this.y = y;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
