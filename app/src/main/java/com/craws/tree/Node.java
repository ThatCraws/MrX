package com.craws.tree;

import java.util.HashSet;
import java.util.Vector;

/**
 * The node of a (directed) graph. Can hold any data-type assigned by the user.
 *
 * @author Julien
 *
 */
public class Node<U, V> {
    /** Holds the data held/represented by the node */
    private U data;

    /** Collects all the edges coming from this node */
    private HashSet<Edge<U, V>> edges;

    /**
     * A Node holds data assigned by the user. Connect to other Nodes via Edge by calling the connectTo()-Method (from the source-node for directed graphs)
     * @param data The data to assign to this Node.
     * @see Node#connectTo(Node, Object)
     */
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
     * @param data The data to give the newly created Edge to hold.
     *
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
     * Removes the Edge connecting the given node to this one from the edge-Collection for this note and the connected one.
     * The Edge will be removed by the GC, if no other references exist.
     *
     * @param toDisconnect
     *            The node to be disconnected.
     * @throws IllegalArgumentException If the given Node is the Node this method is called from.
     * @author Julien
     */
    private void disconnectFrom(final Node<U, V> toDisconnect) {
        if(toDisconnect.equals(this)) {
            throw new IllegalArgumentException("Tried to disconnect Node from itself.");
        }
        for(Edge<U, V> currEdge: edges) {
            if(currEdge.getSource().equals(toDisconnect)) {
                currEdge.getSource().removeEdge(currEdge);
                removeEdge(currEdge);
                return;
            } else if(currEdge.getTarget().equals(toDisconnect)) {
                currEdge.getTarget().removeEdge(currEdge);
                removeEdge(currEdge);
                return;
            }
        }
    }

    private void removeEdge(Edge<U, V> toRemove) {
        edges.remove(toRemove);
    }

    /**
     * Checks if a given node is connected directly to this one via an edge.
     *
     * @param toCheck
     *            The node to be checked for a connection to this one.
     *
     * @return True, if the given Node is directly connected to this one via an Edge
     *          False, if not.
     *
     * @author Julien
     */
    public boolean isConnectedTo(final Node<U, V> toCheck) {
        return getConnectionTo(toCheck) != null;
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

    public Vector<Node<U, V>> getAdjacentNodes() {
        Vector<Node<U, V>> toRet = new Vector<>();
        for (Edge<U, V> currEdge : edges) {
            // Don't add this Node
            if (!currEdge.getSource().equals(this)) {
                toRet.add(currEdge.getSource());
            } else {
                toRet.add(currEdge.getTarget());
            }
        }
        return toRet;
    }

    public U getData() {
        return data;
    }
}