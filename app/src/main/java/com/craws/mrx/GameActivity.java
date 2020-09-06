package com.craws.mrx;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.craws.mrx.engine.GameView;
import com.craws.mrx.state.Ability;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Vehicle;

import java.util.Vector;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private RecyclerView recInventory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);

        gameView = findViewById(R.id.gameView);

        recInventory = findViewById(R.id.recycly);
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
