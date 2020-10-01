package com.craws.mrx.MVVM;

import android.app.Application;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.craws.mrx.engine.GameView;
import com.craws.mrx.engine.GameViewListener;
import com.craws.mrx.graphics.City;
import com.craws.mrx.graphics.Figure;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Player;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Timeline;
import com.craws.mrx.state.Vehicle;
import com.craws.tree.Edge;

import java.util.HashMap;
import java.util.Vector;

public class StateViewModel extends AndroidViewModel implements StateModelObserver, GameViewListener {
    private StateModel stateModel;

    // Collections of drawables
    private Vector<City> cities;
    private HashMap<Place, City> placeCityHashMap;
    private HashMap<City, Place> cityPlaceHashMap;

    private Vector<Figure> figures;

    private Vector<Street> streets;

    // To process user-input
    private StateModel.GAME_PHASE phaseObserver;

    private City selectedCity;
    private Figure selectedFigure;
    private Vector<City> markedCities;

    // --- Observer (for GameActivity/Views) ---
    private Vector<StateViewModelObserver> observers;

    // Simulated inventory for the recyclerView
    private Vector<Ticket> simulatedInventory;

    // Simulated inventory for the recyclerView
    private Timeline simulatedTimeline;

    // The user instruction depending on game-phase
    private MutableLiveData<String> userHelpText;

    // --- Drawing stuff ---
    private Paint paint;

    private boolean showMrX;

    // internal map/canvas dimensions
    private final static float mapWidth = 3700;
    private final static float mapHeight = 2000;

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

    // To be displayed by showMessageAndWaitForClick
    private String userMessage = "";


    // To control app-flow
    private boolean viewsReady;
    private boolean modelReady;

    public StateViewModel(Application app) {
        super(app);

        this.stateModel = new StateModel();
        stateModel.registerObserver(this);

        // --- Map ---
        // Cities
        cities = new Vector<>();
        placeCityHashMap = new HashMap<>();
        cityPlaceHashMap = new HashMap<>();

        // Players
        figures = new Vector<>();
        streets = new Vector<>();

        // Selecting and marking
        markedCities = new Vector<>();

        showMrX = true;

        // Observer data
        observers = new Vector<>();

        modelReady = false;
        viewsReady = false;

        stateModel.getLiveReady().observeForever(ready -> {
            modelReady = ready;
            if(viewsReady && modelReady) {
                Log.d("Setup", "Views were ready first");
                stateModel.startGame();
            }
        });

        simulatedInventory = new Vector<>();
        simulatedTimeline = new Timeline();

        userHelpText = new MutableLiveData<>("");

        // Setting up paint to draw with
        paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(GameView.DEFAULT_TXT_SIZE);
        paint.setStrokeWidth(20);

        stateModel.setupGame();
    }

    public void notifyReady() {
        viewsReady = true;
        if(modelReady) {
            stateModel.startGame();
            Log.d("Setup", "Model was ready first");
        }
    }

    // -----===== GAMEVIEWLISTENER IMPLEMENTATION =====-----
    // ---=== Selection by touch-handling ===---

    @Override
    public boolean onTouchAction(MotionEvent e) {

        // If a user message is displayed
        if (phaseObserver == StateModel.GAME_PHASE.INTERRUPTED || phaseObserver == StateModel.GAME_PHASE.WAIT_FOR_CLICK) {

            // Tell the stateModel about the click
            stateModel.receiveAwaitedClick();
            return false;

        } else {

            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (!scrolling && !scaling) {

                    // When the extra turn ability of the detectives is activated we want to be able to select figures
                    boolean madeFigureSelection = false;
                    if (phaseObserver == StateModel.GAME_PHASE.DET_EXTRA_TURN_CHOOSE_TURN || phaseObserver == StateModel.GAME_PHASE.DET_EXTRA_TURN_CONFIRM) { // These phases require figure-selection to be possible

                        for (int i = 0; i < figures.size(); i++) {
                            Figure figure = figures.get(i);

                            if (figure.collisionCheck((e.getX() + (-viewPortX)) * (1 / mapScaleFactor), (e.getY() + (-viewPortY)) * (1 / mapScaleFactor))) {
                                // If there was already a figure selected, undo the selected-animation first
                                if (selectedFigure != null) {
                                    selectedFigure.deselect();
                                }
                                // If clicked on already selected figure, deselect it
                                if (figure.equals(selectedFigure)) {
                                    selectedFigure = null;
                                    stateModel.setSelectedPlayer(null);
                                    // If clicked on a different figure, select that one
                                } else {
                                    selectedFigure = figure;
                                    selectedFigure.select();
                                    stateModel.setSelectedPlayer(stateModel.getByPort(figure.getPort()));
                                }
                                madeFigureSelection = true;
                            }
                        }
                    }

                    // Same thing for cities, but they can be selected in every phase (well except while interrupted and wait_for_click)
                    boolean madeCitySelection = false;
                    if(!madeFigureSelection) { // So user can't accidentally click a city when selecting a figure
                        for (int i = 0; i < cities.size(); i++) {
                            City city = cities.get(i);
                            // If a city was clicked
                            if (city.collisionCheck((e.getX() + (-viewPortX)) * (1 / mapScaleFactor), (e.getY() + (-viewPortY)) * (1 / mapScaleFactor))) {
                                // If a city was already selected deselect it first
                                if (selectedCity != null) {
                                    selectedCity.deselect();
                                }

                                // If clicked on the already selected city
                                if (city.equals(selectedCity)) {
                                    selectedCity = null;
                                    stateModel.setSelectedPlace(null);
                                    // If clicked on a different city
                                } else {
                                    selectedCity = city;
                                    selectedCity.select();
                                    stateModel.setSelectedPlace(cityPlaceHashMap.get(city));
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
                            stateModel.setSelectedPlace(null);
                        }
                        if (selectedFigure != null && (phaseObserver == StateModel.GAME_PHASE.DET_EXTRA_TURN_CHOOSE_TURN || phaseObserver == StateModel.GAME_PHASE.DET_EXTRA_TURN_CONFIRM)) {
                            selectedFigure.deselect();
                            selectedFigure = null;
                            stateModel.setSelectedPlayer(null);
                        }
                    }

                } else {
                    scrolling = false;
                    scaling = false;
                }
            }
        }

        // if the camera is scrolling right now stop
        if(viewPortTargetX != -1 || viewPortTargetY != -1) {
            viewPortTargetX = -1;
            viewPortTargetY = -1;
        }

        return true;
    }



    /*  ---===============================================---
   -----===== The Camera and Map implementation =====-----
    ---===============================================---
    See https://developer.android.com/training/gestures/scale#java
 */
    // The rectangle in which, the part of the map we're looking at, is displayed
    private RectF dstViewport = new RectF();

    // The position we move the map to (or rather the canvas' Matrix meaning moving to the right on the map is moving the Matrix to the left)
    private float viewPortX = 0f;
    private float viewPortY = 0f;

    private float viewPortTargetX = -1f;
    private float viewPortTargetY = -1f;

    // The further we zoom in the bigger the map gets scaled (and the smaller the viewport)
    private float mapScaleFactor = 1f;

    // When you scroll or scale you don't click
    private boolean scrolling = false;
    private boolean scaling = false;

    // The dimensions of the GameView/SurfaceView
    private int gameViewWidth = 0;
    private int gameViewHeight = 0;


    // ---=== Touch-handling ===---
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (phaseObserver == StateModel.GAME_PHASE.INTERRUPTED || phaseObserver == StateModel.GAME_PHASE.WAIT_FOR_CLICK) {
            return false;
        }

        float oldScaleFactor = mapScaleFactor;

        mapScaleFactor *= detector.getScaleFactor();
        mapScaleFactor = Math.max(0.4f, Math.min(mapScaleFactor, 2.0f));

        // to scale around the center of the screen and not fly across the map while scrolling
        // but don't scroll beyond bounds (just like in onScroll
        if (viewPortX - ((mapScaleFactor * mapWidth) - (oldScaleFactor * mapWidth)) / 2 > 0) {
            viewPortX = 0;
        } else if (viewPortX - ((mapScaleFactor * mapWidth) - (oldScaleFactor * mapWidth)) / 2 < ((mapWidth * mapScaleFactor) - (gameViewWidth)) * -1) {
            viewPortX = ((mapWidth * mapScaleFactor) - (gameViewWidth)) * -1;
        } else {
            viewPortX -= ((mapScaleFactor * mapWidth) - (oldScaleFactor * mapWidth)) / 2;
        }

        // Bounds Y-Axis
        if (viewPortY - ((mapScaleFactor * mapHeight) - (oldScaleFactor * mapHeight)) / 2 > 0) {
            viewPortY = 0;
        } else if (viewPortY - ((mapScaleFactor * mapHeight) - (oldScaleFactor * mapHeight)) / 2 < ((mapHeight * mapScaleFactor) - (gameViewHeight)) * -1) {
            viewPortY = ((mapHeight * mapScaleFactor) - (gameViewHeight)) * -1;
        } else {
            viewPortY -= ((mapScaleFactor * mapHeight) - (oldScaleFactor * mapHeight)) / 2;
        }

        scaling = true;

        return true;
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        if (phaseObserver == StateModel.GAME_PHASE.INTERRUPTED || phaseObserver == StateModel.GAME_PHASE.WAIT_FOR_CLICK) {
            return false;
        }
        if (!scaling) {
            // Bounds x-Axis
            if (viewPortX - distanceX > 0) {
                viewPortX = 0;
            } else if (viewPortX - distanceX < ((mapWidth * mapScaleFactor) - (gameViewWidth)) * -1) {
                viewPortX = ((mapWidth * mapScaleFactor) - (gameViewWidth)) * -1;
            } else {
                viewPortX -= distanceX;
            }

            // Bounds Y-Axis
            if (viewPortY - distanceY > 0) {
                viewPortY = 0;
            } else if (viewPortY - distanceY < ((mapHeight * mapScaleFactor) - (gameViewHeight)) * -1) {
                viewPortY = ((mapHeight * mapScaleFactor) - (gameViewHeight)) * -1;
            } else {
                viewPortY -= distanceY;
            }

            scrolling = true;
        }
        return true;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        gameViewWidth = width;
        gameViewHeight = height;

        dstViewport.set(0, 0, gameViewWidth, gameViewHeight);
    }


    public void notifyBtnConfirmClicked() {
        stateModel.confirmSelection();
    }


    // ---=== Rendering ===---
    @Override
    public void onUpdate() {
        for (int i = 0; i < cities.size(); i++) {
            cities.get(i).update();
        }

        for (int i = 0; i < figures.size(); i++) {
            figures.get(i).update();
        }

        // move camera to next player (viewportTargetY, set in onPlayerActivePlayerChanged
        if (viewPortTargetX != -1) {
            if (viewPortX < -viewPortTargetX) {
                if (viewPortX + 20 > 0) {
                    viewPortX = 0;
                    viewPortTargetX = -1;
                } else {
                    if (viewPortX + 20 >= -viewPortTargetX) {
                        viewPortX = -viewPortTargetX;
                        viewPortTargetX = -1;
                    } else {
                        viewPortX += 20;
                    }
                }

            } else if (viewPortX > -viewPortTargetX) {
                if (viewPortX - 20 < ((mapWidth * mapScaleFactor) - (gameViewWidth)) * -1) {
                    viewPortX = ((mapWidth * mapScaleFactor) - (gameViewWidth)) * -1;
                    viewPortTargetX = -1;
                } else {
                    if (viewPortX - 20 <= -viewPortTargetX) {
                        viewPortX = -viewPortTargetX;
                        viewPortTargetX = -1;
                    } else {
                        viewPortX -= 20;
                    }
                }
            } else {
                viewPortX = viewPortTargetX;
                viewPortTargetX = -1;
            }
        }

        if (viewPortTargetY != -1) {
            if (viewPortY < -viewPortY) {
                if (viewPortY + 20 > 0) {
                    viewPortY = 0;
                    viewPortTargetY = -1;
                } else {
                    if (viewPortY + 20 >= -viewPortTargetY) {
                        viewPortY = -viewPortTargetY;
                        viewPortTargetY = -1;
                    } else {
                        viewPortY += 20;
                    }
                }
            } else if (viewPortY > -viewPortY) {
                if (viewPortY - 20 < ((mapHeight * mapScaleFactor) - (gameViewHeight)) * -1) {
                    viewPortY = ((mapHeight * mapScaleFactor) - (gameViewHeight)) * -1;
                    viewPortTargetY = -1;
                } else {
                    if (viewPortY - 20 <= -viewPortTargetY) {
                        viewPortY = -viewPortTargetY;
                        viewPortTargetY = -1;
                    } else {
                        viewPortY -= 20;
                    }
                }
            } else {
                viewPortY = -viewPortTargetY;
                viewPortTargetY = -1;
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {

        // Set Viewport and map-position
        canvas.clipRect(dstViewport);
        canvas.translate(viewPortX, viewPortY);
        canvas.scale(mapScaleFactor, mapScaleFactor);
        // Start of by clearing the old picture with a new coat of white
        canvas.drawColor(Color.WHITE);


        // save color so we can restore it later (will/should be black)
        @ColorInt int prevColor = paint.getColor();
        // Draw streets
        for(int i = 0; i < streets.size(); i++) {
            City start = streets.get(i).src;
            City target = streets.get(i).target;

            float startX = start.getX() + start.getWidth() / 2f;
            float startY = start.getY() + start.getHeight() / 2f;

            float targetX = target.getX() + target.getWidth() / 2f;
            float targetY = target.getY() + target.getHeight() / 2f;

            switch (streets.get(i).ticket) {
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
        for (int i = 0; i < cities.size(); i++) {
            cities.get(i).draw(canvas, paint);
        }

        // Draw Figures
        for (int i = 0; i < figures.size(); i++) {
            Figure figure = figures.get(i);
            if(!(figure != null && figure.getPort() == 0 && !showMrX)) {
                figures.get(i).draw(canvas, paint);
            }
        }

    }

    public void notifyTicketSelectionChanged(final Vector<Ticket> selectedTickets) {
        stateModel.setSelectedTickets(selectedTickets);
    }

    // GETTERS/SETTERS

    public float[] getPositionByName(final String name) {
        float[] coords = new float[2];
        switch (name) {
            case "Kiel":
                coords[0] = 450f;
                coords[1] = 12f;
                break;
            case "Bremen":
                coords[0] = 100f;
                coords[1] = 200f;
                break;
            case "Hannover":
                coords[0] = 200f;
                coords[1] = 550f;
                break;
            case "Nonne Stadt":
                coords[0] = 400f;
                coords[1] = 400f;
                break;
            case "Murica":
                coords[0] = 500f;
                coords[1] = 600f;
                break;
            case "Hexter":
                coords[0] = 600f;
                coords[1] = 475f;
                break;
            case "Berlin":
                coords[0] = 550f;
                coords[1] = 300f;
                break;
            case "Nowheresville":
                coords[0] = 700f;
                coords[1] = 350f;
                break;
            case "Sylt":
                coords[0] = 1000f;
                coords[1] = 100f;
                break;
            case "New York":
                coords[0] = 1250f;
                coords[1] = 500f;
                break;
            case "Tricity":
                coords[0] = 900f;
                coords[1] = 320f;
                break;
            case "Washington":
                coords[0] = 1100f;
                coords[1] = 750f;
                break;
            case "Grossseistadt":
                coords[0] = 625f;
                coords[1] = 150f;
                break;
            default:
                coords[0] = 0f;
                coords[1] = 0f;
        }

        coords[0] *=2; // just wanna spread the map a little
        coords[1] *=2;
        return coords;
    }

    private Figure getByPort(final int port) {
        for(int i = 0; i < figures.size(); i++) {
            if(figures.get(i).getPort() == port) {
                return figures.get(i);
            }
        }

        return null;
    }


    // So I don't have to save Edges
    private static class Street {
        public City src;
        public City target;
        public Vehicle ticket;

        public Street(final City src, final City target, final Vehicle ticket) {
            this.src = src;
            this.target = target;
            this.ticket = ticket;
        }
    }

    // ---=== StateModelObserver-implementation ===---

    @Override
    public void onPlayerAdded(Player player) {
        Figure toAdd = new Figure(getApplication().getApplicationContext(), player.getPort());

        if(player.getPlace() != null) {
            City position = placeCityHashMap.get(player.getPlace());
            if(position != null) {
                float targetX = position.getX() + position.getWidth() / 2f - toAdd.getWidth() / 2f;
                float targetY = position.getY() + (position.getHeight() / 2f) - toAdd.getHeight();

                toAdd.setTargetPosition(targetX, targetY);
                toAdd.snapToCurrentTarget();
            }
        }
        figures.add(toAdd);
    }

    @Override
    public void onPlaceAdded(Place place) {
        float[] coordinates = getPositionByName(place.getName()); // TODO Think of something that makes sense =)
        City toAdd = new City(getApplication().getApplicationContext(), place.getName(), coordinates[0], coordinates[1]);
        cities.add(toAdd);

        placeCityHashMap.put(place, toAdd);
        cityPlaceHashMap.put(toAdd, place);
    }

    @Override
    public void onStreetAdded(Edge<Place, Vehicle> street) {
        City src = placeCityHashMap.get(street.getSource().getData());
        City target = placeCityHashMap.get(street.getTarget().getData());

        streets.add(new Street(src, target, street.getData()));
    }

    @Override
    public void onPlayerMoved(Player playerMoved, Place movedTo) {
        Figure toMove = getByPort(playerMoved.getPort());
        City target = placeCityHashMap.get(movedTo);

        if(toMove != null && target != null) {
            float targetX = target.getX() + target.getWidth() / 2f - toMove.getWidth() / 2f;
            float targetY = target.getY() + (target.getHeight() / 2f) - toMove.getHeight() ;

            toMove.setTargetPosition(targetX, targetY);
        }

        if (selectedCity != null) {
            selectedCity.deselect(); // else, detectives can see Mr. X's whereabouts after his turn
            selectedCity = null;
        }
    }

    @Override
    public void onPlayerActivePlayerChanged(Player newActivePlayer) {
        Figure figureBefore = null;
        if (selectedFigure != null) {
            figureBefore = selectedFigure;
            selectedFigure.deselect();
        }
        selectedFigure = getByPort(newActivePlayer.getPort());
        if (selectedFigure != null) {
            selectedFigure.select();
        }

        // start scrolling to the active player
        if(selectedFigure != null) {
            // if Mr. X played before just jump to next player (while screen is black), so camera movement does not give away position.
            if(figureBefore != null && figureBefore.getPort() == 0) {
                viewPortX = -(selectedFigure.getX() - gameViewWidth / 2f);
                viewPortY = -(selectedFigure.getY() - gameViewHeight / 2f);
            } else {
                // let camera scroll (done in onUpdate())
                viewPortTargetX = selectedFigure.getX() - gameViewWidth / 2f;
                viewPortTargetY = selectedFigure.getY() - gameViewHeight / 2f;
            }
        }
    }

    @Override
    public void onTimelineEntryAdded(Ticket ticketUsed, Place movedTo) {
        int nextIndex = simulatedTimeline.size();
        simulatedTimeline.addRound(ticketUsed, movedTo);
        for(int i = 0; i < observers.size(); i++) {
            // sent on to be added to the View
            observers.get(i).onTimelineTurnAdded(nextIndex);
        }
    }

    @Override
    public void onTimelineEntryMarked(int position) {
        simulatedTimeline.mark(position);
        markedCities.add(placeCityHashMap.get(simulatedTimeline.getPlaceForRound(position)));
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onTimelineTurnMarked(position);
        }
    }

    @Override
    public void onInventoryLoadFirst(Vector<Ticket> startInventory) {
        simulatedInventory.addAll(startInventory);

        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onInventoryAddAll(startInventory.size());
        }
    }

    @Override
    public void onInventoryChanged(Vector<Ticket> newInventory) {
        int size = simulatedInventory.size();

        simulatedInventory.clear();

        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onInventoryClear(size);
        }

        simulatedInventory.addAll(newInventory);

        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onInventoryAddAll(simulatedInventory.size());
        }
    }

    @Override
    public void onInventoryTicketAdded(Ticket ticketAdded) {
        simulatedInventory.add(ticketAdded);
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onInventoryAdd(simulatedInventory.indexOf(ticketAdded));
        }
    }

    @Override
    public void onInventoryTicketRemoved(int position) {
        simulatedInventory.remove(position);
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onInventoryRemove(position);
        }
    }

    @Override
    public void onPhaseChange(StateModel.GAME_PHASE phase) {
        phaseObserver = phase;

        switch (phaseObserver) {
            case INTERRUPTED:
                stateModel.setContinuable(false);
                break;
            case WAIT_FOR_CLICK:
                stateModel.setContinuable(true);
                break;
            case MRX_CHOOSE_TURN:
            case DET_WON:
                showMrX = true;
                break;
            case DET_CHOOSE_MOVE:
                if(stateModel.getCurrentRound() <= 24) {
                    showMrX = false;
                }
                break;
        }

        setUserInstruction(phase);

        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onPhaseChange(phase);
        }
    }

    @Override
    public void onUserMessageChange(String newMessage) {
        for(int i = 0; i < observers.size(); i++) {
            observers.get(i).onUserMessageChange(newMessage);
        }
    }

    // -------- PHASE-MANAGEMENT --------
    String lastHelpText = "";
    public void setUserInstruction(@NonNull final StateModel.GAME_PHASE phase) {
        String helpText = "";
        switch (phase) {
            case MRX_CHOOSE_TURN:
                helpText = "Click city to move to or the ticket with the ability to activate";
                break;
            case MRX_CHOOSE_TICKET:
                helpText = "Choose the ticket to use for travel";
                break;
            case MRX_CHOOSE_ABILITY_TICKETS:
                helpText = "Choose the (3) tickets to activate ability with";
                break;
            case MRX_ABILITY_CONFIRM:
            case DET_ABILITY_CONFIRM:
                helpText = "Confirm to use tickets and activate ability";
                break;
            case MRX_MOVE_CONFIRM:
            case MRX_SPECIAL_CONFIRM:
            case MRX_EXTRA_TURN_CONFIRM:
            case DET_MOVE_CONFIRM:
            case DET_EXTRA_TURN_CONFIRM:
                helpText = "Confirm selection to make your move";
                break;
            case MRX_MOVE:
            case MRX_SPECIAL_MOVE:
            case MRX_EXTRA_TURN_MOVE:
            case DET_MOVE:
            case DET_EXTRA_TURN_MOVE:
                helpText = "I'm walkin' hee'";
                break;
            case MRX_SPECIAL_CHOOSE_CITY:
                helpText = "Choose city to sneak to";
                break;
            case MRX_EXTRA_TURN_CHOOSE_TURN:
                helpText = "Choose your destination and ticket (twice)";
                break;
            case MRX_EXTRA_TURN_NOT_POSSIBLE:
                helpText = "Can't do that, not enough tickets to actually move twice";
                break;
            case MRX_EXTRA_TURN_ONE_NOT_POSSIBLE:
                helpText = "Can't go there, no tickets to move from there";
                break;
            case MRX_NO_VALID_MOVE_CHOOSE_TURN:
                helpText = "Click confirm to end turn or choose ability tickets";
                break;
            case MRX_NO_VALID_MOVE_CHOOSE_SPECIAL_TICKETS:
                helpText = "Select 3 of the ''shadow ticket''-ability tickets";
                break;
            case MRX_NO_VALID_MOVE_SPECIAL_ACTIVATION_CONFIRM:
                helpText = "Click confirm to activate shadow ticket";
                break;
            case MRX_THROW_TICKETS:
            case DET_THROW_TICKETS:
                helpText = "Choose tickets to throw away, if any";
                break;
            case MRX_NO_VALID_MOVE:
            case DET_NO_VALID_MOVE:
                helpText = "No ticket to make a move from here. Skipping turn";
                break;
            case DET_CHOOSE_MOVE:
                helpText = "Select city and ticket for move with current detective";
                break;
            case DET_SELECT_NEXT:
                helpText = "Click Confirm to end turn or select ability tickets";
                break;
            case DET_CHOOSE_ABILITY_TICKETS:
                helpText = "Choose between 3-5 tickets of the same ability";
                break;

            case DET_EXTRA_TURN:
                helpText = "Placeholder DET_EXTRA_TURN";
                break;
            case DET_EXTRA_TURN_CHOOSE_TURN:
                helpText = "Placeholder DET_EXTRA_TURN_CHOOSE_TURN";
                break;
            case DET_EXTRA_TURN_WIN_CHECK:
                helpText = "Placeholder DET_EXTRA_TURN_WIN_CHECK";
                break;
            case DET_EXTRA_TURN_NOT_POSSIBLE:
                helpText = "Placeholder DET_EXTRA_TURN_NOT_POSSIBLE";
                break;
            case DET_SPECIAL:
                helpText = "Placeholder DET_SPECIAL";
                break;
            case DET_SPECIAL_DO:
                helpText = "Placeholder DET_SPECIAL_DO";
                break;
            case DET_WIN_CHECK:
                helpText = "Placeholder DET_WIN_CHECK";
                break;
            case DET_WON:
                helpText = "Detectives won!";
                break;
            case MRX_WON:
                helpText = "Mr. X won!";
                break;
            case MRX_THROWING_SELECTED_TICKETS:
            case DET_THROWING_SELECTED_TICKETS:
                helpText = "Turn over. Wanna see your inventory?";
                break;
            case MRX_DET_TRANSITION:
            case DET_MRX_TRANSITION:
                helpText = "";
                break;
            case WAIT_FOR_CLICK:
            case MRX_SPECIAL: // Just throw the needed tickets, user won't see this. leave the previous message
            case MRX_EXTRA_TURN:
                helpText = lastHelpText;
                break;
            default:
        }
        userHelpText.postValue(helpText);
        lastHelpText = helpText;
    }

    // ---- GETTERS/SETTERS


    public Vector<Ticket> getSimulatedInventory() {
        return simulatedInventory;
    }

    public Timeline getSimulatedTimeline() {
        return simulatedTimeline;
    }

    public boolean registerObserver(StateViewModelObserver observer) {
        return observers.add(observer);
    }

    public MutableLiveData<String> getUserHelpText() {
        return userHelpText;
    }

    public void notifyContinuable(final boolean continuable) {
        stateModel.setContinuable(continuable);
    }
}
