package com.craws.mrx.engine;

import com.craws.mrx.state.Ticket;

import java.util.Vector;

public interface InventoryChangeListener {
    void onAdd(final Ticket added);
    void onRemove(final int position);
    void onNewInventory(final Vector<Ticket> newInventory);
}
