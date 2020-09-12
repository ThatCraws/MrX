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

import androidx.annotation.ColorInt;

import com.craws.mrx.graphics.City;
import com.craws.mrx.graphics.Render;
import com.craws.mrx.state.GameState;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Player;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Vehicle;
import com.craws.tree.Edge;

import java.util.Stack;


public class GameView extends SurfaceView {

    // ----------- App management -----------
    private Context context;
    private GameThread gameThread;
    // --- Phase Management ---
    private OnPhaseChangeListener phaseChangeListener;
    private GAME_PHASE currPhase;
    private GAME_PHASE nextPhase;
    private boolean continuable;

    public enum GAME_PHASE {
        INTERRUPTED,                // No functionality of the game is given (mainly to display messages to human player)
        MRX_CHOOSE_TURN,            // Move (clicking city) or ability (clicking ticket before city)
        MRX_CHOOSE_ABILITY_TICKETS, // after first Ticket is selected, selected the other two (with same ability, else abort and back to MRX_CHOOSE_TURN)
        MRX_EXTRA_TURN,
        MRX_SPECIAL,
        MRX_CHOOSE_CITY,
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

    // To be displayed by showMessageAndWaitForClick
    private String userMessage = "";

    // ----------- game state/map -----------
    private GameState gameState;
    private City selectedCity;
    private InventoryChangeListener inventoryChangeListener;
    private TimelineChangeListener timelineChangeListener;

    // ----------- graphics -----------
    private SurfaceHolder surfaceHolder;
    private Canvas canvas;
    private Stack<Render> renderStack;
    private Paint paint;

    public final static float DEFAULT_TXT_SIZE = 36;
    public final static float FIGURE_SCALE_FACTOR = .65f;

    final static float mapWidth = 3700;
    final static float mapHeight = 2000;

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
            dstViewport.set(0, 0, getWidth(), getHeight());
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

        scrollDetector = new GestureDetector(context, new ScrollListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        setupGame();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        scrollDetector = new GestureDetector(context, new ScrollListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        setupGame();
    }

    private void setupGame() {
        // ---=== The game-state to display ==---
        continuable = true;
        // for the map
        gameState = new GameState(context);
        selectedCity = null;

        // The things to draw (with)
        surfaceHolder = getHolder();
        paint = new Paint();
        paint.setTextSize(DEFAULT_TXT_SIZE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStrokeWidth(20f);

        renderStack = new Stack<>();

        // When the screen gets resized re-set the Places/Cities
        surfaceHolder.addCallback(new GameViewListener());

        selectedCity = null;

        //update viewport
        dstViewport.set(0f, 0f, (float)getWidth(), (float)getHeight());

        Place pl_bremen = gameState.buildPlace("Bremen", false, 100, 200);
        Place pl_hanno = gameState.buildPlace("Hannover", false, 200, 550);
        Place pl_pig = gameState.buildPlace("Pig", false, 400, 400);
        Place pl_murica = gameState.buildPlace("Murica", true, 500, 600);

        gameState.buildStreet(pl_pig, pl_bremen, Vehicle.FAST);
        gameState.buildStreet(pl_pig, pl_hanno, Vehicle.MEDIUM);
        gameState.buildStreet(pl_pig, pl_murica, Vehicle.SLOW);

        int det = gameState.addDetective("Detestive", pl_hanno);
        int mrx = gameState.addMrX(pl_bremen);

        Player detective = gameState.getPlayerByPort(det);
        Player misterX = gameState.getPlayerByPort(mrx);

        gameThread = new GameThread(this);
    }

    public void startGame() {
        showMessageAndWaitForClick("Mr. X's turn. Detectives don't look.", GAME_PHASE.MRX_CHOOSE_TURN);

        fillInventoryX();
    }

    private void fillInventory() {
        while(gameState.getInventory().size() < 4 + gameState.getPlayers().size()) {
            Ticket toAdd = gameState.drawTicket();
            gameState.giveTicket(1, toAdd);
            if(inventoryChangeListener != null) {
                inventoryChangeListener.onAdd(toAdd);
            }
        }
    }

    private void fillInventoryX() {
        while(gameState.getInventoryX().size() < 8) {
            Ticket toAdd = gameState.drawTicket();
            gameState.giveTicket(0, toAdd);
            if(inventoryChangeListener != null) {
                inventoryChangeListener.onAdd(toAdd);
            }
        }
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

    // initialized in line 158
    GestureDetector scrollDetector;
    ScaleGestureDetector scaleDetector;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // If a user message is displayed
        if(currPhase == GAME_PHASE.INTERRUPTED) {
            // When the text is fully displayed and the user is allowed to continue
            if(continuable) {
                changePhase(nextPhase);
                nextPhase = null;
                return false;
            }
        } else {

            scaleDetector.onTouchEvent(e);
            scrollDetector.onTouchEvent(e);

            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (!scrolling && !scaling) {
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

    // --- Map (Debug/Testing) ---
    // private Paint onlyBorders = new Paint();
    // float massstab = 8f;
    @ColorInt int currColor = 255;

    protected void draw() {
        if(surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

            if(currPhase == GAME_PHASE.INTERRUPTED) {

                final int increment = 10;
                if(currColor - increment > 0) {
                    canvas.drawColor(Color.rgb(currColor, currColor, currColor));
                    currColor -= increment;
                } else {
                    // paint it, black
                    canvas.drawColor(Color.BLACK);

                    // remember color now
                    @ColorInt int prevColor = paint.getColor();
                    // set color to white
                    paint.setColor(Color.WHITE);
                    Paint smallerTxtPaint = new Paint(paint);
                    smallerTxtPaint.setTextSize(DEFAULT_TXT_SIZE * 2 / 3);
                    smallerTxtPaint.setAlpha(255 / 3 * 2);

                    // text in white
                    canvas.drawText(userMessage, getWidth() / 2f, getHeight() / 2f, paint);
                    canvas.drawText("- Touch to continue -", getWidth() / 2f, getHeight() - 75, smallerTxtPaint);
                    // reset color
                    paint.setColor(prevColor);

                    continuable = true;
                }
            } else {

                // Set Viewport and map-position
                canvas.clipRect(dstViewport);
                canvas.translate(viewPortX, viewPortY);
                canvas.scale(mapScaleFactor, mapScaleFactor);
                // Start of by clearing the old picture with a new coat of white
                canvas.drawColor(Color.WHITE);

                // save color so we can restore it later (will/should be black)
                @ColorInt int prevColor = paint.getColor();
                // Draw streets
                for (Edge<Place, Vehicle> currStreet : gameState.getStreets()) {
                    City start = currStreet.getSource().getData().getCity();
                    City target = currStreet.getTarget().getData().getCity();

                    float startX = start.getX() + start.getWidth() / 2f;
                    float startY = start.getY() + start.getHeight() / 2f;

                    float targetX = target.getX() + target.getWidth() / 2f;
                    float targetY = target.getY() + target.getHeight() / 2f;

                    switch (gameState.getStreet(start.getPlace(), target.getPlace())) {
                        case SLOW:
                            paint.setColor(Color.RED);
                            break;
                        case MEDIUM:
                            paint.setColor(Color.YELLOW);
                            break;
                        case FAST:
                            paint.setColor(Color.GREEN);
                    }
                    canvas.drawLine(startX, startY, targetX, targetY, paint);
                }
                paint.setColor(prevColor);

                // Draw Cities
                for (Place toDraw : gameState.getPlaces()) {
                    toDraw.getCity().draw(canvas, paint);
                }
                // Draw detective Figures
                for (Player toDraw : gameState.getPlayers()) {
                    toDraw.getFigure().draw(canvas, paint);
                }
                // Draw Mr. X Figure
                if (gameState.getMrX() != null) {
                    gameState.getMrX().getFigure().draw(canvas, paint);
                }

                // --- Map (Debug/Testing, unfinished) ---
                // onlyBorders.setStyle(Paint.Style.STROKE);
                // onlyBorders.setStrokeWidth(15);
                // canvas.drawRect((-viewPortX + getWidth() - (mapWidth / massstab)), -viewPortY, -viewPortX + getWidth(), -viewPortY + (mapHeight / massstab), onlyBorders);
                // canvas.drawRect((-viewPortX + getWidth() - (mapWidth / massstab)) + (-viewPortX / massstab), -viewPortY + (-viewPortY / massstab), (-viewPortX + getWidth() - (mapWidth / massstab)) + (-viewPortX / massstab) + (getWidth() / massstab * (1 / mapScaleFactor)), -viewPortY + (-viewPortY / massstab) + (getHeight() / massstab), onlyBorders);
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
    public void showMessageAndWaitForClick(final String message, final GAME_PHASE phaseAfter) {
        changePhase(GAME_PHASE.INTERRUPTED);
        continuable = false;

        userMessage = message;
        nextPhase = phaseAfter;

        while(currPhase == GAME_PHASE.INTERRUPTED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void changePhase(final GAME_PHASE phase) {
        currPhase = phase;
        if(phaseChangeListener != null) {
            phaseChangeListener.onPhaseChange(phase);
        }
    }

    // ----------- GETTERS -----------

    public GameState getGameState() {
        return gameState;
    }

    public void setOnPhaseChangeListener(final OnPhaseChangeListener listener) {
        phaseChangeListener = listener;
    }

    public void setInventoryChangeListener(final InventoryChangeListener listener) {
        inventoryChangeListener = listener;
    }

    public void setTimelineChangeListener(final TimelineChangeListener listener) {
        timelineChangeListener = listener;
    }

}
