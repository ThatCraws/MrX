package com.craws.mrx;

import com.craws.tree.Node;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TreeUnitTest {

    Node nodeA;
    Node nodeB;
    Node nodeC;

    @Before
    public void setUp() {
        nodeA = new Node();
        nodeB = new Node();
        nodeC = new Node();
    }


    @Test
    public void testConnectNodes() {
        nodeA.connectTo(nodeB);
        nodeB.connectTo(nodeC);

        assertTrue(nodeA.isConnectedTo(nodeB));
        assertTrue(nodeB.isConnectedTo(nodeA));

        assertTrue(nodeB.isConnectedTo(nodeC));
        assertTrue(nodeC.isConnectedTo(nodeB));

        assertFalse(nodeA.isConnectedTo(nodeC));
        assertFalse(nodeC.isConnectedTo(nodeA));

        assertEquals(1, nodeA.getEdges().size());
        assertEquals(2, nodeB.getEdges().size());
        assertEquals(1, nodeC.getEdges().size());
    }
}
