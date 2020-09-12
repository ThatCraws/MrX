package com.craws.mrx;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StableIdKeyProvider;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.craws.mrx.engine.GameView;
import com.craws.mrx.engine.InventoryChangeListener;
import com.craws.mrx.engine.OnPhaseChangeListener;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Timeline;

import java.util.Vector;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private RecyclerView recInventory;
    private InventoryAdapter adapterInv;
    private RecyclerView recTimeline;
    private RelativeLayout relativeLayoutInventory;

    // The displayed inventory in the RecyclerView. Will have to be built everytime player's change.
    private Vector<Ticket> activeInventory;
    private Timeline timeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        RelativeLayout relativeLayoutInventory = findViewById(R.id.relLayout_inventory);

        gameView = findViewById(R.id.gameView);

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
                        new StableIdKeyProvider(recInventory),
                        new InventoryItemDetailsLookup(recInventory),
                        StorageStrategy.createLongStorage())
                        .withSelectionPredicate(SelectionPredicates.createSelectAnything()).build();

        tracker.addObserver(new SelectionTracker.SelectionObserver<Long>() {
            @Override
            public void onSelectionChanged() {
                super.onSelectionChanged();
                Vector<Ticket> selectedTickets = new Vector<>();
                for(Long currSelection : tracker.getSelection()) {
                    selectedTickets.add(adapterInv.getTicketById(currSelection));
                }

                gameView.setSelectedTickets(selectedTickets);
            }
        });

        adapterInv.setTracker(tracker);

        recInventory.setLayoutManager(new GridLayoutManager(this, 2));

        // The "end turn" button
        Button theButton = findViewById(R.id.btn_confirm);
        theButton.setOnClickListener((view) -> gameView.tryToConfirm());

        // The graphical representation of the timeline
        recTimeline = findViewById(R.id.recycle_timeline);

        timeline = new Timeline();

        final TimelineAdapter adapterTL = new TimelineAdapter(timeline);

        recTimeline.setAdapter(adapterTL);
        recTimeline.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));


        // To manage the game, we listen for phase changes
        gameView.setOnPhaseChangeListener((phase) -> {
            if (phase == GameView.GAME_PHASE.INTERRUPTED) {
                hideMenus();
            } else {
                showMenus();
            }
        });

        // To update the Inventory-RecyclerView
        gameView.setInventoryChangeListener(new InventoryChangeListener() {
            @Override
            public void onAdd(Ticket added) {
                activeInventoryAdd(new Ticket(added.getVehicle(), added.getAbility()));
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


        // Create new Thread so we can leave onCreate (and access the Views) while starting the game.
        Thread gameLogic = new Thread(() -> gameView.startGame());

       gameLogic.start();
    }

    public void hideMenus() {
        runOnUiThread(() -> {
            relativeLayoutInventory = findViewById(R.id.relLayout_inventory);

            if(relativeLayoutInventory != null && recTimeline != null) {
                relativeLayoutInventory.setVisibility(View.GONE);
                recTimeline.setVisibility(View.GONE);
            }
        });
    }

    public void showMenus() {
        runOnUiThread(() -> {
            relativeLayoutInventory = findViewById(R.id.relLayout_inventory);

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
            activeInventory.remove(position);
            adapterInv.notifyItemRemoved(position);
        });
    }

    private void activeInventoryNewInventory(final Vector<Ticket> newInventory) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(() -> {
                int size = activeInventory.size();
                activeInventory.clear();
                adapterInv.notifyItemRangeRemoved(0, size);

                activeInventory.addAll(newInventory);
                adapterInv.notifyItemRangeInserted(0, activeInventory.size() - 1);
        });
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
