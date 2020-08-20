package com.craws.tree;

/**
 * The edge of a (directed) graph. To get an undirected graph create two edges for each direction.
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