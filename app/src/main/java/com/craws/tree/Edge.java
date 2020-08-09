package com.craws.tree;

/**
 * The edge of a (directed) graph. To get an undirected graph create two edges for each direction.
 *
 * @author Julien
 *
 */
public class Edge {
    /** Represents the starting node of the edge*/
    private final Node src;

    /** Represents the destination node of the edge*/
    private final Node target;

    public Edge(final Node src, final Node target) {
        this.src = src;
        this.target = target;
    }

    public Node getSource() {
        return src;
    }
    public Node getTarget() {
        return target;
    }
}
