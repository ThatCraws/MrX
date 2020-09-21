package com.craws.mrx.engine;

import com.craws.mrx.state.Ticket;

import java.util.List;

public interface InventoryChangeListener {
    void onAdd(final int position);
    void onAddAll(final int newSize);

    void onRemove(final int position);
    void onClear(final int oldSize);

}
