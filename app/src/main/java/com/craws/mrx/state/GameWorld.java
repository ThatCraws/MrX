package com.craws.mrx.state;

import com.craws.tree.Node;
import com.craws.tree.Tree;

import java.util.Vector;

public class GameWorld {
    // All the Players (represented by Figure)
    private Vector<Player> players;
    private Player mrX;

    // The game map will be represented by a Tree consisting of the Places(Structural: Node, Graphical: City) and Routes(Structural: Edge, Graphical: A Line).
    private Tree<Place, Route> map;

    // A simulated bag of tickets. Tickets will be drawn on random from here.
    private Vector<Ticket> bagOfTickets;

    public GameWorld() {
        players = new Vector<>();
        bagOfTickets = new Vector<>();
        
        map = new Tree<>();
    }

    public GameWorld(final Node<Place, Route> startPosition) {
        players = new Vector<>();
        bagOfTickets = new Vector<>();

        map = new Tree<>(startPosition);
    }

    public GameWorld(final Place startPosition) {
        players = new Vector<>();
        bagOfTickets = new Vector<>();

        map = new Tree<>(startPosition);
    }
    
    public void addPlayer(final int port, final String alias, final Place startPosition) {
        Player newChallenger = new Player(port, alias, startPosition);
        players.add(newChallenger);
    }

    public void addPlayer(final int port, final String alias) {
        Player newChallenger = new Player(port, alias, null);
        players.add(newChallenger);
    }
}
