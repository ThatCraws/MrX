package com.craws.mrx.state;

import com.craws.tree.Edge;
import com.craws.tree.Node;

/**
 * The routes connect the places. The Players will need a ticket for the right vehicle.
 *
 * @author Julien
 *
 */
public class Route extends Edge {
    /** The ticket needed to travel this route. Should be chosen depending on the length of the route. */
    private Vehicle ticketNeeded;

    public Route(Place src, Place target, Vehicle ticketNeeded) {
        super(src, target);
        this.ticketNeeded = ticketNeeded;
    }

    public Vehicle getTicketNeeded() {
        return ticketNeeded;
    }

}
