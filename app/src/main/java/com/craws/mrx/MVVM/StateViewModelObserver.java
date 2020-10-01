package com.craws.mrx.MVVM;

import java.util.Vector;

public interface StateViewModelObserver {
    // Inventory changes
    void onInventoryAdd(final int position);
    void onInventoryAddAll(final int newSize);

    void onInventoryRemove(final int position);
    void onInventoryClear(final int oldSize);

    // Timeline changes
    void onTimelineTurnAdded(final int position);
    void onTimelineTurnMarked(final int round);

    // Phase changes
    void onPhaseChange(final StateModel.GAME_PHASE phase);
    void onUserMessageChange(final String newMessage);
}
