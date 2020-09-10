package com.craws.mrx.graphics;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.craws.mrx.GameActivity;
import com.craws.mrx.R;
import com.craws.mrx.engine.GameView;
import com.craws.mrx.state.Place;

public class City extends Render {
    private Place place;

    // all the frames
    private static class FRAMES {
        final static int NORMAL = 0;
        final static int SELECTED = 1;
        final static int MARKED = 2;
        final static int GOAL = 4;
        final static int GOAL_SELECTED = 5;
    }

    public City(final Context context, final Place place) {
        super(BitmapFactory.decodeResource(context.getResources(), R.drawable.city_sprites), 4, 2, 0f, 0f);
        this.place = place;

        currFrame = place.isGoal() ? FRAMES.GOAL : FRAMES.NORMAL;
    }

    public City(final Context context, final Place place, final float x, final float y) {
        super(BitmapFactory.decodeResource(context.getResources() , R.drawable.city_sprites), 4, 2, x, y);
        this.place = place;

        currFrame = place.isGoal() ? FRAMES.GOAL : FRAMES.NORMAL;
    }

    public City(final Context context, final Place place, final float x, final float y, final int width, final int height) {
        super(BitmapFactory.decodeResource(context.getResources(), R.drawable.city_sprites), 4, 2, x, y, width, height);
        this.place = place;

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

    public void reset() {
        currFrame = place.isGoal() ? FRAMES.GOAL : FRAMES.NORMAL;
    }

    public void mark() {
        // Goals won't be marked, because only places which Mr. X visited in the last few rounds will be marked.
        currFrame = place.isGoal() ? currFrame : FRAMES.MARKED;
    }

    public Place getPlace() {
        return place;
    }
}
