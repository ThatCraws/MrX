package com.craws.mrx.engine;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public interface GameViewListener {
    boolean onTouchAction(MotionEvent e);

    boolean onScale(ScaleGestureDetector detector);
    boolean onScroll(final MotionEvent event1, final MotionEvent event2, final float distanceX, final float distanceY);

    void onSurfaceChanged(final int width, final int height);

    void onDraw(final Canvas canvas);
    void onUpdate();
}
