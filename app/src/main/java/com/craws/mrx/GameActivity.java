package com.craws.mrx;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.craws.mrx.engine.GameView;
import com.craws.mrx.engine.InventoryChangeListener;
import com.craws.mrx.engine.TimelineChangeListener;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Timeline;

import java.util.Vector;

public class GameActivity extends AppCompatActivity {
    private Thread gameLoopThread;

    private GameView gameView;

    private RelativeLayout relativeLayoutInventory;
    private RecyclerView recInventory;
    private InventoryAdapter adapterInv;

    private RecyclerView recTimeline;
    private TimelineAdapter adapterTL;
    private int currColorIndex;

    private TextView txtInstructions;
    FragmentInterrupted fragmentInterrupted;

    // The displayed inventory in the RecyclerView. Will have to be built every time player's change.
    private Vector<Ticket> activeInventory;
    private Timeline timeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameView = findViewById(R.id.gameView);

        relativeLayoutInventory = findViewById(R.id.relLayout_inventory);

        txtInstructions = findViewById(R.id.textView_instruction);

        // Graphical part of the inventory
        recInventory = findViewById(R.id.recycViewInventory);
        // Starting with Mr. X's Inventory
        activeInventory = new Vector<>();
        adapterInv = new InventoryAdapter(activeInventory);

        recInventory.setAdapter(adapterInv);

        SelectionTracker<Long> tracker =
                new SelectionTracker.Builder<>(
                        "inventorySelection",
                        recInventory,
                        new InventoryItemKeyProvider<Long>(recInventory),
                        new InventoryItemDetailsLookup(recInventory),
                        StorageStrategy.createLongStorage())
                        .withSelectionPredicate(SelectionPredicates.createSelectAnything()).build();

        tracker.addObserver(new SelectionTracker.SelectionObserver<Long>() {
            @Override
            public void onSelectionChanged() {
                super.onSelectionChanged();
                Vector<Ticket> selectedTickets = new Vector<>();
                for(long currSelection : tracker.getSelection()) {
                    selectedTickets.add(adapterInv.getTicketById(currSelection));
                }
                Log.d("SelectionChanged", selectedTickets.toString());
                gameView.setSelectedTickets(selectedTickets);
            }
        });

        adapterInv.setTracker(tracker);

        recInventory.setLayoutManager(new GridLayoutManager(this, 2));
        recInventory.setHasFixedSize(true);

        // The "end turn" button
        Button theButton = findViewById(R.id.btn_confirm);
        theButton.setOnClickListener((view) -> {
            // When confirm is pressed, the gameLoop comes to a hold, so not to mess with the state (especially the selected tickets and place).
            gameView.setPlaying(false);
            try {
                gameLoopThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            gameView.confirmSelection();

            gameView.setPlaying(true);
            gameLoopThread = new Thread(gameView::gameLoop);
            gameLoopThread.start();
        });

        // The graphical representation of the timeline
        recTimeline = findViewById(R.id.recycle_timeline);

        timeline = new Timeline();

        adapterTL = new TimelineAdapter(timeline);

        currColorIndex = 0;

        recTimeline.setAdapter(adapterTL);
        recTimeline.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        // To manage the game, we listen for phase changes
        fragmentInterrupted = new FragmentInterrupted(); // "Cutscenes". Used to block view from Detective-user when they pass the device to the Chad Mr. X
        gameView.setOnPhaseChangeListener((phase) -> {

            if (phase == GameView.GAME_PHASE.INTERRUPTED) {
                startInterrupted();
            } else {
                stopInterrupted();
            }

            setUserInstruction(phase);
        });

        // To update the Inventory-RecyclerView
        gameView.setInventoryChangeListener(new InventoryChangeListener() {
            @Override
            public void onAdd(final Ticket added) {
                activeInventoryAdd(added);
            }

            @Override
            public void onRemove(int position) {
                activeInventoryRemove(position);
            }

            @Override
            public void onNewInventory(Vector<Ticket> newInventory) {
                activeInventoryNewInventory(newInventory);
            }
        });

        gameView.setTimelineChangeListener(new TimelineChangeListener() {
            @Override
            public void onTurnAdded(@Nullable Ticket ticket, Place destination) {
                timelineAdd(ticket, destination);
            }

            @Override
            public void onTurnMarked(int round) {
                markTurn(round);
            }
        });


        // Create new Thread so we can leave onCreate (and access the Views) while starting the game.
        gameLoopThread = new Thread(() -> gameView.startGame());

       gameLoopThread.start();
    }

    // --------======== LISTENER HELPER ========--------
                // -------- INVENTORY --------
    private void activeInventoryAdd(final Ticket toAdd) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(() -> {
            activeInventory.add(toAdd);
            adapterInv.notifyItemInserted(activeInventory.indexOf(toAdd));
        });
    }

    private void activeInventoryRemove(final int position) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(() -> {
            if (adapterInv.getTracker() != null) {
                adapterInv.getTracker().clearSelection();
            }

            activeInventory.remove(position);
            if(position == 0) {
                adapterInv.notifyDataSetChanged();
            } else {
                adapterInv.notifyItemRemoved(position);
            }
        });
    }

    private void activeInventoryNewInventory(final Vector<Ticket> newInventory) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(() -> {
            if (adapterInv.getTracker() != null) {
                adapterInv.getTracker().clearSelection();
            }
            int size = activeInventory.size();
            activeInventory.clear();
            adapterInv.notifyItemRangeRemoved(0, size);

            activeInventory.addAll(newInventory);
            adapterInv.notifyItemRangeInserted(0, activeInventory.size());
        });
    }

                // -------- TIMELINE --------
    private void timelineAdd(final Ticket ticket, final Place place) {
        runOnUiThread(() -> {
            int nextIndex = timeline.size();
            timeline.addRound(ticket, place);
            adapterTL.notifyItemInserted(nextIndex);
        });
    }

    private void markTurn(final int round) {
        runOnUiThread(() -> {
            RecyclerView.LayoutManager layoutManager = recTimeline.getLayoutManager();

            if(layoutManager != null) {
                View view = recTimeline.getLayoutManager().findViewByPosition(round);
                if (view != null) {
                    view.setBackgroundColor(GameView.markColorCoding[currColorIndex++]);
                } else {
                    Log.d("DetSpecialActivity", "It happened... ViewByPosition not found. :really_sad_face:");
                }
            } else {
                Log.d("DetSpecialActivity", "It happened... LayoutManager not found. Da actual fuck.");
            }

            adapterTL.markItem(round, GameView.markColorCoding[currColorIndex++]);
        });
    }

                // -------- PHASE-MANAGEMENT --------
    String lastHelpText = "";
    public void setUserInstruction(final GameView.GAME_PHASE phase) {
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
                helpText = "Placeholder DET_WON";
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
        lastHelpText = helpText;
        final String toSet = helpText;
        runOnUiThread(() -> txtInstructions.setText(toSet));
    }

    public void stopInterrupted() {

        runOnUiThread(() -> {
            if(relativeLayoutInventory != null) {
                if (relativeLayoutInventory.getVisibility() == View.GONE) {
                    relativeLayoutInventory.setVisibility(View.VISIBLE);
                }
            }

            if(fragmentInterrupted.isVisible()) {
                final FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.remove(fragmentInterrupted);
                transaction.commit();
            }
        });
    }

    public void startInterrupted() {

        runOnUiThread(() -> {
            if(relativeLayoutInventory != null) {
                if (relativeLayoutInventory.getVisibility() == View.VISIBLE) {
                    relativeLayoutInventory.setVisibility(View.GONE);
                }
            }

            if(!fragmentInterrupted.isVisible()) {
                final FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.activity_frame_interrupted, fragmentInterrupted);
                transaction.commitNow();
                fragmentInterrupted.setText(gameView.getUserMessage());
            }
            gameView.setContinuable(true);
        });
    }


    //--------======== INHERITED METHODS ========--------

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
    }
}
