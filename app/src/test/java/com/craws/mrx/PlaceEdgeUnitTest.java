package com.craws.mrx;

import com.craws.mrx.state.Route;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Vehicle;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaceEdgeUnitTest {

    Place placeA;
    Place placeB;
    Place placeC;

    @Before
    public void setUp() {
        placeA = new Place(null, "Place A");
        placeB = new Place(null, "Place B");
        placeC = new Place(null, "Place C");
    }


    @Test
    public void testConnectPlaces() {
        placeA.connectTo(placeB, Vehicle.MEDIUM);
        placeB.connectTo(placeC, Vehicle.FAST);

        assertTrue(placeA.isConnectedTo(placeB));
        assertTrue(placeB.isConnectedTo(placeA));

        assertTrue(placeB.isConnectedTo(placeC));
        assertTrue(placeC.isConnectedTo(placeB));

        assertFalse(placeA.isConnectedTo(placeC));
        assertFalse(placeC.isConnectedTo(placeA));

        assertEquals(1, placeA.getRoutes().size());
        assertEquals(2, placeB.getRoutes().size());
        assertEquals(1, placeC.getRoutes().size());
    }
}
