package com.craws.mrx.state;

import android.content.Context;

import com.craws.tree.Node;
import com.craws.tree.Tree;

import java.util.Vector;

public class GameState {
    /* ------ The Detectives and Mr. X ------ */
    private Vector<Player> players;
    private int port;

    private Player mrX;

    private Timeline timeline;

    /* ------ The playing field ------ */
    // The game map will be represented by a Tree consisting of the Places(Structural: Node, Graphical: City) and Routes(Structural: Edge, Graphical: A Line).
    private Tree<Place, Vehicle> map;
    private Vector<Place> startFields;
    private Vector<Place> startFieldsX;

    /* ------ The tickets (Vehicle/Ability) ------ */
    // A simulated bag of tickets. Tickets will be drawn on random from here.
    private Vector<Ticket> bagOfTickets;
    // The detective player's inventory
    private Vector<Ticket> inventory;

    // The Mr. X player's inventory;
    private Vector<Ticket> inventoryX;

    public GameState() {
        map = new Tree<>();
        setup();
    }

    public GameState(final Node<Place, Vehicle> firstNode) {
        map = new Tree<>(firstNode);
        setup();
    }

    public GameState(final Place firstPlace) {
        map = new Tree<>(firstPlace);
        setup();
    }

    private void setup() {
        players = new Vector<>();
        bagOfTickets = new Vector<>();

        timeline = new Timeline();

        inventory = new Vector<>();
        inventoryX = new Vector<>();

        fillTicketBag();

        port = 0;

        startFields = new Vector<>();
        startFieldsX = new Vector<>();

        dealTickets();
    }

    private void fillTicketBag() {
        for(int count = 0; count < 130; count++) {
            Ticket newTicket = new Ticket(Vehicle.values()[(int)(Math.random()*3)], Ability.values()[(int)(Math.random()*2)]);
            bagOfTickets.add(newTicket);
        }
    }

    private void showTickets() {
        for(int i = 0; i < bagOfTickets.size(); i++) {
            System.out.println("Ticket " + i + ": V = " + bagOfTickets.get(i).getVehicle() + "; A = " + bagOfTickets.get(i).getAbility() + ".");
        }
    }

    private void dealTickets() {
        Ticket randoTicket;
        while(inventoryX.size() < 8) {
            randoTicket = getRandomFromVec(bagOfTickets);
            inventoryX.add(randoTicket);
            inventoryX.remove(randoTicket);
        }

        while(inventory.size() < (4 + players.size())) {
            randoTicket = getRandomFromVec(bagOfTickets);
            inventoryX.add(randoTicket);
            inventoryX.remove(randoTicket);
        }
    }

    /**
     * Builds a place in the map. Meaning adding a node to the map that represents the playing field.
     * Connections between Places can be made via buildStreet().
     * @param name The user-displayable name of the Place.
     *
     * @author Julien
     *
     */
    public void buildPlace(final String name) {
        map.insertNode(new Place(name));
    }

    /**
     * Builds a street between to places in the map. Meaning adding a connection between two given nodes on the map that represents the playing field.
     * Nodes can be added via buildPlace().
     *
     * @param start The starting point of the street (starting and ending points do not matter and are just internally managed this way)
     * @param end The ending point of the street (starting and ending points do not matter and are just internally managed this way)
     * @param ticketNeeded The Ticket needed to travel this street
     *
     * @author Julien
     *
     */
    public void buildStreet(final Place start, final Place end, final Vehicle ticketNeeded) {
        final int startIndex = map.getIndexByData(start);
        final int endIndex = map.getIndexByData(end);

        if(startIndex == -1 || endIndex == -1 ) {
            return;
        }

        map.insertEdge(startIndex, endIndex, ticketNeeded);
    }

    /**
     * Creates a detective and adds them to the player-collection
     *
     * @param alias The user-displayable name of the detective
     * @param startPosition The place the detective starts/spawns on. Should be subset of the startFields-Vector or null. Default: null
     *
     * @author Julien
     *
     */
    public void addDetective(final String alias, final Place startPosition) {
        Player newChallenger = new Player(port, alias, startPosition);
        addDetective(newChallenger);
    }

    public void addDetective(final String alias) {
        Player newChallenger = new Player(port, alias, getRandomFromVec(startFields));
        addDetective(newChallenger);
    }

    private void addDetective(Player toAdd) {
        players.add(toAdd);
        port++;
    }

    /**
     * Does a basic move, meaning moving a player to an adjacent place and removing the given ticket corresponding to the street connecting the start and destination place.
     * A ticket has to be given because it is important not to just put any ticket with the right vehicle away, but one with the ability (chosen by the player) too, so the player can strategically collect abilities.
     * If this method returns false, the GameState-data has not been changed.
     *
     * @param toMove The player who is making his move
     * @param destination The place to go to
     * @param toUse The ticket to be used for the journey
     *
     * @return True, if the move was done successfully (player moved, ticket removed).
     *      False,  if the ticket to use is not in the inventory,
     *              if the destination is not directly connected to the current place of the player via a street or
     *              if the street cannot be travelled with the given ticket
     *
     * @author Julien
     *
     */
    public boolean doMove(final Player toMove, final Place destination, Ticket toUse) {
        Vector<Ticket> theInventory;

        // whose inventory to get the Ticket from
        if(toMove.getPort() == 0) {
            theInventory = inventoryX;
        } else {
            theInventory = inventory;
        }

        // needed ticket is actually in the inventory
        if(!theInventory.contains(toUse)) {
            return false;
        }

        // checking that the destination is directly neighbouring the starting place of the player
        if(!map.getAdjacentNodes(map.getIndexByData(toMove.getPlace())).contains(map.getNode(map.getIndexByData(destination)))) {
            return false;
        }

        // do the actual thing
        Vehicle daWay = map.getEdgeData(map.getIndexByData(toMove.getPlace()), map.getIndexByData(destination));
        if(toUse.getVehicle() == daWay) {
            // remove the ticket
            theInventory.remove(toUse);
            // move player
            toMove.setPlace(destination);

            // if Senor X, add move to the timeline[
            if(toMove.getPort() == 0) {
                timeline.addRound(toUse, destination);
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * Reliefs players from the tickets necessary to activate the extra-turn ability
     * The double-turn will be realised via two calls of doTurn() for Mr. X or the doFreeTurn() for the detective(s).
     *
     * @param player The player activating his ability. Will only be used to differentiate between Mr. X and detectives.
     * @param ticketsUsed A vector containing the tickets to be used for the "extra turn"-ability
     *
     * @return True, if the tickets were successfully removed from the inventory.
     *      False,  if the wrong number of tickets were given via the Vector,
     *              if one of the tickets in the given Vector is not in the inventory.
     *
     * @author Julien
     *
     */
    public boolean activateExtraTurn(Player player, Vector<Ticket> ticketsUsed) {
        if (player.getPort() == 0) {
            if(ticketsUsed.size() != 3) {
                return false;
            }
            for(Ticket currTicket: inventoryX) {
                if(!ticketsUsed.contains(currTicket)) {
                    return false;
                }
            }
            for(Ticket toRem: ticketsUsed) {
                inventoryX.remove(toRem);
            }

        } else {
            if(ticketsUsed.size() < 3 || ticketsUsed.size() > 5) {
                return false;
            }
            for(Ticket currTicket: inventory) {
                if(!ticketsUsed.contains(currTicket)) {
                    return false;
                }
            }
            for(Ticket toRem: ticketsUsed) {
                inventory.remove(toRem);
            }
        }
        return true;
    }

    /**
     * Returns the player corresponding to the given port.
     *
     * @param port The port of the player to return (0: Mr. X; 1+: Detectives)
     *
     * @return The Player-Object with the given port.
     *
     * @author Julien
     *
     */
    public Player getPlayerByPort(final int port) {
        for(Player currPlayer: players) {
            if(currPlayer.getPort() == port) {
                return currPlayer;
            }
        }
        return null;
    }

    /**
     * Returns a random element from a given Vector.
     *
     * @param theVec The Vector from which to retrieve a random element.
     *
     * @return A random element of the given Vector.
     *
     * @author Julien
     *
     */
    private <T> T getRandomFromVec(Vector<T> theVec) {
        return theVec.get((int)(Math.random()*theVec.size() - 1));
    }

    // ------ Getters ------
    public Vector<Player> getPlayers() {
        return players;
    }

    public Player getMrX() {
        return mrX;
    }

    public Timeline getTimeline() {
        return timeline;
    }

    public Vector<Ticket> getInventory() {
        return inventory;
    }

    public Vector<Ticket> getInventoryX() {
        return inventoryX;
    }
}
