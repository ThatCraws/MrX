package com.craws.mrx.MVVM;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import com.craws.mrx.state.Ticket;

import java.util.Vector;

public class GameActivity extends AppCompatActivity implements StateViewModelObserver {
    // --- ViewModel ---
    private StateViewModel viewModel;

    // --- GAME-VIEW ---

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
    private FragmentInterrupted fragmentInterrupted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        viewModel = new ViewModelProvider(this).get(StateViewModel.class);

        viewModel.getUserHelpText().observe(this, newHelpText -> txtInstructions.setText(newHelpText));
        viewModel.registerObserver(this);

        // --------======== GAME-VIEW ========--------

        gameView = findViewById(R.id.gameView);
        gameView.setGameViewListener(viewModel);

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


        // --------======== PHASE MANAGEMENT ========--------

        fragmentInterrupted = new FragmentInterrupted(); // "Cutscenes". Used to block view from Detective-user when they pass the device to the Chad Mr. X


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


        viewModel.notifyReady();
    }

    // --------======== STATE-VIEWMODEL OBSERVER IMPLEMENTATION ========--------
    // -------- INVENTORY --------


    @Override
    public void onInventoryAdd(int position) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(() -> adapterInv.notifyItemInserted(position));
    }

    @Override
    public void onInventoryAddAll(int newSize) {
        runOnUiThread(() -> {
            adapterInv.notifyDataSetChanged();
            //adapterInv.notifyItemRangeInserted(0, newSize);
        });
    }

    @Override
    public void onInventoryRemove(int position) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(() -> {
            if (adapterInv.getTracker() != null) {
                adapterInv.getTracker().clearSelection();
            }

            if(recInventory.findViewHolderForAdapterPosition(position) != null) {
                adapterInv.notifyItemRemoved(position);
            }
        });
    }

    @Override
    public void onInventoryClear(int oldSize) {
        runOnUiThread(() -> adapterInv.notifyItemRangeRemoved(0, oldSize));
    }


    // -------- TIMELINE --------
    @Override
    public void onTimelineTurnAdded(final int position) {
        runOnUiThread(() -> adapterTL.notifyItemInserted(position));
    }

    @Override
    public void onTimelineTurnMarked(int round) {
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
                Log.d("DetSpecialActivity", "It happened... LayoutManager not found.");
            }

             */

            adapterTL.markItem(round, StateViewModel.markColorCoding[currColorIndex++ % StateViewModel.markColorCoding.length]);
        });
    }

    // -------- PHASE-MANAGEMENT --------
    @Override
    public void onPhaseChange(StateModel.GAME_PHASE phase) {
        if(phase == StateModel.GAME_PHASE.GAME_OVER) {
            startInterrupted();
        } else if (phase == StateModel.GAME_PHASE.INTERRUPTED) {
            startInterrupted();
        } else {
            stopInterrupted();
        }
    }

    @Override
    public void onUserMessageChange(String newMessage) {
        fragmentInterrupted.setText(newMessage);
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

            viewModel.notifyContinuable(true);
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