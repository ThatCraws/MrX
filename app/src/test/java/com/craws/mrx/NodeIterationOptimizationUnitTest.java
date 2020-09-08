package com.craws.mrx;

import android.util.Log;

import com.craws.mrx.state.GameState;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Vehicle;
import com.craws.tree.Node;
import com.craws.tree.Tree;

import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeIterationOptimizationUnitTest {

    GameState state;
    Tree<String, Integer> tree;
    Vector<String> toRet;

    long timer;

    @Before
    public void setUp() {
        state = new GameState(null);
        try {
            state.buildPlace("1");
            state.buildPlace("2");
            state.buildPlace("3");
            state.buildPlace("4");
            state.buildPlace("5");
            state.buildPlace("6");
            state.buildPlace("7");
            state.buildPlace("8");
            state.buildPlace("9");
            state.buildPlace("10");
            state.buildPlace("1");
            state.buildPlace("2");
            state.buildPlace("3");
            state.buildPlace("4");
            state.buildPlace("5");
            state.buildPlace("6");
            state.buildPlace("7");
            state.buildPlace("8");
            state.buildPlace("9");
            state.buildPlace("10");
            state.buildPlace("1");
            state.buildPlace("2");
            state.buildPlace("3");
            state.buildPlace("4");
            state.buildPlace("5");
            state.buildPlace("6");
            state.buildPlace("7");
            state.buildPlace("8");
            state.buildPlace("9");
            state.buildPlace("10");
            state.buildPlace("1");
            state.buildPlace("2");
            state.buildPlace("3");
            state.buildPlace("4");
            state.buildPlace("5");
            state.buildPlace("6");
            state.buildPlace("7");
            state.buildPlace("8");
            state.buildPlace("9");
            state.buildPlace("10");
            state.buildPlace("1");
            state.buildPlace("2");
            state.buildPlace("3");
            state.buildPlace("4");
            state.buildPlace("5");
            state.buildPlace("6");
            state.buildPlace("7");
            state.buildPlace("8");
            state.buildPlace("9");
            state.buildPlace("10");
            state.buildPlace("1");
            state.buildPlace("2");
            state.buildPlace("3");
            state.buildPlace("4");
            state.buildPlace("5");
            state.buildPlace("6");
            state.buildPlace("7");
            state.buildPlace("8");
            state.buildPlace("9");
            state.buildPlace("10");
            state.buildPlace("1");
            state.buildPlace("2");
            state.buildPlace("3");
            state.buildPlace("4");
            state.buildPlace("5");
            state.buildPlace("6");
            state.buildPlace("7");
            state.buildPlace("8");
            state.buildPlace("9");
            state.buildPlace("10");
            state.buildPlace("1");
            state.buildPlace("2");
            state.buildPlace("3");
            state.buildPlace("4");
            state.buildPlace("5");
            state.buildPlace("6");
            state.buildPlace("7");
            state.buildPlace("8");
            state.buildPlace("9");
            state.buildPlace("10");
            state.buildPlace("1");
            state.buildPlace("2");
            state.buildPlace("3");
            state.buildPlace("4");
            state.buildPlace("5");
            state.buildPlace("6");
            state.buildPlace("7");
            state.buildPlace("8");
            state.buildPlace("9");
            state.buildPlace("10");
            state.buildPlace("1");
            state.buildPlace("2");
            state.buildPlace("3");
            state.buildPlace("4");
            state.buildPlace("5");
            state.buildPlace("6");
            state.buildPlace("7");
            state.buildPlace("8");
            state.buildPlace("9");
            state.buildPlace("10");
        } catch (RuntimeException ignored) {} // would have to mock it otherwise, and I don't really need the Resources.getSystem()-call

        tree = new Tree<>();
        tree.insertNode("1");
        tree.insertNode("2");
        tree.insertNode("3");
        tree.insertNode("4");
        tree.insertNode("5");
        tree.insertNode("6");
        tree.insertNode("7");
        tree.insertNode("8");
        tree.insertNode("9");
        tree.insertNode("10");
        tree.insertNode("1");
        tree.insertNode("2");
        tree.insertNode("3");
        tree.insertNode("4");
        tree.insertNode("5");
        tree.insertNode("6");
        tree.insertNode("7");
        tree.insertNode("8");
        tree.insertNode("9");
        tree.insertNode("10");
        tree.insertNode("1");
        tree.insertNode("2");
        tree.insertNode("3");
        tree.insertNode("4");
        tree.insertNode("5");
        tree.insertNode("6");
        tree.insertNode("7");
        tree.insertNode("8");
        tree.insertNode("9");
        tree.insertNode("10");
        tree.insertNode("1");
        tree.insertNode("2");
        tree.insertNode("3");
        tree.insertNode("4");
        tree.insertNode("5");
        tree.insertNode("6");
        tree.insertNode("7");
        tree.insertNode("8");
        tree.insertNode("9");
        tree.insertNode("10");
        tree.insertNode("1");
        tree.insertNode("2");
        tree.insertNode("3");
        tree.insertNode("4");
        tree.insertNode("5");
        tree.insertNode("6");
        tree.insertNode("7");
        tree.insertNode("8");
        tree.insertNode("9");
        tree.insertNode("10");
        tree.insertNode("1");
        tree.insertNode("2");
        tree.insertNode("3");
        tree.insertNode("4");
        tree.insertNode("5");
        tree.insertNode("6");
        tree.insertNode("7");
        tree.insertNode("8");
        tree.insertNode("9");
        tree.insertNode("10");
        tree.insertNode("1");
        tree.insertNode("2");
        tree.insertNode("3");
        tree.insertNode("4");
        tree.insertNode("5");
        tree.insertNode("6");
        tree.insertNode("7");
        tree.insertNode("8");
        tree.insertNode("9");
        tree.insertNode("10");
        tree.insertNode("1");
        tree.insertNode("2");
        tree.insertNode("3");
        tree.insertNode("4");
        tree.insertNode("5");
        tree.insertNode("6");
        tree.insertNode("7");
        tree.insertNode("8");
        tree.insertNode("9");
        tree.insertNode("10");
        tree.insertNode("1");
        tree.insertNode("2");
        tree.insertNode("3");
        tree.insertNode("4");
        tree.insertNode("5");
        tree.insertNode("6");
        tree.insertNode("7");
        tree.insertNode("8");
        tree.insertNode("9");
        tree.insertNode("10");
        tree.insertNode("1");
        tree.insertNode("2");
        tree.insertNode("3");
        tree.insertNode("4");
        tree.insertNode("5");
        tree.insertNode("6");
        tree.insertNode("7");
        tree.insertNode("8");
        tree.insertNode("9");
        tree.insertNode("10");
    }


    @Test
    public void testViaTreeGetNodeDataSpam() {
        timer = System.nanoTime();
        state.getPlaces();
        long diff = System.nanoTime() - timer;

        System.out.println("Time with GameState.getPlaces().");
        System.out.println("Time: " + diff);
    }

    @Test public void testVectorForIterationGetData() {
        Vector<Node<String, Integer>> theNodes = tree.getNodes();
        timer = System.nanoTime();
        toRet = new Vector<>();
        for(int i = 0; i < theNodes.size(); i++) {
            toRet.add(theNodes.get(i).getData());
        }
        long diff = System.nanoTime() - timer;
        System.out.println("Time with Vector-iteration.");
        System.out.println("Time: " + diff);
    }
}
