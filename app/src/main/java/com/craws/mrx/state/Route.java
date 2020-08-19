package com.craws.mrx.state;

/**
 * The routes connect the places. The Players will need a ticket for the right vehicle.
 *
 * @author Julien
 *
 */
public class Route {
    /** The source Place/node the Route originates from */
    private Place src;
    /** The destination Place/node the Route goes to */
    private Place target;

    /** The ticket needed to travel this route. Should be chosen depending on the length of the route. */
    private Vehicle ticketNeeded;

    public Route(Place src, Place target, Vehicle ticketNeeded) {
        this.src = src;
        this.target = target;
        this.ticketNeeded = ticketNeeded;
    }

    public Place getSrc() {
        return src;
    }

    public Place getTarget() {
        return target;
    }

    public Vehicle getTicketNeeded() {
        return ticketNeeded;
    }

}
