package com.craws.mrx;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.craws.mrx.state.Ticket;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageVehicle;
        public ImageView imageAbility;

        public ViewHolder(final View itemView) {
            super(itemView);

            imageVehicle = itemView.findViewById(R.id.image_vehicle);
            imageAbility = itemView.findViewById(R.id.image_ability);

            itemView.setOnClickListener((view) -> {
                if (tracker != null) {
                    tracker.select(getItemId());
                }
            });
        }

        public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new InventoryItemDetailsLookup.ItemDetails<Long>() {
                @Override
                public int getPosition() {
                    return getAdapterPosition();
                }

                @Override
                public Long getSelectionKey() {
                    return (long)inventory.get(getAdapterPosition()).hashCode();
                }
            };
        }
    }

    private List<Ticket> inventory;
    private SelectionTracker<Long> tracker = null;

    public InventoryAdapter(List<Ticket> inventory) {
        setHasStableIds(true);
        this.inventory = inventory;
    }

    @Override
    public @NonNull InventoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        return new ViewHolder(inflater.inflate(R.layout.inventory_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryAdapter.ViewHolder holder, final int position) {
        Ticket ticket = inventory.get(position);

        switch(ticket.getVehicle()) {
            case SLOW:
                holder.imageVehicle.setImageResource(R.drawable.vehicle_slow);
                break;
            case MEDIUM:
                holder.imageVehicle.setImageResource(R.drawable.vehicle_medium);
                break;
            case FAST:
                holder.imageVehicle.setImageResource(R.drawable.vehicle_fast);
                break;
            default:
                holder.imageVehicle.setImageResource(R.drawable.error);
        }

        switch (ticket.getAbility()) {
            case EXTRA_TURN:
                holder.imageAbility.setImageResource(R.drawable.ability_extra_turn);
                break;
            case SPECIAL:
                holder.imageAbility.setImageResource(R.drawable.ability_special);
                break;
            default:
                holder.imageAbility.setImageResource(R.drawable.error);
        }

        if(tracker != null) {
            holder.itemView.setActivated(tracker.isSelected(getItemId(position)));
        }
    }

    @Override
    public int getItemCount() {
        return inventory.size();
    }

    @Override
    public long getItemId(final int position) {

        return inventory.get(position).hashCode();
    }

    public Ticket getTicketById(final long id) {

        for(Ticket currTicket: inventory) {
            if((long)currTicket.hashCode() == id) {
                return currTicket;
            }
        }
        return null;
    }

    public void setTracker(final SelectionTracker<Long> tracker) {
        this.tracker = tracker;
    }

    public SelectionTracker<Long> getTracker() {
        return tracker;
    }
}
