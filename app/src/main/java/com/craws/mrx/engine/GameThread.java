package com.craws.mrx.engine;

public class GameThread extends Thread {
    private GameView view;

    private boolean running;

    private static long fps = 10;

    public GameThread(GameView view) {
        this.view = view;
        running = false;
    }

    @Override
    public void run() {
        // Frame pacing
        long startTime;
        long sleepTime;
        long ticksPerFrame = 1000 / fps;

        while(running) {
            startTime = System.currentTimeMillis();
            view.draw();
            sleepTime = System.currentTimeMillis() - startTime;
            try {
                if (sleepTime < ticksPerFrame) {
                    sleep(ticksPerFrame - sleepTime);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
