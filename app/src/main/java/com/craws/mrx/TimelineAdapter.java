package com.craws.mrx;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Timeline;
import com.craws.mrx.state.Vehicle;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView txt_itemCount;
        private ImageView img_Vehicle;

        public ViewHolder(final View itemView) {
            super(itemView);

            txt_itemCount = itemView.findViewById(R.id.text_itemCount);
            img_Vehicle = itemView.findViewById(R.id.TL_image_vehicle);
        }

    }

    private Timeline tl;

    public TimelineAdapter(final Timeline tl) {
        this.tl = tl;
    }

    @Override
    public @NonNull TimelineAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        return new ViewHolder(inflater.inflate(R.layout.timeline_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineAdapter.ViewHolder holder, int position) {
        // Set text showing round number
        if(position == 0) {
            holder.txt_itemCount.setText(R.string.timeline_move_start);
        } else {
            holder.txt_itemCount.setText(R.string.timeline_move_count);
            holder.txt_itemCount.append(String.valueOf(position));
        }

        // Get vehicle (might be null)
        Vehicle vehicle;
        Ticket ticket = tl.getTicketForRound(position);

        if(ticket != null) {                // check if Ticket is null, coz
            vehicle = ticket.getVehicle();  // <- calling .getVehicle
        } else {
        // Ticket in Timeline should only be null if it's the first round, so display the start graphic
            holder.img_Vehicle.setImageResource(R.drawable.vehicle_start);
            return;
        }

        switch(vehicle) {
            case SLOW:
                holder.img_Vehicle.setImageResource(R.drawable.vehicle_slow);
                break;
            case MEDIUM:
                holder.img_Vehicle.setImageResource(R.drawable.vehicle_medium);
                break;
            case FAST:
                holder.img_Vehicle.setImageResource(R.drawable.vehicle_fast);
                break;
            case SHADOW:
                holder.img_Vehicle.setImageResource(R.drawable.vehicle_shadow);
                break;
            default:
                holder.img_Vehicle.setImageResource(R.drawable.error);
        }
    }

    @Override
    public int getItemCount() {
        return tl.size();
    }
}
