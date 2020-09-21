package com.craws.mrx.engine;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;



public class GameView extends SurfaceView {

    // ----------- App management -----------
    private Context context;
    private GameThread gameThread;

    // ----------- graphics -----------
    private SurfaceHolder surfaceHolder;
    private Canvas canvas;



    public final static float DEFAULT_TXT_SIZE = 36;
    public final static float FIGURE_SCALE_FACTOR = .65f;


    // --- Listener ---
    private GameViewListener gameViewListener;

    private GestureDetector scrollDetector;
    private ScaleGestureDetector scaleDetector;

    public GameView(Context context) {
        super(context);
        this.context = context;

        scrollDetector = new GestureDetector(context, new ScrollListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(new GameViewSurfaceListener());

        gameThread = new GameThread(this);
        gameThread.start();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        scrollDetector = new GestureDetector(context, new ScrollListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(new GameViewSurfaceListener());

        gameThread = new GameThread(this);
        gameThread.start();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            if (gameViewListener != null) {
                invalidate();
                return gameViewListener.onScale(detector);
            }

            return true;
        }
    }

    private class ScrollListener implements GestureDetector.OnGestureListener {

        // Every Gesture starts with ACTION_DOWN, so we return true here to be able to process the Scroll-Gesture
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
            if (gameViewListener != null) {
                return gameViewListener.onScroll(event1, event2, distanceX, distanceY);
            }
            return true;
        }


        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        // stubs
        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean forwardClick = true;

        scaleDetector.onTouchEvent(e);
        scrollDetector.onTouchEvent(e);
        if(gameViewListener != null) {
            forwardClick = gameViewListener.onTouchAction(e);
        }

        if(forwardClick) {
            performClick();
        }

        return forwardClick;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return false;
    }

    protected void update() {
        if(gameViewListener != null) {
            gameViewListener.onUpdate();
        }
    }

    // --- Map (Debug/Testing) ---
    // private Paint onlyBorders = new Paint();
    // float massstab = 8f;

    protected void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

            if (gameViewListener != null) {
                gameViewListener.onDraw(canvas);
            }
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }


    public void pause() {
        gameThread.setRunning(false);
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        gameThread = new GameThread(this);
        gameThread.setRunning(true);
        gameThread.start();
    }


    // ----------- LISTENER -----------
    public void setTouchListener(final GameViewListener listener) {
        gameViewListener = listener;
    }


    /*  ---=============================================================================---
      -----===== Listener for resizing and Display-dependent scaling/positioning =====-----
       ---=============================================================================---
    */
    private class GameViewSurfaceListener implements SurfaceHolder.Callback {


        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(gameViewListener != null) {
                gameViewListener.onSurfaceChanged(width, height);
            }

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;

            gameThread.setRunning(false);

            while (retry) {
                try {
                    gameThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    /*  ---=============================================================================---  */
}