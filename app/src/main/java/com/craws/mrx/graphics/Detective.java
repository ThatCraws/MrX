package com.craws.mrx.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.craws.mrx.state.Place;
import com.craws.mrx.state.Player;

public class Detective {
    private Player detective;

    private Bitmap bitmap;

    private int x;
    private int y;

    public Detective(Context context, int port, String alias, Place startPosition) {
        detective = new Player(port, alias, startPosition);

        x = 0;
        y = 0;

        switch(detective.getPort()) {
            case 0:
                bitmap = BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.det0);
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
        }

    }

    public Player getDetective() {
        return detective;
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
