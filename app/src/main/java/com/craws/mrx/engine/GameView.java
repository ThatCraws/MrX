package com.craws.mrx.engine;

import android.content.Context;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements Runnable {

    volatile boolean running;

    private Thread gameThread;

    public GameView(Context context) {
        super(context);
    }

    @Override
    public void run() {
        while(running) {
            // update the game state
            update();

            // draw the frame with updated state
            draw();

            // ???
            control();
        }
    }

    private void update() {

    }

    private void draw() {

    }

    private void control() {
        try {
            Thread.sleep(16, 666666); // game is supposed to run at 60fps TODO: Achieve proper frame pacing!
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        running = false;

        try {
            gameThread.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }
}
