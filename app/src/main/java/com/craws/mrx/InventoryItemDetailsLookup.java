package com.craws.mrx;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

public class InventoryItemDetailsLookup extends ItemDetailsLookup<Long> {
    private RecyclerView recycly;

    public InventoryItemDetailsLookup(final RecyclerView recycly) {
        this.recycly = recycly;
    }

    @Nullable
    @Override
    public ItemDetails<Long> getItemDetails(@NonNull MotionEvent event) {
        View clicked = recycly.findChildViewUnder(event.getX(), event.getY());
        if(clicked != null) {
            InventoryAdapter.ViewHolder clickedViewHolder = (InventoryAdapter.ViewHolder)(recycly.getChildViewHolder(clicked));

            return clickedViewHolder.getItemDetails();
        }
        return null;
    }
}
