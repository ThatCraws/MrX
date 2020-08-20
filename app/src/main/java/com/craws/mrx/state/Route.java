package com.craws.mrx.state;

import com.craws.tree.Edge;

/**
 * The routes connect the places. The Players will need a ticket for the right vehicle.
 *
 * @author Julien
 *
 */
public class Route {
    /** The ticket needed to travel this route. Should be chosen depending on the length of the route. */
    private Vehicle ticketNeeded;

    public Route(Vehicle ticketNeeded) {
        this.ticketNeeded = ticketNeeded;
    }

    public Vehicle getTicketNeeded() {
        return ticketNeeded;
    }

}
