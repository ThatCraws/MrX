package com.craws.mrx.engine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.craws.mrx.graphics.City;
import com.craws.mrx.graphics.Render;
import com.craws.mrx.state.GameState;
import com.craws.mrx.state.Place;

import java.util.Arrays;
import java.util.Stack;

public class GameView extends SurfaceView {

    // ----------- App management -----------
    Context context;

    // ----------- game engine -----------
    private GameThread gameThread;

    // ----------- game state -----------
    private GameState gameState;

    // ----------- graphics -----------
    private SurfaceHolder surfaceHolder;
    private Canvas canvas;
    private Paint paint;
    private Stack<Render> renderStack;

    // To draw the world we divide the map into cells. 28x15 = 420, so that leaves a lot of room to place cities.
    // The original game has 200, but we will have less (50% of the map being cities would get too cluttered).
    final static int gridCellsX = 28;
    final static int gridCellsY = 15;

    // The number of Cities randomly put on the map
    final static int numberOfCities = 150;
    // True on the map means there is a City there
    boolean[][] positionMap = new boolean[gridCellsX][gridCellsY];


    /*  ---=============================================================================---
       -----===== Listener for resizing and Display-dependent scaling/positioning =====-----
        ---=============================================================================---
     */
    private class GameViewListener implements SurfaceHolder.Callback {
        private final GameView myParent;

        float cellWidth;
        float cellHeight;

        boolean[][] theMap;

        public GameViewListener(final GameView myParent, final boolean[][] theMap) {
            this.myParent = myParent;
            this.theMap = theMap;

            cellWidth = 0;
            cellHeight = 0;
        }


        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            renderStack.clear();

            pause();

            // ----==== City Creation ====----
            // Divide the actual screen/this surfaceView.
            cellWidth = (float)getWidth()/(float)gridCellsX;
            cellHeight = (float)getHeight()/(float)gridCellsY;
            int nameCount = 0;

            for(int x = 0; x < positionMap.length ; x++) {
                for(int y = 0; y < positionMap[x].length; y++) {
                    if(positionMap[x][y]) {
                        Place currentlyAdded = gameState.buildPlace("City" + (nameCount++ + 1), false);
                        City newRenderCity = new City(context, myParent, (cellWidth * x), (cellHeight * y));

                        // Scale to grid
                        // get the ratio of city-bitmap to grid-size for width and height individually
                        float scaleFactorW = cellWidth / newRenderCity.getWidth();
                        float scaleFactorH = cellHeight / newRenderCity.getHeight();

                        // if the width has to be "scaled more" than height to fit into the grid use its scale-factor/ratio
                        if(scaleFactorW < scaleFactorH) {
                            newRenderCity.resize((int)((newRenderCity.getWidth() * scaleFactorW) * .9f), (int)((newRenderCity.getHeight() * scaleFactorW) * .9f));
                        } else {
                            newRenderCity.resize((int)((newRenderCity.getWidth() * scaleFactorH) * .9f), (int)((newRenderCity.getHeight() * scaleFactorH) * .9f));
                        }
                        newRenderCity.setX((cellWidth * x) + ((cellWidth - newRenderCity.getWidth()) / 2));
                        newRenderCity.setY((cellHeight * y) + ((cellHeight - newRenderCity.getHeight()) / 2));
                        renderStack.add(newRenderCity);
                    }
                }
            }
            resume();
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


    public GameView(Context context) {
        super(context);
        this.context = context;
        gameThread = new GameThread(this);

        startGame();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        gameThread = new GameThread(this);

        startGame();
    }

    private void startGame() {
        // --- Initializing some Vars ---
        // The game-state to display
        gameState = new GameState();

        // The things to draw (with)
        surfaceHolder = getHolder();
        paint = new Paint();
        renderStack = new Stack<>();

        // When the screen gets resized re-set the Places/Cities
        surfaceHolder.addCallback(new GameViewListener(this, positionMap));

        // The grid we put our Cities in. The map is empty for now
        for (boolean[] currColumn : positionMap) {
            Arrays.fill(currColumn, false);
        }

        // Randomly fill the grid with Cities
        int cityCount = 0;
        while(cityCount < numberOfCities) {
            int randomX = (int)Math.round(Math.random()*(gridCellsX - 1));
            int randomY = (int)Math.round(Math.random()*(gridCellsY - 1));

            if(!positionMap[randomX][randomY]) {
                positionMap[randomX][randomY] = true;

                cityCount++;
            }
        }
        //positionMap[0][0] = true;
        //positionMap[0][1] = true;
        //positionMap[1][0] = true;
        //positionMap[1][1] = true;
    }

    protected void update() {
        //for(Render toUpdate: renderStack) {
        //    toUpdate.update();
        //}
    }

    protected void draw() {
        if(surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

            canvas.drawColor(Color.WHITE);

            for(Render toRender: renderStack) {
                toRender.draw(canvas, paint);
            }

            canvas.drawText("Dim: " + getWidth() + "x" + getHeight(), 20, 650, paint);

            surfaceHolder.unlockCanvasAndPost(canvas);
        }

    }

    public void pause() {
        gameThread.setRunning(false);
        try {
            gameThread.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        gameThread = new GameThread(this);
        gameThread.setRunning(true);
        gameThread.start();
    }

    // ----------- GETTERS -----------

    public GameState getGameState() {
        return gameState;
    }
}
