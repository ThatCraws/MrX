package com.craws.mrx.engine;

public class GameThread extends Thread {
    private GameView view;

    private boolean running;

    private static long fps = 30;

    public GameThread(GameView view) {
        this.view = view;
        running = false;
    }

    @Override
    public void run() {
        // Frame pacing
        long startTime;
        long sleepTime;
        long ticksPerFrame = 1000000000 / fps;

        while(running) {
            startTime = System.nanoTime();
            view.update();
            view.draw();
            sleepTime = System.nanoTime() - startTime;
            try {
                if (sleepTime < ticksPerFrame) {
                    sleep(ticksPerFrame / 1000000 - sleepTime / 1000000);
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
