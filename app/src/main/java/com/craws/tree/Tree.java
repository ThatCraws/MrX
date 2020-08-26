package com.craws.tree;

import java.util.Vector;

/**
 * Represents a Tree and supplies methods to manage and retrieve information from the Nodes and Edges.
 *
 * @author Julien
 *
 */
public class Tree<U, V> {
    private Node<U, V> root;

    private Vector<Node<U, V>> nodes;
    private Vector<Edge<U, V>> edges;

    public Tree() {
        this.root = null;
        nodes = new Vector<>();
        edges = new Vector<>();
    }

    public Tree(Node<U, V> root) {
        this.root = root;
        nodes = new Vector<>();
        nodes.add(root);

        edges = new Vector<>();
    }

    public Tree(U rootData) {
        root = new Node<>(rootData);
        nodes = new Vector<>();
        nodes.add(root);

        edges = new Vector<>();
    }

    /**
     * Inserts Node into the tree and returns its index.
     *
     * @param data
     *              The Node's contained data.
     * @return The index of the newly inserted Node.
     * @author Julien
     */
    public int insertNode(U data) {
        Node<U, V> newNode = new Node<>(data);
        if(root == null) {
            root = newNode;
        }
        nodes.add(newNode);
        return nodes.size() - 1;
    }

    /**
     * Connects two nodes via an Edge. If an Edge connecting the Nodes already exists, nothing happens, but the Edge will be returned.
     *
     * @param src
     *              The index of the source node to be connected to the target node.
     * @param target
     *              The index of the target node to be connected to the source node.
     * @param data
     *              The data of the Edge to be created.
     * @return      The Edge connecting the Nodes or null if one of the given indices is not associated with a Node.
     * @author Julien
     */
    public Edge<U, V> insertEdge(final int src, final int target, final V data) {
        Node<U, V> srcNode;
        Node<U, V> targetNode;
        try {
            srcNode = getNode(src);
            targetNode = getNode(target);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
        }

        if(isConnected(src, target)) {
            return srcNode.getConnectionTo(targetNode);
        }

        Edge<U, V> newEdge = srcNode.connectTo(targetNode, data);
        edges.add(newEdge);
        return newEdge;
    }

    /**
     * Checks whether two Nodes are connected by an Edge.
     *
     * @param src
     *              The index of the first Node to be checked for a connection.
     * @param target
     *              The index of the second Node to be checked for a connection.
     * @return true, if the two given Nodes are connected.
     *         false, else (if no Edge connecting the Nodes or indices out of range.
     * @author Julien
     */
    public boolean isConnected(final int src, final int target) {
        if(src == target) {
            throw new IllegalArgumentException(
                    "The nodes to check for connection may not be the same.");
        }
        try {
            return getNode(src).isConnectedTo(getNode(target));
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns the index associated with the given node.
     *
     * @param theNode
     *            The Node to retrieve the index of
     * @return The index of the given node or -1, if it couldn't be found.
     * @author Julien
     */
    public int getIndexByNode(final Node<U, V> theNode) {
        for (int currIndex = 0 ; currIndex < nodes.size() ; currIndex++) {
            if (nodes.get(currIndex).equals(theNode)) {
                return currIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the index associated with the node with the given data.
     *
     * @param theData
     *            The data to look for in the Nodes.
     * @return The index of the given node or -1, if it couldn't be found.
     * @author Julien
     */
    public int getIndexByData(final U theData) {
        for (int currIndex = 0 ; currIndex < nodes.size() ; currIndex++) {
            if (nodes.get(currIndex).getData().equals(theData)) {
                return currIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the data of the Edge connecting the Nodes represented by the given indices.
     *
     * @param src
     *              The index of the first Node connected to the second Node.
     * @param target
     *              The index of the second Node connected to the first Node.
     * @return The data of the Edge connecting the Nodes or Null, if the Edge couldn't be found or the given indices are out of range.
     * @author Julien
     */
    public V getEdgeData(final int src, final int target) {
        Node<U, V> srcNode;
        Node<U, V> targetNode;
        try {
            srcNode = getNode(src);
            targetNode = getNode(target);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return  null;
        }
        for (Edge<U, V> currEdge : edges) {
            if ((currEdge.getSource().equals(srcNode) && currEdge
                    .getTarget().equals(targetNode))
                    || (currEdge.getSource().equals(targetNode) && currEdge
                    .getTarget().equals(srcNode))) {
                return currEdge.getData();
            }
        }
        return null;
    }

    /**
     * Returns the data of the Node represented by the given index.
     *
     * @param node
     *              The index of the Node the data of which to retrieve.
     * @return The data of the Node with the given index.
     * @author Julien
     */
    public U getNodeData(final int node) {
        Node<U, V> theNode;
        try {
            theNode = getNode(node);
        } catch(IndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
        }
        return theNode.getData();
    }

    /**
     * Returns the Node represented by the given index.
     *
     * @param node
     *              The index of the Node to retrieve.
     * @return The Node with the given index.
     * @throws IndexOutOfBoundsException When the given index exceeds the highest index present in the tree.
     * @author Julien
     */
    public Node<U, V> getNode(final int node) {
        if (nodes.size() <= node) {
            throw new IndexOutOfBoundsException(
                    "At least one of the nodes is not in the graph!");
        }
        return nodes.get(node);
    }

    /**
     * Returns a Vector containing all Nodes' indices of the Nodes adjacent to the Node represented by the given index.
     *
     * @param node
     *          The index of the Node the neighbours' indices to return of which to return.
     *
     * @return A vector of all the Nodes' indices that are directly connected to the given one.
     * @author Julien
     */
    public Vector<Integer> getAdjacentNodeIDs(final int node) {
        Node<U, V> theNode;
        // Is the Node present?
        try {
            theNode = getNode(node);
        } catch(IndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
        }

        // save indices in this
        Vector<Integer> toRet = new Vector<>();
        // go through adjacent nodes
        for(Node<U, V> currNode: theNode.getAdjacentNodes()) {
            // get index of current adjacent node. If index could not be found, the node has an edge in its edge-vector connecting it to a node which is not saved in the tree (which should not happen)
            int toAdd = nodes.indexOf(currNode);
            if(toAdd == -1) {
                throw new IllegalStateException("The given Node is internally connected with a Node which is not saved/managed in the Tree.");
            }
            toRet.add(toAdd);
        }

        return toRet;
    }

    /**
     * Checks whether the tree is empty or not.
     *
     * @return true, if no Nodes or Edges were inserted into the Tree so far.
     * @author Julien
     */
    public boolean isEmpty() {
        return nodes.size() == 0 && edges.size() == 0;
    }

    public Vector<Integer> getNodeIDs() {
        Vector<Integer> toRet = new Vector<>();
        for(int i = 0; i < nodes.size(); i++) {
            toRet.add(i);
        }
        return toRet;
    }

    // ----------- GETTERS -----------

    // Should this even stay? To the outside I wanna handle IDs and not Nodes...
    public Node<U, V> getRoot() {
        return root;
    }
    // Should this even stay? To the outside I wanna handle IDs and not Nodes...
    public Vector<Node<U, V>> getNodes() {
        return nodes;
    }

    public int getNumberOfNodes() {
        return nodes.size();
    }

    public int getNumberOfEdges() {
        return edges.size();
    }
    // ----------- SETTERS -----------

    public void setRoot(final Node<U, V> newRoot) {
        root = newRoot;
    }
}
