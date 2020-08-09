package com.craws.tree;

import java.util.HashSet;

/**
 * The node of a (directed) graph. Does not contain data yet, but is meant to be derived and given a data-field that way.
 *
 * @author Julien
 *
 */
public class Node {
    /** Collects all the edges coming from this node */
    private HashSet<Edge> edges;

    public Node() {
        edges = new HashSet<Edge>();
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
    public Edge connectTo(final Node target) {
        Edge connection = new Edge(this, target);
        connectTo(connection);
        target.connectTo(connection);
        return connection;
    }

    private void connectTo(final Edge toAdd) {
        edges.add(toAdd);
    }

    /**
     * Checks if a given node is connected directly to this one via an edge.
     *
     * @param toCheck
     *            The node to be checked for a connection to this one.
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

    public HashSet<Edge> getEdges() {
        return edges;
    }
}
