package com.craws.mrx.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.craws.mrx.state.Place;
import com.craws.mrx.state.Player;

public class Figure implements Render {
    private Context context;
    private Player player;

    private Bitmap originalBitmap;
    private Bitmap bitmap;

    private float x;
    private float y;

    /* ---------========== ATTENTION PLEASE!!! ==========---------
    TODO: REWORK THIS WHOLE SHIT LIKE YOU DID WITH THE CITY-CLASS AND/OR MAYBE MAKE RENDER A CLASS SO YOU CAN SHORTEN ALL SUBCLASSES BY A LOT LOT.

     */
    public Figure(Context context, int port, String alias, Place startPosition) {
        this.context = context;

        player = new Player(port, alias, startPosition);

        x = 0f;
        y = 0f;

        final int width = 59;
        final int height = 117;

        originalBitmap = getRightResource();
    }

    public Figure(Context context, int port, String alias, Place startPosition, final int width, final int height) {
        this.context = context;

        player = new Player(port, alias, startPosition);

        x = 0f;
        y = 0f;

        originalBitmap = getRightResource();
    }

    private Bitmap getRightResource() {
        switch(player.getPort()) {
            case 0:
                return BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.mrx);
            case 1:
                return BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det1);
            case 2:
                return BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det2);
            case 3:
                return BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det3);
            case 4:
                return BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det4);
            case 5:
                return BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det5);
            default:
                return null; // TODO: Get "ERROR"-Bitmap
        }
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {

    }

    @Override
    public void update() {
        // TODO: Don't teleport. Let the Figure move to the destination.
    }

    public Player getPlayer() {
        return player;
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
    public float getY() {
        return y;
    }

    @Override
    public void setX(final float x) {
        this.x = x;
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
