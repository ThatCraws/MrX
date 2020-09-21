package com.craws.mrx.MVVM;

import com.craws.mrx.state.Place;
import com.craws.mrx.state.Player;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Vehicle;
import com.craws.tree.Edge;

import java.util.Vector;

public interface StateModelObserver {
    // Map building
    void onPlayerAdded(final Player player);
    void onPlaceAdded(final Place place);
    void onStreetAdded(final Edge<Place, Vehicle> street);

    // Turns
    void onPlayerMoved(final Player playerMoved, final Place movedTo);
    void onPlayerActivePlayerChanged(final Player newActivePlayer);

    // Timeline
    void onTimelineEntryAdded(final Ticket ticketUsed, final Place movedTo);
    void onTimelineEntryMarked(final int position);

    // Inventory
    void onInventoryChanged(final Vector<Ticket> newInventory);
    void onInventoryTicketAdded(final Ticket ticketAdded);
    void onInventoryTicketRemoved(final int position);

    // Phase management
    void onPhaseChange(final StateModel.GAME_PHASE phase);
}
