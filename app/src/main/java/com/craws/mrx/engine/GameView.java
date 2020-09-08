package com.craws.mrx.engine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.craws.mrx.GameActivity;
import com.craws.mrx.graphics.City;
import com.craws.mrx.graphics.Figure;
import com.craws.mrx.graphics.Render;
import com.craws.mrx.state.GameState;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Player;
import com.craws.mrx.state.Ticket;

import java.util.Arrays;
import java.util.Stack;
import java.util.Vector;


public class GameView extends SurfaceView {

    // ----------- App management -----------
    private GameActivity parent = null; // Gotta set this after instantiation =( Via setParent
    Context context;

    // ----------- game engine -----------
    private GameThread gameThread;
    private enum GAME_FLOW {
        MRX_CHOOSE_TURN,            // Move (clicking city) or ability (clicking ticket before city)
        MRX_CHOOSE_ABILITY_TICKETS, // after first Ticket is selected, selected the other two (with same ability, else abort and back to MRX_CHOOSE_TURN)
        MRX_EXTRA_TURN,
        MRX_SPECIAL,
        MRX_MOVE,                   // To move the figure and add to the timeline
        MRX_WIN_CHECK,              // Position check after Mr. X turn (he can't make himself lose). Make Mr. X disappear for detective's turn.
        MRX_THROW_TICKETS,          // Mr. X can throw as many tickets as he wants and then restock to 8 Tickets.
        DET_CHOOSE_CITY,            // All detectives have to move before being able to activate an ability
        DET_CHOOSE_TICKET,          // For travel
        DET_MOVE,
        DET_ABILITY,                // Round may not just end here. Either ask if ability should be activated or give a "end turn"-button.
        DET_CHOOSE_ABILITY_TICKETS, // Choose 3-5 tickets, so "finish"-button or something will be needed (Ability-buttons below Inventory?)
        DET_EXTRA_TURN,             // Choose City and travel there free of cost
        DET_SPECIAL,                // Retrieve Location From Timeline (and set the city-sprite accordingly).
        DET_WIN_CHECK,              // lame
        DET_THROW_TICKETS           // Even the detective-player is allowed to throw as many tickets as they want and restock (to 4 + No. of controlled detectives)
    }


    // ----------- game state -----------
    private GameState gameState;
    private City selectedCity;
    private Vector<Ticket> selectedTickets;

    // ----------- graphics -----------
    private SurfaceHolder surfaceHolder;
    private Canvas canvas;
    private Paint paint;
    private Stack<Render> renderStack;

    // To draw the world we divide the map into cells. 28x15 = 420, so that leaves a lot of room to place cities.
    // The original game has 200, but we will have less (50% of the map being cities would get too cluttered).
    final static int gridCellsX = 7;
    final static int gridCellsY = 5;

    final static float mapWidth = 3700;
    final static float mapHeight = 2000;


    // The number of Cities randomly put on the map
    final static int numberOfCities = 10;
    // True on the map means there is a City there
    boolean[][] positionMap = new boolean[gridCellsX][gridCellsY];

    // TODO REMOVE AFTER TESTING
    private int theOnePlayer = 0;

    /*  ---=============================================================================---
       -----===== Listener for resizing and Display-dependent scaling/positioning =====-----
        ---=============================================================================---
     */
    private class GameViewListener implements SurfaceHolder.Callback {


        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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

        scrollDetector = new GestureDetector(context, new ScrollListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        startGame();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        gameThread = new GameThread(this);

        scrollDetector = new GestureDetector(context, new ScrollListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        startGame();
    }

    private void startGame() {
        // ---=== The game-state to display ==---
        // for the map
        gameState = new GameState(context);
        selectedCity = null;
        selectedTickets = new Vector<>();

        // The things to draw (with)
        surfaceHolder = getHolder();
        paint = new Paint();
        renderStack = new Stack<>();

        // When the screen gets resized re-set the Places/Cities
        surfaceHolder.addCallback(new GameViewListener());

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

        /* ........................................................................................
           .____________....__________......_______....____________.............................................
           .|____   ____|...|   ______|.../.....___)...|____   ____|...........................................
           ......|  |.......|  |______ ...\.....(...........|  |..................................................
           ......|  |.......|   ______|....\.....\..........|  |....
           ......|  |.......|  |______.....).....)..........|  |....
           ......|__|.......|_________|...|.____/...........|__|....

         */

        float cellWidth;
        float cellHeight;

        selectedCity = null;
        selectedTickets = new Vector<>();

        renderStack.clear();

        pause();

        //update viewport
        dstViewport.set(0f, 0f, (float)getWidth(), (float)getHeight());

        // ----==== City Creation ====----
        // Divide the actual screen/this surfaceView.
        cellWidth = mapWidth/(float)gridCellsX;
        cellHeight = mapHeight/(float)gridCellsY;
        int nameCount = 0;

        for(int x = 0; x < positionMap.length ; x++) {
            for(int y = 0; y < positionMap[x].length; y++) {
                if(positionMap[x][y]) {
                    Place currentlyAdded = gameState.buildPlace("City" + (nameCount++ + 1), nameCount == 1);
                    City newRenderCity = currentlyAdded.getCity();
                    newRenderCity.moveTo(cellWidth * x, cellHeight * y);


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
                    newRenderCity.moveTo(cellWidth * x + (cellWidth - newRenderCity.getWidth()) / 2, cellHeight * y + (cellHeight - newRenderCity.getHeight()) / 2);
                    renderStack.add(newRenderCity);

                    if(nameCount == 2) {
                        gameState.addDetective("The not chosen one");
                        theOnePlayer = gameState.addDetective("The chosen one");
                        gameState.getPlayerByPort(theOnePlayer).setPlace(currentlyAdded);
                        Figure playerOne = gameState.getPlayerByPort(theOnePlayer).getFigure();
                        playerOne.snapToCurrentCity();
                    }
                }
            }
        }

        resume();

        //---------------------------------------------------------------------------------------------------------------
        //---------------------------------------------------------------------------------------------------------------
    }


    /*  ---===============================================---
       -----===== The Camera and Map implementation =====-----
        ---===============================================---
        See https://developer.android.com/training/gestures/scale#java
     */
    // The rectangle in which the part of the map we're looking at is displayed
    private RectF dstViewport = new RectF();

    // The position we move the map to (or rather the canvas' Matrix meaning moving to the right on the map is moving the Matrix to the left)
    private float viewPortX = 0f;
    private float viewPortY = 0f;

    // The further we zoom in the bigger the map gets scaled (and the smaller the viewport)
    private float mapScaleFactor = 1f;

    // When you scroll or scale you don't click
    private boolean scrolling = false;
    private boolean scaling = false;

    private class ScrollListener implements GestureDetector.OnGestureListener {

        // Every Gesture starts with ACTION_DOWN, so we return true here to be able to process the Scoll-Gesture
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {

            if(!scaling) {
                // Bounds x-Axis
                if (viewPortX - distanceX > 0) {
                    viewPortX = 0;
                } else if (viewPortX - distanceX < ((mapWidth * mapScaleFactor) - (getWidth())) * -1) {
                    viewPortX = ((mapWidth * mapScaleFactor) - (getWidth())) * -1;
                } else {
                    viewPortX -= distanceX;
                }

                // Bounds Y-Axis
                if (viewPortY - distanceY > 0) {
                    viewPortY = 0;
                } else if (viewPortY - distanceY < ((mapHeight * mapScaleFactor) - (getHeight())) * -1) {
                    viewPortY = ((mapHeight * mapScaleFactor) - (getHeight())) * -1;
                } else {
                    viewPortY -= distanceY;
                }

                scrolling = true;
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override public boolean onScale(ScaleGestureDetector detector) {

            float oldScaleFactor = mapScaleFactor;

            mapScaleFactor *= detector.getScaleFactor();
            mapScaleFactor = Math.max(0.4f, Math.min(mapScaleFactor, 2.0f));

            // to scale around the center of the screen and not fly across the map while scrolling
            // but don't scroll beyond bounds (just like in onScroll
            if (viewPortX - ((mapScaleFactor * mapWidth) - (oldScaleFactor * mapWidth)) / 2 > 0) {
                viewPortX = 0;
            } else if (viewPortX - ((mapScaleFactor * mapWidth) - (oldScaleFactor * mapWidth)) / 2 < ((mapWidth * mapScaleFactor) - (getWidth())) * -1) {
                viewPortX = ((mapWidth * mapScaleFactor) - (getWidth())) * -1;
            } else {
                viewPortX -= ((mapScaleFactor * mapWidth) - (oldScaleFactor * mapWidth)) / 2;
            }

            // Bounds Y-Axis
            if (viewPortY - ((mapScaleFactor * mapHeight) - (oldScaleFactor * mapHeight)) / 2 > 0) {
                viewPortY = 0;
            } else if (viewPortY - ((mapScaleFactor * mapHeight) - (oldScaleFactor * mapHeight)) / 2 < ((mapHeight * mapScaleFactor) - (getHeight())) * -1) {
                viewPortY = ((mapHeight * mapScaleFactor) - (getHeight())) * -1;
            } else {
                viewPortY -= ((mapScaleFactor * mapHeight) - (oldScaleFactor * mapHeight)) / 2;
            }

            invalidate();

            scaling = true;

            return true;
        }
    }

    // initialized in lines 158
    GestureDetector scrollDetector;
    ScaleGestureDetector scaleDetector;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        scrollDetector.onTouchEvent(e);

        if (e.getAction() == MotionEvent.ACTION_UP) {
            if(!scrolling && !scaling) {
                for (Place place : gameState.getPlaces()) {
                    if (place.getCity().collisionCheck((e.getX() + (-viewPortX)) * (1 / mapScaleFactor), (e.getY() + (-viewPortY)) * (1 / mapScaleFactor))) {
                        if (selectedCity != null) {
                            selectedCity.reset();
                        }
                        selectedCity = place.getCity();
                        selectedCity.select();
                    }
                }
            } else {
                scrolling = false;
                scaling = false;
            }
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return false;
    }

    protected void update() {
        for(Place toUpdate: gameState.getPlaces()) {
            toUpdate.getCity().update();
        }
        for(Player toUpdate: gameState.getPlayers()) {
            toUpdate.getFigure().update();
        }
        if(gameState.getMrX() != null) {
            gameState.getMrX().getFigure().update();
        }
    }

    private Paint txtPaint = new Paint();
    // --- Map (Debug/Testing) ---
    // private Paint onlyBorders = new Paint();
    // float massstab = 8f;

    private Ticket lastUsed;

    public void setLastUsed(Ticket lastUsed) {
        this.lastUsed = lastUsed;
    }

    public void moveBitch() {
        if (selectedCity != null) {
            gameState.getPlayerByPort(theOnePlayer).setPlace(selectedCity.getPlace());
            selectedCity.reset();
            selectedCity = null;
        }
    }

    protected void draw() {
        if(surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

            dstViewport.set(0, 0, getWidth(), getHeight());
            canvas.clipRect(dstViewport);

            canvas.translate(viewPortX, viewPortY);

            canvas.scale(mapScaleFactor, mapScaleFactor);

            canvas.drawColor(Color.WHITE);

            for(Place toUpdate: gameState.getPlaces()) {
                toUpdate.getCity().draw(canvas, paint);
            }
            for(Player toUpdate: gameState.getPlayers()) {
                toUpdate.getFigure().draw(canvas, paint);
            }
            if(gameState.getMrX() != null) {
                gameState.getMrX().getFigure().draw(canvas, paint);
            }

            txtPaint.setTextSize(72);

            // --- Map (Debug/Testing) ---
            // onlyBorders.setStyle(Paint.Style.STROKE);
            // onlyBorders.setStrokeWidth(15);
            // canvas.drawRect((-viewPortX + getWidth() - (mapWidth / massstab)), -viewPortY, -viewPortX + getWidth(), -viewPortY + (mapHeight / massstab), onlyBorders);
            // canvas.drawRect((-viewPortX + getWidth() - (mapWidth / massstab)) + (-viewPortX / massstab), -viewPortY + (-viewPortY / massstab), (-viewPortX + getWidth() - (mapWidth / massstab)) + (-viewPortX / massstab) + (getWidth() / massstab * (1 / mapScaleFactor)), -viewPortY + (-viewPortY / massstab) + (getHeight() / massstab), onlyBorders);

            if(lastUsed != null) {
                canvas.drawText("Last ticket used:", 1000, 650, txtPaint);
                canvas.drawText("Vehicle: " + lastUsed.getVehicle(), 1000, 800, txtPaint);
                canvas.drawText("Ability: " + lastUsed.getAbility(), 1000, 950, txtPaint);
            }

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

    /** Blacks out screen and shows message until screen is touched.
     * To communicate with human players and hide the screen from detective before Mr. X takes over.
     *
     * @param message The message to show the player until he touches the screen.
     */
    public void showMessageAndWaitForClick(final String message) {
        // TODO: IMPLEMENT. Don't forget to ignore the touch for the gameplay
    }

    // ----------- GETTERS -----------

    public GameState getGameState() {
        return gameState;
    }

    public void setParent(final GameActivity parent) {
        this.parent = parent;
    }
}
