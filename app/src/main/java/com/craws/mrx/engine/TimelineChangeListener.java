package com.craws.mrx.engine;

import androidx.annotation.Nullable;

import com.craws.mrx.state.Place;
import com.craws.mrx.state.Ticket;

public interface TimelineChangeListener {
    void onTurnAdded(@Nullable final Ticket ticket, final Place destination);
    void onTurnMarked(final int round);
}
