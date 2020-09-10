package com.craws.mrx;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.craws.mrx.state.Timeline;
import com.craws.mrx.state.Vehicle;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView txt_itemCount;
        private ImageView img_Vehicle;
        private TextView txt_place;

        public ViewHolder(final View itemView) {
            super(itemView);

            txt_itemCount = itemView.findViewById(R.id.text_itemCount);
            img_Vehicle = itemView.findViewById(R.id.TL_image_vehicle);
            txt_place = itemView.findViewById(R.id.text_place);
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
        Vehicle vehicle = tl.getTicketForRound(position).getVehicle();

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
            default:
                holder.img_Vehicle.setImageResource(R.drawable.error);
        }

        holder.txt_place.setText(tl.getPlaceForRound(position).getName());

        holder.txt_itemCount.setText(R.string.timeline_move_count);
        holder.txt_itemCount.append(String.valueOf(position));
    }

    @Override
    public int getItemCount() {
        return tl.size();
    }
}