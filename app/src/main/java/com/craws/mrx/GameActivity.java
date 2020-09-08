package com.craws.mrx;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;

import com.craws.mrx.engine.GameView;
import com.craws.mrx.state.Ability;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Timeline;
import com.craws.mrx.state.Vehicle;

import java.util.Vector;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private RecyclerView recInventory;
    private RecyclerView recTimeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);

        gameView = findViewById(R.id.gameView);
        gameView.setParent(this);

        // Graphical part of the inventory
        recInventory = findViewById(R.id.recycViewInventory);
        final Vector<Ticket> inventory = new Vector<>();
        inventory.add(new Ticket(Vehicle.FAST, Ability.EXTRA_TURN));
        inventory.add(new Ticket(Vehicle.MEDIUM, Ability.SPECIAL));
        inventory.add(new Ticket(Vehicle.SLOW, Ability.SPECIAL));
        inventory.add(new Ticket(Vehicle.MEDIUM, Ability.EXTRA_TURN));
        inventory.add(new Ticket(Vehicle.FAST, Ability.SPECIAL));
        inventory.add(new Ticket(Vehicle.SLOW, Ability.EXTRA_TURN));
        inventory.add(new Ticket(Vehicle.FAST, Ability.EXTRA_TURN));
        inventory.add(new Ticket(Vehicle.MEDIUM, Ability.SPECIAL));
        inventory.add(new Ticket(Vehicle.SLOW, Ability.SPECIAL));
        inventory.add(new Ticket(Vehicle.MEDIUM, Ability.EXTRA_TURN));
        inventory.add(new Ticket(Vehicle.FAST, Ability.SPECIAL));
        inventory.add(new Ticket(Vehicle.SLOW, Ability.EXTRA_TURN));

        final InventoryAdapter adapter = new InventoryAdapter(inventory);
        // Handle Ticket getting clicked
        adapter.setOnItemClickListener(new InventoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                System.out.println("V: " + inventory.get(position).getVehicle() + "; A: " + inventory.get(position).getAbility());
                Ticket toRemove = inventory.get(position);
                inventory.remove(toRemove);
                adapter.notifyItemRemoved(position);
                gameView.setLastUsed(toRemove);
            }
        });

        recInventory.setAdapter(adapter);
        recInventory.setLayoutManager(new GridLayoutManager(this, 2));

        // The "end turn" button
        Button theButton = findViewById(R.id.btn_end_turn);
        theButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.moveBitch();
                if( recInventory.getVisibility() == VISIBLE) {
                    recInventory.setVisibility(GONE);
                } else {
                    recInventory.setVisibility(VISIBLE);
                }
            }
        });

        // The graphical representation of the timeline
        recTimeline = findViewById(R.id.recycle_timeline);

        Place p1 = new Place(this, "Place1");
        Place p2 = new Place(this, "Place2");
        Timeline tl = new Timeline();
        tl.addRound(inventory.get((int)(Math.random() * (inventory.size() - 1))), p1);
        tl.addRound(inventory.get((int)(Math.random() * (inventory.size() - 1))), p2);
        tl.addRound(inventory.get((int)(Math.random() * (inventory.size() - 1))), p1);
        tl.addRound(inventory.get((int)(Math.random() * (inventory.size() - 1))), p2);

        final TimelineAdapter adapterTL = new TimelineAdapter(tl);

        recTimeline.setAdapter(adapterTL);
        recTimeline.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
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
