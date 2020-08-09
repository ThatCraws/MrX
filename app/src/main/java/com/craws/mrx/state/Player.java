package com.craws.mrx.state;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * The player class saves the current state of the player's character. They travel between the places looking for Mr. X
 *
 * @author Julien
 *
 */
public class Player {
    /** The port of a player. Like an ID*/
    private int port;
    /** The player's name*/
    private String alias;
    /** The current place the player resides in */
    private Place currPlace;
    /** The player's inventory in which he collects his tickets */
    private ArrayList<Ticket> inventory;

    public Player(int port, String alias, Place startPosition) {
        this.port = port;
        this.alias = alias;
        this.currPlace = startPosition;
        this.inventory = new ArrayList<Ticket>();
    }

    /**
     * Moves the player to another place by changing his current place.
     *
     * @param dest
     *            The place to move the player to.
     *
     * @author Julien
     */
    private void moveTo(Place dest) {
        currPlace = dest;
    }

    /**
     * Adds the given ticket to the inventory.
     *
     * @param toGive
     *            The ticket to be added to the inventory
     *
     * @author Julien
     */
    public void giveTicket(Ticket toGive) {
        inventory.add(toGive);
    }

    /**
     * Removes the given ticket from the inventory.
     *
     * @param toUse
     *            The ticket to be removed from the inventory
     * @return true, if the ticket was successfully removed from the inventory.
     *          false, if the ticket is not in the inventory.
     *
     * @author Julien
     */
    private boolean useTicket(Ticket toUse) {
        if(inventory.contains(toUse)) {
            inventory.remove(toUse);
            return true;
        }
        return  false;
    }

    /**
     * Returns a HashSet of all the routes connecting from the player's current place.
     *
     * @return A HashSet of  all the routes connecting from the player's current place.
     *
     * @author Julien
     */
    public HashSet<Route> getRoutesForTurn() {
        return currPlace.getRoutes();
    }

    /**
     * Moves the player to a given place. Checks that the move is valid beforehand though (unlike {@see #moveTo} which is used in this method).
     *
     * @param target
     *              The target place to move the player to.
     * @param ticketUsed
     *              The ticket used.
     *
     * @return true, if the move was successfully done.
     *          false, if the move was invalid.
     *
     * @author Julien
     */
    public boolean doTurn(Place target, Ticket ticketUsed) {
        for (Route currRoute: getRoutesForTurn()) {
            if(target == currRoute.getTarget() || target == currRoute.getSource()) {
                if(currRoute.getTicketNeeded().equals(ticketUsed.getVehicle()) && useTicket(ticketUsed)) {
                    moveTo(target);
                    return true;
                }
            }
        }
        return false;
    }


    public int getPort() {
        return port;
    }

    public String getAlias() {
        return alias;
    }

    public Place getCurrPlace() {
        return currPlace;
    }

    public ArrayList<Ticket> getInventory() {
        return inventory;
    }
}
