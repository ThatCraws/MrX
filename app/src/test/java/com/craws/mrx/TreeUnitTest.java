package com.craws.mrx;

import com.craws.mrx.state.Place;
import com.craws.mrx.state.Route;
import com.craws.mrx.state.Vehicle;
import com.craws.tree.Tree;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TreeUnitTest {

    Tree<Place, Route> theWorld;

    Place placeA;
    Place placeB;
    Place placeC;

    @Before
    public void setUp() {
        placeA = new Place(null, "Place A");
        placeB = new Place(null, "Place B");
        placeC = new Place(null, "Place C");

        theWorld = new Tree<>(placeA);
    }


    @Test
    public void testConnectPlaces() {
        int rootIndex = theWorld.getIndexByNode(theWorld.getRoot());
        int placeBIndex = theWorld.insertNode(placeB);
        theWorld.insertEdge(rootIndex, placeBIndex, new Route(Vehicle.MEDIUM));

        int placeCIndex = theWorld.insertNode(placeC);
        theWorld.insertEdge(placeBIndex, placeCIndex, new Route(Vehicle.FAST));

        assertTrue(theWorld.isConnected(rootIndex, placeBIndex));

        assertTrue(theWorld.isConnected(rootIndex, placeBIndex));
        assertTrue(theWorld.isConnected(rootIndex, placeBIndex));
        assertTrue(theWorld.isConnected(placeBIndex, rootIndex));

        assertTrue(theWorld.isConnected(placeBIndex, placeCIndex));
        assertTrue(theWorld.isConnected(placeCIndex, placeBIndex));

        assertFalse(theWorld.isConnected(rootIndex, placeCIndex));
        assertFalse(theWorld.isConnected(placeCIndex, rootIndex));

        assertEquals(1, theWorld.getNode(rootIndex).getEdges().size());
        assertEquals(2, theWorld.getNode(placeBIndex).getEdges().size());
        assertEquals(1, theWorld.getNode(placeCIndex).getEdges().size());
    }
}
