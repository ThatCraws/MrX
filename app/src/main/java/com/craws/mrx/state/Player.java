package com.craws.mrx.state;

/**
 * The player class saves the current state of the player's character and gives basic functionality all players share.
 * They travel between the places looking for Mr. X (or are Mr. X running from the detectives).
 *
 * On instantiation automatically creates a Figure for graphically represent this Player.
 *
 * @author Julien
 *
 */
public class Player {
    /** The port of a player. Like an ID. ID 0 is reserved for Mr. X while 1-4 represent the detectives. */
    private int port;
    /** The player's name*/
    private String alias;
    /** The current place the player resides in */
    private Place place;

    /** Constructor for creating a new Player
     *
     * @param port The port of a player. Like an ID
     * @param alias The player's name
     * @param startPosition The starting position/place of the player (optional, default value: null)
     */
    public Player(int port, String alias, Place startPosition) {
        this.port = port;
        this.alias = alias;
        this.place = startPosition;
    }

    public Player(int port, String alias) {
        this.port = port;
        this.alias = alias;
        this.place = null;
    }

    public int getPort() {
        return port;
    }

    public String getAlias() {
        return alias;
    }

    public Place getPlace() {
        return place;
    }

    /**
     * Moves the player to another place by changing his current place.
     *
     * @param dest
     *            The place to move the player to.
     *
     * @author Julien
     */
    public void setPlace(Place dest) {
        place = dest;
    }
}
