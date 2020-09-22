package com.craws.mrx.MVVM;

public interface InventoryChangeListener {
    void onAdd(final int position);
    void onAddAll(final int newSize);

    void onRemove(final int position);
    void onClear(final int oldSize);

}
