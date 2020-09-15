package com.craws.mrx;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView;

class InventoryItemKeyProvider<K> extends ItemKeyProvider<Long> {
    RecyclerView recyclerView;


    protected InventoryItemKeyProvider(RecyclerView recyclerView) {
        super(ItemKeyProvider.SCOPE_MAPPED);
        this.recyclerView = recyclerView;
    }

    @Nullable
    @Override
    public Long getKey(int position) {
        RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();

        if(adapter != null) {
            return adapter.getItemId(position);
        }
        throw new NullPointerException("Adapter of RecyclerView is null");
    }

    @Override
    public int getPosition(@NonNull Long key) {
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForItemId(key);
        if(holder != null) {
            return holder.getLayoutPosition();
        }
        throw new NullPointerException("ViewHolder with key " + key + " could not be found.");
    }
}