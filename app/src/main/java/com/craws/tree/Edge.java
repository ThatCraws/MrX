package com.craws.tree;

/**
 * The edge of a (directed) graph. The Edge is designed to hold data the type of which can be freely assigned by the user.
 * Can be used as a directed graph by always connecting two Nodes from the source-node.
 *
 * @author Julien
 *
 */
public class Edge<U, V> {
    /** Represents the starting node of the edge*/
    private final  Node<U, V> src;

    /** Represents the destination node of the edge*/
    private final Node<U, V> target;

    /** Holds the data held/represented by the edge (yeah, I know...) TODO: Find a solution that does not suck major ballz */
    private final V data;

    /**
     * An Edge connects to given Nodes and can hold data assigned by the user.
     * @param src The source-node (for a directed graph, else the order does not influence anything)
     * @param target The target-node (for a directed graph, else the order does not influence anything)
     * @param data The data to be held. May be null.
     */
    public Edge(final Node<U, V> src, final Node<U, V> target, final V data) {
        this.src = src;
        this.target = target;
        this.data = data;
    }

    public Node<U, V> getSource() {
        return src;
    }

    public Node<U, V> getTarget() {
        return target;
    }

    public V getData() {
        return  data;
    }
}