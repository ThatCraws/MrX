package com.craws.mrx.engine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.ColorInt;

import com.craws.mrx.graphics.City;
import com.craws.mrx.graphics.Figure;
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
        DET_MRX_TRANSITION,                 // besides managing the transition from Detectives' turn to Mr. X's, it is the start state
        INTERRUPTED,                        // Displaying message to user and waiting for click (start via ShowMessageAndWaitForClick())
        WAIT_FOR_CLICK,                     // Just waiting for click (start via waitForClick())

        // ----------------- MR. X -----------------
        MRX_NO_VALID_MOVE,
        MRX_NO_VALID_MOVE_CHOOSE_TURN,
        MRX_NO_VALID_MOVE_CHOOSE_SPECIAL_TICKETS,
        MRX_NO_VALID_MOVE_SPECIAL_ACTIVATION_CONFIRM,

        MRX_CHOOSE_TURN,                    // Move (clicking city) or ability (clicking ticket before city)
        MRX_CHOOSE_ABILITY_TICKETS,         // after first Ticket is selected, selected the other two (with same ability, else abort and back to MRX_CHOOSE_TURN
        MRX_ABILITY_CONFIRM,                // when all necessary Tickets are selected, ask to confirm to spend the tickets and activate the ability -> MRX_EXTRA_TURN or MRX_SPECIAL
        MRX_EXTRA_TURN,                     // The usage of the ability has been confirmed here. Prepare.
        MRX_EXTRA_TURN_CHOOSE_TURN,         // Wait for user to select one ticket and one directly connected city
        MRX_EXTRA_TURN_CONFIRM,
        MRX_EXTRA_TURN_MOVE,
        MRX_EXTRA_TURN_NOT_POSSIBLE,
        MRX_EXTRA_TURN_ONE_NOT_POSSIBLE,    // This means the ability was successfully activated, but the user tried to move to a place with his first turn, from where he
        // couldn't do his second move (because he is going to a place that only has streets which require tickets not in his inventory)

        MRX_SPECIAL,                        // The usage of the ability has been confirmed. Prepare.
        MRX_SPECIAL_CHOOSE_CITY,
        MRX_SPECIAL_CONFIRM,
        MRX_SPECIAL_MOVE,

        MRX_CHOOSE_TICKET,                  // If no ability has been selected (but a City to move to), choose Ticket to move with.
        MRX_MOVE_CONFIRM,                   // When a ticket matching the street to be travelled upon is selected, wait for confirmation to actually make the move
        MRX_MOVE,                           // To move the figure and add to the timeline
        MRX_WIN_CHECK,                      // Position check after Mr. X turn (he can't make himself lose). Make Mr. X disappear for detective's turn.
        MRX_THROW_TICKETS,                  // Mr. X can throw as many tickets as he wants and then restock to 8 Tickets. (this phase is just for the player to select the tickets(
        MRX_THROWING_SELECTED_TICKETS,      // Actually throws away the selected tickets and restocks back to 8

        MRX_DET_TRANSITION,

        // ----------------- DETECTIVES -----------------
        DET_NO_VALID_MOVE,

        DET_CHOOSE_MOVE,                    // All detectives have to move before being able to activate an ability.
        DET_MOVE_CONFIRM,
        DET_MOVE,

        DET_SELECT_NEXT,                    // If user clicks confirm without tickets selected, end turn (DET_WIN_CHECK).
                                            // If he selects a ticket tell him to select his 3 ability tickets (DET_CHOOSE_ABILITY_TICKETS)

        DET_CHOOSE_ABILITY_TICKETS,         // Choose 3-5 tickets, so "finish"-button or something will be needed
        DET_ABILITY_CONFIRM,
                                            // Throw away tickets for SPECIAL

        DET_EXTRA_TURN,                     // Check if the free ticket gotten via the extra turn can even be used (is any detective on a place with a street that needs that kind of ticket?)
        DET_EXTRA_TURN_CHOOSE_TURN,         // Choose detective and City and travel there free of cost
        DET_EXTRA_TURN_CONFIRM,
        DET_EXTRA_TURN_MOVE,
        DET_EXTRA_TURN_NOT_POSSIBLE,
        DET_EXTRA_TURN_WIN_CHECK,

        DET_SPECIAL,                        // Throw away tickets for ability/prepare ability.
        DET_SPECIAL_DO,                     // Retrieve Location From Timeline (and set the city-sprite accordingly).

        DET_WIN_CHECK,                      // lame
        DET_THROW_TICKETS,                  // Even the detective-player is allowed to throw as many tickets as they want and restock (to 4 + No. of controlled detectives)
        DET_THROWING_SELECTED_TICKETS,

        MRX_WON,
        DET_WON,
        GAME_OVER
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
    private Vector<City> markedCities;
    private Figure selectedFigure;
    private Vector<Ticket> selectedTickets;
    // to preserve the above values which might otherwise get re-polled between testing values for validity and actually using them for actions
    // these do not get set each iteration, but only when the move got confirmed, so they can't be changed before
    // the confirmed action is applied (clicking confirm in the same tick as unselecting ticket for example would lead to NullPointerException)
    Figure toTravel;
    City toTravelTo;
    Vector<Ticket> toUseForTravel;

    private boolean showMrX; // MrX is hidden in detectives' turn

    // ----------- graphics -----------
    private SurfaceHolder surfaceHolder;
    private Canvas canvas;
    private Stack<Render> renderStack;
    private Paint paint;
    // These colors will be used to mark the cities (when the detectives' special ability to mark Mr. X's position a few rounds ago is activated) //
    // and the the associated position in the timeline.
    public static final @ColorInt
    int[] markColorCoding = {
            Color.BLUE,
            Color.CYAN,
            Color.MAGENTA,
            Color.argb(255, 64, 160, 64),     // Own green to not be confused with the FAST-street
            Color.argb(255, 224, 192, 32),   // Own yellow to not be confused with the MEDIUM-street
            Color.argb(255, 192, 64, 64),      // Own red to not be confused with the SLOW-street
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
        currPhase = GAME_PHASE.DET_MRX_TRANSITION;
        nextPhase = GAME_PHASE.DET_MRX_TRANSITION;
        continuable = true;

        // for the map
        gameState = new GameState(context);
        selectedTickets = new Vector<>();
        selectedCity = null;
        markedCities = new Vector<>();
        selectedFigure = null;
        // helpers (like selectedTickets(RightNow) and selectedCity(RightNow), but not overwritten every game loop iteration)
        toTravel = null;
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
        dstViewport.set(0f, 0f, (float) getWidth(), (float) getHeight());

        Place pl_kiel = gameState.buildPlace("Kiel", false, 450, 125);
        Place pl_bremen = gameState.buildPlace("Bremen", false, 100, 200);
        Place pl_hanno = gameState.buildPlace("Hannover", false, 200, 550);
        Place pl_pig = gameState.buildPlace("Pig", false, 400, 400);
        Place pl_murica = gameState.buildPlace("Murica", true, 500, 600);
        Place pl_kaffstadt = gameState.buildPlace("Kaffster", false, 600, 475);
        Place pl_berlin = gameState.buildPlace("Berlin", false, 550, 300);

        Place pl_forgotten_island = gameState.buildPlace("Nowheresville", false, 750, 350);
        Place pl_sylt = gameState.buildPlace("Sylt", false, 1000, 100);
        Place pl_newYork = gameState.buildPlace("New York", false, 1250, 500);
        Place pl_washington = gameState.buildPlace("Washington", false, 1100, 750);

        gameState.buildStreet(pl_pig, pl_bremen, Vehicle.FAST);
        gameState.buildStreet(pl_pig, pl_hanno, Vehicle.MEDIUM);
        gameState.buildStreet(pl_pig, pl_murica, Vehicle.SLOW);
        gameState.buildStreet(pl_pig, pl_kaffstadt, Vehicle.MEDIUM);
        gameState.buildStreet(pl_berlin, pl_kaffstadt, Vehicle.FAST);
        gameState.buildStreet(pl_kiel, pl_bremen, Vehicle.FAST);
        gameState.buildStreet(pl_kiel, pl_berlin, Vehicle.FAST);

        gameState.buildStreet(pl_forgotten_island, pl_sylt, Vehicle.FAST);
        gameState.buildStreet(pl_forgotten_island, pl_newYork, Vehicle.MEDIUM);
        gameState.buildStreet(pl_forgotten_island, pl_washington, Vehicle.SLOW);
        gameState.buildStreet(pl_sylt, pl_newYork, Vehicle.MEDIUM);
        gameState.buildStreet(pl_newYork, pl_washington, Vehicle.FAST);

        int det = gameState.addDetective("Detestive", pl_hanno);
        int detTwo = gameState.addDetective("Numero dos", pl_kiel);
        int mrx = gameState.addMrX(pl_forgotten_island);

        Player detective = gameState.getPlayerByPort(det);
        Player misterX = gameState.getPlayerByPort(mrx);

        fillInventoryX();
        fillInventory();
    }

    public void startGame() {
        // true as long as the game is not won/lost
        playing = true;

        // Since we can also go back in phases, this flag is to only show the notification whose turn it is once.
        firstPhaseIteration = true;

        // Set start position of Mr. X
        gameState.getTimeline().addRound(null, gameState.getMrX().getPlace());
        if (timelineChangeListener != null) {
            timelineChangeListener.onTurnAdded(null, gameState.getMrX().getPlace());
        }

        gameLoop();
    }

    // These are directly bound to the game loop so they are here and not with the other class-members
    private boolean firstPhaseIteration = true;
    private Player currDetective;
    private int extraTurnCounter = 1;
    private int abilityPower;

    public void gameLoop() {
        // ----===== GAME LOOP =====-----
        while (playing) {

            City selectedCityRightNow = selectedCity;
            Vector<Ticket> selectedTicketsRightNow = selectedTickets;
            Figure selectedFigureRightNow = selectedFigure;

            switch (currPhase) {
                case DET_MRX_TRANSITION:

                    showMessageAndWaitForClick("Mr. X's turn. Detectives don't look.", GAME_PHASE.MRX_CHOOSE_TURN);
                    break;

                case MRX_CHOOSE_TURN: { // ----===== Mr. X's turn starts =====-----
                    if (firstPhaseIteration) {

                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onNewInventory(gameState.getInventoryX());
                        }

                        firstPhaseIteration = false;
                        showMrX = true;
                    }

                    if (!hasValidMove(gameState.getMrX())) {
                        changePhase(GAME_PHASE.MRX_NO_VALID_MOVE);
                        break;
                    }

                    // City was selected
                    if (selectedCityRightNow != null) {
                        // is city connected to MrX's current position?
                        Vehicle connection = gameState.getStreet(gameState.getMrX().getPlace(), selectedCityRightNow.getPlace());

                        // is city empty (no detective)
                        boolean cityFree = true;
                        for(int i = 0; i < gameState.getDetectives().size(); i++) {
                            if(gameState.getDetectives().get(i).getPlace().getCity() == selectedCityRightNow) {
                                cityFree = false;
                                break;
                            }
                        }

                        if (connection != null && cityFree) {
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
                    if (selectedCityRightNow == null || gameState.getStreet(gameState.getMrX().getPlace(), selectedCityRightNow.getPlace()) == null) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                    } else if (selectedTicketsRightNow.size() == 1) {
                        if (selectedTicketsRightNow.get(0).getVehicle() == gameState.getStreet(gameState.getMrX().getPlace(), selectedCityRightNow.getPlace())) {
                            changePhase(GAME_PHASE.MRX_MOVE_CONFIRM);
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
                            changePhase(GAME_PHASE.MRX_ABILITY_CONFIRM);
                        }
                    }
                    break;
                }

                case MRX_ABILITY_CONFIRM: { // ----===== waiting for Mr. X to confirm to activate ability and use the selected tickets =====-----
                    // If Tickets get deselected go back to choosing what to do
                    if (selectedTicketsRightNow == null || selectedTicketsRightNow.size() != 3) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                    }
                    break;
                }

                case MRX_MOVE_CONFIRM: { // ----===== waiting for Mr. X to confirm to move and use the selected ticket =====-----
                    // If Tickets or City get deselected (or more than one ticket) go back to choosing what to do
                    if (selectedTicketsRightNow == null || selectedTicketsRightNow.size() != 1 || selectedCityRightNow == null) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                        // Also go back if a city gets selected that is not connected to Mr. X's position or the connecting street can't be travelled with the selected ticket
                    } else if (gameState.getStreet(gameState.getMrX().getPlace(), selectedCity.getPlace()) != selectedTicketsRightNow.get(0).getVehicle()) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_ABILITY_TICKETS);
                    }
                    break;
                }

                case MRX_MOVE: { // ----===== move got confirmed and the tickets and place saved =====-----
                    // GameState uses Vector.remove to take Ticket. This uses the first occurrence just as Vector.indexOf, so we use that to get the index.
                    Vector<Ticket> inventory = new Vector<>(gameState.getInventoryX());

                    int ticketIndex = inventory.indexOf(toUseForTravel.get(0));
                    inventory.remove(ticketIndex);
                    gameState.doMove(0, toTravelTo.getPlace(), toUseForTravel.get(0));
                    if (inventoryChangeListener != null) {
                        inventoryChangeListener.onRemove(ticketIndex);
                    }
                    if (timelineChangeListener != null) {
                        timelineChangeListener.onTurnAdded(toUseForTravel.get(0), toTravelTo.getPlace());
                    }
                    changePhase(GAME_PHASE.MRX_WIN_CHECK);
                    break;
                }

                case MRX_EXTRA_TURN: { // ----===== extra turn ability got confirmed, check if possible and throw tickets or tell user they can't =====-----
                    Vector<Ticket> inventory = new Vector<>(gameState.getInventoryX());

                    // This ability may only be activated, if the two turns can actually be made.
                    // That means, after the 3 EXTRA_TURN-ability tickets are gone, there have to be enough tickets left in the inventory
                    // for the player to do 2 moves. So not only are two tickets needed, but it has to be possible two use both to travel to neighbouring cities (and their neighbours)
                    Vector<Ticket> simulationInventory = new Vector<>(inventory);
                    // simulate the ability-tickets being thrown away
                    for (int i = 0; i < toUseForTravel.size(); i++) {
                        simulationInventory.remove(toUseForTravel.get(i));
                    }

                    Vector<Place> neighbours = gameState.getSurroundingPlaces(gameState.getMrX().getPlace());
                    Player mrX = gameState.getMrX();

                    boolean movePossible = false;

                    for (int i = 0; i < neighbours.size(); i++) {

                        // is city empty (no detective)
                        boolean cityFree = true;
                        for(int j = 0; j < gameState.getDetectives().size(); j++) {
                            if(gameState.getDetectives().get(j).getPlace().getCity() == selectedCityRightNow) {
                                cityFree = false;
                                break;
                            }
                        }

                        if(cityFree) {

                            Vehicle connection = gameState.getStreet(mrX.getPlace(), neighbours.get(i)); // can't be null (else it wouldn't be returned by getSurroundingPlaces)

                            // Check inventory for ticket to get to the current neighbour
                            for (int j = 0; j < simulationInventory.size(); j++) {
                                if (simulationInventory.get(j).getVehicle() == connection) {
                                    // don't use this ticket again
                                    Ticket usedAlready = simulationInventory.get(j);
                                    // temporarily remove used ticket
                                    simulationInventory.remove(usedAlready);

                                    // we have to make two moves, so check the neighbours of the current neighbour
                                    if (hasValidMove(neighbours.get(i), gameState.getMrX().getPort())) {
                                        movePossible = true;
                                        break;
                                    }
                                }
                            }

                        }


                        if (movePossible) {
                            break;
                        }
                    }

                    if (!movePossible) {
                        changePhase(GAME_PHASE.MRX_EXTRA_TURN_NOT_POSSIBLE);
                        break;
                    }

                    // Remove the tickets from the copy first, to have access to indexOf
                    for (Ticket currTicket : toUseForTravel) {
                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onRemove(inventory.indexOf(currTicket));
                        }
                        inventory.remove(currTicket);
                    }

                    gameState.activateAbility(0, toUseForTravel, Ability.EXTRA_TURN);
                    changePhase(GAME_PHASE.MRX_EXTRA_TURN_CHOOSE_TURN);

                    break;
                }

                case MRX_EXTRA_TURN_CHOOSE_TURN: { // ----===== extra turn ability activate and tickets thrown. Wait for user to make his turn =====-----

                    if (selectedCityRightNow != null) {
                        Vehicle connection = gameState.getStreet(gameState.getMrX().getPlace(), selectedCityRightNow.getPlace());
                        // There just has to be a directly connected city selected and exactly one ticket.
                        // Also the ticket has to match the street connecting Mr. X's position to the selected city
                        if (connection != null &&
                                selectedTicketsRightNow != null && selectedTicketsRightNow.size() == 1 &&
                                selectedTicketsRightNow.get(0).getVehicle() == connection) {
                            changePhase(GAME_PHASE.MRX_EXTRA_TURN_CONFIRM);
                        }
                    }
                    break;
                }

                case MRX_EXTRA_TURN_CONFIRM: { // ----===== extra turn ability got confirmed and city and ticket selected. Wait for confirmation =====-----
                    if (selectedCityRightNow == null ||
                            selectedTicketsRightNow == null || selectedTicketsRightNow.size() != 1 ||
                            selectedTicketsRightNow.get(0).getVehicle() != gameState.getStreet(gameState.getMrX().getPlace(), selectedCityRightNow.getPlace())) {
                        changePhase(GAME_PHASE.MRX_EXTRA_TURN_CHOOSE_TURN);
                    }
                    break;
                }

                case MRX_EXTRA_TURN_MOVE: { // ----===== extra turn ability got confirmed, city and ticket selected and the move confirmed =====-----
                    // We want to make sure that the user does not go to a place on his first move, from where he can't do his second move (because of missing ticket).
                    boolean canMoveFromGoal = true;

                    if (extraTurnCounter == 1) {
                        Vector<Ticket> simulationInventory = new Vector<>(gameState.getInventoryX());

                        // we wanna simulate the state of the inventory after this first move (and make sure we can move from there)
                        simulationInventory.remove(toUseForTravel.get(0));

                        // if this gets set to true while looking for a valid second move, we found one
                        canMoveFromGoal = hasValidMove(toTravelTo.getPlace(), gameState.getMrX().getPort());
                    }

                    // this only applies on the first of the two extra turns (which is why I used the NOT-expression for locality)
                    if (!canMoveFromGoal) {
                        // since we reached this point, there is a valid move (else we wouldn't let the user throw the tickets for this ability),
                        // so let the user know he can't do this turn and then go back to MRX_EXTRA_TURN_CHOOSE_TURN
                        changePhase(GAME_PHASE.MRX_EXTRA_TURN_ONE_NOT_POSSIBLE);
                    } else {

                        Vector<Ticket> inventory = new Vector<>(gameState.getInventoryX());

                        // update timeline
                        gameState.getTimeline().addRound(toUseForTravel.get(0), toTravelTo.getPlace());
                        if (timelineChangeListener != null) {
                            timelineChangeListener.onTurnAdded(toUseForTravel.get(0), toTravelTo.getPlace());
                        }

                        // remember ticket
                        int ticketIndex = inventory.indexOf(toUseForTravel.get(0));

                        // let the game move Mr. X
                        gameState.doMove(0, toTravelTo.getPlace(), toUseForTravel.get(0));

                        // Update inventory
                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onRemove(ticketIndex);
                        }
                        inventory.remove(ticketIndex);

                        if (extraTurnCounter++ >= 2) {
                            changePhase(GAME_PHASE.MRX_WIN_CHECK);
                            extraTurnCounter = 1;
                        } else {
                            changePhase(GAME_PHASE.MRX_EXTRA_TURN_CHOOSE_TURN);
                        }
                    }
                    break;
                }

                case MRX_EXTRA_TURN_NOT_POSSIBLE:
                case MRX_EXTRA_TURN_ONE_NOT_POSSIBLE:
                case DET_EXTRA_TURN_NOT_POSSIBLE: {

                    switch (currPhase) {
                        case MRX_EXTRA_TURN_NOT_POSSIBLE:
                            changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                            break;
                        case MRX_EXTRA_TURN_ONE_NOT_POSSIBLE:
                            changePhase(GAME_PHASE.MRX_EXTRA_TURN_CHOOSE_TURN);
                            break;
                        case DET_EXTRA_TURN_NOT_POSSIBLE:
                            changePhase(GAME_PHASE.DET_CHOOSE_ABILITY_TICKETS);
                            break;
                    }

                    break;
                }

                case MRX_SPECIAL: { // ----===== shadow ticket got confirmed, the tickets get used =====-----
                    Vector<Ticket> inventory = new Vector<>(gameState.getInventoryX());

                    for (Ticket currTicket : toUseForTravel) {
                        int ticketIndex = inventory.indexOf(currTicket);
                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onRemove(ticketIndex);
                        }
                        inventory.remove(ticketIndex);
                    }

                    gameState.activateAbility(0, toUseForTravel, Ability.SPECIAL);

                    changePhase(GAME_PHASE.MRX_SPECIAL_CHOOSE_CITY);

                    break;
                }
                case MRX_SPECIAL_CHOOSE_CITY: { // ----===== Waiting for the user to select a City to be travelled to anonymously =====-----
                    if (selectedCityRightNow != null && gameState.getStreet(gameState.getMrX().getPlace(), selectedCity.getPlace()) != null) {
                        changePhase(GAME_PHASE.MRX_SPECIAL_CONFIRM);
                    }
                    break;
                }
                case MRX_SPECIAL_CONFIRM: { // ----===== Waiting for the player to confirm the selected city (unselecting City -> ask user for city again) =====-----
                    // if city gets deselected or other (non-reachable) city gets selected, go back
                    if (selectedCityRightNow == null || gameState.getStreet(gameState.getMrX().getPlace(), selectedCityRightNow.getPlace()) == null) {
                        changePhase(GAME_PHASE.MRX_SPECIAL_CHOOSE_CITY);
                    }
                    break;
                }
                case MRX_SPECIAL_MOVE: { // ----===== Shadow Ticket move got confirmed. Move, put ticket in timeline =====-----
                    Ticket ticketOfShadows = new Ticket(Vehicle.SHADOW, Ability.SHADOW);
                    gameState.doFreeMove(0, toTravelTo.getPlace());
                    gameState.getTimeline().addRound(ticketOfShadows, toTravelTo.getPlace());
                    if (timelineChangeListener != null) {
                        timelineChangeListener.onTurnAdded(ticketOfShadows, toTravelTo.getPlace());
                    }
                    changePhase(GAME_PHASE.MRX_WIN_CHECK);
                    break;
                }

                case MRX_WIN_CHECK: {
                    if (gameState.isGameLost()) {
                        Log.d("win-check_mrx", "Mr. X won");
                        changePhase(GAME_PHASE.MRX_WON);
                    } else {
                        Log.d("win-check_mrx", "Mr. X did not win =(");
                    }
                    changePhase(GAME_PHASE.MRX_THROW_TICKETS);
                    break;
                }

                case MRX_THROW_TICKETS:
                case DET_THROW_TICKETS: {
                    // Just let the CPU relax a bit
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }

                case MRX_THROWING_SELECTED_TICKETS: {
                    Vector<Ticket> inventory = new Vector<>(gameState.getInventoryX());

                    for (int i = 0; i < toUseForTravel.size(); i++) {

                        // update user UI inventory
                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onRemove(inventory.indexOf(toUseForTravel.get(i)));
                        }

                        // remove from inventory in game-state
                        gameState.getInventoryX().remove(toUseForTravel.get(i));

                        // remove from model inventory
                        inventory.remove(toUseForTravel.get(i));
                    }

                    while (gameState.getInventoryX().size() < 8) {
                        Ticket toGive = gameState.drawTicket();

                        // add to model inventory
                        inventory.add(toGive);

                        int ticketIndex = inventory.indexOf(toGive);

                        // add to user UI inventory
                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onAdd(toGive);
                        }

                        // add to inventory in game-state
                        gameState.giveTicket(0, toGive);
                    }

                    firstPhaseIteration = true;

                    // let user take a look at his new tickets and the marvelous animation of removing and adding tickets to/from inventory
                    waitForClick(GAME_PHASE.MRX_DET_TRANSITION);

                    break;
                }

                case MRX_DET_TRANSITION: {
                    showMessageAndWaitForClick("Detectives' turn", GAME_PHASE.DET_CHOOSE_MOVE);
                    break;
                }

                case MRX_NO_VALID_MOVE: {
                    waitForClick(GAME_PHASE.MRX_NO_VALID_MOVE_CHOOSE_TURN);
                    break;
                }

                case MRX_NO_VALID_MOVE_CHOOSE_TURN:  {
                    if(selectedTicketsRightNow != null && selectedTicketsRightNow.size() > 0) {
                        changePhase(GAME_PHASE.MRX_NO_VALID_MOVE_CHOOSE_SPECIAL_TICKETS);
                    }
                    break;
                }

                case MRX_NO_VALID_MOVE_CHOOSE_SPECIAL_TICKETS: {
                    if(selectedTicketsRightNow == null || selectedTicketsRightNow.size() == 0) {
                        changePhase(GAME_PHASE.MRX_NO_VALID_MOVE_CHOOSE_TURN);
                    } else if(selectedTicketsRightNow.size() == 3) {
                        boolean allTicketsSpecial = true;
                        for(int i = 0; i < selectedTicketsRightNow.size(); i++) {
                            if(selectedTicketsRightNow.get(i).getAbility() != Ability.SPECIAL) {
                                allTicketsSpecial = false;
                                break;
                            }
                        }
                        if(allTicketsSpecial) {
                            changePhase(GAME_PHASE.MRX_NO_VALID_MOVE_SPECIAL_ACTIVATION_CONFIRM);
                        }
                    }
                    break;
                }

                case MRX_NO_VALID_MOVE_SPECIAL_ACTIVATION_CONFIRM: {
                    if(selectedTicketsRightNow == null || selectedTicketsRightNow.size() != 3) {
                        changePhase(GAME_PHASE.MRX_NO_VALID_MOVE_CHOOSE_SPECIAL_TICKETS);
                    } else {
                        boolean allTicketsSpecial = true;
                        for(int i = 0; i < selectedTicketsRightNow.size(); i++) {
                            if(selectedTicketsRightNow.get(i).getAbility() != Ability.SPECIAL) {
                                allTicketsSpecial = false;
                                break;
                            }
                        }

                        if(!allTicketsSpecial) {
                            changePhase(GAME_PHASE.MRX_NO_VALID_MOVE_CHOOSE_SPECIAL_TICKETS);
                        }
                    }
                    break;
                }
                // --------------------------------------------------------------------------------------------------------
                // ---------------------------------------------- DETECTIVES ----------------------------------------------
                // --------------------------------------------------------------------------------------------------------
                case DET_CHOOSE_MOVE: {

                    if (firstPhaseIteration) {

                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onNewInventory(gameState.getInventory());
                        }
                        currDetective = gameState.getDetectives().get(0);

                        firstPhaseIteration = false;

                        break;
                    }
                    if(showMrX) { // So Mr. X does not disappear before the showMessageAndWaitForClick()-box is there
                        showMrX = false;
                    }

                    if (!hasValidMove(currDetective)) {
                        changePhase(GAME_PHASE.DET_NO_VALID_MOVE);
                        break;
                    }

                    // Highlight current detective so user knows which one to move
                    currDetective.getFigure().select();

                    // since first all detectives move and then an ability can be activated, we can just
                    // check for a neighbouring city and one ticket (matching the street) selected.
                    if (selectedCityRightNow != null && selectedTicketsRightNow != null && selectedTicketsRightNow.size() == 1) {
                        Vehicle connection = gameState.getStreet(currDetective.getPlace(), selectedCityRightNow.getPlace());
                        if (connection != null && connection == selectedTicketsRightNow.get(0).getVehicle()) {
                            changePhase(GAME_PHASE.DET_MOVE_CONFIRM);
                        }
                    }
                    break;
                }

                case DET_MOVE_CONFIRM: {
                    // check if there is still a move to confirm
                    if (selectedCityRightNow == null || selectedTicketsRightNow == null || selectedTicketsRightNow.size() != 1) {
                        changePhase(GAME_PHASE.DET_CHOOSE_MOVE);
                    } else {
                        Vehicle connection = gameState.getStreet(currDetective.getPlace(), selectedCityRightNow.getPlace());
                        if (connection == null || connection != selectedTicketsRightNow.get(0).getVehicle()) {
                            changePhase(GAME_PHASE.DET_CHOOSE_MOVE);
                        }
                    }
                    break;
                }

                case DET_MOVE: {
                    // GameState uses Vector.remove to take Ticket. This uses the first occurrence just as Vector.indexOf, so we use that to get the index.
                    Vector<Ticket> inventory = new Vector<>(gameState.getInventory());

                    int ticketIndex = inventory.indexOf(toUseForTravel.get(0));
                    inventory.remove(ticketIndex);
                    gameState.doMove(currDetective.getPort(), toTravelTo.getPlace(), toUseForTravel.get(0));
                    if (inventoryChangeListener != null) {
                        inventoryChangeListener.onRemove(ticketIndex);
                    }

                    // Remove highlighting because turn is over for that detective
                    currDetective.getFigure().deselect();

                    // Check win-condition right after detective moved (waiting until all detectives have moved would be really weird for the player
                    changePhase(GAME_PHASE.DET_WIN_CHECK);

                    break;
                }

                case DET_WIN_CHECK: {

                    if (gameState.isGameWon()) {
                        changePhase(GAME_PHASE.DET_WON);
                        break;
                    }

                    if (gameState.getDetectives().lastElement().getPort() == currDetective.getPort()) { // avoiding equals here
                        changePhase(GAME_PHASE.DET_SELECT_NEXT);
                    } else {
                        int detIndex = gameState.getDetectives().indexOf(currDetective);
                        currDetective = gameState.getDetectives().get(detIndex + 1);
                        changePhase(GAME_PHASE.DET_CHOOSE_MOVE);
                    }

                    break;
                }

                case DET_SELECT_NEXT: {
                    // if user clicks confirm without selecting tickets, the turn will end (DET_THROW_TICKETS), else we will assume they want to activate an ability
                    if (selectedTicketsRightNow != null && selectedTicketsRightNow.size() > 0) {
                        changePhase(GAME_PHASE.DET_CHOOSE_ABILITY_TICKETS);
                    }
                    break;
                }

                case DET_CHOOSE_ABILITY_TICKETS: {
                    if (selectedTicketsRightNow == null || selectedTicketsRightNow.size() == 0) {
                        changePhase(GAME_PHASE.DET_SELECT_NEXT);
                    } else if (selectedTicketsRightNow.size() >= 3 && selectedTicketsRightNow.size() <= 5) {
                        Ability sampleAbility = selectedTicketsRightNow.get(0).getAbility();
                        boolean ticketsAreAllTheSame = true;

                        for (int i = 0; i < selectedTicketsRightNow.size(); i++) {
                            if (sampleAbility != selectedTicketsRightNow.get(i).getAbility()) {
                                ticketsAreAllTheSame = false;
                            }
                        }

                        if (ticketsAreAllTheSame) {
                            changePhase(GAME_PHASE.DET_ABILITY_CONFIRM);
                        }
                    }
                    break;
                }

                case DET_ABILITY_CONFIRM: {
                    if (selectedTicketsRightNow == null || selectedTicketsRightNow.size() < 3 || selectedTicketsRightNow.size() > 5) {
                        changePhase(GAME_PHASE.DET_CHOOSE_ABILITY_TICKETS);
                    } else {
                        boolean allTicketsAreTheSame = true;
                        Ability sampleAbility = selectedTicketsRightNow.get(0).getAbility();
                        for(int i = 0; i < selectedTicketsRightNow.size(); i++) {
                            if(selectedTicketsRightNow.get(i).getAbility() != sampleAbility) {
                                allTicketsAreTheSame = false;
                            }
                        }
                        if(!allTicketsAreTheSame) {
                            changePhase(GAME_PHASE.DET_CHOOSE_ABILITY_TICKETS);
                        }
                    }
                    break;
                }

                case DET_SPECIAL: {

                    Vector<Ticket> inventory = new Vector<>(gameState.getInventory());

                    for (int i = 0; i < toUseForTravel.size(); i++) {
                        int ticketIndex = inventory.indexOf(toUseForTravel.get(i));
                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onRemove(ticketIndex);
                        }
                        inventory.remove(ticketIndex);
                    }

                    gameState.activateAbility(currDetective.getPort(), toUseForTravel, Ability.SPECIAL);

                    abilityPower = toUseForTravel.size() - 3; // Value between 0-2

                    changePhase(GAME_PHASE.DET_SPECIAL_DO);
                    break;
                }

                case DET_SPECIAL_DO: {

                    int currRound = gameState.getTimeline().size() - 1; // round (counting start positions, so after first move of Mr. X, it's 2)

                    // if 3 tickets are used, mark position 4 moves before the current position, for 4 tickets 3 before, for 5 tickets 2 before
                    int indexToMark = Math.max(currRound - (4 - abilityPower), 1);

                    Log.d("DetSpecial", "Timeline size: " + gameState.getTimeline().size());
                    Log.d("DetSpecial", "Ability power: " + abilityPower);
                    Log.d("DetSpecial", "Index: " + indexToMark);

                    if (timelineChangeListener != null) {
                        timelineChangeListener.onTurnMarked(indexToMark); // 1: current round, 2: last round, ..., timeline.size: start-position
                    }

                    markedCities.add(gameState.getTimeline().getPlaceForRound(indexToMark).getCity());

                    changePhase(GAME_PHASE.DET_THROW_TICKETS);
                    break;
                }

                case DET_EXTRA_TURN: {

                    abilityPower = toUseForTravel.size() - 3; // Value between 0-2

                    Vehicle potentialFreeTicket;
                    switch (abilityPower) {
                        case 0:
                            potentialFreeTicket = Vehicle.SLOW;
                            break;
                        case 1:
                            potentialFreeTicket = Vehicle.MEDIUM;
                            break;
                        default:
                            potentialFreeTicket = Vehicle.FAST;
                            break;
                    }

                    // we will assume that we can not use the ticket until proven otherwise
                    boolean canUseTicket = false;

                    for(int i = 0; i < gameState.getDetectives().size(); i++) {
                        Player currDetective = gameState.getDetectives().get(i);

                        Vector<Place> neighbours = gameState.getSurroundingPlaces(currDetective.getPlace());

                        // Check all the cities the current detective could reach
                        for(int j = 0; j < neighbours.size(); j++) {
                            Vehicle connection = gameState.getStreet(currDetective.getPlace(), neighbours.get(j));

                            if(connection == potentialFreeTicket) {
                                canUseTicket = true;
                                break;
                            }
                        }
                        if(canUseTicket) {
                            break;
                        }
                    }

                    if(!canUseTicket) {
                        changePhase(GAME_PHASE.DET_EXTRA_TURN_NOT_POSSIBLE);
                        break;
                    }

                    Vector<Ticket> inventory = new Vector<>(gameState.getInventory());

                    for (int i = 0; i < toUseForTravel.size(); i++) {
                        int ticketIndex = inventory.indexOf(toUseForTravel.get(i));
                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onRemove(ticketIndex);
                        }
                        inventory.remove(ticketIndex);
                    }

                    gameState.activateAbility(currDetective.getPort(), toUseForTravel, Ability.EXTRA_TURN);

                    changePhase(GAME_PHASE.DET_EXTRA_TURN_CHOOSE_TURN);
                    break; // For the sake of the gameLoop-mechanics break here even though we will end up in DET_EXTRA_TURN_CHOOSE_TURN anyway

                }

                case DET_EXTRA_TURN_CHOOSE_TURN: {
                    Vehicle freeTicket;

                    switch (abilityPower) {
                        case 0:
                            freeTicket = Vehicle.SLOW;
                            break;
                        case 1:
                            freeTicket = Vehicle.MEDIUM;
                            break;
                        default:
                            freeTicket = Vehicle.FAST;
                            break;
                    }

                    if (selectedFigureRightNow != null && selectedCityRightNow != null) {
                        Vehicle connection = gameState.getStreet(selectedFigureRightNow.getPlayer().getPlace(), selectedCityRightNow.getPlace());

                        if (connection == freeTicket) {
                            changePhase(GAME_PHASE.DET_EXTRA_TURN_CONFIRM);
                        }
                    }

                    break;
                }

                case DET_EXTRA_TURN_CONFIRM: {
                    Vehicle freeTicket;

                    switch (abilityPower) {
                        case 0:
                            freeTicket = Vehicle.SLOW;
                            break;
                        case 1:
                            freeTicket = Vehicle.MEDIUM;
                            break;
                        default:
                            freeTicket = Vehicle.FAST;
                            break;
                    }

                    if (selectedFigureRightNow == null || selectedCityRightNow == null ||
                            gameState.getStreet(selectedFigureRightNow.getPlayer().getPlace(), selectedCityRightNow.getPlace()) != freeTicket) {
                        changePhase(GAME_PHASE.DET_EXTRA_TURN_CHOOSE_TURN);
                    }
                    break;
                }

                case DET_EXTRA_TURN_MOVE: {

                    gameState.doFreeMove(toTravel.getPlayer().getPort(), toTravelTo.getPlace());

                    toTravel.deselect();

                    // Check win-condition right after detective moved (waiting until all detectives have moved would be really weird for the player
                    changePhase(GAME_PHASE.DET_EXTRA_TURN_WIN_CHECK);
                    break;
                }

                case DET_EXTRA_TURN_WIN_CHECK: {
                    if (gameState.isGameWon()) {
                        changePhase(GAME_PHASE.DET_WON);
                    } else {
                        changePhase(GAME_PHASE.DET_THROW_TICKETS);
                    }
                    break;
                }

                case DET_THROWING_SELECTED_TICKETS: {
                    Vector<Ticket> inventory = new Vector<>(gameState.getInventory());

                    for (int i = 0; i < toUseForTravel.size(); i++) {

                        // update user UI inventory
                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onRemove(inventory.indexOf(toUseForTravel.get(i)));
                        }

                        // remove from inventory in game-state
                        gameState.getInventory().remove(toUseForTravel.get(i));

                        // remove from model inventory
                        inventory.remove(toUseForTravel.get(i));
                    }

                    while (gameState.getInventory().size() < gameState.getDetectives().size() + 4) {
                        Ticket toGive = gameState.drawTicket();

                        // add to model inventory
                        inventory.add(toGive);

                        int ticketIndex = inventory.indexOf(toGive);

                        // add to user UI inventory
                        if (inventoryChangeListener != null) {
                            inventoryChangeListener.onAdd(toGive);
                        }

                        // add to inventory in game-state
                        gameState.giveTicket(currDetective.getPort(), toGive);
                    }

                    firstPhaseIteration = true;

                    // let user take a look at his new tickets and the marvelous animation of removing and adding tickets to/from inventory
                    waitForClick(GAME_PHASE.DET_MRX_TRANSITION);

                    break;
                }

                case DET_NO_VALID_MOVE: {

                    if (currDetective.getPort() == gameState.getDetectives().lastElement().getPort()) { // avoiding equals here
                        waitForClick(GAME_PHASE.DET_SELECT_NEXT);
                    } else {
                        int detIndex = gameState.getDetectives().indexOf(currDetective);
                        currDetective = gameState.getDetectives().get(detIndex + 1);
                        waitForClick(GAME_PHASE.DET_CHOOSE_MOVE);
                    }

                    break;
                }

                case MRX_WON:
                case DET_WON: {
                    showMrX = true;
                    waitForClick(GAME_PHASE.GAME_OVER);
                    break;
                }

                default:
                    break;
            }
        }
    }

    public void setPlaying(final boolean playing) {
        this.playing = playing;
    }

    private void fillInventory() {
        while (gameState.getInventory().size() < 4 + gameState.getDetectives().size()) {
            Ticket toAdd = gameState.drawTicket();
            gameState.giveTicket(1, toAdd);
        }
    }

    private void fillInventoryX() {
        while (gameState.getInventoryX().size() < 8) {
            Ticket toAdd = gameState.drawTicket();
            gameState.giveTicket(0, toAdd);
        }
    }

    private boolean hasValidMove(final Place from, final int port) {
        Vector<Ticket> inventory;
        boolean isMrX = port == 0;
        if(isMrX) {
            inventory = gameState.getInventoryX();
        } else {
            inventory = gameState.getInventory();
        }

        Vector<Place> neighbours = gameState.getSurroundingPlaces(from);

        // look at neighbours
        for (int i = 0; i < neighbours.size(); i++) {
            // get ticket needed to visit current neighbour
            Vehicle connection = gameState.getStreet(from, neighbours.get(i));

            boolean placeOccupied = false;
            if(isMrX) {
                for(int j = 0; j < gameState.getDetectives().size(); j++) {
                    if(neighbours.get(i) == gameState.getDetectives().get(j).getPlace()) {
                        placeOccupied = true;
                    }
                }
            }

            if(!isMrX || !placeOccupied) {
                // look into inventory
                for (int j = 0; j < inventory.size(); j++) {
                    // if needed ticket is in inventory
                    if (connection == inventory.get(j).getVehicle()) {
                        // that's a valid move
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasValidMove(final Player toCheckFor) {
        return hasValidMove(toCheckFor.getPlace(), toCheckFor.getPort());
    }

    public void confirmSelection() {
        toUseForTravel = new Vector<>(selectedTickets);
        toTravel = selectedFigure;
        toTravelTo = selectedCity;


        switch (currPhase) {
            case MRX_MOVE_CONFIRM: {
                // if city was deselected in the meantime
                if (toTravelTo == null) {
                    return;
                }

                selectedCity.deselect();
                selectedCity = null;

                changePhase(GAME_PHASE.MRX_MOVE);
                break;
            }
            case MRX_ABILITY_CONFIRM: {
                if (toUseForTravel.size() == 3) {
                    if (toUseForTravel.get(0).getAbility() == Ability.EXTRA_TURN) {
                        changePhase(GAME_PHASE.MRX_EXTRA_TURN);
                    } else {
                        changePhase(GAME_PHASE.MRX_SPECIAL);
                    }
                }
                break;
            }
            case MRX_EXTRA_TURN_CONFIRM: // The conditions were checked before, just set the selectedCity and Ticket to use
            case MRX_SPECIAL_CONFIRM:
            case DET_MOVE_CONFIRM: {

                if (selectedCity != null) {
                    selectedCity.deselect();
                    selectedCity = null;
                }

                if (currPhase == GAME_PHASE.MRX_SPECIAL_CONFIRM) {
                    changePhase(GAME_PHASE.MRX_SPECIAL_MOVE);
                } else if (currPhase == GAME_PHASE.MRX_EXTRA_TURN_CONFIRM) {
                    changePhase(GAME_PHASE.MRX_EXTRA_TURN_MOVE);
                } else if (currPhase == GAME_PHASE.DET_MOVE_CONFIRM) {
                    changePhase(GAME_PHASE.DET_MOVE);
                }
                break;
            }

            case MRX_NO_VALID_MOVE_CHOOSE_TURN: {
                changePhase(GAME_PHASE.MRX_THROW_TICKETS);
                break;
            }

            case MRX_NO_VALID_MOVE_SPECIAL_ACTIVATION_CONFIRM: {
                changePhase(GAME_PHASE.MRX_SPECIAL);
                break;
            }

            case MRX_THROW_TICKETS: {
                changePhase(GAME_PHASE.MRX_THROWING_SELECTED_TICKETS);
                break;
            }

            case DET_THROW_TICKETS: {
                changePhase(GAME_PHASE.DET_THROWING_SELECTED_TICKETS);
                break;
            }

            case DET_SELECT_NEXT: {
                changePhase(GAME_PHASE.DET_THROW_TICKETS);
                break;
            }

            case DET_ABILITY_CONFIRM: {
                if(toUseForTravel.size() >= 3 && toUseForTravel.size() <= 5) {
                    if(toUseForTravel.get(0).getAbility() == Ability.EXTRA_TURN) {
                        changePhase(GAME_PHASE.DET_EXTRA_TURN);
                    } else {
                        changePhase(GAME_PHASE.DET_SPECIAL);
                    }
                }
                break;
            }

            case DET_EXTRA_TURN_CONFIRM: {
                changePhase(GAME_PHASE.DET_EXTRA_TURN_MOVE);
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

            if (!scaling) {
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
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

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
        if (currPhase == GAME_PHASE.INTERRUPTED || currPhase == GAME_PHASE.WAIT_FOR_CLICK) {
            // When the text is fully displayed and the user is allowed to continue
            if (continuable) {
                synchronized (this) {
                    Log.d("waitForClicks", "Before: " + currPhase);
                    Log.d("waitForClicks", "After: " + nextPhase);
                    changePhase(nextPhase);
                    nextPhase = null;
                    currAlpha = 0;
                    return false;
                }
            }
        } else {

            scaleDetector.onTouchEvent(e);
            scrollDetector.onTouchEvent(e);

            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (!scrolling && !scaling) {

                    // When the extra turn ability of the detectives is activated we want to be able to select figures
                    boolean madeFigureSelection = false;
                    if (currPhase == GAME_PHASE.DET_EXTRA_TURN_CHOOSE_TURN || currPhase == GAME_PHASE.DET_EXTRA_TURN_CONFIRM) { // These phases require figure-selection to be possible

                        for (int i = 0; i < gameState.getDetectives().size(); i++) {
                            Player player = gameState.getDetectives().get(i);

                            if (player.getFigure().collisionCheck((e.getX() + (-viewPortX)) * (1 / mapScaleFactor), (e.getY() + (-viewPortY)) * (1 / mapScaleFactor))) {
                                // If there was already a figure selected, undo the selected-animation first
                                if (selectedFigure != null) {
                                    selectedFigure.deselect();
                                }
                                // If clicked on already selected figure, deselect it
                                if (player.getFigure().equals(selectedFigure)) {
                                    selectedFigure = null;
                                    // If clicked on a different figure, select that one
                                } else {
                                    selectedFigure = player.getFigure();
                                    selectedFigure.select();
                                }
                                madeFigureSelection = true;
                            }
                        }
                    }

                    // Same thing for cities, but they can be selected in every phase (well except while interrupted and wait_for_click)
                    boolean madeCitySelection = false;
                    if(!madeFigureSelection) { // So user can't accidentally click a city when selecting a figure
                        for (int i = 0; i < gameState.getPlaces().size(); i++) {
                            Place place = gameState.getPlaces().get(i);
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
                    }

                    // If no place or figure was clicked/the background was clicked, deselect city and figure
                    if (!madeCitySelection && !madeFigureSelection) {
                        if (selectedCity != null) {
                            selectedCity.deselect();
                            selectedCity = null;
                        }
                        if (selectedFigure != null) {
                            selectedFigure.deselect();
                            selectedFigure = null;
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
        for (Place toUpdate : gameState.getPlaces()) {
            toUpdate.getCity().update();
        }
        for (Player toUpdate : gameState.getDetectives()) {
            toUpdate.getFigure().update();
        }
        if (gameState.getMrX() != null) {
            gameState.getMrX().getFigure().update();
        }
    }

    // --- Map (Debug/Testing) ---
    // private Paint onlyBorders = new Paint();
    // float massstab = 8f;
    @ColorInt
    int currAlpha = 0;

    protected void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

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

            // Draw marking of marked cities
            for (int i = 0; i < markedCities.size(); i++) {
                City city = markedCities.get(i);

                float padding = city.getWidth() * 0.1f;

                // Colors will just repeat once we run out of them...
                paint.setColor(markColorCoding[i++ % markColorCoding.length]);

                canvas.drawOval(city.getX() - padding, city.getY() - padding, city.getX() + city.getWidth() + padding, city.getY() + city.getHeight() + padding, paint);
                paint.setColor(Color.WHITE);
                canvas.drawOval(city.getX(), city.getY(), city.getX() + city.getWidth(), city.getY() + city.getHeight(), paint);
            }
            paint.setColor(prevColor);

            // Draw Cities
            for (int i = 0; i < gameState.getPlaces().size(); i++) {
                gameState.getPlaces().get(i).getCity().draw(canvas, paint);
            }

            // Draw detective Figures
            for (Player toDraw : gameState.getDetectives()) {
                toDraw.getFigure().draw(canvas, paint);
            }
            // Draw Mr. X Figure
            if (showMrX) {
                if (gameState.getMrX() != null) {
                    gameState.getMrX().getFigure().draw(canvas, paint);
                }
            }

            // --- Map (Debug/Testing, unfinished) ---
            // onlyBorders.setStyle(Paint.Style.STROKE);
            // onlyBorders.setStrokeWidth(15);
            // canvas.drawRect((-viewPortX + getWidth() - (mapWidth / massstab)), -viewPortY, -viewPortX + getWidth(), -viewPortY + (mapHeight / massstab), onlyBorders);
            // canvas.drawRect((-viewPortX + getWidth() - (mapWidth / massstab)) + (-viewPortX / massstab), -viewPortY + (-viewPortY / massstab), (-viewPortX + getWidth() - (mapWidth / massstab)) + (-viewPortX / massstab) + (getWidth() / massstab * (1 / mapScaleFactor)), -viewPortY + (-viewPortY / massstab) + (getHeight() / massstab), onlyBorders);
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
        changePhase(GAME_PHASE.INTERRUPTED, phaseAfter);
        continuable = false;

        userMessage = message;
    }

    public void waitForClick(final GAME_PHASE phaseAfter) {
        changePhase(GAME_PHASE.WAIT_FOR_CLICK, phaseAfter);
        continuable = true;
    }

    public synchronized void changePhase(final GAME_PHASE phase, final GAME_PHASE phaseAfter) {
        changePhase(phase);
        nextPhase = phaseAfter;
    }
    public synchronized void changePhase(final GAME_PHASE phase) {
        currPhase = phase;
        Log.d("PhaseChange", currPhase.toString());
        if(phaseChangeListener != null) {
            phaseChangeListener.onPhaseChange(phase);
        }
    }

    // ----------- GETTERS/SETTERS -----------

    public GameState getGameState() {
        return gameState;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setContinuable(boolean continuable) {
        this.continuable = continuable;
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
