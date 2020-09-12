package com.craws.mrx;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.craws.mrx.state.Ticket;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    // When an item/ticket gets clicked we wanna hear about it
    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    private OnItemClickListener itemClickListener;

    // Provide this method for parent-objects to propagate the click-event through to them.
    public void setOnItemClickListener(final OnItemClickListener listener) {
        itemClickListener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageVehicle;
        public ImageView imageAbility;

        public ViewHolder(final View itemView) {
            super(itemView);

            imageVehicle = itemView.findViewById(R.id.image_vehicle);
            imageAbility = itemView.findViewById(R.id.image_ability);

            // If an InventoryAdapter.OnItemClickListener is set and this item(/ViewHolder) gets clicked, forward the Click-handling to that listener
            itemView.setOnClickListener((view) -> {
                if (tracker != null) {
                    tracker.select(getItemId());
                }
            });
        }

        public InventoryItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new InventoryItemDetailsLookup.ItemDetails<Long>() {
                @Override
                public int getPosition() {
                    return getAdapterPosition();
                }

                @Override
                public Long getSelectionKey() {
                    return getItemId();
                }
            };
        }
    }

    private List<Ticket> inventory;
    private SelectionTracker<Long> tracker = null;

    public InventoryAdapter(List<Ticket> inventory) {
        this.inventory = inventory;
        setHasStableIds(true);
    }

    @Override
    public @NonNull InventoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        return new ViewHolder(inflater.inflate(R.layout.inventory_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryAdapter.ViewHolder holder, int position) {
        Ticket ticket = inventory.get(position);
        ImageView vehicle;
        ImageView ability;

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
            holder.itemView.setActivated(tracker.isSelected((long) position));
        }
    }
    @Override
    public int getItemCount() {
        return inventory.size();
    }

    @Override
    public long getItemId(final int position) {
        return (long)position;
    }

    public Ticket getTicketById(final long id) {
        return inventory.get((int)id);
    }

    public void setTracker(final SelectionTracker<Long> tracker) {
        this.tracker = tracker;
    }
}
