package com.craws.mrx.graphics;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.craws.mrx.R;
import com.craws.mrx.state.Place;

public class City extends Render {
    private Place place;
    private boolean marked;

    // all the frames
    private static class FRAMES {
        final static int NORMAL = 0;
        final static int SELECTED = 1;
        final static int GOAL = 2;
        final static int GOAL_SELECTED = 3;

        final static int FRAMES_W = 2;
        final static int FRAMES_H = 2;
    }

    public City(final Context context, final Place place) {
        super(BitmapFactory.decodeResource(context.getResources(), R.drawable.city_sprites), FRAMES.FRAMES_W, FRAMES.FRAMES_H, 0f, 0f);
        this.place = place;
        marked = false;

        currFrame = place.isGoal() ? FRAMES.GOAL : FRAMES.NORMAL;
        updateViewport();
    }

    public City(final Context context, final Place place, final float x, final float y) {
        super(BitmapFactory.decodeResource(context.getResources() , R.drawable.city_sprites), FRAMES.FRAMES_W, FRAMES.FRAMES_H, x, y);
        this.place = place;
        marked = false;

        currFrame = place.isGoal() ? FRAMES.GOAL : FRAMES.NORMAL;
    }

    public City(final Context context, final Place place, final float x, final float y, final int width, final int height) {
        super(BitmapFactory.decodeResource(context.getResources(), R.drawable.city_sprites), FRAMES.FRAMES_W, FRAMES.FRAMES_H, x, y, width, height);
        this.place = place;
        marked = false;

        currFrame = place.isGoal() ? FRAMES.GOAL : FRAMES.NORMAL;
    }

    @Override
    public void draw(final Canvas canvas, final Paint paint) {
        super.draw(canvas, paint);

        canvas.drawText(place.getName(), targetViewport.left + getWidth() / 2f, targetViewport.bottom + 20, paint);
    }

    @Override
    public void update() {
        updateViewport();
    }

    public void select() {
        currFrame = place.isGoal() ? FRAMES.GOAL_SELECTED : FRAMES.SELECTED;
    }

    public void deselect() {
        currFrame = place.isGoal() ? FRAMES.GOAL : FRAMES.NORMAL;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(final boolean marked) {
        this.marked = marked;
    }

    public Place getPlace() {
        return place;
    }
}
