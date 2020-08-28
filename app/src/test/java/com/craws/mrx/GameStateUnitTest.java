package com.craws.mrx;

import com.craws.mrx.state.Ability;
import com.craws.mrx.state.GameState;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Vehicle;

import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.*;

/*
 * Testing the GameState. Building a Demo-Map and Hunting the players across the board.
 *
 */

public class GameStateUnitTest {

    GameState state;

    Place placeA;
    Place placeB;
    Place placeC;
    Place placeD;

    int port;

    /** Building a map to test on.
     *  Root
        |A|
         | SLOW
        |B|
MEDIUM /  \ FAST
     |C|  |D|
     */
    @Before
    public void setUp() {
        state = new GameState();

        placeA = state.buildPlace("Place A");
        placeB = state.buildPlace("Place B") ;
        placeC = state.buildPlace("Place C");
        placeD = state.buildPlace("Place D");

        state.buildStreet(placeA, placeB, Vehicle.SLOW);
        state.buildStreet(placeB, placeC, Vehicle.MEDIUM);
        state.buildStreet(placeB, placeD, Vehicle.FAST);

        try {
            assertEquals(Vehicle.SLOW, state.getStreet(placeA, placeB));
            assertEquals(Vehicle.MEDIUM, state.getStreet(placeB, placeC));
            assertEquals(Vehicle.FAST, state.getStreet(placeB, placeD));
        } catch (IllegalArgumentException e) {
            fail("One or more of the Streets were not successfully build.");
        }

        port = state.addDetective("Mr. Testective", placeA);
        assertEquals(1, port);
        assertEquals(placeA, state.getPlayerByPort(port).getPlace());
    }

    @Test
    public void testTicketInspector() {

        Ticket fromA2B = new Ticket(Vehicle.SLOW, Ability.SPECIAL);
        Ticket fromB2C = new Ticket(Vehicle.MEDIUM, Ability.SPECIAL);
        Ticket fromB2D = new Ticket(Vehicle.FAST, Ability.SPECIAL);
        state.giveTicket(port, fromA2B);
        state.giveTicket(port, fromB2C);
        state.giveTicket(port, fromB2D);

        // using wrong Ticket (or specifically, wrong Vehicle)
        assertFalse(state.doMove(port, placeB, fromB2C));
        // using right Ticket
        assertTrue(state.doMove(port, placeB, fromA2B));

        // did the Player move?
        assertEquals(placeB, state.getPlayerByPort(port).getPlace());
        // did the Ticket disappear from the inventory and can you only use Tickets which are currently in the inventory?
        assertFalse(state.doMove(port, placeA, fromA2B));

        // setting up next test (from placeC)
        assertTrue(state.doMove(port,placeC, fromB2C));

        state.giveTicket(port, fromA2B);
        state.giveTicket(port, fromB2C);

        // Can two non-connected Places be directly travelled to with any Ticket?
        assertFalse(state.doMove(port, placeD, fromA2B));
        assertFalse(state.doMove(port, placeD, fromB2C));
        assertFalse(state.doMove(port, placeD, fromB2D));
        assertEquals(state.getPlayerByPort(port).getPlace(), placeC);

        // Go to placeD through B
        assertTrue(state.doMove(port, placeB, fromB2C)); // fromB2C is also fromC2B, just need a MEDIUM-Ticket
        assertTrue(state.doMove(port, placeD, fromB2D));
        assertEquals(state.getPlayerByPort(port).getPlace(), placeD);

        // Just the slow fromA2B-Ticket should be left
        assertEquals(1, state.getInventory().size());
        assertEquals(state.getInventory().get(0), fromA2B);
    }

    @Test public void testAbilityTicketInspector() {
        Ticket special1 = new Ticket(Vehicle.SLOW, Ability.SPECIAL);
        Ticket special2 = new Ticket(Vehicle.MEDIUM, Ability.SPECIAL);
        Ticket special3 = new Ticket(Vehicle.FAST, Ability.SPECIAL);

        Vector<Ticket> toUse = new Vector<>();

        state.giveTicket(port, special1);
        state.giveTicket(port, special2);

        toUse.add(special1);
        toUse.add(special2);

        // not enough Tickets
        assertFalse(state.activateAbility(port, toUse, Ability.SPECIAL));
        toUse.add(special3);
        assertFalse(state.activateAbility(port, toUse, Ability.SPECIAL));

        state.giveTicket(port, special3);

        // the wrong kind of Tickets
        assertFalse(state.activateAbility(port, toUse, Ability.EXTRA_TURN));

        // the just right Tickets
        assertTrue(state.activateAbility(port, toUse, Ability.SPECIAL));
        // were all Tickets removed?
        assertTrue(state.getInventory().isEmpty());

        Ticket special4 = new  Ticket(Vehicle.MEDIUM, Ability.SPECIAL);

        state.giveTicket(port, special1);
        state.giveTicket(port, special2);
        state.giveTicket(port, special3);
        state.giveTicket(port, special4);

        toUse.add(special4);

        // use ability with more than 3 Tickets
        assertTrue(state.activateAbility(port, toUse, Ability.SPECIAL));
        assertTrue(state.getInventory().isEmpty());
    }

    @Test
    public void testTicketInspectorX() {
        port = state.addMrX(placeA);

        Ticket fromA2B = new Ticket(Vehicle.SLOW, Ability.SPECIAL);
        Ticket fromB2C = new Ticket(Vehicle.MEDIUM, Ability.SPECIAL);
        Ticket fromB2D = new Ticket(Vehicle.FAST, Ability.SPECIAL);

        state.giveTicket(port, fromA2B);
        state.giveTicket(port, fromB2C);
        state.giveTicket(port, fromB2D);

        // using wrong Ticket (or specifically, wrong Vehicle)
        assertFalse(state.doMove(port, placeB, fromB2C));
        // using right Ticket
        assertTrue(state.doMove(port, placeB, fromA2B));

        // did the Player move?
        assertEquals(placeB, state.getPlayerByPort(port).getPlace());
        // did the Ticket disappear from the inventory and can you only use Tickets which are currently in the inventory?
        assertFalse(state.doMove(port, placeA, fromA2B));

        // setting up next test (from placeC)
        assertTrue(state.doMove(port,placeC, fromB2C));

        state.giveTicket(port, fromA2B);
        state.giveTicket(port, fromB2C);

        // Can two non-connected Places be directly travelled to with any Ticket?
        assertFalse(state.doMove(port, placeD, fromA2B));
        assertFalse(state.doMove(port, placeD, fromB2C));
        assertFalse(state.doMove(port, placeD, fromB2D));
        assertEquals(state.getPlayerByPort(port).getPlace(), placeC);

        // Go to placeD through B
        assertTrue(state.doMove(port, placeB, fromB2C)); // fromB2C is also fromC2B, just need a MEDIUM-Ticket
        assertTrue(state.doMove(port, placeD, fromB2D));
        assertEquals(state.getPlayerByPort(port).getPlace(), placeD);

        // Just the slow fromA2B-Ticket should be left
        assertEquals(1, state.getInventoryX().size());
        assertEquals(state.getInventoryX().get(0), fromA2B);
    }

    @Test public void testAbilityTicketInspectorX() {
        port = state.addMrX(placeA);

        Ticket special1 = new Ticket(Vehicle.SLOW, Ability.SPECIAL);
        Ticket special2 = new Ticket(Vehicle.MEDIUM, Ability.SPECIAL);
        Ticket special3 = new Ticket(Vehicle.FAST, Ability.SPECIAL);

        Vector<Ticket> toUse = new Vector<>();

        state.giveTicket(port, special1);
        state.giveTicket(port, special2);

        toUse.add(special1);
        toUse.add(special2);

        // not enough Tickets
        assertFalse(state.activateAbility(port, toUse, Ability.SPECIAL));
        toUse.add(special3);
        assertFalse(state.activateAbility(port, toUse, Ability.SPECIAL));

        state.giveTicket(port, special3);

        // the wrong kind of Tickets
        assertFalse(state.activateAbility(port, toUse, Ability.EXTRA_TURN));

        // the just right Tickets
        assertTrue(state.activateAbility(port, toUse, Ability.SPECIAL));
        // were all Tickets removed?
        assertTrue(state.getInventoryX().isEmpty());

        Ticket special4 = new  Ticket(Vehicle.MEDIUM, Ability.SPECIAL);

        state.giveTicket(port, special1);
        state.giveTicket(port, special2);
        state.giveTicket(port, special3);
        state.giveTicket(port, special4);

        toUse.add(special4);

        // use ability with more than 3 Tickets (Which does not work for Mr. X)
        assertFalse(state.activateAbility(port, toUse, Ability.SPECIAL));

        // use special2-4 for the ability
        toUse.remove(special1);
        assertTrue(state.activateAbility(port, toUse, Ability.SPECIAL));

        // use last ticket to move from placeA to placeB via the special1 SLOW-ticket
        assertTrue(state.doMove(port, placeB, special1));

        assertTrue(state.getInventoryX().isEmpty());
    }

    @Test
    public void testGameOver() {
        // set Mr. X in the middle
        int portX = state.addMrX(placeB);

        // not yet surrounded
        assertFalse(state.isGameLost());
        assertFalse(state.isGameWon());

        // add a detective to the lower left of Mr. X
        state.addDetective("Is it getting hot in here?", placeC);

        // still one way to escape for Mr. X (lower right)
        assertFalse(state.isGameLost());
        assertFalse(state.isGameWon());
        // adding detective to the last place to escape to
        state.addDetective("One water please.", placeD);

        assertFalse(state.isGameLost());
        assertTrue(state.isGameWon());

        // Letting Mr. X win. Adding a goal-place
        Place goaldenCity = state.buildPlace("City of thieves", true);
        state.buildStreet(placeB, goaldenCity, Vehicle.FAST);

        /* Map looks like this now.
                Root
         Det -> |A|
            SLOW |   FAST
       Mr. X -> |B| ----- |Goal|
        MEDIUM /  \ FAST
       Det-> |C|  |D| <-Det
     */

        state.giveTicket(portX, new Ticket(Vehicle.FAST, Ability.SPECIAL));

        state.doMove(portX, goaldenCity, state.getInventoryX().firstElement());

        // Mr. X won
        assertTrue(state.isGameLost());
        assertFalse(state.isGameWon());

        // isPlaceOccupied-Test for empty City
        assertFalse(state.isPlaceOccupied(placeB));
        // for Mr. X in a City
        assertTrue(state.isPlaceOccupied(goaldenCity));
        // for Detective in a City
        assertTrue(state.isPlaceOccupied(placeA));
    }
}