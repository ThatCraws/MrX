package com.craws.mrx.MVVM;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.craws.mrx.FragmentInterrupted;
import com.craws.mrx.InventoryAdapter;
import com.craws.mrx.InventoryItemDetailsLookup;
import com.craws.mrx.InventoryItemKeyProvider;
import com.craws.mrx.R;
import com.craws.mrx.TimelineAdapter;
import com.craws.mrx.engine.GameView;
import com.craws.mrx.engine.InventoryChangeListener;
import com.craws.mrx.engine.TimelineChangeListener;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Ticket;

import java.util.List;
import java.util.Vector;

public class GameActivity extends AppCompatActivity {
    // --- ViewModel ---
    private StateViewModel viewModel;

    // --- GAME-VIEW ---
    private Thread gameLoopThread;

    private GameView gameView;

    // --- INVENTORY ---

    private RelativeLayout relativeLayoutInventory;
    private RecyclerView recInventory;
    private InventoryAdapter adapterInv;

    // --- TIMELINE ---
    private RecyclerView recTimeline;
    private TimelineAdapter adapterTL;
    private int currColorIndex;

    // --- PHASES ---
    private TextView txtInstructions;
    FragmentInterrupted fragmentInterrupted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        viewModel = new ViewModelProvider(this).get(StateViewModel.class);

        viewModel.getUserHelpText().observe(this, newHelpText -> txtInstructions.setText(newHelpText));

        // --------======== GAME-VIEW ========--------

        gameView = findViewById(R.id.gameView);
        gameView.setTouchListener(viewModel);

        // --------======== INVENTORY ========--------
        // --- FINDING VIEWS FROM LAYOUT ---
        relativeLayoutInventory = findViewById(R.id.relLayout_inventory);
        // Graphical part of the inventory
        recInventory = findViewById(R.id.recycViewInventory);

        // --- INITIALIZING INVENTORY/ADAPTER ---
        adapterInv = new InventoryAdapter(viewModel.getSimulatedInventory());

        recInventory.setAdapter(adapterInv);

        // --- SELECTION TRACKING ---
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
                viewModel.notifyTicketSelectionChanged(selectedTickets);
            }
        });

        adapterInv.setTracker(tracker);

        // --- LAYOUT ---
        recInventory.setLayoutManager(new GridLayoutManager(this, 2));
        recInventory.setHasFixedSize(true);

        // --- LISTENER ---
        // To update the Inventory-RecyclerView
        viewModel.setInventoryChangeListener(new InventoryChangeListener() {
            @Override
            public void onAdd(final int position) {
                activeInventoryAdd(position);
            }

            @Override
            public void onAddAll(final int newSize) {
                activeInventoryAddAll(newSize);
            }

            @Override
            public void onRemove(int position) {
                activeInventoryRemove(position);
            }

            @Override
            public void onClear(int oldSize) {
                activeInventoryClear(oldSize);
            }
        });


        // --------======== PHASE MANAGEMENT ========--------

        // --- LISTENER ---
        // To manage the game, we listen for phase changes
        fragmentInterrupted = new FragmentInterrupted(); // "Cutscenes". Used to block view from Detective-user when they pass the device to the Chad Mr. X
        viewModel.getModelUserMessage().observe(this, newUserMessage -> fragmentInterrupted.setText(newUserMessage));

        viewModel.setPhaseChangeListener((phase) -> {

            if (phase == StateModel.GAME_PHASE.INTERRUPTED) {
                startInterrupted();
            } else {
                stopInterrupted();
            }
        });


        // --- FIND VIEWS ---
        txtInstructions = findViewById(R.id.textView_instruction);
        // - confirm button -
        Button theButton = findViewById(R.id.btn_confirm);

        theButton.setOnClickListener((view) -> {
            // When confirm is pressed, the gameLoop comes to a hold, so not to mess with the state (especially the selected tickets and place).
            viewModel.notifyBtnConfirmClicked();

        });


        // --------======== TIMELINE ========--------

        // The graphical representation of the timeline
        currColorIndex = 0; // for marking on the timeline
        // --- INITIALIZE VIEWS/ADAPTER ---
        recTimeline = findViewById(R.id.recycle_timeline);

        adapterTL = new TimelineAdapter(viewModel.getSimulatedTimeline());

        recTimeline.setAdapter(adapterTL);
        recTimeline.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));


        // --- LISTENER ---
        viewModel.setTimelineChangeListener(new TimelineChangeListener() {
            @Override
            public void onTurnAdded(@Nullable Ticket ticket, Place destination) {
                timelineAdd(ticket, destination);
            }

            @Override
            public void onTurnMarked(int round) {
                markTurn(round);
            }
        });

        viewModel.setupGame();
        viewModel.startGame();
    }

    // --------======== LISTENER HELPER ========--------
                // -------- INVENTORY --------
    private void activeInventoryAdd(final int position) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(() -> adapterInv.notifyItemInserted(position));
    }

    private void activeInventoryRemove(final int position) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(() -> {
            if (adapterInv.getTracker() != null) {
                adapterInv.getTracker().clearSelection();
            }

            if(position == 0) {
                adapterInv.notifyDataSetChanged();
            } else {
                adapterInv.notifyItemRemoved(position);
            }
        });
    }

    private void activeInventoryClear(final int oldSize) {
        runOnUiThread(() -> adapterInv.notifyItemRangeRemoved(0, oldSize));
    }

    private void activeInventoryAddAll(final int newSize) {
        runOnUiThread(() -> adapterInv.notifyItemRangeInserted(0, newSize));
    }

    // -------- TIMELINE --------
    private void timelineAdd(final Ticket ticket, final Place place) {
        runOnUiThread(() -> {
            int nextIndex = viewModel.getSimulatedTimeline().size() - 1;
            adapterTL.notifyItemInserted(nextIndex);
        });
    }

    private void markTurn(final int round) {
        runOnUiThread(() -> {
            RecyclerView.LayoutManager layoutManager = recTimeline.getLayoutManager();

            /*if(layoutManager != null) {
                View view = layoutManager.findViewByPosition(round);
                if (view != null) {
                    view.setBackgroundColor(StateViewModel.markColorCoding[currColorIndex++ % StateViewModel.markColorCoding.length]);
                } else {
                    Log.d("DetSpecialActivity", "It happened... ViewByPosition not found. :really_sad_face:");
                }
            } else {
                Log.d("DetSpecialActivity", "It happened... LayoutManager not found. Da actual fuck.");
            }

             */

            adapterTL.markItem(round, StateViewModel.markColorCoding[currColorIndex++ % StateViewModel.markColorCoding.length]);
        });
    }


    public void startInterrupted() {

        runOnUiThread(() -> {
            if(!fragmentInterrupted.isVisible()) {
                final FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.activity_frame_interrupted, fragmentInterrupted);
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                transaction.commitNow();
            }

            if(relativeLayoutInventory != null) {
                if (relativeLayoutInventory.getVisibility() == View.VISIBLE) {
                    relativeLayoutInventory.setVisibility(View.GONE);
                }
            }

            viewModel.setModelContinuable(true);
        });
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
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                transaction.commit();
            }
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
