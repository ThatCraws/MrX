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
import com.craws.mrx.state.Ability;
import com.craws.mrx.state.GameState;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Player;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Vehicle;
import com.craws.tree.Edge;

import java.util.Stack;
import java.util.Vector;


public class GameView extends SurfaceView {

    // ----------- App management -----------
    private Context context;
    private GameThread gameThread;
    private boolean playing;
    // --- Phase Management ---
    private OnPhaseChangeListener phaseChangeListener;
    private GAME_PHASE currPhase;
    private GAME_PHASE nextPhase;
    private boolean continuable;

    public enum GAME_PHASE {
        UNINITIALIZED,
        INTERRUPTED,                // No functionality of the game is given (mainly to display messages to human player)
        MRX_CHOOSE_TURN,            // Move (clicking city) or ability (clicking ticket before city)
        MRX_CHOOSE_ABILITY_TICKETS, // after first Ticket is selected, selected the other two (with same ability, else abort and back to MRX_CHOOSE_TURN
        MRX_CONFIRM_ABILITY,        // when all necessary Tickets are selected, ask to confirm to spend the tickets and activate the ability -> MRX_EXTRA_TURN or MRX_SPECIAL
        MRX_EXTRA_TURN,
        MRX_SPECIAL,
        MRX_CHOOSE_CITY,            // This phase is exclusively for abilities and will be entered once or twice depending on the ability.

        MRX_CHOOSE_TICKET,          // If no ability has been selected (but a City to move to), choose Ticket to move with.
        MRX_CONFIRM_MOVE,           // When a ticket matching the street to be travelled upon is selected, wait for confirmation to actually make the move
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
    // inform activity/update recyclerViews
    private InventoryChangeListener inventoryChangeListener;
    private TimelineChangeListener timelineChangeListener;
    // keep track of selection
    private City selectedCity;
    private Vector<Ticket> selectedTickets;
    // to preserve the above values which might otherwise get re-polled between testing values for validity and actually using them for actions
    // these do not get set each iteration, but only when the move got confirmed, so they can't be changed before
    // the confirmed action is applied (clicking confirm in the same tick as unselecting ticket for example would lead to NullPointerException)
    Place toTravelTo;
    Vector<Ticket> toUseForTravel;

    // ----------- graphics -----------
    private SurfaceHolder surfaceHolder;
    private Canvas canvas;
    private Stack<Render> renderStack;
    private Paint paint;
    // These colors will be used to mark the cities (when the detectives' special ability to mark Mr. X's position a few rounds ago is activated) //
    // and the the associated position in the timeline.
    private static final @ColorInt int[] markColorCoding = {
            Color.BLUE,
            Color.CYAN,
            Color.MAGENTA,
            Color.argb(255, 64,160,64),     // Own green to not be confused with the FAST-street
            Color.argb(255, 224, 192,32),   // Own yellow to not be confused with the MEDIUM-street
            Color.argb(255,192,64,64),      // Own red to not be confused with the SLOW-street
            Color.WHITE,
            Color.BLACK};

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
        currPhase = GAME_PHASE.UNINITIALIZED;
        nextPhase = GAME_PHASE.UNINITIALIZED;
        continuable = true;

        // for the map
        gameState = new GameState(context);
        selectedTickets = new Vector<>();
        selectedCity = null;
        // helpers (like selectedTickets(RightNow) and selectedCity(RightNow), but not overwritten every game loop iteration)
        toTravelTo = null;
        toUseForTravel = new Vector<>();

        // The things to draw (with)
        surfaceHolder = getHolder();
        paint = new Paint();
        paint.setTextSize(DEFAULT_TXT_SIZE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStrokeWidth(20f);

        renderStack = new Stack<>();

        // When the screen gets resized re-set the Places/Cities
        surfaceHolder.addCallback(new GameViewListener());

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

        fillInventoryX();
        fillInventory();
    }

    public void startGame() {
        // true as long as the game is not won/lost
        playing = true;

        // To give to the recycler
        activeInventory = new Vector<>();

        // Since we can also go back in phases, this flag is to only show the notification whose turn it is once.
        firstPhaseIteration = true;

        // Set start position of Mr. X
        gameState.getTimeline().addRound(null, gameState.getMrX().getPlace());
        if(timelineChangeListener != null) {
            timelineChangeListener.onTurnAdded(null, gameState.getMrX().getPlace());
        }

        gameLoop();
    }

    // These are directly bound to the game loop so they are here and not with the other class-members
    Vector<Ticket> activeInventory;
    private boolean firstPhaseIteration = true;

    public void gameLoop() {
        // ----===== GAME LOOP =====-----
        while(playing) {

            City selectedCityRightNow = selectedCity;
            Vector<Ticket> selectedTicketsRightNow = selectedTickets;

            //System.out.println(currPhase);
            switch (currPhase) {
                case UNINITIALIZED:
                case MRX_CHOOSE_TURN: { // ----===== Mr. X's turn starts =====-----
                    if (firstPhaseIteration) {
                        showMessageAndWaitForClick("Mr. X's turn. Detectives don't look.", GAME_PHASE.MRX_CHOOSE_TURN);
                        firstPhaseIteration = false;
                    }

                    if (!activeInventory.equals(gameState.getInventoryX())) {
                        activeInventory = gameState.getInventoryX();
                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onNewInventory(activeInventory);
                        }
                    }

                    // City was selected
                    if (selectedCityRightNow != null) {
                        // is city connected to MrX's current position?
                        Vehicle connection = gameState.getStreet(gameState.getMrX().getPlace(), selectedCityRightNow.getPlace());
                        if (connection != null) {
                            changePhase(GAME_PHASE.MRX_CHOOSE_TICKET);
                            break;
                        }
                    }
                    // if we got here then no city that is currently reachable is selected.
                    if (!selectedTicketsRightNow.isEmpty()) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_ABILITY_TICKETS);
                    }
                    break;
                }

                case MRX_CHOOSE_TICKET: { // ----===== Mr. X chooses to move =====-----

                    // if city gets deselected, go back to mrx choosing what to do this turn
                    if (selectedCityRightNow == null) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                    } else if (selectedTicketsRightNow.size() == 1) {
                        if (selectedTicketsRightNow.get(0).getVehicle() == gameState.getStreet(gameState.getMrX().getPlace(), selectedCityRightNow.getPlace())) {
                            changePhase(GAME_PHASE.MRX_CONFIRM_MOVE);
                        }
                    // if the player selects more than one ticket we assume he wants to activate an ability instead
                    } else if (selectedTicketsRightNow.size() > 1) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_ABILITY_TICKETS);
                    }
                    break;
                }

                case MRX_CHOOSE_ABILITY_TICKETS: { // ----===== Mr. X chooses to activate ability =====-----
                    // if tickets get deselected go back to mrx choosing what to do this turn
                    if (selectedTicketsRightNow == null || selectedTicketsRightNow.size() == 0) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                     // if one ticket and a (neighbouring) city are selected, we will assume the player tries to move instead.
                    } else if (selectedCityRightNow != null && selectedTicketsRightNow.size() == 1) {
                        // is city connected to MrX's current position?
                        Vehicle connection = gameState.getStreet(gameState.getMrX().getPlace(), selectedCityRightNow.getPlace());
                        if (connection != null) {
                            changePhase(GAME_PHASE.MRX_CHOOSE_TICKET);
                        }
                    } else if (selectedTicketsRightNow.size() == 3) {
                        // first tickets ability is the one to be activated
                        Ability toActivate = selectedTicketsRightNow.get(0).getAbility();
                        boolean match = true;
                        // iterate the other tickets (first one is in toActivate)
                        for (int i = 1; i < selectedTicketsRightNow.size(); i++) {
                            // match starts as true and only stays that way, if all following tickets have the same ability
                            match &= selectedTicketsRightNow.get(i).getAbility().equals(toActivate);
                        }
                        if (match) {
                            changePhase(GAME_PHASE.MRX_CONFIRM_ABILITY);
                        }
                    }
                    break;
                }

                case MRX_CONFIRM_ABILITY: { // ----===== waiting for Mr. X to confirm to activate ability and use the selected tickets =====-----
                    // If Tickets get deselected go back to choosing what to do
                    if (selectedTicketsRightNow == null || selectedTicketsRightNow.size() != 3) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                    }
                    break;
                }

                case MRX_CONFIRM_MOVE: { // ----===== waiting for Mr. X to confirm to move and use the selected ticket =====-----
                    // If Tickets or City get deselected go back to choosing what to do
                    if(selectedTicketsRightNow == null || selectedTicketsRightNow.size() != 1 || selectedCityRightNow == null) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                    // Also go back if a city gets selected that is not connected to Mr. X's position or the connecting street can't be travelled with the selected ticket
                    } else if (gameState.getStreet(gameState.getMrX().getPlace(), selectedCity.getPlace()) != selectedTicketsRightNow.get(0).getVehicle()) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_ABILITY_TICKETS);
                    }
                    break;
                }

                case MRX_MOVE: { // ----===== move got confirmed and the tickets and place saved =====-----
                    // GameState uses Vector.remove to take Ticket. This uses the first occurence just as Vector.indexOf, so we use that to get the index.
                    int ticketIndex = activeInventory.indexOf(toUseForTravel.get(0));
                    gameState.doMove(0,toTravelTo, toUseForTravel.get(0));
                    if(inventoryChangeListener != null) {
                        inventoryChangeListener.onRemove(ticketIndex);
                    }
                    if(timelineChangeListener != null) {
                        timelineChangeListener.onTurnAdded(toUseForTravel.get(0), toTravelTo);
                    }
                    changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                    break;
                }

                case MRX_EXTRA_TURN: {
                    System.out.println("Extra turn");
                    break;
                }
                case MRX_SPECIAL: {
                    System.out.println("Special");
                    break;
                }
                default:
                    System.out.println("Nuttin");
                    break;
            }
        }
    }

    public void setPlaying(final boolean playing) {
        this.playing = playing;
    }

    private void fillInventory() {
        while(gameState.getInventory().size() < 4 + gameState.getPlayers().size()) {
            Ticket toAdd = gameState.drawTicket();
            gameState.giveTicket(1, toAdd);
        }
    }

    private void fillInventoryX() {
        while(gameState.getInventoryX().size() < 8) {
            Ticket toAdd = gameState.drawTicket();
            gameState.giveTicket(0, toAdd);
        }
    }

    public void confirmSelection() {
        toUseForTravel = new Vector<>(selectedTickets);

        switch(currPhase) {
            case MRX_CONFIRM_MOVE: {
                // if city was deselected in the meantime
                if(selectedCity == null) {
                    changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                    return;
                }

                toTravelTo = selectedCity.getPlace();
                selectedCity.deselect();
                selectedCity = null;

                changePhase(GAME_PHASE.MRX_MOVE);
                break;
            }
            case MRX_CONFIRM_ABILITY: {
                if(toUseForTravel.size() == 3) {
                    if(toUseForTravel.get(0).getAbility() == Ability.EXTRA_TURN) {
                        changePhase(GAME_PHASE.MRX_EXTRA_TURN);
                    } else {
                        changePhase(GAME_PHASE.MRX_SPECIAL);
                    }
                }
                break;
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
                return true;
            }
        } else {

            scaleDetector.onTouchEvent(e);
            scrollDetector.onTouchEvent(e);

            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (!scrolling && !scaling) {
                    boolean madeCitySelection = false;
                    for (Place place : gameState.getPlaces()) {
                        // If a city was clicked
                        if (place.getCity().collisionCheck((e.getX() + (-viewPortX)) * (1 / mapScaleFactor), (e.getY() + (-viewPortY)) * (1 / mapScaleFactor))) {
                            // If a city was already selected deselect it first
                            if (selectedCity != null) {
                                selectedCity.deselect();
                            }
                            // If clicked on the already selected city
                            if (place.getCity().equals(selectedCity)) {
                                selectedCity = null;
                                // If clicked on a different city
                            } else {
                                selectedCity = place.getCity();
                                selectedCity.select();
                            }
                            madeCitySelection = true;
                        }
                    }
                    // If no place at all was clicked/the background was clicked, deselect city
                    if(!madeCitySelection && selectedCity != null) {
                        selectedCity.deselect();
                        selectedCity = null;
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
            } else if(currPhase != GAME_PHASE.UNINITIALIZED) {

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


                // Draw Cities
                int markedCityCounter = 0;
                for (int i = 0; i < gameState.getPlaces().size() ; i++) {
                    Place toDraw = gameState.getPlaces().get(i);
                    if(toDraw.getCity().isMarked()) {
                        float padding = toDraw.getCity().getWidth() * 0.1f;
                        City city = toDraw.getCity();

                        // Colors will just repeat once we run out of them...
                        paint.setColor(markColorCoding[markedCityCounter++ % markColorCoding.length]);

                        canvas.drawOval(city.getX() - padding, city.getY() - padding, city.getX() + city.getWidth() + padding, city.getY() + city.getHeight() + padding, paint);
                        paint.setColor(Color.WHITE);
                        canvas.drawOval(city.getX(), city.getY(), city.getX() + city.getWidth(), city.getY() + city.getHeight(), paint);
                    }
                    paint.setColor(prevColor);
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

                String helpText = "";
                switch (currPhase) {
                    case MRX_CHOOSE_TURN:
                        helpText = "Click city to move to or the ticket with the ability to activate.";
                        break;
                    case MRX_CHOOSE_TICKET:
                        helpText = "Choose the ticket to use for travel";
                        break;
                    case MRX_CHOOSE_ABILITY_TICKETS:
                        helpText = "Choose the (3) tickets to activate ability with";
                        break;
                    case MRX_CONFIRM_ABILITY:
                    case MRX_CONFIRM_MOVE:
                        helpText = "Confirm selection to make your move";
                        break;
                    case MRX_MOVE:
                        helpText = "I'm walkin' hee'.";
                        break;
                    case MRX_EXTRA_TURN:
                    case MRX_SPECIAL:
                        helpText = "Doing the move";
                        break;
                }

                canvas.drawText(helpText, -viewPortX + (getWidth() * (1/mapScaleFactor) / 2f), -viewPortY + getHeight() * (1/mapScaleFactor) - 75, paint);

                // --- Map (Debug/Testing, unfinished) ---
                // onlyBorders.setStyle(Paint.Style.STROKE);
                // onlyBorders.setStrokeWidth(15);
                // canvas.drawRect((-viewPortX + getWidth() - (mapWidth / massstab)), -viewPortY, -viewPortX + getWidth(), -viewPortY + (mapHeight / massstab), onlyBorders);
                // canvas.drawRect((-viewPortX + getWidth() - (mapWidth / massstab)) + (-viewPortX / massstab), -viewPortY + (-viewPortY / massstab), (-viewPortX + getWidth() - (mapWidth / massstab)) + (-viewPortX / massstab) + (getWidth() / massstab * (1 / mapScaleFactor)), -viewPortY + (-viewPortY / massstab) + (getHeight() / massstab), onlyBorders);
            } else {
                canvas.drawColor(Color.WHITE);
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
    }

    public void changePhase(final GAME_PHASE phase) {
        currPhase = phase;
        if(phaseChangeListener != null) {
            phaseChangeListener.onPhaseChange(phase);
        }
    }

    // ----------- GETTERS/SETTERS -----------

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

    public void setSelectedTickets(final Vector<Ticket> selectedTickets) {
        this.selectedTickets = new Vector<>(selectedTickets);
    }
}
