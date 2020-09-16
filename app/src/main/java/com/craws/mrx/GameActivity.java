package com.craws.mrx;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.craws.mrx.engine.GameView;
import com.craws.mrx.engine.InventoryChangeListener;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Timeline;

import java.util.Vector;

public class GameActivity extends AppCompatActivity {
    private Thread gameLoopThread;

    private GameView gameView;
    private RecyclerView recInventory;
    private InventoryAdapter adapterInv;
    private RecyclerView recTimeline;
    private TimelineAdapter adapterTL;
    private RelativeLayout relativeLayoutInventory;

    private TextView txtInstructions;

    // The displayed inventory in the RecyclerView. Will have to be built every time player's change.
    private Vector<Ticket> activeInventory;
    private Timeline timeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameView = findViewById(R.id.gameView);

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
                System.out.println(selectedTickets);
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

        recTimeline.setAdapter(adapterTL);
        recTimeline.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));


        // To manage the game, we listen for phase changes
        gameView.setOnPhaseChangeListener((phase) -> {
            if (phase == GameView.GAME_PHASE.INTERRUPTED) {
                hideMenus();
            } else {
                showMenus();
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

        gameView.setTimelineChangeListener(this::timelineAdd);


        // Create new Thread so we can leave onCreate (and access the Views) while starting the game.
        gameLoopThread = new Thread(() -> gameView.startGame());

       gameLoopThread.start();
    }

    public void hideMenus() {
        runOnUiThread(() -> {
            relativeLayoutInventory = findViewById(R.id.relLayout_inventory);
            recTimeline = findViewById(R.id.recycle_timeline);

            if(relativeLayoutInventory != null && recTimeline != null) {
                relativeLayoutInventory.setVisibility(View.GONE);
                recTimeline.setVisibility(View.GONE);
            }
        });
    }

    public void showMenus() {
        runOnUiThread(() -> {
            relativeLayoutInventory = findViewById(R.id.relLayout_inventory);
            recTimeline = findViewById(R.id.recycle_timeline);

            if(relativeLayoutInventory != null && recTimeline != null) {
                relativeLayoutInventory.setVisibility(View.VISIBLE);
                recTimeline.setVisibility(View.VISIBLE);
            }
        });
    }

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

    private void timelineAdd(final Ticket ticket, final Place place) {
        runOnUiThread(() -> {
            int nextIndex = timeline.size();
            timeline.addRound(ticket, place);
            adapterTL.notifyItemInserted(nextIndex);
        });
    }

    public void setUserInstruction(final GameView.GAME_PHASE phase) {
        switch (phase) {
            case MRX_CHOOSE_TURN:
                txtInstructions.setText(R.string.user_instructions_mrx_choose_turn);
                break;
            case MRX_CHOOSE_TICKET:
                txtInstructions.setText(R.string.user_instructions_mrx_choose_ticket);
                break;
            case MRX_CHOOSE_ABILITY_TICKETS:
                txtInstructions.setText(R.string.user_instructions_mrx_choose_ability_tickets);
                break;
            case MRX_ABILITY_CONFIRM:
                txtInstructions.setText(R.string.user_instructions_mrx_confirm_ability);
                break;
            case MRX_MOVE_CONFIRM:
            case MRX_SPECIAL_CONFIRM:
            case MRX_EXTRA_TURN_CONFIRM:
                txtInstructions.setText(R.string.user_instructions_mrx_confirm_move);
                break;
            case MRX_MOVE:
            case MRX_SPECIAL_MOVE:
            case MRX_EXTRA_TURN_MOVE:
                txtInstructions.setText(R.string.user_instructions_mrx_move);
                break;
            case MRX_SPECIAL_CHOOSE_CITY:
                txtInstructions.setText(R.string.user_instructions_mrx_special_choose_city);
                break;
            case MRX_EXTRA_TURN_CHOOSE_TURN:
                txtInstructions.setText(R.string.user_instructions_mrx_extra_turn_choose_turn);
                break;
            case MRX_EXTRA_TURN_NOT_POSSIBLE:
                txtInstructions.setText(R.string.user_instructions_mrx_extra_turn_not_possible);
                break;
            case MRX_EXTRA_TURN_ONE_NOT_POSSIBLE:
                txtInstructions.setText(R.string.user_instructions_mrx_extra_turn_one_not_possible);
                break;
            case MRX_THROW_TICKETS:
                txtInstructions.setText(R.string.user_instructions_mrx_throw_tickets);
                break;
            case MRX_SPECIAL: // Just throw the needed tickets, user won't see this
            case MRX_EXTRA_TURN:
            case MRX_THROWING_SELECTED_TICKETS:
            default:
                //txtInstructions.setText("");
        }
    }

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
