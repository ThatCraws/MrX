package com.craws.mrx;

import com.craws.mrx.state.Ability;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Player;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Vehicle;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerUnitTest {

    /**
    private static Player player;
    private static Place cityA;
    private static Place cityB;
    private static Place cityC;

    @BeforeClass
    public static void setUp() {
        cityA = new Place(null, "Brem");
        cityB = new Place(null, "Kaffster");
        cityC = new Place(null, "real Place");

        cityA.connectTo(cityB, Vehicle.FAST);
        cityB.connectTo(cityC, Vehicle.SLOW);

        player = new Player(1, "Jack", cityA);
    }

    @Test
    public void testConnectedPlaces() {
        assertTrue(cityA.isConnectedTo(cityB));
        assertTrue(cityB.isConnectedTo(cityA));

        assertTrue(cityB.isConnectedTo(cityC));
        assertTrue(cityC.isConnectedTo(cityB));

        assertFalse(cityA.isConnectedTo(cityC));
        assertFalse(cityC.isConnectedTo(cityA));

        assertEquals(1, cityA.getRoutes().size());
        assertEquals(2, cityB.getRoutes().size());
        assertEquals(1, cityC.getRoutes().size());
    }

    @Test
    public void testMovingPlayer() {
        Ticket goldenTicket = new Ticket(Vehicle.FAST, Ability.SPECIAL);
        Ticket wrongTicket = new Ticket(Vehicle.SLOW, Ability.SPECIAL);

        // try moving without ticket
        assertFalse(player.doTurn(cityB, goldenTicket));
        assertFalse(player.doTurn(cityC, goldenTicket));

        // try moving with wrong ticket
        player.giveTicket(wrongTicket);

        assertFalse(player.doTurn(cityB, wrongTicket));
        assertFalse(player.doTurn(cityC, wrongTicket));

        // move with right ticket
        player.giveTicket(goldenTicket);

        assertTrue(player.getInventory().contains(goldenTicket));

        assertTrue(player.doTurn(cityB, goldenTicket));

        // make sure ticket is gone
        assertFalse(player.getInventory().contains(goldenTicket));
        // other ticket still there
        assertTrue(player.getInventory().contains(wrongTicket));
        // player actually moved
        assertEquals(cityB, player.getCurrPlace());

        // use wrong ticket to move to city C
        assertTrue(player.doTurn(cityC, wrongTicket));
        // make sure ticket is gone
        assertFalse(player.getInventory().contains(wrongTicket));
        // player actually moved
        assertEquals(cityC, player.getCurrPlace());

    }
    */
}
