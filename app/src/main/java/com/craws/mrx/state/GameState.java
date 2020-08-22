package com.craws.mrx.state;

import android.content.Context;

import com.craws.tree.Node;
import com.craws.tree.Tree;

import java.util.Vector;

public class GameState {
    /** ------ The Detectives and Mr. X ------ */
    private Vector<Player> players;
    private int port;

    private Player mrX;

    // Will track Mr. X's every move. TODO: Is Vector the best choice? Mayyyyyyyyybe Stack... or something entirely different
    private Timeline timeline;

    /** ------ The playing field ------ */
    // The game map will be represented by a Tree consisting of the Places(Structural: Node, Graphical: City) and Routes(Structural: Edge, Graphical: A Line).
    private Tree<Place, Vehicle> map;

    /** ------ The tickets (Vehicle/Ability) ------ */
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

        mrX = new Player(port, "Mr. X", null);
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

    public void buildPlace(final String name, final Context context, final int x, final int y) {
        map.insertNode(new Place(name));
    }

    public void buildStreet(final Place start, final Place end, final Vehicle ticketNeeded) {
        final int startIndex = map.getIndexByData(start);
        final int endIndex = map.getIndexByData(end);

        if(startIndex == -1 || endIndex == -1 ) {
            return;
        }

        map.insertEdge(startIndex, endIndex, ticketNeeded);
    }
    
    public void addPlayer(final String alias, final Place startPosition) {
        Player newChallenger = new Player(port, alias, startPosition);
        addPlayer(newChallenger);
    }

    public void addPlayer(final String alias) {
        Player newChallenger = new Player(port, alias);
        addPlayer(newChallenger);
    }

    private void addPlayer(Player toAdd) {
        players.add(toAdd);
        port++;
    }

    public boolean doMove(final Player toMove, final Place destination, Ticket toUse) {
        Vector<Ticket> theInventory;
        if(toMove.getPort() == 0) {
            theInventory = inventoryX;
        } else {
            theInventory = inventory;
        }

        if(!theInventory.contains(toUse)) {
            return false;
        }

        Vehicle daWay = map.getEdgeData(map.getIndexByData(toMove.getPlace()), map.getIndexByData(destination));
        if(toUse.getVehicle() == daWay) {
            // remove the ticket
            theInventory.remove(toUse);
            // move player
            toMove.setPlace(destination);

            if(toMove.getPort() == 0) {
                timeline.addRound(toUse, destination);
            }

            return true;
        } else {
            return false;
        }
    }

    public Player getPlayerByPort(final int port) {
        for(Player currPlayer: players) {
            if(currPlayer.getPort() == port) {
                return currPlayer;
            }
        }
        return null;
    }
}
