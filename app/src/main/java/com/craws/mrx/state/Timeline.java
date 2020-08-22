package com.craws.mrx.state;

import androidx.annotation.Nullable;

import java.util.Vector;

public class Timeline {
    private Vector<Ticket> k;
    private Vector<Place> v;

    public Timeline() {
        k = new Vector<>();
        v = new Vector<>();
    }

    public int size() {
        if(k.size() == v.size()) {
            return k.size();
        } else {
            return -1;
        }
    }

    public boolean isEmpty() {
        return k.isEmpty() && v.isEmpty();
    }

    public boolean containsPair(Ticket key, Place value) {
        return k.contains(key) && v.contains(value);
    }

    public void addRound(Ticket key, Place value) {
        k.add(key);
        v.add(value);
    }

    public Ticket getTicketForRound(final int round) {
        if(round > v.size()) {
            throw new IndexOutOfBoundsException("Asked timeline for a (ticket from a) round that has not been played yet.");
        }
        return k.get(round - 1);
    }

    public Place getPlaceForRound(final int round) {
        if(round > v.size()) {
            throw new IndexOutOfBoundsException("Asked timeline for a (place from a) round that has not been played yet.");
        }
        return v.get(round - 1);
    }


}
