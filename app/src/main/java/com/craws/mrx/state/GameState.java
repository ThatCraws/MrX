package com.craws.mrx.state;

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

        port = 1;
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

    /**
     * Builds a place in the map. Meaning adding a node to the map that represents the playing field.
     * Connections between Places can be made via buildStreet().
     * @param name The user-displayable name of the Place.
     *
     * @author Julien
     *
     */
    public Place buildPlace(final String name) {
        Place newPlace = new Place(name);
        map.insertNode(newPlace);
        return newPlace;
    }

    /**
     * Builds a street between to places in the map. Meaning adding a connection between two given nodes on the map that represents the playing field.
     * Nodes can be added via buildPlace().
     *
     * @param start The starting point of the street (starting and ending points do not matter and are just internally managed this way)
     * @param destination The ending point of the street (starting and ending points do not matter and are just internally managed this way)
     * @param ticketNeeded The Ticket needed to travel this street
     *
     * @author Julien
     *
     */
    public void buildStreet(final Place start, final Place destination, final Vehicle ticketNeeded) {
        final int startIndex = map.getIndexByData(start);
        final int endIndex = map.getIndexByData(destination);

        if(startIndex == -1 || endIndex == -1 ) {
            return;
        }

        map.insertEdge(startIndex, endIndex, ticketNeeded);
    }

    /**
     * Creates a Mr. X.
     *
     * @param startPosition The place Mr. X starts/spawns on.
     *
     * @see GameState#addDetective(String, Place)
     * @see GameState#addDetective(String)
     *
     * @return The port of Mr. X, whic is always 0 (just for consistence with the addDetective()-method.
     * @see GameState#addDetective(String, Place)
     * @see GameState#addDetective(String)
     * 
     * @author Julien
     *
     */
    public int addMrX(final Place startPosition) {
        mrX = new Player(0, "Mr. X", startPosition);
        return 0;
    }

    public  void addMrX() {
        mrX = new Player(0, "MrX", null);
    }

    /**
     * Creates a detective and adds them to the player-collection.
     *
     * @param alias The user-displayable name of the detective
     * @param startPosition The place the detective starts/spawns on.
     *
     * @see GameState#addMrX(Place)
     * @see GameState#addMrX()
     *
     * @return The port of the newly added Detective
     *
     * @author Julien
     *
     */
    public int addDetective(final String alias, final Place startPosition) {
        Player newChallenger = new Player(port, alias, startPosition);
        return addDetective(newChallenger);
    }

    public int addDetective(final String alias) {
        Player newChallenger = new Player(port, alias, null);
        return addDetective(newChallenger);
    }

    private int addDetective(Player toAdd) {
        players.add(toAdd);
        return port++;
    }

    /**
     * Does a basic move, meaning moving a player to an adjacent place and removing the given ticket corresponding to the street connecting the start and destination place.
     * A ticket has to be given because it is important not to just put any ticket with the right vehicle away, but one with the ability (chosen by the player) too, so the player can strategically collect abilities.
     * If this method returns false, the GameState-data has not been changed.
     *
     * @param port The port of the player who is making his move
     * @param destination The place to go to
     * @param toUse The ticket to be used for the journey
     *
     * @return True, if the move was done successfully (player moved, ticket removed).
     *      False,  if the port is not associated with any player,
     *              if the ticket to use is not in the inventory,
     *              if the destination is not directly connected to the current place of the player via a street or
     *              if the street cannot be travelled with the given ticket
     *
     * @author Julien
     *
     */
    public boolean doMove(final int port, final Place destination, Ticket toUse) {
        Vector<Ticket> theInventory;
        Player toMove = getPlayerByPort(port);

        if(toMove == null) {
            return false;
        }

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
     * Reliefs players from the tickets necessary to activate the given ability.
     * The specific ability's effects will have to be done after calling this method.
     * via two calls of doTurn() for Mr. X or the doFreeTurn() for the detective(s).
     *
     * @param port The port of the player activating his ability. Will only be used to differentiate between Mr. X and detectives.
     * @param ticketsUsed A vector containing the tickets to be used for the "extra turn"-ability
     * @param toActivate The
     *
     * @return True, if the tickets were successfully removed from the inventory.
     *      False,  if the given port is not associated with any player,
     *              if the wrong number of tickets were given via the Vector (if there are more than 3 Tickets for an ability of Mr. X nothing will be done and false returned),
     *              if one of the tickets in the given Vector is not in the inventory,
     *              if one of the tickets is not for the "extra turn"-ability.
     *
     * @see com.craws.mrx.state.GameState#doFreeTurn(int, Place)
     * @author Julien
     *
     */
    public boolean activateAbility(final int port, final Vector<Ticket> ticketsUsed, Ability toActivate) {
        Player player = getPlayerByPort(port);

        if(player == null) {
            return false;
        }

        if (player.getPort() == 0) {
            if(ticketsUsed.size() != 3) {
                return false;
            }
            for(Ticket currTicket: ticketsUsed) {
                if(!inventoryX.contains(currTicket) || currTicket.getAbility() != toActivate) {
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
            for(Ticket currTicket: ticketsUsed) {
                if(currTicket.getAbility() != toActivate || !inventory.contains(currTicket)) {
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
     * Moves the player to an adjacent Place without the need to use a Ticket.
     * @param port The port of the player to be moved for free.
     * @param destination The destination place to let the player go to.
     */
    public void doFreeTurn(final int port, final Place destination) {
        Player player = getPlayerByPort(port);
        if(player == null) {
            return;
        }

        if(map.isConnected(map.getIndexByData(player.getPlace()), map.getIndexByData(destination))) {
            player.setPlace(destination);
        }
    }

    /**
     * Gives a free ticket to a given player
     * @param port The port of the Player to receive the ticket. This is only for differentiating between Mr. X and detectives.
     * @param toGive The Ticket to give to the specified Player.
     */
    public void giveTicket(final int port, final Ticket toGive) {
        if(port == 0) {
            inventoryX.add(toGive);
        } else {
            inventory.add(toGive);
        }
    }

    /**
     * Removes a ticket from a given player
     * @param port The port of the Player to take the ticket from. This is only for differentiating between Mr. X and detectives.
     * @param toTake The Ticket to take from the specified Player.
     *
     * @return True, if a Ticket was successfully removed from an inventory.
     *          False, if the Ticket was not present in the Inventory to begin with.
     */
    public boolean takeTicket(final int port, final Ticket toTake) {
        if(port == 0) {
            return inventoryX.remove(toTake);
        } else {
            return inventory.remove(toTake);
        }
    }

    public Vehicle getStreet(final Place start, final Place destination) {
        final int startIndex = map.getIndexByData(start);
        final int endIndex = map.getIndexByData(destination);

        if(startIndex == -1 || endIndex == -1 ) {
            throw new IllegalArgumentException("One of the given Places is not present in the map.");
        }
        return map.getEdgeData(startIndex, endIndex);
    }

    /**
     * Returns the player corresponding to the given port.
     *
     * @param port The port of the player to return (0: Mr. X; 1+: Detectives).
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
