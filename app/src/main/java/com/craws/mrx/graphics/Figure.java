package com.craws.mrx.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.craws.mrx.state.Place;
import com.craws.mrx.state.Player;

public class Figure implements Render {
    private Player player;

    private Bitmap bitmap;

    private int x;
    private int y;

    private int width;
    private int height;

    public Figure(Context context, int port, String alias, Place startPosition) {
        player = new Player(port, alias, startPosition);

        x = 0;
        y = 0;

        width = 59;
        height = 117;

        switch(player.getPort()) {
            case 0:
                bitmap = BitmapFactory.decodeResource(context.getResources(),com.craws.mrx.R.drawable.mrx);
                break;
            case 1:
                bitmap = BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det1);
                break;
            case 2:
                bitmap = BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det2);
                break;
            case 3:
                bitmap = BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det3);
                break;
            case 4:
                bitmap = BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det4);
                break;
            case 5:
                bitmap = BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det5);
                break;
        }
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

    }

    public void update() {
        // TODO: Don't teleport. Let the Figure move to the destination.
        // x = player.getCurrPlace().getGraphic().getX() - (bitmap.getWidth() / 2) + (player.getCurrPlace().getGraphic().getBitmap().getWidth() / 2);
        // y = player.getCurrPlace().getGraphic().getY() - (bitmap.getHeight() ) + (player.getCurrPlace().getGraphic().getBitmap().getHeight() / 2);
    }

    public Player getPlayer() {
        return player;
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
