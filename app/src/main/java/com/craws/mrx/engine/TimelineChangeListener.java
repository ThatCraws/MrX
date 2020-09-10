package com.craws.mrx.engine;

import androidx.annotation.Nullable;

import com.craws.mrx.state.Place;
import com.craws.mrx.state.Ticket;

public interface TimelineChangeListener {
    void onTurnAdded(final Place destination, @Nullable final Ticket ticket);
}
