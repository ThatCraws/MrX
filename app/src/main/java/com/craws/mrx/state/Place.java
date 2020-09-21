package com.craws.mrx.state;

/**
 * Representing a place players can visit, meaning they end and start their turn on a place and can move from one to another.
 * Places have names that will be displayed to the user and are connected by routes.
 *
 * @author Julien
 *
 */
public class Place {

    /** The name of the place to be shown to the player */
    private String name;
    /** Flag that shows, if this Place is a goal-field for Mr. X to get to to win the game. */
    private boolean goal;

    public Place(final String name, final boolean goal) {
        this.name = name;
        this.goal = goal;
    }

    public Place(final String name) {
        this.name = name;
        this.goal = false;
    }

    public String getName() {
        return name;
    }

    public boolean isGoal() {
        return goal;
    }
}
