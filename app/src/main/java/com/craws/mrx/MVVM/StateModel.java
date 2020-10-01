package com.craws.mrx.MVVM;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.craws.mrx.state.Ability;
import com.craws.mrx.state.GameState;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Player;
import com.craws.mrx.state.ShadowTicket;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Vehicle;
import com.craws.tree.Edge;

import java.util.Vector;

public class StateModel {

    private GameState gameState;

    // inform ViewModel (and anyone who wants to know) about changes
    private Vector<StateModelObserver> observers;
    private MutableLiveData<Boolean> liveReady;

    // ---- Game-Phases ---
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


    // --- Game Loop ---
    Thread gameThread;
    private boolean playing;

    // The user-message "pauses"
    private boolean continuable;

    // Game phase management
    private GAME_PHASE currPhase;
    private GAME_PHASE nextPhase; // while showing the user message (GAME_PHASE.INTERRUPTED) we gotta remember what the next phase will be

    private Player highlightedPlayer; // for user to see which player's turn it is (especially detectives)

    // Selected Players/Places/Tickets, means for the user to interact with the game.
    private Player toTravel;
    private Place toTravelTo;
    private Vector<Ticket> toUseForTravel = new Vector<>();

    // keeping track of the current state of the game
    private boolean firstTurn = true;

    private int currentRound = 0;
    private boolean firstPhaseIteration = true;
    private Player currDetective;
    private int extraTurnCounter = 1;
    private int abilityPower;

    public StateModel() {
        gameState = new GameState();

        observers = new Vector<>();

        liveReady = new MutableLiveData<>(false);
    }

    public void setupGame() {
        // ---=== The game-state to display ==---
        currPhase = GAME_PHASE.DET_MRX_TRANSITION;
        nextPhase = GAME_PHASE.DET_MRX_TRANSITION;
        continuable = true;

        // helpers (like selectedTickets(RightNow) and selectedCity(RightNow), but not overwritten every game loop iteration)
        toTravel = null;
        toTravelTo = null;
        toUseForTravel = new Vector<>();

        // TODO ------------=================== DELETE LATER ====================-------------

        Place pl_kiel = addPlace("Kiel", false);
        Place pl_bremen = addPlace("Bremen", false);
        Place pl_hanno = addPlace("Hannover", false);
        Place pl_nonne = addPlace("Nonne Stadt", false);
        Place pl_murica = addPlace("Murica", true);
        Place pl_hexter = addPlace("Hexter", false);
        Place pl_berlin = addPlace("Berlin", false);

        Place pl_bridge = addPlace("Grossseistadt");
        Place pl_forgotten_island = addPlace("Nowheresville", false);
        Place pl_sylt = addPlace("Sylt", false);
        Place pl_newYork = addPlace("New York", false);
        Place pl_washington = addPlace("Washington", false);
        Place pl_tristate = addPlace("Tricity", false);

        addStreet(pl_nonne, pl_bremen, Vehicle.MEDIUM);
        addStreet(pl_nonne, pl_hanno, Vehicle.SLOW);
        addStreet(pl_nonne, pl_murica, Vehicle.MEDIUM);
        addStreet(pl_nonne, pl_hexter, Vehicle.SLOW);
        addStreet(pl_berlin, pl_hexter, Vehicle.SLOW);
        addStreet(pl_kiel, pl_bremen, Vehicle.MEDIUM);
        addStreet(pl_kiel, pl_berlin, Vehicle.FAST);

        addStreet(pl_forgotten_island, pl_sylt, Vehicle.MEDIUM);
        addStreet(pl_forgotten_island, pl_newYork, Vehicle.FAST);
        addStreet(pl_forgotten_island, pl_washington, Vehicle.FAST);
        addStreet(pl_sylt, pl_newYork, Vehicle.FAST);
        addStreet(pl_newYork, pl_washington, Vehicle.SLOW);
        addStreet(pl_forgotten_island, pl_berlin, Vehicle.SLOW);

        addStreet(pl_bridge, pl_kiel, Vehicle.SLOW);
        addStreet(pl_bridge, pl_sylt, Vehicle.MEDIUM);
        addStreet(pl_kiel, pl_sylt, Vehicle.FAST);

        addStreet(pl_tristate, pl_forgotten_island, Vehicle.MEDIUM);
        addStreet(pl_tristate, pl_sylt, Vehicle.SLOW);
        addStreet(pl_tristate, pl_newYork, Vehicle.MEDIUM);


        addDetective("Detestive", pl_hanno);
        addDetective("DetTwo", pl_kiel);
        Player mrx = addMrX(pl_forgotten_island);

        fillInventoryX();
        fillInventory();

        liveReady.setValue(true);
    }



    public void startGame() {
        // true as long as the game is not won/lost
        playing = true;

        // Since we can also go back in phases, this flag is to only show the notification whose turn it is once.
        firstPhaseIteration = true;

        // Set start position of Mr. X
        gameState.getTimeline().addRound(null, gameState.getMrX().getPlace());

        // Notify Observers
        onTimelineEntryAdded(null, gameState.getMrX().getPlace());

        gameThread = new Thread(this::gameLoop);
        gameThread.start();
    }

    // ----===== GAME LOOP =====-----
    Place selectedPlace;
    Player selectedPlayer;
    Vector<Ticket> selectedTickets = new Vector<>();

    public void gameLoop() {
        while (playing) {

            Place selectedPlaceRightNow = selectedPlace;
            Vector<Ticket> selectedTicketsRightNow = selectedTickets;
            Player selectedPlayerRightNow = selectedPlayer;

            switch (currPhase) {

                case DET_MRX_TRANSITION: {

                    currentRound++;
                    showMessageAndWaitForClick("Mr. X's turn. Detectives don't look.", GAME_PHASE.MRX_CHOOSE_TURN);
                    break;
                }

                case MRX_CHOOSE_TURN: { // ----===== Mr. X's turn starts =====-----
                    if (firstPhaseIteration) {

                        // Notify Observers
                        if(firstTurn) {
                            onInventorySetStartInventory(gameState.getInventoryX());
                            firstTurn = false;
                        } else {
                            onInventoryChangeActiveInventory(gameState.getInventoryX());
                        }


                        firstPhaseIteration = false;

                    }

                    if(highlightedPlayer != gameState.getMrX()) {
                        onPlayerActivePlayerChanged(gameState.getMrX());
                    }

                    if (!hasValidMove(gameState.getMrX())) {
                        changePhase(GAME_PHASE.MRX_NO_VALID_MOVE);
                        break;
                    }

                    // City was selected
                    if (selectedPlaceRightNow != null) {
                        // is city connected to MrX's current position?
                        Vehicle connection = gameState.getStreet(gameState.getMrX().getPlace(), selectedPlaceRightNow);

                        // is city empty (no detective)
                        boolean cityFree = true;
                        for(int i = 0; i < gameState.getDetectives().size(); i++) {
                            if(gameState.getDetectives().get(i).getPlace() == selectedPlaceRightNow) {
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
                    if (selectedPlaceRightNow == null || gameState.getStreet(gameState.getMrX().getPlace(), selectedPlaceRightNow) == null) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                    } else if (selectedTicketsRightNow.size() == 1) {
                        if (selectedTicketsRightNow.get(0).getVehicle() == gameState.getStreet(gameState.getMrX().getPlace(), selectedPlaceRightNow)) {
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
                    } else if (selectedPlaceRightNow != null && selectedTicketsRightNow.size() == 1) {
                        // is city connected to MrX's current position?
                        Vehicle connection = gameState.getStreet(gameState.getMrX().getPlace(), selectedPlaceRightNow);
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
                    if (selectedTicketsRightNow == null || selectedTicketsRightNow.size() != 1 || selectedPlaceRightNow == null) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_TURN);
                        // Also go back if a city gets selected that is not connected to Mr. X's position or the connecting street can't be travelled with the selected ticket
                    } else if (gameState.getStreet(gameState.getMrX().getPlace(), selectedPlaceRightNow) != selectedTicketsRightNow.get(0).getVehicle()) {
                        changePhase(GAME_PHASE.MRX_CHOOSE_ABILITY_TICKETS);
                    }
                    break;
                }

                case MRX_MOVE: { // ----===== move got confirmed and the tickets and place saved =====-----
                    // GameState uses Vector.remove to take Ticket. This uses the first occurrence just as Vector.indexOf, so we use that to get the index.
                    Vector<Ticket> inventory = new Vector<>(gameState.getInventoryX());

                    int ticketIndex = inventory.indexOf(toUseForTravel.get(0));
                    inventory.remove(ticketIndex);
                    gameState.doMove(0, toTravelTo, toUseForTravel.get(0));

                    // Notify Observers
                    onPlayerMove(gameState.getPlayerByPort(0), toTravelTo);
                    onInventoryTicketRemoved(ticketIndex);
                    onTimelineEntryAdded(toUseForTravel.get(0), toTravelTo);

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
                            if(gameState.getDetectives().get(j).getPlace() == selectedPlaceRightNow) {
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

                        // Notify Observers
                        onInventoryTicketRemoved(inventory.indexOf(currTicket));

                        inventory.remove(currTicket);
                    }

                    gameState.activateAbility(0, toUseForTravel, Ability.EXTRA_TURN);
                    changePhase(GAME_PHASE.MRX_EXTRA_TURN_CHOOSE_TURN);

                    break;
                }

                case MRX_EXTRA_TURN_CHOOSE_TURN: { // ----===== extra turn ability activate and tickets thrown. Wait for user to make his turn =====-----

                    if (selectedPlaceRightNow != null) {
                        Vehicle connection = gameState.getStreet(gameState.getMrX().getPlace(), selectedPlaceRightNow);
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
                    if (selectedPlaceRightNow == null ||
                            selectedTicketsRightNow == null || selectedTicketsRightNow.size() != 1 ||
                            selectedTicketsRightNow.get(0).getVehicle() != gameState.getStreet(gameState.getMrX().getPlace(), selectedPlaceRightNow)) {
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
                        canMoveFromGoal = hasValidMove(toTravelTo, gameState.getMrX().getPort());
                    }

                    // this only applies on the first of the two extra turns (which is why I used the NOT-expression for locality)
                    if (!canMoveFromGoal) {
                        // since we reached this point, there is a valid move (else we wouldn't let the user throw the tickets for this ability),
                        // so let the user know he can't do this turn and then go back to MRX_EXTRA_TURN_CHOOSE_TURN
                        changePhase(GAME_PHASE.MRX_EXTRA_TURN_ONE_NOT_POSSIBLE);
                    } else {

                        Vector<Ticket> inventory = new Vector<>(gameState.getInventoryX());

                        // remember ticket
                        int ticketIndex = inventory.indexOf(toUseForTravel.get(0));
                        inventory.remove(ticketIndex);

                        // let the game move Mr. X
                        gameState.doMove(0, toTravelTo, toUseForTravel.get(0));

                        // Notify Observers
                        onInventoryTicketRemoved(ticketIndex);
                        onPlayerMove(gameState.getPlayerByPort(0), toTravelTo);
                        onTimelineEntryAdded(toUseForTravel.get(0), toTravelTo);

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

                        // Notify Observers
                        onInventoryTicketRemoved(ticketIndex);

                        inventory.remove(ticketIndex);
                    }

                    gameState.activateAbility(0, toUseForTravel, Ability.SPECIAL);

                    changePhase(GAME_PHASE.MRX_SPECIAL_CHOOSE_CITY);

                    break;
                }
                case MRX_SPECIAL_CHOOSE_CITY: { // ----===== Waiting for the user to select a City to be travelled to anonymously =====-----
                    if (selectedPlaceRightNow != null && gameState.getStreet(gameState.getMrX().getPlace(), selectedPlaceRightNow) != null) {
                        changePhase(GAME_PHASE.MRX_SPECIAL_CONFIRM);
                    }
                    break;
                }
                case MRX_SPECIAL_CONFIRM: { // ----===== Waiting for the player to confirm the selected city (unselecting City -> ask user for city again) =====-----
                    // if city gets deselected or other (non-reachable) city gets selected, go back
                    if (selectedPlaceRightNow == null || gameState.getStreet(gameState.getMrX().getPlace(), selectedPlaceRightNow) == null) {
                        changePhase(GAME_PHASE.MRX_SPECIAL_CHOOSE_CITY);
                    }
                    break;
                }
                case MRX_SPECIAL_MOVE: { // ----===== Shadow Ticket move got confirmed. Move, put ticket in timeline =====-----
                    Ticket ticketOfShadows = new ShadowTicket();
                    gameState.doFreeMove(0, toTravelTo);
                    gameState.getTimeline().addRound(ticketOfShadows, toTravelTo);

                    // Notify Observers
                    onPlayerMove(gameState.getPlayerByPort(0), toTravelTo);
                    onTimelineEntryAdded(ticketOfShadows, toTravelTo);

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

                        // Notify Observers
                        onInventoryTicketRemoved(inventory.indexOf(toUseForTravel.get(i)));

                        // remove from inventory in game-state
                        gameState.getInventoryX().remove(toUseForTravel.get(i));

                        // remove from model inventory
                        inventory.remove(toUseForTravel.get(i));
                    }

                    while (gameState.getInventoryX().size() < 8) {
                        Ticket toGive = gameState.drawTicket();

                        // add to model inventory
                        inventory.add(toGive);

                        // add to inventory in game-state
                        gameState.giveTicket(0, toGive);

                        // Notify Observers
                        onInventoryTicketAdded(toGive);

                    }

                    // let user take a look at his new tickets and the marvelous animation of removing and adding tickets to/from inventory
                    waitForClick(GAME_PHASE.MRX_DET_TRANSITION);

                    break;
                }

                case MRX_DET_TRANSITION: {
                    firstPhaseIteration = true;
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

                        currDetective = gameState.getDetectives().get(0);

                        // Notify Observers
                        onInventoryChangeActiveInventory(gameState.getInventory());

                        firstPhaseIteration = false;

                        break;
                    }

                    if(highlightedPlayer != currDetective) {
                        onPlayerActivePlayerChanged(currDetective);
                    }

                    if (!hasValidMove(currDetective)) {
                        changePhase(GAME_PHASE.DET_NO_VALID_MOVE);
                        break;
                    }

                    // since first all detectives move and then an ability can be activated, we can just
                    // check for a neighbouring city and one ticket (matching the street) selected.
                    if (selectedPlaceRightNow != null && selectedTicketsRightNow != null && selectedTicketsRightNow.size() == 1) {
                        Vehicle connection = gameState.getStreet(currDetective.getPlace(), selectedPlaceRightNow);
                        if (connection != null && connection == selectedTicketsRightNow.get(0).getVehicle()) {
                            changePhase(GAME_PHASE.DET_MOVE_CONFIRM);
                        }
                    }
                    break;
                }

                case DET_MOVE_CONFIRM: {
                    // check if there is still a move to confirm
                    if (selectedPlaceRightNow == null || selectedTicketsRightNow == null || selectedTicketsRightNow.size() != 1) {
                        changePhase(GAME_PHASE.DET_CHOOSE_MOVE);
                    } else {
                        Vehicle connection = gameState.getStreet(currDetective.getPlace(), selectedPlaceRightNow);
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
                    gameState.doMove(currDetective.getPort(), toTravelTo, toUseForTravel.get(0));

                    // Notify Observers
                    onInventoryTicketRemoved(ticketIndex);
                    onPlayerMove(currDetective, toTravelTo);

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
                        currDetective = gameState.getDetectives().get(detIndex + 1); // Observers get notified about active player change in DET_CHOOSE_MOVE

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

                        // Notify Observers
                        onInventoryTicketRemoved(ticketIndex);

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

                    // Notify Observers
                    onTimelineEntryMarked(indexToMark); // 1: current round, 2: last round, ..., timeline.size: start-position

                    //markedPlaces.add(gameState.getTimeline().getPlaceForRound(indexToMark));

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

                        // Notify Observers
                        onInventoryTicketRemoved(ticketIndex);

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

                    if (selectedPlayerRightNow != null && selectedPlaceRightNow != null) {
                        Vehicle connection = gameState.getStreet(selectedPlayerRightNow.getPlace(), selectedPlaceRightNow);

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

                    if (selectedPlayerRightNow == null || selectedPlaceRightNow == null ||
                            gameState.getStreet(selectedPlayerRightNow.getPlace(), selectedPlaceRightNow) != freeTicket) {
                        changePhase(GAME_PHASE.DET_EXTRA_TURN_CHOOSE_TURN);
                    }
                    break;
                }

                case DET_EXTRA_TURN_MOVE: {

                    gameState.doFreeMove(toTravel.getPort(), toTravelTo);

                    // Notify Observers
                    onPlayerMove(toTravel, toTravelTo);


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

                        // Notify Observers
                        onInventoryTicketRemoved(inventory.indexOf(toUseForTravel.get(i)));

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

                        // Notify Observers
                        onInventoryTicketAdded(toGive);

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
                    waitForClick(GAME_PHASE.GAME_OVER);
                    break;
                }
                case GAME_OVER: {
                    gameOverScreen();
                }

                default:
                    break;
            }
        }
    }

    public void confirmSelection() {

        // pause main loop to confirm
        playing = false;

        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        toUseForTravel = new Vector<>(selectedTickets);
        toTravel = selectedPlayer;
        toTravelTo = selectedPlace;


        switch (currPhase) {

            case MRX_MOVE_CONFIRM: {
                // if city was deselected in the meantime
                if (toTravelTo == null) {
                    return;
                }

                selectedPlace = null;

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
            case MRX_SPECIAL_CONFIRM: // The conditions were checked before, just set the selectedCity and Ticket to use
            case MRX_EXTRA_TURN_CONFIRM:
            case DET_MOVE_CONFIRM: {

                if (currPhase == GAME_PHASE.MRX_SPECIAL_CONFIRM) {
                    changePhase(GAME_PHASE.MRX_SPECIAL_MOVE);
                } else if (currPhase == GAME_PHASE.MRX_EXTRA_TURN_CONFIRM) {
                    changePhase(GAME_PHASE.MRX_EXTRA_TURN_MOVE);
                } else {
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

        playing = true;
        gameThread = new Thread(this::gameLoop);
        gameThread.start();
    }


    // ----==== GAME LOOP ====----
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


    // ---- PHASE MANAGEMENT ----

    /** Blacks out screen and shows message until screen is touched.
     * To communicate with human players and hide the screen from detective before Mr. X takes over.
     *
     * @param message The message to show the player until he touches the screen.
     */
    private void showMessageAndWaitForClick(final String message, final GAME_PHASE phaseAfter) {
        // liveUserMessage.postValue(message);
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onUserMessageChange(message);
        }

        changePhase(GAME_PHASE.INTERRUPTED, phaseAfter);
        continuable = false;
    }

    private void waitForClick(final GAME_PHASE phaseAfter) {
        changePhase(GAME_PHASE.WAIT_FOR_CLICK, phaseAfter);
        continuable = true;
    }

    public void receiveAwaitedClick() {
        if (continuable) {
            synchronized (this) {
                Log.d("waitForClicks", "Before: " + currPhase);
                Log.d("waitForClicks", "After: " + nextPhase);
                changePhase(nextPhase);
                nextPhase = null;

            }
        }
    }

    public void gameOverScreen() {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onUserMessageChange("GAME OVER, YAY!");
        }
    }

    private synchronized void changePhase(final GAME_PHASE phase, final GAME_PHASE phaseAfter) {
        changePhase(phase);
        nextPhase = phaseAfter;
    }

    // ------ Map building helper (notifying the observers) ------

    public Player addDetective(final String name, final Place startPosition) {
        int playerId = gameState.addDetective(name, startPosition);

        Player player = gameState.getPlayerByPort(playerId);

        statePlayerAdded(player);

        return player;
    }

    public Player addMrX(final Place startPosition) {
        int playerId = gameState.addMrX(startPosition);

        Player player = gameState.getPlayerByPort(playerId);

        statePlayerAdded(player);

        return player;
    }

    public Place addPlace (final String name, final boolean goal) {
        Place place = gameState.buildPlace(name, goal);

        statePlaceAdded(place);

        return place;
    }
    public Place addPlace (final String name) {
        return addPlace(name, false);
    }

    public Edge<Place, Vehicle> addStreet (final Place start, final Place end, final Vehicle ticket) {
        Edge<Place, Vehicle> street = gameState.buildStreet(start, end, ticket);

        stateStreetAdded(street);

        return street;
    }

    // --------====== OBSERVER MANAGEMENT =======------
    public boolean registerObserver(final StateModelObserver observer) {
        return observers.add(observer);
    }

    // ------ OBSERVER NOTIFICATION ------
    // Map events
    private void onPlayerMove(final Player player, final Place target) {
        statePlayerMoved(player, target);
    }

    private void onPlayerActivePlayerChanged(final Player newActivePlayer) {
        highlightedPlayer = newActivePlayer;
        statePlayerNewActivePlayer(newActivePlayer);
    }

    // --- Phases ---
    private synchronized void changePhase(final GAME_PHASE phase) {
        currPhase = phase;
        Log.d("PhaseChange", currPhase.toString());

        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onPhaseChange(phase);
        }
    }

    // --- Timeline ---

    private void onTimelineEntryAdded(final Ticket ticketUsed, final Place movedTo) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onTimelineEntryAdded(ticketUsed, movedTo);
        }
    }

    private void onTimelineEntryMarked(final int position) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onTimelineEntryMarked(position);
        }
    }


    // --- Inventory ---

    private void onInventoryChangeActiveInventory(final Vector<Ticket> newInventory) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onInventoryChanged(newInventory);
        }
    }

    private void onInventoryTicketRemoved(final int position) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onInventoryTicketRemoved(position);
        }
    }

    private void onInventoryTicketAdded(final Ticket newTicket) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onInventoryTicketAdded(newTicket);
        }
    }

    private void onInventorySetStartInventory(final Vector<Ticket> startInventory) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onInventoryLoadFirst(startInventory);
        }
    }


    // --- Map Changes ---

    private void statePlaceAdded(final Place place) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onPlaceAdded(place);
        }
    }

    private void stateStreetAdded(final Edge<Place, Vehicle> street) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onStreetAdded(street);
        }
    }

    private void statePlayerAdded(final Player player) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onPlayerAdded(player);
        }
    }

    private void statePlayerMoved(final Player player, final Place movedTo) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onPlayerMoved(player, movedTo);
        }
    }

    private void statePlayerNewActivePlayer(final Player player) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onPlayerActivePlayerChanged(player);
        }
    }



    // ---- Impure GETTERS ----
    public Player getByPort(final int port) {
        return gameState.getPlayerByPort(port);
    }


    // ---- GETTERS/SETTERS ----

    public Thread getGameThread() {
        return gameThread;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public MutableLiveData<Boolean> getLiveReady() {
        return liveReady;
    }

    public void setSelectedPlace(final Place selectedPlace) {
        this.selectedPlace = selectedPlace;
    }

    public void setSelectedPlayer(Player selectedPlayer) {
        this.selectedPlayer = selectedPlayer;
    }

    public void setSelectedTickets(Vector<Ticket> selectedTickets) {
        this.selectedTickets = selectedTickets;
    }

    public void setContinuable(boolean continuable) {
        this.continuable = continuable;
    }
}
