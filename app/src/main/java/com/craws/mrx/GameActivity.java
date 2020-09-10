package com.craws.mrx;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.craws.mrx.engine.GameView;
import com.craws.mrx.engine.InventoryChangeListener;
import com.craws.mrx.engine.OnPhaseChangeListener;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Timeline;

import java.util.Vector;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private RecyclerView recInventory;
    private InventoryAdapter adapterInv;
    private RecyclerView recTimeline;

    // The displayed inventory in the RecyclerView. Will have to be built everytime player's change.
    private Vector<Ticket> activeInventory;
    private Timeline timeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameView = findViewById(R.id.gameView);

        // Graphical part of the inventory
        recInventory = findViewById(R.id.recycViewInventory);
        // Starting with Mr. X's Inventory
        activeInventory = new Vector<>();
        adapterInv = new InventoryAdapter(activeInventory);
        // DO THIS INSTEAD https://developer.android.com/guide/topics/ui/layout/recyclerview
        /*adapterInv.setOnItemClickListener(new InventoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                System.out.println("V: " + activeInventory.get(position).getVehicle() + "; A: " + activeInventory.get(position).getAbility());
                Ticket toRemove = activeInventory.get(position);
                activeInventory.remove(toRemove);
                adapterInv.notifyItemRemoved(position);
            }
        });

         */

        recInventory.setAdapter(adapterInv);
        recInventory.setLayoutManager(new GridLayoutManager(this, 2));

        // The "end turn" button
        Button theButton = findViewById(R.id.btn_end_turn);
        theButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        // The graphical representation of the timeline
        recTimeline = findViewById(R.id.recycle_timeline);

        timeline = new Timeline();

        final TimelineAdapter adapterTL = new TimelineAdapter(timeline);

        recTimeline.setAdapter(adapterTL);
        recTimeline.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));


        // To manage the game, we listen for phase changes
        gameView.setOnPhaseChangeListener(new OnPhaseChangeListener() {
            @Override
            public void onPhaseChange(final GameView.GAME_PHASE phase) {
                if (phase == GameView.GAME_PHASE.INTERRUPTED) {
                    hideMenus();
                } else {
                    showMenus();
                }
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
        Thread gameLogic = new Thread(new Runnable() {
            @Override
            public void run() {
                gameView.startGame();
            }
        });

        gameLogic.start();
    }

    public void hideMenus() {
        if(findViewById(R.id.relLayout_inventory) != null && recTimeline != null) {
            findViewById(R.id.relLayout_inventory).setVisibility(View.GONE);
            recTimeline.setVisibility(View.GONE);
        }
    }

    public void showMenus() {
        if(findViewById(R.id.relLayout_inventory) != null && recTimeline != null) {
            findViewById(R.id.relLayout_inventory).setVisibility(View.VISIBLE);
            recTimeline.setVisibility(View.VISIBLE);
        }
    }

    private void activeInventoryAdd(final Ticket toAdd) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activeInventory.add(toAdd);
                adapterInv.notifyItemInserted(activeInventory.indexOf(toAdd));
            }
        });
    }

    private void activeInventoryRemove(final int position) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activeInventory.remove(position);
                adapterInv.notifyItemRemoved(position);
            }
        });
    }

    private void activeInventoryNewInventory(final Vector<Ticket> newInventory) {
        // RecyclerView may only be changed by the Thread that created it
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int size = activeInventory.size();
                activeInventory = new Vector<>();
                adapterInv.notifyItemRangeRemoved(0, size);

                for(int i = 0; i < newInventory.size(); i++) {
                    activeInventory.add(newInventory.get(i));
                    adapterInv.notifyItemInserted(i);
                }
            }
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
