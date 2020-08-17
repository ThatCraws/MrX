package com.craws.mrx.state;

import com.craws.mrx.graphics.City;
import com.craws.tree.Edge;
import com.craws.tree.Node;

import java.util.HashSet;

/**
 * Representing a place players can visit, meaning they end and start their turn on a place and can move from one to another.
 * Places have names that will be displayed to the user and are connected by routes.
 *
 * @author Julien
 *
 */
public class Place extends com.craws.tree.Node {

    /** The graphical representation of this Place. */
    private City parent;
    /** The name of the place to be shown to the player */
    private String name;

    private HashSet<Route> edges;

    public Place(City parent, String name) {
        super();
        this.parent = parent;
        this.name = name;
        edges = new HashSet<>();
    }

    /**
     * Connects this place to another given place.
     * It is recommended to check if a route already exists beforehand via isConnectedTo().
     *
     * @param target
     *            The place to connect this one to.
     * @param ticketNeeded
     *            The ticket you need.
     * @return The newly created route to connect the places.
     * @author Julien
     */
    public Edge connectTo(final Place target, final Vehicle ticketNeeded) {
        Route connection = new Route(this, target, ticketNeeded);
        connectTo(connection);
        target.connectTo(connection);
        return connection;
    }

    private void connectTo(final Route toAdd) {
        edges.add(toAdd);
    }

    /**
     * Checks if a given place is connected directly to this one via a place.
     *
     * @param toCheck
     *            The place to be checked for a connection to this one.
     * @author Julien
     */
    public boolean isConnectedTo(final Node toCheck) {
        for (Edge currEdge:edges) {
            if(currEdge.getTarget().equals(toCheck) || currEdge.getSource().equals(toCheck)) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public City getParent() {
        return parent;
    }

    public HashSet<Route> getRoutes() {
        return edges;
    }
}
