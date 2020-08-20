package com.craws.tree;

import java.util.HashSet;

/**
 * The node of a (directed) graph. Does not contain data yet, but is meant to be derived and given a data-field that way.
 *
 * @author Julien
 *
 */
public class Node<U, V> {
    /** Holds the data held/represented by the node */
    private U data;

    /** Collects all the edges coming from this node */
    private HashSet<Edge<U, V>> edges;

    public Node(U data) {
        this.data = data;
        edges = new HashSet<>();
    }

    /**
     * Connects this node to a given node.
     * It is recommended to check if a connection already exists beforehand via isConnectedTo().
     *
     * @param target
     *            The node to connect this one to.
     * @return The newly created edge to connect the nodes.
     * @author Julien
     */
    public Edge<U, V> connectTo(final Node<U, V> target, final V data) {
        Edge<U, V> connection = new Edge<>(this, target, data);
        connectTo(connection);
        target.connectTo(connection);
        return connection;
    }

    private void connectTo(final Edge<U, V> toAdd) {
        edges.add(toAdd);
    }

    /**
     * Returns the Edge connecting the given node to this one.
     *
     * @param toCheck
     *            The node to be checked for a connection to this one.
     * @return The Edge connecting the given node to this one or null if they are not connected.
     * @author Julien
     */
    public Edge<U, V> getConnectionTo(final Node<U, V> toCheck) {
        if(toCheck.equals(this)) {
            throw new IllegalArgumentException("The nodes to check for connection must not be the same.");
        }
        for (Edge<U, V> currEdge:edges) {
            if(currEdge.getTarget().equals(toCheck) || currEdge.getSource().equals(toCheck)) {
                return currEdge;
            }
        }
        return null;
    }

    /**
     * Checks if a given node is connected directly to this one via an edge.
     *
     * @param toCheck
     *            The node to be checked for a connection to this one.
     * @author Julien
     */
    public boolean isConnectedTo(final Node<U, V> toCheck) {
        return getConnectionTo(toCheck) != null;
    }

    public U getData() {
        return data;
    }

    public HashSet<Edge<U, V>> getEdges() {
        return edges;
    }
}